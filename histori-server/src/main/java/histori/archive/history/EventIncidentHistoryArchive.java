package histori.archive.history;

import histori.archive.EntityArchive;
import histori.model.history.EventIncidentHistory;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity public class EventIncidentHistoryArchive extends EventIncidentHistory implements EntityArchive {

    @Override public void beforeCreate() {}

    @Column(nullable=false, length=UUID_MAXLEN)
    @Getter @Setter private String originalUuid;

}