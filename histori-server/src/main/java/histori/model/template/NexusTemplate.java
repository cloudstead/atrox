package histori.model.template;

import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.wizard.model.json.JSONBUserType;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

public class NexusTemplate {

    public static final String NEXUS_TEMPLATE_JSONB_TYPE = JSONBUserType.JSONB_TYPE+"_NexusTemplate";

    @Getter @Setter private String name;
    @Getter @Setter private String markdown;
    public boolean hasMarkdown () { return !empty(markdown); }

    @Getter @Setter private String geoJson;
    @Getter @Setter private TimeRangeTemplate timeRange;

    @Getter @Setter private NexusTagTemplate[] tags;
    public boolean hasTags () { return !empty(tags); }

}
