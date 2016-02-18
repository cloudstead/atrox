package histori.model.tag_schema;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class TagSchemaField {

    @Getter @Setter private String name;

    @Getter @Setter private TagSchemaFieldType fieldType;

    @Getter @Setter private boolean required;
    @Getter @Setter private boolean multiple;

    @Getter @Setter private List<String> enumValues;

}
