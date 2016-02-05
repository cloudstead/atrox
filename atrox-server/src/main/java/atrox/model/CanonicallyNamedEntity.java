package atrox.model;

import atrox.model.tags.EntityTag;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.model.ResultPage;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.wizard.model.UniquelyNamedEntity.NAME_MAXLEN;

/**
 * A CanonicallyNamedEntity has a name, which must be unique, and a canonical name, which must also be unique.
 * The canonical name simplifies the inbound name, in the interest of avoiding entities whose names differ only in
 * terms of
 */
@MappedSuperclass @NoArgsConstructor @Accessors(chain=true)
public abstract class CanonicallyNamedEntity extends AccountOwnedEntity {

    public CanonicallyNamedEntity (String name) { setName(name); }

    public static final String[] UNIQUES = {"canonicalName"};
    @Override public String[] getUniqueProperties() { return UNIQUES; }
    @Override public String getSortField() { return UNIQUES[0]; }
    @Override public ResultPage.SortOrder getSortOrder() { return ResultPage.SortOrder.ASC; }

    @Column(length=NAME_MAXLEN, unique=true, nullable=false, updatable=false)
    @Size(min=2, max=NAME_MAXLEN, message="err.name.length")
    @Getter private String name;
    public CanonicallyNamedEntity setName (String val) { canonicalName = canonicalize(val); return this; }

    public static String canonicalize(String val) {
        return empty(val) ? "" : val.replaceAll("\\W+", "_").toLowerCase();
    }

    @Column(length=NAME_MAXLEN, unique=true, nullable=false, updatable=false)
    @Getter @Setter private String canonicalName;

    @Transient @Getter @Setter private Map<String, List<EntityTag>> tags = new HashMap<>();

    public void addTag (EntityTag tag) {
        final String tagName = getTagName(tag);
        List<EntityTag> tagList = getTagList(tagName);
        tagList.add(tag);
    }

    public void addTags(List<EntityTag> newTags) {
        if (empty(newTags)) return;
        getTagList(getTagName(newTags.get(0))).addAll(newTags);
    }

    public String getTagName(EntityTag tag) {
        return tag.getClass().getSimpleName();
    }

    public List<EntityTag> getTagList(String tagName) {
        List<EntityTag> tagList = tags.get(tagName);
        if (tagList == null) {
            tagList = new ArrayList<>();
            tags.put(tagName, tagList);
        }
        return tagList;
    }

    public List<EntityTag> getTags (String name) { return tags.get(name); }
    public List<EntityTag> getTags (EntityTag tag) { return tags.get(getTagName(tag)); }

}
