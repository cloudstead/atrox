package atrox.resources;

import atrox.dao.MapImageDAO;
import atrox.model.Account;
import atrox.model.MapImage;
import atrox.server.AtroxConfiguration;
import cloudos.service.asset.AssetStorageService;
import cloudos.service.asset.AssetStream;
import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.graphics.ImageTransformConfig;
import org.cobbzilla.util.io.FileUtil;
import org.cobbzilla.wizard.util.StreamStreamingOutput;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

import static atrox.ApiConstants.*;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;
import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.wizard.resources.ResourceUtil.*;

@Path(MAP_IMAGES_ENDPOINT)
@Service @Slf4j
public class MapImagesResource {

    @Autowired private AtroxConfiguration configuration;
    @Autowired private MapImageDAO mapImageDAO;

    @POST
    @Consumes(MULTIPART_FORM_DATA)
    @Produces(APPLICATION_JSON)
    public Response uploadFile(@Context HttpContext ctx,
                               @FormDataParam("file") InputStream fileStream,
                               @FormDataParam("file") FormDataContentDisposition fileDetail) {

        final Account account = userPrincipal(ctx);

        final String fileName = fileDetail.getFileName();
        if (!isLegalImageFile(fileName)) return invalid("err.image.invalidExtension");

        final String storageUri = configuration.getAssetStorageService().store(fileStream, fileName);

        MapImage image = mapImageDAO.findByOwnerAndUri(account.getUuid(), storageUri);
        if (image == null) {
            image = (MapImage) new MapImage()
                    .setUri(storageUri)
                    .setFileName(fileName)
                    .setOwner(account.getUuid());
            mapImageDAO.create(image);
        }

        image.setUrl(getImageUrl(image));
        return ok(image);
    }

    private boolean isLegalImageFile(String fileName) {
        if (fileName == null) return false;
        fileName = fileName.toLowerCase();
        return fileName.endsWith(".png") || fileName.endsWith(".gif")
                || fileName.endsWith(".jpg") || fileName.endsWith(".jpeg");
    }

    @GET
    @Path(EP_PUBLIC+EP_GET_MAP_IMAGE+"/{uriOrName: .+}")
    public Response findByUuid (@Context HttpContext ctx,
                                @PathParam("uriOrName") String uriOrName) {

        final Account account = optionalUserPrincipal(ctx);
        uriOrName = getUriFromName(uriOrName, account);

        final AssetStream asset = configuration.getAssetStorageService().load(uriOrName);
        if (asset == null) return notFound(uriOrName);

        return Response.ok(new StreamStreamingOutput(asset.getStream()))
                .header(HttpHeaders.CONTENT_TYPE, asset.getContentType())
                .build();
    }

    @GET
    @Path(EP_PUBLIC+EP_TRANSFORM_MAP_IMAGE+"/{xform}/{uriOrName: .+}")
    public Response transformMapImage (@Context HttpContext ctx,
                                       @PathParam("xform") String xformConfig,
                                       @PathParam("uriOrName") String uriOrName) {

        final Account account = optionalUserPrincipal(ctx);
        uriOrName = getUriFromName(uriOrName, account);

        final AssetStorageService storageService = configuration.getAssetStorageService();
        final ImageTransformConfig xform = new ImageTransformConfig(xformConfig);

        final String xformUri = getXformUri(uriOrName, xform);
        AssetStream asset = storageService.load(xformUri);
        if (asset == null) {
            AssetStream origAsset = storageService.load(uriOrName);
            if (origAsset == null) return notFound(uriOrName);
            final File xformFile = transform(origAsset, xform);
            try (InputStream in = new FileInputStream(xformFile)) {
                storageService.store(in, origAsset.getUri(), xformUri);

            } catch (Exception e) {
                return die("transformMapImage: error storing: "+e, e);
            }
            asset = storageService.load(xformUri);
            if (asset == null) die("transformMapImage: error loading after stored");
        }
        return Response.ok(new StreamStreamingOutput(asset.getStream()))
                .header(HttpHeaders.CONTENT_TYPE, asset.getContentType())
                .build();
    }

    private String getXformUri(String uri, ImageTransformConfig xform) {
        int lastDot = uri.lastIndexOf('.');
        if (lastDot == -1 || lastDot == uri.length()-1) die("getXformUri: invalid: "+uri);
        return uri.substring(0, lastDot) + "_" + xform.toString() + uri.substring(lastDot);
    }

    @GET
    @Produces(APPLICATION_JSON)
    public Response findAllForOwner (@Context HttpContext ctx) {
        // todo: add a POST path that accepts a ResultPage, if a user has a large number of images
        final Account account = userPrincipal(ctx);
        final List<MapImage> images = mapImageDAO.findByOwner(account);
        for (MapImage i : images) {
            i.setUrl(getImageUrl(i));
        }
        return ok(images);
    }

    public String getUriFromName(@PathParam("uriOrName") String uriOrName, Account account) {
        if (account != null) {
            final MapImage found = mapImageDAO.findByOwnerAndName(account.getUuid(), uriOrName);
            if (found != null) uriOrName = found.getUri();
        }
        return uriOrName;
    }

    public String getImageUrl(MapImage i) {
        return configuration.getApiUriBase()+MAP_IMAGES_ENDPOINT+EP_PUBLIC+EP_GET_MAP_IMAGE+"/"+i.getUri();
    }

    private File transform(AssetStream asset, ImageTransformConfig xform) {

        final String ext = FileUtil.extension(asset.getUri());
        final String formatName = asset.getFormatName();
        final InputStream assetStream = asset.getStream();

        return transformToFile(xform, ext, formatName, assetStream);
    }

    public static File transformToFile(ImageTransformConfig xform, String ext, String formatName, InputStream assetStream) {
        try (InputStream imageInput = assetStream) {
            BufferedImage originalImage = ImageIO.read(imageInput);
            int type = originalImage.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : originalImage.getType();

            BufferedImage resizedImage = new BufferedImage(xform.getWidth(), xform.getHeight(), type);
            Graphics2D g = resizedImage.createGraphics();
            g.drawImage(originalImage, 0, 0, xform.getWidth(), xform.getHeight(), null);

            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_DITHERING,
                    RenderingHints.VALUE_DITHER_ENABLE);

            g.dispose();

            final File xformOutfile = File.createTempFile("transform", ext);
            ImageIO.write(resizedImage, formatName, xformOutfile);

            return xformOutfile;

        } catch (Exception e) {
            return die("transform: "+e, e);
        }
    }
}
