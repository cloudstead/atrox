package atrox.model;

import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.wizard.model.IdentifiableBase;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Transient;

@Entity
public class Citation extends CanonicallyNamedEntity {

    @Transient
    public String getUrl () { return getName(); }
    public void setUrl (String val) { setName(val); }

    // Can be just about anything: WorldEvent, WorldActor, EventActor, EventType, EventTypeTag, Ideology or IdeologyTag
    @Column(nullable=false, length=UUID_MAXLEN)
    @Getter @Setter private String entity;

    @Column(length=200)
    @Getter @Setter private String author;

    @Column(length=500)
    @Getter @Setter private String title;

    @Column(length=200)
    @Getter @Setter private String publisher;

    @Column(length=100)
    @Getter @Setter private String publishDate;

}
