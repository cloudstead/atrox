package histori.model.support;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.validation.HasValue;

import java.util.Map;

@NoArgsConstructor @AllArgsConstructor @Accessors(chain=true)
public class BulkCreateNexusRequest {

    @HasValue(message="err.nexus.bulkLoad.url.empty")
    @Getter @Setter private String url;

    @Getter @Setter private Map<String, String> extraTags;

}
