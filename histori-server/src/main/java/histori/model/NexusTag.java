package histori.model;

import histori.model.base.NexusTagBase;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import javax.persistence.*;

import static org.cobbzilla.util.reflect.ReflectionUtil.copy;

@Entity @Accessors(chain=true) @NoArgsConstructor
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"owner", "nexus", "tagName", "schemaValues"}))
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class NexusTag extends NexusTagBase {

    public NexusTag(NexusTag nexusTag) { copy(this, nexusTag, NexusTag.VALUE_FIELDS); }
}
