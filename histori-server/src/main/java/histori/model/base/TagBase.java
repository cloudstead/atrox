package histori.model.base;

import histori.model.CanonicalEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

import static histori.ApiConstants.NAME_MAXLEN;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

@MappedSuperclass @NoArgsConstructor @Accessors(chain=true)
public class TagBase extends CanonicalEntity {

    public TagBase (String name) { super(name); }

    @Column(length=NAME_MAXLEN)
    @Getter @Setter private String tagType;
    public boolean hasTagType() { return !empty(tagType); }

}
