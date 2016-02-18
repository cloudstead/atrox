package histori.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.cobbzilla.wizard.model.Identifiable;

import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

public interface VersionedEntity extends Identifiable {

    @JsonIgnore public String[] getIdentifiers ();
    @JsonIgnore public String[] getIdentifierFields ();

    public int getVersion ();
    public VersionedEntity setVersion (int version);

}
