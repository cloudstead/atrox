package atrox.archive.tag;

import atrox.archive.EntityArchive;
import atrox.model.tag.IdeaTag;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity public class IdeaTagArchive extends IdeaTag implements EntityArchive {

    @Override public void beforeCreate() {}

    @Column(nullable=false, length=UUID_MAXLEN)
    @Getter @Setter private String originalUuid;

}

