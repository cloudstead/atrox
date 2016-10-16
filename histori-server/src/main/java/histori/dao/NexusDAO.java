package histori.dao;

import histori.dao.archive.NexusArchiveDAO;
import histori.dao.search.NexusSearchResults;
import histori.dao.shard.NexusShardDAO;
import histori.model.*;
import histori.model.support.EntityVisibility;
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

import static histori.model.CanonicalEntity.canonicalize;
import static histori.model.TagType.EVENT_TYPE;
import static org.cobbzilla.util.json.JsonUtil.toJsonOrDie;

@Repository @Slf4j
public class NexusDAO extends ShardedEntityDAO<Nexus, NexusShardDAO> {

    @Autowired private NexusArchiveDAO nexusArchiveDAO;

    @Autowired private DatabaseConfiguration database;
    @Override public ShardSetConfiguration getShardConfiguration() { return database.getShard("nexus"); }

    @Autowired @Getter @Setter private SuperNexusDAO superNexusDAO;
    @Autowired @Getter @Setter private TagDAO tagDAO;
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

}
