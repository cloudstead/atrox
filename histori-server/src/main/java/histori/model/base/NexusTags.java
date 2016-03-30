package histori.model.base;

import com.fasterxml.jackson.annotation.JsonIgnore;
import edu.emory.mathcs.backport.java.util.Arrays;
import histori.model.NexusTag;
import org.cobbzilla.util.collection.MapBuilder;
import org.cobbzilla.util.collection.mappy.MappySortedSet;

import javax.persistence.Transient;
import java.util.*;

import static histori.model.CanonicalEntity.canonicalize;
import static histori.model.TagType.EVENT_TYPE;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.json.JsonUtil.toJsonOrDie;

public class NexusTags extends ArrayList<NexusTag> {

    @JsonIgnore private NexusBase nexus;

    public NexusTags(NexusBase nexus) { this.nexus = nexus; }

    public NexusTags(NexusBase nexus, NexusTag[] nexusTags) {
        this(nexus);
        addAll(Arrays.asList(nexusTags));
    }

    public NexusTags(NexusBase nexus, List<NexusTag> tags) {
        this(nexus);
        addAll(tags);
    }

    @Override public NexusTag set(int index, NexusTag element) {
        final NexusTag rval = super.set(index, element);
        nexus.setTagsJson(toJsonOrDie(this));
        return rval;
    }

    @Override public boolean add(NexusTag nexusTag) {
        final boolean rval = super.add(nexusTag);
        nexus.setTagsJson(toJsonOrDie(this));
        return rval;
    }

    @Override public void add(int index, NexusTag element) {
        super.add(index, element);
        nexus.setTagsJson(toJsonOrDie(this));
    }

    @Override public NexusTag remove(int index) {
        final NexusTag rval = super.remove(index);
        nexus.setTagsJson(toJsonOrDie(this));
        return rval;
    }

    @Override public boolean remove(Object o) {
        final boolean rval = super.remove(o);
        nexus.setTagsJson(toJsonOrDie(this));
        return rval;
    }

    @Override public void clear() {
        super.clear();
        nexus.setTagsJson(toJsonOrDie(this));
    }

    @Override public boolean addAll(Collection<? extends NexusTag> c) {
        final boolean rval = super.addAll(c);
        nexus.setTagsJson(toJsonOrDie(this));
        return rval;
    }

    @Override public boolean addAll(int index, Collection<? extends NexusTag> c) {
        final boolean rval = super.addAll(index, c);
        nexus.setTagsJson(toJsonOrDie(this));
        return rval;
    }

    @Override protected void removeRange(int fromIndex, int toIndex) {
        super.removeRange(fromIndex, toIndex);
        nexus.setTagsJson(toJsonOrDie(this));
    }

    @Override public boolean removeAll(Collection<?> c) {
        final boolean rval = super.removeAll(c);
        nexus.setTagsJson(toJsonOrDie(this));
        return rval;
    }

    @Override public boolean retainAll(Collection<?> c) {
        final boolean rval = super.retainAll(c);
        nexus.setTagsJson(toJsonOrDie(this));
        return rval;
    }

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
        add(tag);
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
            for (NexusTag tag : this) {
                if (canonicalize(tag.getTagName()).equals(canonical)) found.add(tag);
            }
        }
        return found;
    }

    public List<NexusTag> getTag(String tagType, String name) {
        final List<NexusTag> found = new ArrayList<>();
        if (!isEmpty()) {
            final String canonical = canonicalize(name);
            for (NexusTag tag : this) {
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
        for (NexusTag tag : this) {
            if (canonicalize(tag.getTagName()).equals(canonical)) return true;
        }
        return false;
    }

    public boolean hasTag(String name, String type) {
        if (isEmpty() || empty(name)) return false;
        final String canonical = canonicalize(name);
        for (NexusTag tag : this) {
            if (type != null && (!tag.hasTagType() || !tag.getTagType().equals(type))) continue;
            if (canonicalize(tag.getTagName()).equals(canonical)) return true;
        }
        return false;
    }

    @JsonIgnore @Transient public int getTagCount() { return isEmpty() ? 0 : size(); }

    public boolean hasExactTag(NexusTag match) {
        if (isEmpty()) return false;
        for (NexusTag tag : this) {
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
            for (NexusTag tag : this) if (tag.getTagType().equalsIgnoreCase(type)) found.add(tag);
        }
        return found;
    }

    @JsonIgnore @Transient public String getFirstEventType () {
        if (!isEmpty()) {
            for (NexusTag tag : this) if (tag.hasTagType() && tag.getTagType().equalsIgnoreCase(EVENT_TYPE)) return tag.getTagName();
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
        for (NexusTag tag : this) {
            map.put(tag.getCanonicalName(), tag);
        }
        return map;
    }


}
