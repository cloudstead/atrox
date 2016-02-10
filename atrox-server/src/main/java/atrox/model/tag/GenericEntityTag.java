package atrox.model.tag;

import atrox.model.SocialEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.util.reflect.ReflectionUtil;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import static atrox.ApiConstants.ENTITY_TYPE_MAXLEN;

@MappedSuperclass
public abstract class GenericEntityTag extends SocialEntity {

    public static Class<? extends GenericEntityTag>[] TAG_TYPES
            = new Class[]{ CitationTag.class, IdeaTag.class };

    // Can be just about anything
    @Column(nullable=false, length=UUID_MAXLEN)
    @Getter @Setter private String entity;

    // What type the above points to. For convenience.
    @Column(length=ENTITY_TYPE_MAXLEN, nullable=false, updatable=false)
    @Getter @Setter private String entityType;

    @JsonIgnore @Transient public String tagField() { return simpleName().replace("Tag", "").toLowerCase(); }
    @JsonIgnore @Transient public String tagFieldValue() {
        return (String) ReflectionUtil.get(this, tagField());
    }

    @JsonIgnore @Transient @Getter(lazy=true) private final String[] associated = initAssociated();
    private String[] initAssociated() { return new String[]{"entity", tagField()}; }

}
