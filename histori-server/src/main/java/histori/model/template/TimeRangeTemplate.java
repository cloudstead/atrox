package histori.model.template;

import com.fasterxml.jackson.annotation.JsonIgnore;
import histori.model.support.TimePoint;
import histori.model.support.TimeRange;
import histori.wiki.WikiDateFormat;
import lombok.Getter;
import lombok.Setter;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;

public class TimeRangeTemplate {

    @Getter @Setter private String start;
    @Getter @Setter private String end;

}
