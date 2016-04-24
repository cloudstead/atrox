package histori.model.base;

import com.fasterxml.jackson.annotation.JsonIgnore;
import edu.emory.mathcs.backport.java.util.Arrays;
import histori.model.NexusTag;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.cobbzilla.util.collection.MapBuilder;
import org.cobbzilla.util.collection.mappy.MappySortedSet;
import org.cobbzilla.wizard.model.json.JSONBUserType;

import javax.persistence.Transient;
import java.util.*;

import static histori.model.CanonicalEntity.canonicalize;
import static histori.model.TagType.EVENT_TYPE;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

@NoArgsConstructor
public class NexusTags implements Iterable<NexusTag> {

    public static final String JSONB_TYPE = JSONBUserType.JSONB_TYPE+"_NexusTagsType";

    @Getter @Setter private ArrayList<NexusTag> tags = new ArrayList<>();

    @JsonIgnore public boolean isEmpty () { return tags.isEmpty(); }
    public int size() { return tags.size(); }
    public NexusTag get(int i) { return tags.get(i); }

    @Override public Iterator<NexusTag> iterator() { return tags.iterator(); }

    public NexusTags(NexusTag[] nexusTags) { tags.addAll((Collection<NexusTag>) Arrays.asList(nexusTags)); }

    public NexusTags(Collection<NexusTag> tags) { tags.addAll(tags); }

    @Override public boolean equals(Object o) {
        return (o instanceof NexusTags) && getTagMap().equals(((NexusTags) o).getTagMap());
    }

    @Override public int hashCode() { return getTagMap().hashCode(); }

    public NexusTags addTag (NexusTag tag) {
        if (tag == null) return this; // should never happen, but just in case (ConflictFinder had a bug that did this once)
        for (Iterator<NexusTag> iter = iterator(); iter.hasNext();) {
            NexusTag existing = iter.next();
            if (existing == null) {
                // should never happen, but just in case (ConflictFinder had a bug that did this once)
                iter.remove();
                continue;
            }
            if (tag.isSameTag(existing)) return this; // re-adding an identical tag is OK, just a noop
        }
        tags.add(tag);
        return this;
    }

    public NexusTags addTag (String name) { return addTag(name, null, null); }

    public NexusTags addTag (String name, String tagType) { return addTag(name, tagType, null); }

    public NexusTags addTag (String name, Map<String, String> tagFields) { return addTag(name, null, tagFields); }

    public NexusTags addTag (String name, String tagType, String field, String value) {
        return addTag(name, tagType, MapBuilder.build(field, value));
    }

    public NexusTags addTag (String name, String tagType, Map<String, String> tagFields) {
        final NexusTag tag = new NexusTag().setTagName(name);
        if (!empty(tagFields)) {
            for (Map.Entry<String, String> field : tagFields.entrySet()) {
                tag.setValue(field.getKey(), field.getValue());
            }
        }
        if (!empty(tagType)) tag.setTagType(tagType);
        return addTag(tag);
    }

    public List<NexusTag> getTag(String name) {
        final List<NexusTag> found = new ArrayList<>();
        if (!isEmpty()) {
            final String canonical = canonicalize(name);
            for (NexusTag tag : this.tags) {
                if (canonicalize(tag.getTagName()).equals(canonical)) found.add(tag);
            }
        }
        return found;
    }

    public List<NexusTag> getTag(String tagType, String name) {
        final List<NexusTag> found = new ArrayList<>();
        if (!isEmpty()) {
            final String canonical = canonicalize(name);
            for (NexusTag tag : this.tags) {
                if (tag.getTagType().equalsIgnoreCase(tagType) && canonicalize(tag.getTagName()).equals(canonical)) {
                    found.add(tag);
                }
            }
        }
        return found;
    }

    public boolean hasTag(String name) {
        if (isEmpty() || empty(name)) return false;
        final String canonical = canonicalize(name);
        for (NexusTag tag : this.tags) {
            if (canonicalize(tag.getTagName()).equals(canonical)) return true;
        }
        return false;
    }

    public boolean hasTag(String name, String type) {
        if (isEmpty() || empty(name)) return false;
        final String canonical = canonicalize(name);
        for (NexusTag tag : this.tags) {
            if (type != null && (!tag.hasTagType() || !tag.getTagType().equals(type))) continue;
            if (canonicalize(tag.getTagName()).equals(canonical)) return true;
        }
        return false;
    }

    @JsonIgnore @Transient public int getTagCount() { return isEmpty() ? 0 : tags.size(); }

    public boolean hasExactTag(NexusTag match) {
        if (isEmpty()) return false;
        for (NexusTag tag : this.tags) {
            if (!tag.getTagName().equalsIgnoreCase(match.getTagName())) continue;
            if (!tag.getTagType().equalsIgnoreCase(match.getTagType())) continue;

            if (!tag.hasSchemaValues()) {
                if (match.hasSchemaValues()) continue;
                return true;
            }
            if (!match.hasSchemaValues()) continue;

            if (!tag.getSchemaValueMap().equals(match.getSchemaValueMap())) continue;

            return true;
        }
        return false;
    }

    public NexusTag getFirstTag(String name) {
        final List<NexusTag> found = getTag(name);
        return empty(found) ? null : found.get(0);
    }

    public List<NexusTag> getTagsByType(String type) {
        final List<NexusTag> found = new ArrayList<>();
        if (!isEmpty()) {
            for (NexusTag tag : this.tags) if (tag.getTagType().equalsIgnoreCase(type)) found.add(tag);
        }
        return found;
    }

    @JsonIgnore @Transient public String getFirstEventType () {
        if (!isEmpty()) {
            for (NexusTag tag : this.tags) if (tag.hasTagType() && tag.getTagType().equalsIgnoreCase(EVENT_TYPE)) return tag.getTagName();
        }
        return null;
    }

    public void removeTag(String uuid) {
        if (isEmpty()) return;
        for (Iterator<NexusTag> iter = iterator(); iter.hasNext(); ) {
            final NexusTag tag = iter.next();
            if (tag.hasUuid() && tag.getUuid().equals(uuid)) iter.remove();
        }
    }

    @JsonIgnore @Transient public MappySortedSet<String, NexusTag> getTagMap () {
        final MappySortedSet<String, NexusTag> map = new MappySortedSet<>();
        for (NexusTag tag : this.tags) {
            map.put(tag.getCanonicalName(), tag);
        }
        return map;
    }

}
