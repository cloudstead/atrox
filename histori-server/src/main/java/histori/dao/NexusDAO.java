package histori.dao;

import histori.dao.archive.NexusArchiveDAO;
import histori.dao.search.NexusSearchResults;
import histori.dao.shard.NexusShardDAO;
import histori.model.*;
import histori.model.base.NexusTags;
import histori.model.support.EntityVisibility;
import histori.model.support.NexusRequest;
import histori.model.support.TimeRange;
import histori.model.tag_schema.TagSchemaField;
import histori.model.tag_schema.TagSchemaFieldType;
import histori.model.tag_schema.TagSchemaValue;
import histori.wiki.WikiDateFormat;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.cache.redis.RedisService;
import org.cobbzilla.wizard.server.config.DatabaseConfiguration;
import org.cobbzilla.wizard.server.config.ShardSetConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.validation.Valid;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static histori.ApiConstants.NAME_MAXLEN;
import static histori.model.CanonicalEntity.canonicalize;
import static histori.model.TagType.EVENT_TYPE;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.json.JsonUtil.toJsonOrDie;
import static org.cobbzilla.util.reflect.ReflectionUtil.copy;
import static org.cobbzilla.util.string.StringUtil.isNumber;
import static org.cobbzilla.wizard.resources.ResourceUtil.forbiddenEx;
import static org.cobbzilla.wizard.resources.ResourceUtil.invalidEx;

@Repository @Slf4j
public class NexusDAO extends ShardedEntityDAO<Nexus, NexusShardDAO> {

    @Autowired private NexusArchiveDAO nexusArchiveDAO;

    @Autowired private DatabaseConfiguration database;
    @Override public ShardSetConfiguration getShardConfiguration() { return database.getShard("nexus"); }

    @Autowired @Getter @Setter private SuperNexusDAO superNexusDAO;
    @Autowired @Getter @Setter private TagDAO tagDAO;
    @Autowired @Getter @Setter private TagTypeDAO tagTypeDAO;
    @Autowired @Getter @Setter private AccountDAO accountDAO;
    @Autowired @Getter @Setter private RedisService redisService;
    @Autowired @Getter @Setter private BookDAO bookDAO;

    @Getter(lazy=true) private final RedisService nexusCache = initNexusCache();
    private RedisService initNexusCache() { return redisService.prefixNamespace("nexus-cache:", null); }

    @Override public Object preCreate(@Valid Nexus nexus) {
        nexus.prepareForSave();

        // ensure tag is present, or create it if not
        if (nexus.hasNexusType()) {
            nexus.setNexusType(tagDAO.create(new Tag(nexus.getNexusType(), EVENT_TYPE)).getCanonicalName());
        }

        ensureBookExists(nexus);

        // create version
        VersionedEntityDAO.incrementVersionAndArchive(nexus, this, nexusArchiveDAO);

        // if this version is authoritative, unset authoritative flag if set on another version
        if (nexus.isAuthoritative()) {
            final Nexus authoritative = findByName(nexus.getName());
            if (authoritative != null) {
                authoritative.setAuthoritative(false);
                update(authoritative);
            }
        }

        return super.preCreate(nexus);
    }

    @Override public Object preUpdate(@Valid Nexus nexus) {
        nexus.prepareForSave();

        // ensure event_type tag corresponding to nexusType is present, or create it if not
        if (nexus.hasNexusType()) {
            // what tags already exist?
            final NexusTag typeTag = new NexusTag().setTagName(nexus.getNexusType()).setTagType(EVENT_TYPE);
            if (!nexus.getTags().hasExactTag(typeTag)) {
                nexus.getTags().addTag(typeTag);
                return typeTag;
            } else {
                // nexusType already matches one of the event_type tags
            }
        } else {
            nexus.setNexusType(nexus.getTags().getFirstEventType());
        }

        ensureBookExists(nexus);

        // create version
        VersionedEntityDAO.incrementVersionAndArchive(nexus, this, nexusArchiveDAO);

        // if this version is authoritative, unset authoritative flag if set on another version
        if (nexus.isAuthoritative()) {
            final Nexus authoritative = findByName(nexus.getName());
            if (authoritative != null && !authoritative.getUuid().equals(nexus.getUuid())) {
                authoritative.setAuthoritative(false);
                update(authoritative);
            }
        }

        return super.preUpdate(nexus);
    }

    public void ensureBookExists(@Valid Nexus nexus) {
        if (nexus.hasBook()) {
            Book book = bookDAO.findByName(nexus.getBook());
            if (book == null) {
                book = bookDAO.create((Book) new Book()
                        .setName(nexus.getBook())
                        .setShortName(nexus.getBook())
                        .setOwner(nexus.getOwner()));
            }
            nexus.setBook(book.getShortName());
            nexus.setInOwnerBook(book.getOwner().equals(nexus.getOwner()));
        } else {
            nexus.setInOwnerBook(false);
        }
    }

    @Override public Nexus postCreate(Nexus nexus, Object context) {
        postProcessNexus(nexus);
        return super.postCreate(nexus, context);
    }

    @Override public Nexus postUpdate(Nexus nexus, Object context) {
        postProcessNexus(nexus);
        return super.postUpdate(nexus, context);
    }

    public void postProcessNexus(Nexus nexus) {
        getNexusCache().set(nexus.getUuid(), toJsonOrDie(nexus));
        superNexusDAO.updateSuperNexus(nexus);
        tagDAO.updateTags(nexus);
        // todo: when we have multiple API servers, we'll need to broadcast this to all API servers...
        final Account account = accountDAO.findByUuid(nexus.getOwner());
        NexusSearchResults.removeFromCache(nexus, account);
    }

    public Nexus findByOwnerAndName(Account account, String name) {
        return findByUniqueFieldsNoCache("owner", account.getUuid(), "canonicalName", canonicalize(name));
    }

    public List<Nexus> findByOwner(Account account) { return findByField("owner", account.getUuid()); }

    @Override public String getNameField() { return "canonicalName"; }

    @Override public Nexus findByName(String name) {
        return findByUniqueFields("canonicalName", canonicalize(name), "authoritative", true);
    }

    public List<Nexus> findByNameAndVisibleToAccountInSearchResults(String name, Account account) {
        final List<Nexus> found = findByField("canonicalName", canonicalize(name));
        for (Iterator<Nexus> iter = found.iterator(); iter.hasNext(); ) {
            final Nexus nexus = iter.next();
            if (!nexus.isVisibleTo(account) || nexus.getVisibility() == EntityVisibility.hidden) iter.remove();
        }
        return found;
    }

    public List<Nexus> findByBook(String name) { return findByField("book", name); }

    public List<Nexus> findByOwnerAndFeed(Account account, Feed feed) {
        return findByFields("owner", account.getUuid(), "feed", feed.getUuid());
    }

    public static final String[] CREATE_FIELDS = {"name", "nexusType", "geoJson", "timeRange", "markdown", "visibility", "tags", "authoritative"};
    public static final String[] UPDATE_FIELDS = {"geoJson", "nexusType", "timeRange", "markdown", "visibility", "tags", "authoritative", "bounds"};

    public Nexus createOrUpdateNexus(Account account, Nexus request) {
        final String name = request.getName();
        if (request.isAuthoritative() && !account.isAdmin()) throw forbiddenEx();
        if (request.emptyGeo()) throw invalidEx("err.geo.empty");

        scrubTags(request);

        Nexus nexus = findByOwnerAndName(account, name);
        if (nexus != null) {
            if (!updateNexus(request, nexus)) return nexus;
            nexus.setOwnerAccount(account); // for sanity. nothing can change the owner.
            nexus = update(nexus);
        } else {
            nexus = new Nexus();
            copy(nexus, request, CREATE_FIELDS);
            nexus.setOwnerAccount(account);
            nexus = create(nexus);
        }
        return nexus;
    }

    public boolean updateNexus(@Valid Nexus request, Nexus nexus) {
        scrubTags(request);
        final Nexus backup = new Nexus();
        copy(backup, nexus);
        copy(nexus, request, UPDATE_FIELDS);
        if (backup.equals(nexus)) {
            log.info("no changes made, not saving: "+request.getName());
            return false;
        }
        return true;
    }

    public void scrubTags(Nexus nexus) {
        // remove tags with null names, or names that are too long
        // ensure other tags exist
        final NexusTags tags = nexus.getTags();
        for (Iterator<NexusTag> iter = tags.iterator(); iter.hasNext(); ) {
            NexusTag tag = iter.next();
            if (empty(tag.getCanonicalName()) || tag.getCanonicalName().length() > NAME_MAXLEN) {
                log.warn("scrubTags: removing invalid tag: "+tag);
                iter.remove();
                continue;
            }
            // only allow schema values with designated types, perform type conversion on date fields
            final TagType tagType = tagTypeDAO.findByCanonicalName(tag.getCanonicalType());
            if (tagType == null) {
                log.warn("scrubTags: remove invalid tag (no such tag type): "+tag);
                iter.remove();
                continue;
            }
            final Map<String, TagSchemaField> fields = tagType.getSchema().getFieldMap();
            if (tag.hasSchemaValues() && !tag.getCanonicalType().equals("meta")) {
                final TagSchemaValue[] values = tag.getValues();
                int newSize = values.length;
                for (int i = 0; i< values.length; i++) {
                    final TagSchemaValue value = values[i];
                    final TagSchemaField tagSchemaField = fields.get(value.getCanonicalField());
                    if (tagSchemaField == null) {
                        values[i] = null;
                        newSize--;
                        continue;
                    }
                    final String schemaVal = value.getValue();
                    if (!isNumber(schemaVal) && tagSchemaField.getFieldType() == TagSchemaFieldType.date) {
                        // parse the date
                        final TimeRange parsed = WikiDateFormat.parse(schemaVal);
                        if (parsed == null) throw invalidEx("err."+tagSchemaField.getName()+".invalid", "Unparseable date: "+schemaVal, schemaVal);
                        value.setValue(parsed.getStartPoint().toString());
                    }
                }
                final TagSchemaValue[] updated;
                if (newSize != values.length) {
                    updated = new TagSchemaValue[newSize];
                    int i = 0;
                    for (TagSchemaValue value : values) {
                        if (value != null) updated[i++] = value;
                    }
                    tag.setValues(updated);
                }
            }
        }
    }

    public Nexus updateByOwnerAndName(Account account, String name, NexusRequest request, Nexus idNexus) {
        Nexus nexus = findByOwnerAndName(account, name);
        if (nexus == null) {
            nexus = new Nexus();
            copy(nexus, idNexus, CREATE_FIELDS);
            copy(nexus, request, UPDATE_FIELDS);
            nexus.setOrigin(idNexus.getUuid());
            nexus.setOwnerAccount(account);
            nexus = create(nexus);

        } else if (!idNexus.isOwner(account)) {
            scrubTags(request);
            copy(nexus, idNexus, CREATE_FIELDS);
            copy(nexus, request, UPDATE_FIELDS);
            nexus.setOrigin(idNexus.getUuid());
            nexus.setOwnerAccount(account);
            nexus = update(nexus);

        } else {
            scrubTags(request);
            if (!updateNexus(request, nexus)) return nexus;
            nexus.setOrigin(idNexus.getUuid());
            nexus.setOwnerAccount(account);
            nexus = update(nexus);
        }
        return nexus;
    }
}
