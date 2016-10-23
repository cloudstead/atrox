package histori.model.tag_schema;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

public class TagSchema {

    @Getter @Setter private List<TagSchemaField> fields;

    @JsonIgnore public Map<String, TagSchemaField> getFieldMap() {
        final Map<String, TagSchemaField> map = new HashMap<>();
        if (!empty(fields)) for (TagSchemaField f : fields) map.put(f.getName(), f);
        return map;
    }

}
