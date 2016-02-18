package histori.archive;


import histori.model.Tag;
import histori.model.VersionedEntity;
import histori.model.base.TagBase;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity @Accessors(chain=true)
@Table(uniqueConstraints = @UniqueConstraint(name = "tag_archive_uniq", columnNames = {"canonicalName", "version"}))
public class TagArchive extends TagBase implements EntityArchive {

    @Column(length=UUID_MAXLEN*10, nullable=false, updatable=false)
    @Getter @Setter private String identifier;

    @Override public String getIdentifier(VersionedEntity entity) { return ((Tag) entity).getCanonicalName(); }

}
