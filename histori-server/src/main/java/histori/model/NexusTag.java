package histori.model;

import histori.model.base.NexusTagBase;
import lombok.experimental.Accessors;

import javax.persistence.*;

@Entity @Accessors(chain=true)
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"owner", "nexus", "tagName"}))
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class NexusTag extends NexusTagBase {}
