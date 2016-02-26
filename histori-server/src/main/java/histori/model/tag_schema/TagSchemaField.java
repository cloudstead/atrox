package histori.model.tag_schema;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@NoArgsConstructor @AllArgsConstructor
public class TagSchemaField {

    @Getter @Setter private String name;

    @Getter @Setter private TagSchemaFieldType fieldType;

    @Getter @Setter private boolean required;
    @Getter @Setter private boolean multiple;

    @Getter @Setter private List<String> enumValues;

}
