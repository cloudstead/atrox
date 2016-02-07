package atrox.resources;

import atrox.dao.AccountOwnedEntityDAO;
import atrox.dao.CanonicallyNamedEntityDAO;
import atrox.dao.tags.TagDAO;
import atrox.model.Account;
import atrox.model.AccountOwnedEntity;
import atrox.model.CanonicallyNamedEntity;
import atrox.model.support.TagOrder;
import atrox.model.support.TagSearchType;
import atrox.model.support.TimePoint;
import atrox.model.tags.EntityTag;
import atrox.server.AtroxConfiguration;
import com.sun.jersey.api.core.HttpContext;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.dao.DAO;
import org.cobbzilla.wizard.dao.SearchResults;
import org.cobbzilla.wizard.model.ResultPage;
import org.cobbzilla.wizard.validation.ValidationResult;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static atrox.ApiConstants.*;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.cobbzilla.util.json.JsonUtil.fromJsonOrDie;
import static org.cobbzilla.util.reflect.ReflectionUtil.copy;
import static org.cobbzilla.util.reflect.ReflectionUtil.instantiate;
import static org.cobbzilla.wizard.resources.ResourceUtil.*;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@NoArgsConstructor @Slf4j
public class MapModelsResource<E extends AccountOwnedEntity> {

    private static final Map<String, MapModelsResource> resourceCache = new ConcurrentHashMap<>(20);
    public static final String[] EXCLUDE_FROM_EDITING = new String[]{"owner", "votes"};

    public static MapModelsResource getResource(String entityType, AtroxConfiguration configuration) {
        MapModelsResource resource = resourceCache.get(entityType);
        if (resource == null) {
            final Class<AccountOwnedEntity> clazz = ENTITY_CLASS_MAP.get(entityType);
            resource = new MapModelsResource((AccountOwnedEntity) instantiate(ENTITY_CLASS_MAP.get(clazz.getSimpleName())));
            configuration.autowire(resource);
            resourceCache.put(clazz.getSimpleName(), resource);
        }
        return resource;
    }

    private E entityProto;

    public Map<String, String> getBounds(TimePoint start, TimePoint end) { return entityProto.getBounds(start, end); }
    public String getSortField() { return entityProto.getSortField(); }
    public ResultPage.SortOrder getSortOrder() { return entityProto.getSortOrder(); }

    private boolean isCanonical;
    private boolean isTag;

    @SuppressWarnings("SpringJavaAutowiredMembersInspection")
    @Autowired private AtroxConfiguration configuration;

    @Getter(lazy=true) private final AccountOwnedEntityDAO<E> dao = initDao();
    private AccountOwnedEntityDAO<E> initDao() {
        DAO foundDao = configuration.getDaoForEntityClass(entityProto.getClass());
        return (AccountOwnedEntityDAO<E>) foundDao;
    }

    private TagDAO tagDao(Class entityClass) {
        return (TagDAO) configuration.getDaoForEntityClass(entityClass);
    }

    private CanonicallyNamedEntityDAO canonicalDao() {
        return (CanonicallyNamedEntityDAO) getDao();
    }

    private CanonicallyNamedEntityDAO canonicalDao(String entityType) {
        return (CanonicallyNamedEntityDAO) dao(entityType);
    }

    public DAO dao(String entityType) {
        final Class<?> entityClass = ENTITY_CLASS_MAP.get(entityType);
        return configuration.getDaoForEntityClass(entityClass);
    }

    public MapModelsResource(E entityProto) {
        this.entityProto = entityProto;
        this.isCanonical = (entityProto instanceof CanonicallyNamedEntity);
        this.isTag = (entityProto instanceof EntityTag);
    }

    public E fromJson(String json) { return (E) fromJsonOrDie(json, entityProto.getClass()); }

    public E populate (Account account, E entity, TagSearchType tagSearchType, TagOrder tagOrder) {
        if (entity == null) return null;

        final ValidationResult validationResult = getDao().populateAssociated(account.getUuid(), entity);
        if (!validationResult.isEmpty()) throw invalidEx(validationResult); // sanity check

        return tagSearchType == TagSearchType.none ? entity : getDao().populateTags(account, entity, tagSearchType, tagOrder);
    }

    public ValidationResult populate(Account account, E entity, TagSearchType tagSearchType, TagOrder tagOrder, ValidationResult validationResult) {
        if (entity == null) return null;
        getDao().populateAssociated(account.getUuid(), entity, validationResult);
        if (!validationResult.isEmpty()) throw invalidEx(validationResult); // sanity check
        if (tagSearchType != TagSearchType.none) entity = getDao().populateTags(account, entity, tagSearchType, tagOrder);
        return validationResult;
    }

    @GET
    @Path("/{uuidOrName}")
    public Response findByUuid(@Context HttpContext ctx,
                               @PathParam("uuidOrName") String uuidOrName,
                               @QueryParam("tags") String tags,
                               @QueryParam("tag_order") String tagOrder) {

        final Account account = optionalUserPrincipal(ctx);
        E found = getDao().findByUuid(uuidOrName);
        try {
            found = populate(account, found, TagSearchType.create(tags), TagOrder.create(tagOrder));
        } catch (Exception e) {
            log.warn("findByUuid: error populating (uuid="+uuidOrName+"): "+e, e);
        }
        switch (found.getVisibility()) {
            case everyone: return ok(found);

            case owner: return account == null || !(account.isAdmin() || found.isOwner(account.getUuid()))
                    ? forbidden()
                    : ok(found);

            default: return notFound(uuidOrName);
        }
    }

    @POST
    @Path("/{uuid}")
    @Consumes(APPLICATION_JSON)
    public Response editByUuid(@Context HttpContext ctx,
                               @PathParam("uuid") String uuid,
                               String json) {
        final Account account = userPrincipal(ctx);
        final AccountOwnedEntityDAO<E> dao = getDao();
        final E found = dao.findByUuid(uuid);

        if (found == null) return notFound(uuid);
        if (!found.isOwner(account.getUuid())) return forbidden();

        // "manually" convert JSON to typed object
        final E newEntity = fromJson(json);

        // "manually" run validator
        ValidationResult validationResult = configuration.getValidator().validate(newEntity);

        // allow associated properties to be specified by name or uuid, enforce referential integrity
        validationResult = populate(account, newEntity, TagSearchType.mine, TagOrder.newest, validationResult);

        if (!validationResult.isEmpty()) return invalid(validationResult);

        // uuid in the path takes precedence
        newEntity.setUuid(uuid);
        copy(found, newEntity, null, EXCLUDE_FROM_EDITING);

        return ok(dao.update(newEntity));
    }

    @DELETE
    @Path("/{uuid}")
    public Response deleteByUuid(@Context HttpContext ctx,
                                 @PathParam("uuid") String uuid) {
        final Account account = userPrincipal(ctx);
        final AccountOwnedEntityDAO<E> dao = getDao();
        final E found = dao.findByUuid(uuid);

        if (found == null) return notFound(uuid);
        if (!(account.isAdmin() || found.isOwner(account.getUuid()))) return forbidden();

        dao.delete(uuid);
        return ok();
    }

    @POST
    @Consumes(APPLICATION_JSON)
    public Response addOrEdit(@Context HttpContext ctx, String json) {

        final Account account = userPrincipal(ctx);
        final E entity = fromJson(json);
        if (entity == null) return serverError();

        final AccountOwnedEntityDAO<E> dao = getDao();

        if (entity.hasUuid()) {
            final E found = dao.findByUuid(entity.getUuid());
            if (found != null) {
                if (account.isAdmin() || found.isOwner(account.getUuid())) {
                    copy(found, entity, null, EXCLUDE_FROM_EDITING);
                    return ok(dao.update(found));
                } else {
                    return forbidden();
                }
            } else {
                // has a uuid but does not exist -- wtf?
                return notFound(entity.getUuid());
            }
        } else {
            entity.setOwner(account.getUuid());
            return ok(dao.create(entity));
        }
    }

    @GET
    @Path(EP_AUTOCOMPLETE + "/{field}/{nameFragment}")
    public Response findByNameStartsWith(@Context HttpContext ctx,
                                         @PathParam("field") String field,
                                         @PathParam("nameFragment") String nameFragment) {
        final Account account = optionalUserPrincipal(ctx);

        if (isCanonical && (field.equals("-") || field.equals("name"))) {
            return ok(canonicalDao().findByCanonicalNameStartsWith(nameFragment));

        } else {
            return ok(getDao().findByFieldStartsWith(account, field, nameFragment));
        }
    }

    @GET
    @Path(EP_BY_DATE + "/{start}")
    public Response findByDate(@Context HttpContext ctx,
                               @PathParam("start") String startDate,
                               @QueryParam("tags") String tags,
                               @QueryParam("tag_order") String tagOrder) {
        return findByDate(ctx, startDate, "-", tags, tagOrder);
    }

    @GET
    @Path(EP_BY_DATE + "/{start}/{end}")
    public Response findByDate(@Context HttpContext ctx,
                               @PathParam("start") String startDate,
                               @PathParam("end") String endDate,
                               @QueryParam("tags") String tags,
                               @QueryParam("tag_order") String tagOrder) {

        final Account account = optionalUserPrincipal(ctx);

        final TimePoint start = startDate.equals("-") ? TimePoint.MIN_VALUE : new TimePoint(startDate);
        final TimePoint end = endDate.equals("-") ? new TimePoint(System.currentTimeMillis()) : new TimePoint(endDate);

        final AccountOwnedEntityDAO<E> dao = getDao();

        final Map<String, String> bounds = getBounds(start, end);

        final ResultPage page = new ResultPage(1, 100, getSortField(), getSortOrder(), null, bounds);
        final SearchResults<E> searchResults = dao.search(page);
        if (isCanonical) {
            for (E result : searchResults.getResults()) {
                try {
                    populate(account, result, TagSearchType.create(tags), TagOrder.create(tagOrder));
                } catch (Exception e) {
                    log.warn("findByDate: error populating (uuid="+result.getUuid()+"): "+e, e);
                }
            }
        }
        return ok(searchResults);
    }

}