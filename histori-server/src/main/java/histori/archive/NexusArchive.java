package histori.archive;

import histori.model.base.NexusBase;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity @Accessors(chain=true)
@Table(uniqueConstraints = @UniqueConstraint(name = "nexus_archive_uniq", columnNames = {"owner", "name", "version"}))
public class NexusArchive extends NexusBase implements EntityArchive {

    @Getter @Setter private String originalUuid;
    public boolean archiveUuid () { return true; }

}
