package histori.model.support;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import javax.persistence.*;
import java.math.BigInteger;

@Embeddable @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode(of={"startPoint", "endPoint"})
public class TimeRange {

    @AttributeOverrides({
            @AttributeOverride(name="instant",column=@Column(name="startInstant", nullable=false, columnDefinition="numeric(29,0)")),
            @AttributeOverride(name="year",column=@Column(name="startYear")),
            @AttributeOverride(name="month",column=@Column(name="startMonth")),
            @AttributeOverride(name="day",column=@Column(name="startDay")),
            @AttributeOverride(name="hour",column=@Column(name="startHour")),
            @AttributeOverride(name="minute",column=@Column(name="startMinute")),
            @AttributeOverride(name="second",column=@Column(name="startSecond")),
    })
    @Embedded @Getter @Setter private TimePoint startPoint;
    public boolean hasStart() { return startPoint != null; }

    @AttributeOverrides({
            @AttributeOverride(name="instant",column=@Column(name="endInstant", columnDefinition="numeric(29,0)")),
            @AttributeOverride(name="year",column=@Column(name="endYear")),
            @AttributeOverride(name="month",column=@Column(name="endMonth")),
            @AttributeOverride(name="day",column=@Column(name="endDay")),
            @AttributeOverride(name="hour",column=@Column(name="endHour")),
            @AttributeOverride(name="minute",column=@Column(name="endMinute")),
            @AttributeOverride(name="second",column=@Column(name="endSecond")),
    })
    @Embedded @Setter private TimePoint endPoint;
    public TimePoint getEndPoint() { return !hasEnd() ? null : endPoint; }
    public boolean hasEnd() { return endPoint != null && !endPoint.equals(startPoint) && endPoint.getYear() != 0; }

    public TimeRange(String startDate, String endDate) { this(new TimePoint(startDate), new TimePoint(endDate)); }

    public TimeRange(String startDate) { setStartPoint(new TimePoint(startDate)); }

    public TimeRange(TimePoint start) { this.startPoint = start.initInstant(); }

    @Override public String toString() { return hasEnd() ? startPoint + "_" + endPoint : startPoint.toString(); }

    // convenience methods
    public BigInteger start () { return getStartPoint().getInstant(); }
    public BigInteger end () { return hasEnd() ? getEndPoint().getInstant() : start(); }

    @JsonIgnore @Transient public TimePoint getOrCreateStartPoint() {
        if (startPoint == null) startPoint = new TimePoint();
        return startPoint;
    }
    @JsonIgnore @Transient public TimePoint getOrCreateEndPoint() {
        if (endPoint == null) endPoint = new TimePoint();
        return endPoint;
    }

    /**
     * @param timeRange range to compare
     * @return true if the range argument has a either start date that is before, or an end date that is after, the range of this
     */
    public boolean isOutside(TimeRange timeRange) {
        return timeRange.start().compareTo(start()) < 0
            || timeRange.end().compareTo(end()) > 0;
    }

}
