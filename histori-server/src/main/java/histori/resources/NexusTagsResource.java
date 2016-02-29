package histori.resources;

import com.sun.jersey.api.core.HttpContext;
import histori.dao.AccountDAO;
import histori.dao.NexusTagDAO;
import histori.dao.TagDAO;
import histori.dao.TagTypeDAO;
import histori.model.*;
import histori.model.support.EntityVisibility;
import histori.model.support.RelationshipType;
import histori.model.support.RoleType;
import histori.model.tag_schema.TagSchema;
import histori.model.tag_schema.TagSchemaField;
import histori.model.tag_schema.TagSchemaFieldType;
import histori.model.tag_schema.TagSchemaValue;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.string.ValidationRegexes;
import org.cobbzilla.wizard.validation.ValidationResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

import static histori.model.CanonicalEntity.canonicalize;
import static histori.model.tag_schema.TagSchemaFieldType.*;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.reflect.ReflectionUtil.copy;
import static org.cobbzilla.util.string.StringUtil.urlDecode;
import static org.cobbzilla.wizard.resources.ResourceUtil.*;

/**
 * GET    /{nameOrUuid}/tags        -- find all tags for a nexus
 * GET    /{nameOrUuid}/tags/{name} -- find all tags with name
 * PUT    /{nameOrUuid}/tags/{name} -- create tag by name
 * POST   /{nameOrUuid}/tags/{uuid} -- update tag by uuid
 * DELETE /{nameOrUuid}/tags/{uuid} -- delete tag by uuid
 */
@SuppressWarnings("SpringJavaAutowiredMembersInspection")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Slf4j
public class NexusTagsResource {

    private static final String[] CREATE_FIELDS = {"schemaValues", "markdown"};
    private static final String[] UPDATE_FIELDS = CREATE_FIELDS;

    public static final String ENCODE_PREFIX = "~";

    @Autowired private AccountDAO accountDAO;
    @Autowired private NexusTagDAO nexusTagDAO;
    @Autowired private TagDAO tagDAO;
    @Autowired private TagTypeDAO tagTypeDAO;

    private Nexus nexus;

    public NexusTagsResource(Nexus nexus) { this.nexus = nexus; }

    class NexusTagContext {
        public Account account;
        public NexusTag nexusTag;
        public Response response;
        public boolean hasResponse () { return response != null; }

        public NexusTagContext (HttpContext ctx) { this(ctx, true); }

        public NexusTagContext(HttpContext ctx, boolean requireOwner) {
            account = (Account) (requireOwner ? userPrincipal(ctx) : optionalUserPrincipal(ctx));
            if (nexus == null) response = notFound();
            if (nexus.getVisibility() != EntityVisibility.everyone) {
                if (account == null || !nexus.getOwner().equals(account.getUuid())) response = forbidden();
            }
        }

        public NexusTagContext(HttpContext ctx, String uuid) {
            this(ctx, true);
            nexusTag = nexusTagDAO.findByUuid(uuid);
            if (nexusTag == null) response = notFound(uuid);
            if (account == null || !nexusTag.getOwner().equals(account.getUuid())) response = forbidden();
        }
    }

    /**
     * Find tags for a Nexus
     * @param context session token
     * @param visibility if 'everyone', return all publicly visible tags
     *                   if 'owner', return only tags created by the caller
     *                   if 'hidden', return only hidden tags created by the caller
     * @return a list of tags, depending on visibility
     */
    @GET
    public Response findTags(@Context HttpContext context,
                             @QueryParam("visibility") String visibility) {
        final NexusTagContext ctx = new NexusTagContext(context);
        if (ctx.hasResponse()) return ctx.response;

        final EntityVisibility vis = EntityVisibility.create(visibility, EntityVisibility.everyone);
        return ok(nexusTagDAO.findByNexus(ctx.account, nexus.getUuid(), vis));
    }

    /**
     * Find tags of a given name for a Nexus
     * @param context session token
     * @param tagName name of the tag
     * @param visibility if 'everyone', return all publicly visible tags
     *                   if 'owner', return only tags created by the caller
     *                   if 'hidden', return only hidden tags created by the caller
     * @return a list of tags
     */
    @GET
    @Path("/{tagName: .+}")
    public Response findTag(@Context HttpContext context,
                            @PathParam("tagName") String tagName,
                            @QueryParam("visibility") String visibility) {

        final NexusTagContext ctx = new NexusTagContext(context);
        if (ctx.hasResponse()) return ctx.response;

        while (tagName.startsWith(ENCODE_PREFIX)) tagName = urlDecode(tagName.substring(1));

        final EntityVisibility vis = EntityVisibility.create(visibility, EntityVisibility.everyone);
        return ok(nexusTagDAO.findByNexusAndName(ctx.account, nexus.getUuid(), tagName, vis));
    }

    @PUT
    @Path("/{tagName: .+}")
    public Response createTag(@Context HttpContext context,
                              @PathParam("tagName") String tagName,
                              @Valid NexusTag nexusTag) {
        final NexusTagContext ctx = new NexusTagContext(context, true);
        if (ctx.hasResponse()) return ctx.response;

        while (tagName.startsWith(ENCODE_PREFIX)) tagName = urlDecode(tagName.substring(1));

        final String canonical = canonicalize(tagName);
        if (!canonicalize(nexusTag.getTagName()).equals(canonical)) return invalid("err.tagName.mismatch");

        TagType tagType = tagTypeDAO.findByCanonicalName(nexusTag.getTagType());
        if (tagType == null) {
            // maybe it already exists?
            final Tag existingTag = tagDAO.findByCanonicalName(canonical);
            if (existingTag != null) {
                final String tagTypeName = existingTag.getTagType();
                tagType = tagTypeDAO.findByCanonicalName(canonicalize(tagTypeName));
            }
        }

        final Tag tag = tagDAO.findOrCreateByCanonical(new Tag(nexusTag.getTagName(), tagType));

        final Response validationResult = validateTagSchema(ctx.account, nexusTag, tagType);
        if (validationResult != null) return validationResult;

        NexusTag newTag = new NexusTag();
        copy(newTag, nexusTag, CREATE_FIELDS);
        newTag.setNexus(nexus.getUuid())
                .setTagName(canonical)
                .setTagType(tagType == null ? null : tagType.getCanonicalName())
                .setOwner(ctx.account.getUuid());

        try {
            newTag = nexusTagDAO.create(newTag);
        } catch (DataIntegrityViolationException e) {
            log.warn("Duplicate tag, not creating: " + newTag);
        }
        return ok(newTag);
    }

    @POST
    @Path("/{uuid: .+}")
    public Response updateTag(@Context HttpContext context,
                              @PathParam("uuid") String tagUuid,
                              @Valid NexusTag nexusTag) {

        final NexusTagContext ctx = new NexusTagContext(context, tagUuid);
        if (ctx.hasResponse()) return ctx.response;

        final TagType tagType = tagTypeDAO.findByCanonicalName(ctx.nexusTag.getTagType());
        final Response validationResult = validateTagSchema(ctx.account, nexusTag, tagType);
        if (validationResult != null) return validationResult;

        copy(ctx.nexusTag, nexusTag, UPDATE_FIELDS);
        ctx.nexusTag.setNexus(nexus.getUuid());

        return ok(nexusTagDAO.update(ctx.nexusTag));
    }

    @DELETE
    @Path("/{uuid: .+}")
    public Response deleteTag(@Context HttpContext context,
                              @PathParam("uuid") String tagUuid) {

        final NexusTagContext ctx = new NexusTagContext(context, tagUuid);
        if (ctx.hasResponse()) return ctx.response;

        nexusTagDAO.delete(ctx.nexusTag.getUuid());
        return ok();
    }

    public Response validateTagSchema(Account account, @Valid NexusTag nexusTag, TagType tagType) {
        if (tagType == null || !tagType.hasSchema()) {
            if (nexusTag.hasSchemaValues()) return invalid("err.tag.noValuesAllowed");

        } else {
            final TagSchema schema;
            try {
                schema = tagType.getSchema();
            } catch (Exception e) {
                log.warn("validateTagSchema: error calling tagType.getSchema: "+e);
                return invalid("err.schemaJson.invalid", tagType.getSchemaJson());
            }
            final ValidationResult validationResult = validateTagSchemaValues(account, schema, nexusTag.getValues());
            if (!validationResult.isEmpty()) return invalid(validationResult);
        }
        return null;
    }

    private ValidationResult validateTagSchemaValues(Account account, TagSchema schema, TagSchemaValue[] schemaValues) {

        final ValidationResult result = new ValidationResult();

        if (empty(schema) && !empty(schemaValues)) {
            result.addViolation("err.tagType.schema.noValuesAllowed");
            return result;
        }
        if (schemaValues == null) schemaValues = new TagSchemaValue[0];

        final Map<String, String> values = new HashMap<>();
        for (TagSchemaValue value : schemaValues) {
            values.put(value.getField(), value.getValue());
        }

        for (TagSchemaField field : schema.getFields()) {
            final String fieldName = field.getName();
            final String value = values.get(fieldName);
            if (empty(value)) {
                if (field.isRequired()) addViolation(result, fieldName, "required");
                continue;
            }
            Tag found;
            switch (field.getFieldType()) {
                case integer:
                    if (!ValidationRegexes.INTEGER_PATTERN.matcher(value).matches()) {
                        addViolation(result, fieldName, "notInteger");
                    }
                    break;

                case decimal:
                    if (!ValidationRegexes.DECIMAL_PATTERN.matcher(value).matches()) {
                        addViolation(result, fieldName, "notDecimal");
                    }
                    break;

                case world_actor:
                case event:
                case event_type:
                case person:
                case idea:
                case result:
                case tag:
                    if (value.length() < 2) {
                        log.warn("Ignoring short value: "+value);
                        continue;
                    }
                    found = tagDAO.findByCanonicalName(value);
                    if (found == null) {
                        // create a new tag...
                        Tag newTag = new Tag(value);
                        newTag.setTagType(field.getFieldType().name());
                        newTag = tagDAO.create(newTag);

                    } else if (!found.getTagType().equals(field.getFieldType().name())) {
                        // allow "persons" and "events" to be upgraded to "world actor"
                        if ((found.getTagType().equals(person.name()) || found.getTagType().equals(event.name()))
                                && field.getFieldType().equals(world_actor)) {
                            found.setTagType(world_actor.name());
                            tagDAO.update(found);
                        } else {
                            addViolation(result, fieldName, "wrongType");
                        }
                    }
                    break;

                case citation:
                    if (!ValidationRegexes.URL_PATTERN.matcher(value).matches()) {
                        addViolation(result, fieldName, "notURL");
                    } else {
                        found = tagDAO.findByCanonicalName(value);
                        if (found == null) {
                            Tag newTag = new Tag(value);
                            newTag.setTagType(field.getFieldType().name());
                            newTag = tagDAO.create(newTag);

                        } else if (!found.getTagType().equals(TagSchemaFieldType.citation.name())) {
                            addViolation(result, fieldName, "wrongType");
                        }
                    }
                    break;

                case relationship_type:
                    if (!RelationshipType.isValid(value)) {
                        addViolation(result, fieldName, "invalidRelationshipType");
                    }
                    break;

                case role_type:
                    if (!RoleType.isValid(value)) {
                        addViolation(result, fieldName, "invalidRoleType");
                    }
                    break;

                default:
                    addViolation(result, fieldName, "unknownFieldType");
            }
        }

        return result;
    }

    private void addViolation(ValidationResult result, String fieldName, String err) {
        result.addViolation("err.field."+err, "field "+fieldName+" had an error", fieldName);
    }

}
