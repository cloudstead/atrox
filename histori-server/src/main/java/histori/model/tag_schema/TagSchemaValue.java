package histori.model.tag_schema;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor @AllArgsConstructor
public class TagSchemaValue {

    @Getter @Setter private String field;
    @Getter @Setter private String value;

}
