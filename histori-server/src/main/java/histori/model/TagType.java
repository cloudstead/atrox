package histori.model;

import histori.model.base.TagTypeBase;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

@Entity @NoArgsConstructor @Accessors(chain=true)
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class TagType extends TagTypeBase {

    public static final String EVENT_TYPE = "event_type";

    public TagType (String name) { super(name); }

}
