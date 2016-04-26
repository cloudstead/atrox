package histori.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.api.core.HttpRequestContext;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;
import edu.emory.mathcs.backport.java.util.Arrays;
import histori.dao.NexusDAO;
import histori.model.Account;
import histori.model.Nexus;
import histori.model.support.BulkCreateNexusRequest;
import histori.model.support.NexusRequest;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.cobbzilla.util.http.HttpUtil;
import org.cobbzilla.util.io.FileUtil;
import org.cobbzilla.wizard.validation.ValidationResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static histori.ApiConstants.*;
import static histori.resources.NexusResource.createNexus;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.io.Decompressors.isDecompressible;
import static org.cobbzilla.util.io.Decompressors.unroll;
import static org.cobbzilla.util.io.FileUtil.listFilesRecursively;
import static org.cobbzilla.util.json.JsonUtil.JSON_FILES;
import static org.cobbzilla.util.json.JsonUtil.fromJson;
import static org.cobbzilla.util.json.JsonUtil.fromJsonOrDie;
import static org.cobbzilla.wizard.resources.ResourceUtil.*;

/**
 * POST   /file    -- multi-part upload of a zipfile or tar.gz/tar.bz2 archive
 * POST   /load    -- use a BulkCreateNexusRequest object, loads file via URL
 */
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path(BULK_ENDPOINT)
@Service @Slf4j
public class BulkNexusResource {

    public static final String BULK_TAG_PREFIX = "histori-tag-";

    @Autowired private NexusDAO nexusDAO;

    @POST
    @Consumes(MULTIPART_FORM_DATA)
    @Produces(APPLICATION_JSON)
    @Path(EP_FILE)
    public Response bulkCreateNexuses(@Context HttpContext ctx,
                                      @FormDataParam("file") InputStream fileStream,
                                      @FormDataParam("file") FormDataContentDisposition fileDetail) {
        final Account account = userPrincipal(ctx);

        final String fileName = fileDetail.getFileName();
        final Map<String, String> extraTags = getExtraTags(ctx.getRequest());
        if (!isDecompressible(fileName)) return invalid("err.nexus.bulkLoad.invalidExtension");

        final String ext = FileUtil.extension(fileName);
        final ValidationResult result = new ValidationResult();
        try {
            @Cleanup("delete") final File temp = File.createTempFile("bulk-nexus", ext);
            temp.deleteOnExit();
            FileUtil.toFile(temp, fileStream);
            bulkLoad(account, temp, extraTags, result);

        } catch (Exception e) {
            log.error("bulkCreateNexuses: "+e, e);
            return serverError();
        }

        return result.isEmpty() ? ok() : invalid(result);
    }

    @POST
    @Path(EP_LOAD)
    public Response bulkCreateNexuses(@Context HttpContext ctx,
                                      @Valid BulkCreateNexusRequest request) {

        final Account account = userPrincipal(ctx);

        final String url = request.getUrl();
        if (!isDecompressible(url)) return invalid("err.nexus.bulkLoad.invalidExtension");

        final String ext = FileUtil.extension(url);
        final ValidationResult result = new ValidationResult();
        try {
            @Cleanup("delete") final File temp = File.createTempFile("bulk-nexus", ext);
            temp.deleteOnExit();

            @Cleanup final InputStream in = HttpUtil.get(url);
            FileUtil.toFile(temp, in);
            bulkLoad(account, temp, request.getExtraTags(), result);

        } catch (Exception e) {
            log.error("bulkCreateNexuses: "+e, e);
            return serverError();
        }

        return result.isEmpty() ? ok() : invalid(result);
    }

    public Map<String, String> getExtraTags(HttpRequestContext request) {
        final Map<String, String> extraTags = new HashMap<>();
        if (!empty(request.getRequestHeaders())) {
            for (Map.Entry<String, List<String>> entry : request.getRequestHeaders().entrySet()) {
                if (entry.getKey().startsWith(BULK_TAG_PREFIX)) {
                    for (String value : entry.getValue()){
                        extraTags.put(entry.getKey().substring(BULK_TAG_PREFIX.length()), value);
                    }
                }
            }
        }
        return extraTags;
    }

    public void bulkLoad(Account account, File temp, Map<String, String> extraTags, ValidationResult result) throws Exception {
        File dir = null;
        try {
            dir = unroll(temp);
            for (File f : listFilesRecursively(dir, JSON_FILES)) {
                try {
                    final JsonNode rawRequest = fromJsonOrDie(f, JsonNode.class);
                    final List<NexusRequest> requests = new ArrayList<>();
                    if (rawRequest.isArray()) {
                        final NexusRequest[] found = fromJson(rawRequest, "", NexusRequest[].class);
                        requests.addAll(Arrays.asList(found));
                    } else {
                        requests.add(fromJson(rawRequest, "", NexusRequest.class));
                    }
                    for (NexusRequest request : requests) {
                        if (extraTags != null) {
                            for (Map.Entry<String, String> tag : extraTags.entrySet()) {
                                request.getTags().addTag(tag.getKey(), tag.getValue());
                            }
                        }
                        final Nexus nexus = createNexus(account, request, nexusDAO);
                        if (nexus == null) {
                            // should never happen
                            result.addViolation("err.nexus.bulkLoad.loadError", "Error creating nexus: " + f.getName(), f.getName());
                        }
                    }

                } catch (Exception e) {
                    result.addViolation("err.nexus.bulkLoad.loadError", "Error loading nexus: " + f.getName() + ": " + e, f.getName());
                }
            }
        } finally {
            FileUtils.deleteQuietly(dir);
        }
    }
}
