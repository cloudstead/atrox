package histori.archive;


import histori.model.base.TagBase;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity @Accessors(chain=true)
@Table(uniqueConstraints = @UniqueConstraint(name = "tag_archive_uniq", columnNames = {"canonicalName", "version"}))
public class TagArchive extends TagBase implements EntityArchive {

    @Getter @Setter private String originalUuid;
    public boolean archiveUuid () { return false; }

}
