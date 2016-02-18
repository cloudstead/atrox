package histori.archive;

import histori.model.base.TagTypeBase;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity @Accessors(chain=true)
@Table(uniqueConstraints = @UniqueConstraint(name = "tag_type_archive_uniq", columnNames = {"canonicalName", "version"}))
public class TagTypeArchive extends TagTypeBase implements EntityArchive {

    @Getter @Setter private String originalUuid;
    public boolean archiveUuid () { return false; }

}
