package histori.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import histori.model.tag_schema.TagSchema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Transient;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.json.JsonUtil.fromJsonOrDie;
import static org.cobbzilla.util.json.JsonUtil.toJsonOrDie;

@Entity @NoArgsConstructor @Accessors(chain=true)
public class TagType extends CanonicalEntity {

    public static final String EVENT_TYPE = "event_type";

    public TagType (String name) { super(name); }

    @Column(length=32000, nullable=true, updatable=false)
    @JsonIgnore @Getter @Setter private String schemaJson;

    @Transient
    public TagSchema getSchema () { return empty(schemaJson) ? null : fromJsonOrDie(schemaJson, TagSchema.class); }
    public TagType setSchema (TagSchema schema) { return setSchemaJson(empty(schema) ? null : toJsonOrDie(schema)); }
    public boolean hasSchema() { return !empty(getSchemaJson()); }
}
