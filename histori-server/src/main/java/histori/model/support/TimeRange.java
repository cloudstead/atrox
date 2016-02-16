package histori.model.support;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor @AllArgsConstructor
public class TimeRange {

    @Getter @Setter private TimePoint start;
    @Getter @Setter private TimePoint end;

    public boolean hasEnd() { return end != null; }

    public TimeRange(TimePoint start) { this.start = start; }

}
