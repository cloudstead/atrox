package atrox.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Transient;

@Entity
public class Citation extends CanonicallyNamedEntity {

    public static final String[] ASSOCIATED = {"entity"};
    @Transient public String[] getAssociated() { return ASSOCIATED; }

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

}
