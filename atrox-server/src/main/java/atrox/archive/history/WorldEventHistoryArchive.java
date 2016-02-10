package atrox.archive.history;

import atrox.archive.EntityArchive;
import atrox.model.history.WorldEventHistory;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity public class WorldEventHistoryArchive extends WorldEventHistory implements EntityArchive {

    @Override public void beforeCreate() {}

    @Column(nullable=false, length=UUID_MAXLEN)
    @Getter @Setter private String originalUuid;

}

