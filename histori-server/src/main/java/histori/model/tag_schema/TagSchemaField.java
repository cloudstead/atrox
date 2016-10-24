package histori.model.tag_schema;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@NoArgsConstructor @AllArgsConstructor
public class TagSchemaField {

    public static final String LAST_ACCESSED = "last-accessed";

    @Getter @Setter private String name;

    @Getter @Setter private TagSchemaFieldType fieldType;

    @Getter @Setter private boolean required;
    @Getter @Setter private boolean multiple;

    @Getter @Setter private List<String> enumValues;

}
