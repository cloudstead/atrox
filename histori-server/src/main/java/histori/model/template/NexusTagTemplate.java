package histori.model.template;

import histori.model.NexusTag;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Transient;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

public class NexusTagTemplate extends NexusTag {

    // only used in feeds
    @Transient @Getter @Setter private String splitName;
    public boolean hasSplitName () { return !empty(splitName); }

}
