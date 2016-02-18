package histori.resources;

import com.sun.jersey.api.core.HttpContext;
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

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

import static histori.model.CanonicalEntity.canonicalize;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.reflect.ReflectionUtil.copy;
import static org.cobbzilla.wizard.resources.ResourceUtil.*;

/**
 * GET    /{nameOrUuid}/tags        -- find tags for a nexus
 * GET    /{nameOrUuid}/tags/{name} -- find tags with a name
 * PUT    /{nameOrUuid}/tags/{name} -- create tag by name
 * POST   /{nameOrUuid}/tags/{name} -- update tag by name
 * DELETE /{nameOrUuid}/tags/{name} -- delete tag by name
 */
@SuppressWarnings("SpringJavaAutowiredMembersInspection")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Slf4j
public class NexusTagsResource {

    private static final String[] CREATE_FIELDS = {"schemaValues", "commentary"};
    private static final String[] UPDATE_FIELDS = CREATE_FIELDS;

    @Autowired private NexusTagDAO nexusTagDAO;
    @Autowired private TagDAO tagDAO;
    @Autowired private TagTypeDAO tagTypeDAO;

    private Nexus nexus;

    public NexusTagsResource(Nexus nexus) { this.nexus = nexus; }

    /**
     * Find tags for a Nexus
     * @param ctx session token
     * @param visibility if 'everyone', return all publicly visible tags
     *                   if 'owner', return only tags created by the caller
     *                   if 'hidden', return only hidden tags created by the caller
     * @return a list of tags, depending on visibility
     */
    @GET
    public Response findTags(@Context HttpContext ctx,
                             @QueryParam("visibility") String visibility) {

        final Account account = optionalUserPrincipal(ctx);
        if (nexus == null) return notFound();

        final EntityVisibility vis = EntityVisibility.create(visibility, EntityVisibility.everyone);
        return ok(nexusTagDAO.findByNexus(account, nexus.getUuid(), vis));
    }

    /**
     * Find tags of a given name for a Nexus
     * @param ctx session token
     * @param tagName name of the tag
     * @param visibility if 'everyone', return all publicly visible tags
     *                   if 'owner', return only tags created by the caller
     *                   if 'hidden', return only hidden tags created by the caller
     * @return a list of tags
     */
    @GET
    @Path("/{tagName}")
    public Response findTag(@Context HttpContext ctx,
                            @PathParam("tagName") String tagName,
                            @QueryParam("visibility") String visibility) {

        final Account account = optionalUserPrincipal(ctx);
        if (nexus == null) return notFound();

        final EntityVisibility vis = EntityVisibility.create(visibility, EntityVisibility.everyone);
        return ok(nexusTagDAO.findByNexusAndName(account, nexus.getUuid(), tagName, vis));
    }

    @PUT
    @Path("/{tagName}")
    public Response createTag(@Context HttpContext ctx,
                              @PathParam("tagName") String tagName,
                              @Valid NexusTag nexusTag) {

        final Account account = userPrincipal(ctx);
        if (nexus == null) return notFound();

        final String canonical = canonicalize(tagName);
        if (!canonicalize(nexusTag.getTagName()).equals(canonical)) return invalid("err.tagName.mismatch");

        final NexusTag found = nexusTagDAO.findByNexusAndOwnerAndName(nexus.getUuid(), account, tagName);
        if (found != null) return invalid("err.tagExists");

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

        final NexusTag newTag = new NexusTag();
        copy(newTag, nexusTag, CREATE_FIELDS);
        newTag
                .setNexus(nexus.getUuid())
                .setTagName(canonical)
                .setTagType(tagType == null ? null : tagType.getCanonicalName())
                .setOwner(account.getUuid());

        return ok(nexusTagDAO.create(newTag));
    }

    @POST
    @Path("/{tagName}")
    public Response updateTag(@Context HttpContext ctx,
                              @PathParam("tagName") String tagName,
                              @Valid NexusTag nexusTag) {

        final Account account = userPrincipal(ctx);
        if (nexus == null) return notFound();

        if (!nexusTag.getTagName().equals(tagName)) return invalid("err.tagName.mismatch");

        final NexusTag found = nexusTagDAO.findByNexusAndOwnerAndName(nexus.getUuid(), account, tagName);
        if (found == null) return notFound(tagName);

        copy(found, nexusTag, UPDATE_FIELDS);

        return ok(nexusTagDAO.update(found));
    }

    private ValidationResult validateTagSchemaValues(TagSchema schema, TagSchemaValue[] schemaValues) {

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
                if (field.isRequired()) result.addViolation("err." + fieldName + ".required");
                continue;
            }
            Tag found;
            switch (field.getFieldType()) {
                case integer:
                    if (!ValidationRegexes.INTEGER_PATTERN.matcher(value).matches()) {
                        result.addViolation("err." + fieldName + ".notAnInteger");
                    }
                    break;

                case decimal:
                    if (!ValidationRegexes.DECIMAL_PATTERN.matcher(value).matches()) {
                        result.addViolation("err." + fieldName + ".notAnInteger");
                    }
                    break;

                case world_actor:
                case world_event:
                case event_type:
                case idea:
                case result:
                case tag:
                    found = tagDAO.findByCanonicalName(value);
                    if (found == null) {
                        // create a new tag...
                        Tag newTag = new Tag(value);
                        newTag.setTagType(field.getFieldType().name());
                        newTag = tagDAO.create(newTag);

                    } else if (!found.getTagType().equals(field.getFieldType().name())) {
                        result.addViolation("err." + fieldName + ".wrongType");
                    }
                    break;

                case citation:
                    if (!ValidationRegexes.URL_PATTERN.matcher(value).matches()) {
                        result.addViolation("err." + fieldName + ".notURL");
                    } else {
                        found = tagDAO.findByCanonicalName(value);
                        if (found == null) {
                            Tag newTag = new Tag(value);
                            newTag.setTagType(field.getFieldType().name());
                            newTag = tagDAO.create(newTag);

                        } else if (!found.getTagType().equals(TagSchemaFieldType.citation.name())) {
                            result.addViolation("err." + fieldName + ".wrongType");
                        }
                    }
                    break;

                case relationship_type:
                    if (!RelationshipType.isValid(value)) {
                        result.addViolation("err." + fieldName + ".invalidRelationshipType");
                    }
                    break;

                case role_type:
                    if (!RoleType.isValid(value)) {
                        result.addViolation("err." + fieldName + ".invalidRoleType");
                    }
                    break;

                default:
                    result.addViolation("err." + fieldName + ".unknownFieldType");
            }
        }

        return result;
    }

}
