package histori.model.history;

import histori.model.SocialEntity;
import histori.model.canonical.CanonicalEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import org.cobbzilla.util.reflect.ReflectionUtil;

import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

@MappedSuperclass
public abstract class EntityHistory extends SocialEntity {

    @JsonIgnore @Transient @Getter(lazy=true) private final String[] associated = initAssociated();

    private String[] initAssociated() { return new String[]{getCanonicalField()}; }

    @JsonIgnore @Transient public String getCanonicalField() { return simpleName().replace("History", "").toLowerCase(); }

    @JsonIgnore @Transient public String getCanonicalFieldValue() {
        return (String) ReflectionUtil.get(this, getCanonicalField());
    }

    @JsonIgnore @Transient @Getter private CanonicalEntity canonical;
    public void setCanonical(CanonicalEntity canonical) {
        this.canonical = canonical;
        ReflectionUtil.set(this, getCanonicalField(), canonical.getUuid());
        addAssociation(canonical);
    }

}
