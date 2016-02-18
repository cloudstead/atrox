package histori.model;

import histori.model.base.TagBase;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

@Entity @NoArgsConstructor @Accessors(chain=true)
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class Tag extends TagBase {

    public Tag (String name) { super(name); }

    public Tag (String name, String type) { super(name); setTagType(type); }

    public Tag(String tagName, TagType tagType) {
        this(tagName, tagType == null ? null : tagType.getCanonicalName());
    }

}
