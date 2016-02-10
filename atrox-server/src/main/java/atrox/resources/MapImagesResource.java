package atrox.resources;

import atrox.dao.MapImageDAO;
import atrox.model.Account;
import atrox.model.MapImage;
import atrox.server.AtroxConfiguration;
import cloudos.service.asset.AssetStream;
import com.jhlabs.map.proj.Projection;
import com.jhlabs.map.proj.ProjectionFactory;
import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.util.StreamStreamingOutput;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.List;

import static atrox.ApiConstants.*;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;
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

        final AssetStream asset = configuration.getAssetStorageService().load(uriOrName);
        if (asset == null) return notFound(uriOrName);

        InputStream xformedStream = transform(asset, xformConfig);
        return Response.ok(new StreamStreamingOutput(xformedStream))
                .header(HttpHeaders.CONTENT_TYPE, asset.getContentType())
                .build();
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

    private InputStream transform(AssetStream asset, String xformConfig) {
//        String[] args = parseArgs(xformConfig);
        final String[] args = {};
        Projection projection = ProjectionFactory.fromPROJ4Specification(args);

        return null;
    }
}
