package atrox.model.archive.tags;

import atrox.model.archive.EntityArchive;
import atrox.model.tags.WorldActorTag;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity public class WorldActorTagArchive extends WorldActorTag implements EntityArchive {

    @Override public void beforeCreate() {}

    @Column(nullable=false, length=UUID_MAXLEN)
    @Getter @Setter private String originalUuid;

}

