package histori.model.tag_schema;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum TagSchemaFieldType {

    integer, decimal, string,
    world_actor, event, person, event_type, idea, impact, result,
    role_type, relationship_type, citation, tag, date;

    @JsonCreator public static TagSchemaFieldType create (String val) { return valueOf(val.toLowerCase()); }

}
