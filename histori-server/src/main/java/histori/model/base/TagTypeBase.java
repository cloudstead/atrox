package histori.model.base;

import com.fasterxml.jackson.annotation.JsonIgnore;
import histori.model.CanonicalEntity;
import histori.model.tag_schema.TagSchema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.json.JsonUtil.fromJsonOrDie;
import static org.cobbzilla.util.json.JsonUtil.toJsonOrDie;

@MappedSuperclass @NoArgsConstructor @Accessors(chain=true)
public class TagTypeBase extends CanonicalEntity {

    public TagTypeBase (String name) { super(name); }

    @Column(length=32000, nullable=true, updatable=false)
    @JsonIgnore @Getter @Setter private String schemaJson;

    @Transient
    public TagSchema getSchema () { return empty(schemaJson) ? null : fromJsonOrDie(schemaJson, TagSchema.class); }
    public TagTypeBase setSchema (TagSchema schema) { return setSchemaJson(empty(schema) ? null : toJsonOrDie(schema)); }

}
