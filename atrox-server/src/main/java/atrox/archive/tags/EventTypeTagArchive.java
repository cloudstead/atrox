package atrox.archive.tags;

import atrox.archive.EntityArchive;
import atrox.model.tags.EventTypeTag;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity public class EventTypeTagArchive extends EventTypeTag implements EntityArchive {

    @Override public void beforeCreate() {}

    @Column(nullable=false, length=UUID_MAXLEN)
    @Getter @Setter private String originalUuid;

}

