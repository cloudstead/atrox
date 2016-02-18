package histori.model.base;

import histori.model.CanonicalEntity;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

import static histori.ApiConstants.NAME_MAXLEN;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

@MappedSuperclass @NoArgsConstructor @Accessors(chain=true)
public class TagBase extends CanonicalEntity {

    public TagBase (String name) { super(name); }

    @Column(length=NAME_MAXLEN)
    private String tagType;

    public String getTagType() { return tagType == null ? null : canonicalize(tagType); }
    public void setTagType(String tagType) { this.tagType = (tagType != null) ? canonicalize(tagType) : null; }

    public boolean hasTagType() { return !empty(tagType); }

}
