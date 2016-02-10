package atrox.resources.history;

import atrox.dao.canonical.CanonicalEntityDAO;
import atrox.dao.history.EntityHistoryDAO;
import atrox.model.Account;
import atrox.model.canonical.CanonicalEntity;
import atrox.model.history.EntityHistory;
import atrox.model.support.EntitySearchOrder;
import atrox.model.support.EntitySearchType;
import atrox.model.support.TimePoint;
import atrox.server.AtroxConfiguration;
import com.sun.jersey.api.core.HttpContext;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.map.HashedMap;
import org.cobbzilla.wizard.dao.DAO;
import org.cobbzilla.wizard.dao.SearchResults;
import org.cobbzilla.wizard.model.ResultPage;
import org.cobbzilla.wizard.validation.ValidationResult;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static atrox.ApiConstants.*;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.cobbzilla.util.json.JsonUtil.fromJsonOrDie;
import static org.cobbzilla.util.reflect.ReflectionUtil.copy;
import static org.cobbzilla.util.reflect.ReflectionUtil.instantiate;
import static org.cobbzilla.wizard.resources.ResourceUtil.*;

/**
 * Endpoints for working with canonical entities and their histories:
 *
 * GET  /id/{uuid}                           -- find a single history by uuid
 *
 * GET  /date/{start}                        -- find histories for all canonicals from start to current date
 * GET  /date/{start}/{end}                  -- find histories for all canonicals between start and end
 *
 * POST /canonical/{name}                    -- create or update caller's history entity for this canonical
 * GET  /canonical/{name}                    -- find histories for the canonical
 * GET  /canonical/{name}/date/{start}       -- find histories for the canonical, from the start to current date
 * GET  /canonical/{name}/date/{start}/{end} -- find histories for the canonical, between start and end
 * GET  /canonical/{name}/ideas              -- summarize idea tags across all histories for this canonical
 * GET  /canonical/{name}/ideas/{idea}       -- list all idea tags with a given name across all histories for this canonical
 * GET  /canonical/{name}/citations          -- summarize citation tags across all histories for this canonical
 * GET  /canonical/{name}/citations/{url}    -- list all citation tags with a given url across all histories for this canonical
 *
 * @param <C> The type of CanonicalEntity
 * @param <H> The type of EntityHistory
 */
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@NoArgsConstructor @Slf4j
public class HistoriesResource<C extends CanonicalEntity, H extends EntityHistory> {

    private static final Map<String, HistoriesResource> resourceCache = new ConcurrentHashMap<>(20);
    public static final String[] EXCLUDE_FROM_EDITING = new String[]{"uuid", "owner", "votes"};

    public static HistoriesResource getResource(String canonicalType, AtroxConfiguration configuration) {
        HistoriesResource resource = resourceCache.get(canonicalType);
        if (resource == null) {
            final Class<CanonicalEntity> clazz = CANONICAL_ENTITY_CLASS_MAP.get(canonicalType);
            resource = new HistoriesResource(instantiate(clazz), instantiate(getHistoryClass(clazz)));
            configuration.autowire(resource);
            resourceCache.put(clazz.getSimpleName(), resource);
        }
        return resource;
    }

    private C canonicalProto;
    private H historyProto;

    public Map<String, String> getBounds(TimePoint start, TimePoint end) { return historyProto.getBounds(start, end); }
    public String getSortField() { return historyProto.getSortField(); }
    public ResultPage.SortOrder getSortOrder() { return historyProto.getSortOrder(); }

    @SuppressWarnings("SpringJavaAutowiredMembersInspection")
    @Autowired private AtroxConfiguration configuration;

    @Getter(lazy=true) private final CanonicalEntityDAO<C> canonicalDao = initDao();
    private CanonicalEntityDAO<C> initDao() { return (CanonicalEntityDAO<C>) configuration.getDaoForEntityClass(canonicalProto.getClass()); }

    @Getter(lazy=true) private final EntityHistoryDAO<H> historyDao = initHistoryDao();
    private EntityHistoryDAO<H> initHistoryDao() { return (EntityHistoryDAO<H>) configuration.getDaoForEntityClass(historyProto.getClass()); }

    private CanonicalEntityDAO canonicalDao(String entityType) { return (CanonicalEntityDAO) dao(entityType); }

    public DAO dao(String entityType) {
        final Class<?> entityClass = ENTITY_CLASS_MAP.get(entityType);
        return configuration.getDaoForEntityClass(entityClass);
    }

    public HistoriesResource(C canonicalProto, H historyProto) {
        this.canonicalProto = canonicalProto;
        this.historyProto = historyProto;
    }

    public H historyFromJson(String json) { return (H) fromJsonOrDie(json, historyProto.getClass()); }

    public ValidationResult populateCanonical(H history, ValidationResult validationResult) {
        if (history == null) return null;
        getHistoryDao().populateAssociated(history, validationResult);
        if (validationResult != null && !validationResult.isEmpty()) throw invalidEx(validationResult); // sanity check
        return validationResult;
    }

    @GET
    @Path(EP_BY_ID + "/{uuid}")
    public Response findHistoryByUuid(@Context HttpContext ctx,
                                      @PathParam("uuid") String uuidOrName) {

        final Account account = optionalUserPrincipal(ctx);
        final H history = this.getHistoryDao().findByUuid(uuidOrName);
        if (history == null) return notFound(uuidOrName);

        populateCanonical(history, null);

        switch (history.getVisibility()) {
            case everyone: return ok(history);

            case owner: return account == null || !(account.isAdmin() || history.isOwner(account.getUuid()))
                    ? forbidden()
                    : ok(history);

            default: return notFound(uuidOrName);
        }
    }

    @GET
    @Path(EP_CANONICAL + "/{uuidOrName}")
    @Consumes(APPLICATION_JSON)
    public Response findHistoriesForCanonical(@Context HttpContext ctx,
                                              @PathParam("uuidOrName") String uuidOrName,
                                              @QueryParam("searchType") EntitySearchType searchType,
                                              @QueryParam("searchOrder") EntitySearchOrder searchOrder) {

        if (searchType == null) searchType = EntitySearchType.mine;
        if (searchOrder == null) searchOrder = EntitySearchOrder.newest;

        final Account account = optionalUserPrincipal(ctx);
        final C canonical = getCanonicalDao().findByUuid(uuidOrName);
        if (canonical == null) return notFound(uuidOrName);

        return ok(getHistoryDao().findAllByCanonical(account, canonical, searchType, searchOrder));
    }

    @POST
    @Path(EP_CANONICAL + "/{uuidOrName}")
    @Consumes(APPLICATION_JSON)
    public Response createOrEditHistoryForCanonical(@Context HttpContext ctx,
                                                    @PathParam("uuidOrName") String uuidOrName,
                                                    String json) {

        final Account account = userPrincipal(ctx);

        H history = getHistoryDao().findByUuid(uuidOrName);
        if (history != null) {
            if (!(account.isAdmin() || history.isOwner(account.getUuid()))) return forbidden();
        } else {
            history = getHistoryDao().newEntity();
            history.setOwner(account.getUuid());
        }

        // "manually" convert JSON to typed object
        final H updates = historyFromJson(json);

        // "manually" run validator; lookup canonical references (translate from name->uuid)
        final ValidationResult validationResult = configuration.getValidator().validate(updates);
        populateCanonical(updates, validationResult);
        if (!validationResult.isEmpty()) return invalid(validationResult);

        updateRelationships(history, updates);

        copy(history, updates, null, EXCLUDE_FROM_EDITING);

        return ok(getHistoryDao().update(history));
    }

    protected void updateRelationships(H history, H updates) {
        // todo: update tags: ideas, citations
    }

    @DELETE
    @Path(EP_BY_ID + "/{uuid}")
    public Response deleteByUuid(@Context HttpContext ctx,
                                 @PathParam("uuid") String uuid) {
        final Account account = userPrincipal(ctx);

        // todo: also accept a uuid or name of a canonical entity, delete the owner's history, if there is one
        final H history = getHistoryDao().findByUuid(uuid);

        if (history == null) return notFound(uuid);
        if (!(account.isAdmin() || history.isOwner(account.getUuid()))) return forbidden();

        getHistoryDao().delete(uuid);
        return ok();
    }

    @POST
    @Consumes(APPLICATION_JSON)
    public Response addOrEdit(@Context HttpContext ctx, String json) {

        final Account account = userPrincipal(ctx);
        final H updates = historyFromJson(json);
        if (updates == null) return serverError();

        // "manually" run validator; lookup canonical references (translate from name->uuid)
        final ValidationResult validationResult = configuration.getValidator().validate(updates);
        populateCanonical(updates, validationResult);
        if (!validationResult.isEmpty()) return invalid(validationResult);

        if (updates.hasUuid()) {
            final H history = getHistoryDao().findByUuid(updates.getUuid());
            if (history != null) {
                if (account.isAdmin() || history.isOwner(account.getUuid())) {
                    copy(history, updates, null, EXCLUDE_FROM_EDITING);
                    return ok(getHistoryDao().update(history));
                } else {
                    return forbidden();
                }
            } else {
                // has a uuid but does not exist -- wtf?
                return notFound(updates.getUuid());
            }
        } else {
            updates.setOwner(account.getUuid());
            return ok(getHistoryDao().create(updates));
        }
    }

    @GET
    @Path(EP_AUTOCOMPLETE + "/{nameFragment}")
    public Response findByNameStartsWith(@Context HttpContext ctx,
                                         @PathParam("nameFragment") String nameFragment) {
        return ok(getCanonicalDao().findByCanonicalNameStartsWith(nameFragment));
    }

    @GET
    @Path(EP_AUTOCOMPLETE + "/{field}/{nameFragment}")
    public Response findByNameStartsWith(@Context HttpContext ctx,
                                         @PathParam("field") String field,
                                         @PathParam("nameFragment") String nameFragment) {
        final Account account = optionalUserPrincipal(ctx);

            if (field.equals("-") || field.equals("name")) {
            return ok(getCanonicalDao().findByCanonicalNameStartsWith(nameFragment));

        } else {
            return ok(getHistoryDao().findByFieldStartsWith(account, field, nameFragment));
        }
    }

    @GET
    @Path(EP_BY_DATE + "/{start}")
    public Response findByDate(@Context HttpContext ctx,
                               @PathParam("start") String startDate,
                               @QueryParam("ideas") String tags,
                               @QueryParam("tag_order") String tagOrder) {
        return findByDate(ctx, startDate, "-", tags, tagOrder);
    }

    @GET
    @Path(EP_BY_DATE + "/{start}/{end}")
    public Response findByDate(@Context HttpContext ctx,
                               @PathParam("start") String startDate,
                               @PathParam("end") String endDate,
                               @QueryParam("filter") String filter,
                               @QueryParam("sort") String sort) {

        final Account account = optionalUserPrincipal(ctx);

        final TimePoint start = startDate.equals("-") ? TimePoint.MIN_VALUE : new TimePoint(startDate);
        final TimePoint end = endDate.equals("-") ? new TimePoint(System.currentTimeMillis()) : new TimePoint(endDate);

        final Map<String, String> bounds = getBounds(start, end);
        // todo: add bounds for filter and sort

        final ResultPage page = new ResultPage(1, 100, getSortField(), getSortOrder(), null, bounds);
        final Map<String, C> canonicalCache = new HashedMap();
        final SearchResults<H> historyResults = getHistoryDao().search(page);
        for (H history : historyResults.getResults()) {
            C canonical = canonicalCache.get(history.getCanonicalFieldValue());
            if (canonical == null) {
                try {
                    populateCanonical(history, null);
                    canonical = (C) history.getCanonical();
                } catch (Exception e) {
                    log.warn("findByDate: error populating (uuid="+ history.getUuid()+"): "+e, e);
                }
            }
            canonical.addHistory(history);
        }
        return ok(new SearchResults<>(new ArrayList<Object>(canonicalCache.values()), historyResults.getTotalCount()));
    }

}