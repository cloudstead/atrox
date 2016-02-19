package histori.model.base;

import com.fasterxml.jackson.annotation.JsonIgnore;
import histori.model.SocialEntity;
import histori.model.tag_schema.TagSchemaValue;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.cobbzilla.util.collection.ArrayUtil;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import javax.validation.constraints.Size;

import static histori.ApiConstants.NAME_MAXLEN;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.json.JsonUtil.fromJsonOrDie;
import static org.cobbzilla.util.json.JsonUtil.toJsonOrDie;

@MappedSuperclass @Accessors(chain=true) @ToString(of={"tagName", "tagType"})
public class NexusTagBase extends SocialEntity {

    @Column(length=UUID_MAXLEN, nullable=false, updatable=false)
    @Getter @Setter private String nexus;

    @Size(max=NAME_MAXLEN, message="err.tagName.tooLong")
    @Column(length=NAME_MAXLEN, nullable=false, updatable=false)
    @Getter @Setter private String tagName;

    // denormalized
    @Size(max=NAME_MAXLEN, message="err.tagType.tooLong")
    @Column(length=NAME_MAXLEN)
    @Getter @Setter private String tagType;

    @Size(max=32000, message="err.schemaValues.tooLong")
    @Column(length=32000)
    @JsonIgnore @Getter @Setter private String schemaValues;
    public boolean hasSchemaValues () { return !empty(schemaValues); }

    @Transient
    public TagSchemaValue[] getValues () { return empty(schemaValues) ? null : fromJsonOrDie(schemaValues, TagSchemaValue[].class); }
    public NexusTagBase setValues (TagSchemaValue[] values) { return setSchemaValues(empty(values) ? null : toJsonOrDie(values)); }

    public void setValue(String field, String value) {
        TagSchemaValue[] values = getValues();
        if (values == null) {
            values = new TagSchemaValue[] { new TagSchemaValue(field, value) };
        } else {
            values = ArrayUtil.append(values, new TagSchemaValue(field, value));
        }
        setValues(values);
    }

    private static final String[] ID_FIELDS = new String[]{"owner", "nexus", "tagName"};
    @Override public String[] getIdentifiers() { return new String[] {getOwner(), getNexus(), getTagName()}; }
    @Override public String[] getIdentifierFields() { return ID_FIELDS; }

}
