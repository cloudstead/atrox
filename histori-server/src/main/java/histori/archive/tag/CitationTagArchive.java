package histori.archive.tag;

import histori.archive.EntityArchive;
import histori.model.tag.CitationTag;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity public class CitationTagArchive extends CitationTag implements EntityArchive {

    @Override public void beforeCreate() {}

    @Column(nullable=false, length=UUID_MAXLEN)
    @Getter @Setter private String originalUuid;

}

