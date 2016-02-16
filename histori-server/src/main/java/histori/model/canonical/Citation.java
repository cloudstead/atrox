package histori.model.canonical;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.persistence.*;

@Entity @Inheritance(strategy=InheritanceType.TABLE_PER_CLASS) @NoArgsConstructor @Accessors(chain=true)
public class Citation extends CanonicalEntity {

    public Citation (String url) { super(url); }

    @Transient
    public String getUrl () { return getName(); }
    public void setUrl (String val) { setName(val); }

    @Column(length=200)
    @Getter @Setter private String author;

    @Column(length=500)
    @Getter @Setter private String title;

    @Column(length=200)
    @Getter @Setter private String publisher;

    @Column(length=100)
    @Getter @Setter private String publishDate;

    @Column(length=200)
    @Getter @Setter private String citationNotes;

}
