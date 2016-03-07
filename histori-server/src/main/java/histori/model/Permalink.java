package histori.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.model.UniquelyNamedEntity;
import org.cobbzilla.wizard.validation.HasValue;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.validation.constraints.Size;

@Entity @Accessors(chain=true) @ToString(of={"name","json"})
public class Permalink extends UniquelyNamedEntity {

    @HasValue(message="err.permalink.json.empty")
    @Size(max=16000, message="err.permalink.json.length")
    @Column(length=16000, nullable=false)
    @Getter @Setter private String json;

}
