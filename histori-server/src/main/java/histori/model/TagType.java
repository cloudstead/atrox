package histori.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import histori.model.base.TagTypeBase;
import histori.model.tag_schema.TagSchema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.persistence.*;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.json.JsonUtil.fromJsonOrDie;
import static org.cobbzilla.util.json.JsonUtil.toJsonOrDie;

@Entity @NoArgsConstructor @Accessors(chain=true)
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class TagType extends TagTypeBase {

    public TagType (String name) { super(name); }

}
