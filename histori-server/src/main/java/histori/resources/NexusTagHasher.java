package histori.resources;

import histori.model.NexusTag;
import org.cobbzilla.util.collection.CustomHashSet;

import java.util.List;

import static histori.model.CanonicalEntity.canonicalize;
import static org.cobbzilla.util.security.ShaUtil.sha256_hex;

public class NexusTagHasher implements CustomHashSet.Hasher<NexusTag> {

    public static CustomHashSet.Hasher<NexusTag> instance = new NexusTagHasher();

    private static final String SEP = "|||";

    @Override public String hash(NexusTag tag) {
        return sha256_hex(canonicalize(tag.getTagName()) + SEP + canonicalize(tag.getTagType()) + SEP + tag.getSchemaHash());
    }

    public static CustomHashSet<NexusTag> valueTagHash(List<NexusTag> tags) {
        return new CustomHashSet<>(NexusTag.class, instance, tags);
    }

}
