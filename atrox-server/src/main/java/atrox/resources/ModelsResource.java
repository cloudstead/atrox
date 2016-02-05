package atrox.resources;

import atrox.dao.AccountOwnedEntityDAO;
import atrox.dao.CanonicallyNamedEntityDAO;
import atrox.dao.tags.TagDAO;
import atrox.model.*;
import atrox.model.support.TimePoint;
import atrox.model.tags.*;
import atrox.server.AtroxConfiguration;
import com.sun.jersey.api.core.HttpContext;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.dao.DAO;
import org.cobbzilla.wizard.dao.SearchResults;
import org.cobbzilla.wizard.model.ResultPage;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static atrox.ApiConstants.*;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.cobbzilla.util.json.JsonUtil.fromJsonOrDie;
import static org.cobbzilla.util.reflect.ReflectionUtil.instantiate;
import static org.cobbzilla.wizard.resources.ResourceUtil.*;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path(NAMED_ENTITIES_ENDPOINT)
@NoArgsConstructor @Slf4j
public class ModelsResource<E extends AccountOwnedEntity> {

    private static final Class[] NAMED_ENTITIES = {
            Citation.class, EffectType.class, EventType.class,
            Ideology.class, WorldActor.class, WorldEvent.class
    };

    private static final Class[] TAG_ENTITIES = {
            CitationTag.class, EventActorTag.class, EventEffectTag.class, EventTypeTag.class,
            IdeologyTag.class, WorldActorTag.class, WorldEventTag.class
    };

    private static final Map<String, Class<AccountOwnedEntity>> entityClassMap = new HashMap<>();
    static { for (Class c : NAMED_ENTITIES) { entityClassMap.put(c.getSimpleName(), c); } }

    public static ModelsResource getResource(String entityType, AtroxConfiguration configuration) {
        return new ModelsResource(instantiate(entityClassMap.get(entityType)));
    }

    private E entityProto;

    public Map<String, String> getBounds(TimePoint start, TimePoint end) { return entityProto.getBounds(start, end); }
    public String getSortField() { return entityProto.getSortField(); }
    public ResultPage.SortOrder getSortOrder() { return entityProto.getSortOrder(); }

    private boolean isCanonical;

    @SuppressWarnings("SpringJavaAutowiredMembersInspection")
    @Autowired private AtroxConfiguration configuration;

    @Getter(lazy=true) private final AccountOwnedEntityDAO<E> dao = initDao();

    private AccountOwnedEntityDAO<E> initDao() {
        DAO foundDao = configuration.getDaoForEntityClass(entityProto.getClass());
        return (AccountOwnedEntityDAO<E>) foundDao;
    }

    private TagDAO getDaoForTag(Class entityClass) {
        return (TagDAO) configuration.getDaoForEntityClass(entityClass);
    }

    private CanonicallyNamedEntityDAO canonicalDao() {
        return (CanonicallyNamedEntityDAO) getDao();
    }

    public ModelsResource(E entityProto) {
        this.entityProto = entityProto;
        this.isCanonical = (entityProto instanceof CanonicallyNamedEntity);
    }

    @GET
    @Path(EP_BY_NAME + "/{name}")
    public Response findByNameStartsWith(@Context HttpContext ctx,
                                         @PathParam("name") String name) {
        final Account account = optionalUserPrincipal(ctx);
        if (isCanonical) {
            return ok(canonicalDao().findByCanonicalNameStartsWith(account, name));
        } else {
            return invalid();
        }
    }

    @POST
    @Path(EP_EDIT)
    @Consumes(APPLICATION_JSON)
    public Response edit(@Context HttpContext ctx, String json) {

        final Account account = userPrincipal(ctx);
        final E entity = (E) fromJsonOrDie(json, entityProto.getClass());

        final AccountOwnedEntityDAO<E> dao = getDao();

        if (entity != null && entity.hasUuid()) {
            final E found = dao.findByUuid(entity.getUuid());
            if (found != null) {
                if (account.isAdmin() || found.getOwner().equals(account.getUuid())) {
                    entity.setOwner(account.getUuid());
                    return ok(dao.update(entity));
                } else {
                    return forbidden();
                }
            }
        }
        return ok(dao.create(entity));
    }

    @GET
    @Path(EP_BY_DATE + "/{start}")
    public Response findByName(@Context HttpContext ctx,
                               @PathParam("start") String startDate) {
        return findByName(ctx, startDate, "-");
    }

    @GET
    @Path(EP_BY_DATE + "/{start}/{end}")
    public Response findByName(@Context HttpContext ctx,
                               @PathParam("start") String startDate,
                               @PathParam("end") String endDate) {

        final Account account = optionalUserPrincipal(ctx);

        final TimePoint start = startDate.equals("-") ? TimePoint.MIN_VALUE : new TimePoint(startDate);
        final TimePoint end = endDate.equals("-") ? new TimePoint(System.currentTimeMillis()) : new TimePoint(endDate);

        final AccountOwnedEntityDAO<E> dao = getDao();

        final Map<String, String> bounds = getBounds(start, end);

        final ResultPage page = new ResultPage(1, 100, getSortField(), getSortOrder(), null, bounds);
        final SearchResults<E> searchResults = dao.search(page);
        if (isCanonical) {
            final List<E> updatedResults = new ArrayList<>(searchResults.count());
            for (E result : searchResults.getResults()) {
                updatedResults.add(attachTags(account, result));
            }
            searchResults.setResults(updatedResults);
        }
        return ok(searchResults);
    }

    private E attachTags(Account account, E entity) {
        for (Class<EntityTag> tagClass : TAG_ENTITIES) {
            ((CanonicallyNamedEntity) entity).addTags(getDaoForTag(tagClass).findTopTags(account));
        }
        return entity;
    }

}