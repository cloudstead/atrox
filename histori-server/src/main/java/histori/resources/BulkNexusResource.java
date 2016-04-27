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
import histori.model.support.BulkLoadResult;
import histori.model.support.NexusRequest;
import lombok.AllArgsConstructor;
import lombok.Cleanup;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.cobbzilla.util.http.HttpUtil;
import org.cobbzilla.util.io.FileUtil;
import org.cobbzilla.wizard.cache.redis.RedisService;
import org.cobbzilla.wizard.validation.ValidationResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static histori.ApiConstants.*;
import static histori.resources.NexusResource.createNexus;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.io.Decompressors.isDecompressible;
import static org.cobbzilla.util.io.Decompressors.unroll;
import static org.cobbzilla.util.io.FileUtil.abs;
import static org.cobbzilla.util.io.FileUtil.listFilesRecursively;
import static org.cobbzilla.util.json.JsonUtil.JSON_FILES;
import static org.cobbzilla.util.json.JsonUtil.fromJson;
import static org.cobbzilla.util.json.JsonUtil.fromJsonOrDie;
import static org.cobbzilla.wizard.resources.ResourceUtil.*;

/**
 * POST   /file    -- multi-part upload of a zipfile or tar.gz/tar.bz2 archive
 * POST   /load    -- use a BulkCreateNexusRequest object, loads file via URL
 * GET    /        -- get status of current bulk load
 * POST   /cancel  -- cancel current bulk load job
 */
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path(BULK_ENDPOINT)
@Service @Slf4j
public class BulkNexusResource {

    public static final String BULK_TAG_PREFIX = "histori-tag-";

    private ExecutorService executor = Executors.newFixedThreadPool(10);

    @Autowired private NexusDAO nexusDAO;

    @Autowired private RedisService redisService;
    @Getter(lazy=true) private final RedisService redis = initRedis();
    private RedisService initRedis() { return redisService.prefixNamespace(getClass().getSimpleName()); }

    @GET
    public Response getBulkJobStatus (@Context HttpContext ctx) {
        final Account account = userPrincipal(ctx);
        final BulkLoadResult result = getRedis().getObject(account.getUuid(), BulkLoadResult.class);
        final boolean cancelled = cancelled(account.getUuid());
        result.setCancelled(cancelled);
        return ok(result);
    }

    @POST
    @Path(EP_CANCEL)
    public Response cancelBulkJob (@Context HttpContext ctx) {
        final Account account = userPrincipal(ctx);
        final boolean cancelled = cancelled(account.getUuid());
        if (cancelled) return invalid("err.nexus.bulkLoad.alreadyCancelled");
        cancel(account.getUuid());
        return ok();
    }

    @POST
    @Consumes(MULTIPART_FORM_DATA)
    @Produces(APPLICATION_JSON)
    @Path(EP_FILE)
    public Response bulkCreateNexuses(@Context HttpContext ctx,
                                      @FormDataParam("file") InputStream fileStream,
                                      @FormDataParam("file") FormDataContentDisposition fileDetail,
                                      @QueryParam("force") boolean force) {
        final Account account = userPrincipal(ctx);

        final String fileName = fileDetail.getFileName();
        final Map<String, String> extraTags = getExtraTags(ctx.getRequest());
        if (!isDecompressible(fileName)) return invalid("err.nexus.bulkLoad.invalidExtension");

        final String ext = FileUtil.extension(fileName);
        try {
            final File temp = File.createTempFile("bulk-nexus", ext);
            temp.deleteOnExit();
            FileUtil.toFile(temp, fileStream);
            bulkLoad(account, temp, extraTags, force);
            return ok();

        } catch (Exception e) {
            log.error("bulkCreateNexuses: "+e, e);
            return serverError();
        }
    }

    @POST
    @Path(EP_LOAD)
    public Response bukCreateNexuses(@Context HttpContext ctx,
                                     @Valid BulkCreateNexusRequest request,
                                     @QueryParam("force") boolean force) {

        final Account account = userPrincipal(ctx);

        final String url = request.getUrl();
        if (!isDecompressible(url)) return invalid("err.nexus.bulkLoad.invalidExtension");

        final String ext = FileUtil.extension(url);
        try {
            @Cleanup("delete") final File temp = File.createTempFile("bulk-nexus", ext);
            temp.deleteOnExit();

            @Cleanup final InputStream in = HttpUtil.get(url);
            FileUtil.toFile(temp, in);
            bulkLoad(account, temp, request.getExtraTags(), force);
            return ok();

        } catch (Exception e) {
            log.error("bulkCreateNexuses: "+e, e);
            return serverError();
        }
    }

    protected boolean cancelled(String accountUuid) {
        return Boolean.valueOf(getRedis().get(cancellationKey(accountUuid)));
    }
    protected void cancel(String accountUuid) {
        getRedis().set(cancellationKey(accountUuid), Boolean.TRUE.toString());
    }
    protected void clearCancelledFlag(String accountUuid) {
        getRedis().del(cancellationKey(accountUuid));
    }

    private String cancellationKey(String accountUuid) { return accountUuid +":cancelled"; }

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

    private static final long MAX_BULK_JOB_AGE = TimeUnit.DAYS.toMillis(1);

    public void bulkLoad(Account account, File temp, Map<String, String> extraTags, boolean force) throws Exception {
        final BulkLoadResult result = getRedis().getObject(account.getUuid(), BulkLoadResult.class);
        if (!force && result != null && !result.isCompleted() && result.getAge() < MAX_BULK_JOB_AGE) {
            throw invalidEx("err.nexus.bulkLoad.alreadyRunning");
        }
        final BulkLoadJob job = new BulkLoadJob(account, temp, extraTags);
        clearCancelledFlag(account.getUuid());
        executor.submit(job);
    }

    @AllArgsConstructor
    private class BulkLoadJob implements Callable<BulkLoadResult> {

        private Account account;
        private File temp;
        private Map<String, String> extraTags;
        private final BulkLoadResult result = new BulkLoadResult();

        public void updateRedis() { getRedis().setObject(account.getUuid(), result); }
        public boolean isCancelled() { return cancelled(account.getUuid()); }

        @Override public BulkLoadResult call() throws Exception {
            final ValidationResult invalids = this.result.getValidation();
            final List<String> successes = this.result.getSuccesses();
            File dir = null;
            try {
                dir = unroll(temp);
                for (File f : listFilesRecursively(dir, JSON_FILES)) {
                    final String fileName = abs(f).substring(abs(dir).length()+1);
                    if (isCancelled()) {
                        result.setCancelled(true);
                        break;
                    }
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
                                log.info("bulkLoad: error creating from: " + abs(f));
                                invalids.addViolation("err.nexus.bulkLoad.loadError", "Error creating nexus: " + f.getName(), fileName);
                            } else {
                                log.info("bulkLoad: imported " + nexus.getName() + " (canonical: " + nexus.getCanonicalName() + ")");
                                successes.add(fileName);
                            }
                        }

                    } catch (Exception e) {
                        log.info("bulkLoad: error importing " + abs(f));
                        invalids.addViolation("err.nexus.bulkLoad.loadError", "Error loading nexus: " + f.getName() + ": " + e + "\n" + ExceptionUtils.getStackTrace(e), fileName);
                    }
                    updateRedis();
                }

            } catch (Exception e) {
                result.setException(e);
                updateRedis();

            } finally {
                result.setCompleted(true);
                updateRedis();
                FileUtils.deleteQuietly(dir);
                FileUtils.deleteQuietly(temp);
            }
            return result;
        }
    }
}
