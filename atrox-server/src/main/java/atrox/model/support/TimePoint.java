package atrox.model.support;

import atrox.ApiConstants;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import javax.persistence.Embeddable;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;

@Embeddable @NoArgsConstructor @EqualsAndHashCode(of={"instant"})
public class TimePoint {

    // allows us to represent any TimePoint as a long
    //                                  year____MMddHHmmss
    public static final long YEAR_MULTIPLIER = 10000000000L;

    // do not use '-' since year can be negative!
    public static final String TP_SEP = "_";

    public static final String DATE_TIME_PATTERN = "yyyy" + TP_SEP + "MM" + TP_SEP + "dd" + TP_SEP + "HH" + TP_SEP + "mm" + TP_SEP + "ss";
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormat.forPattern(DATE_TIME_PATTERN);

    public static final long MIN_YEAR = -1_000_000;
    public static final long MAX_YEAR = 10_000;

    public static final TimePoint MIN_VALUE = new TimePoint(String.valueOf(MIN_YEAR));

    public TimePoint(long timestamp) { this(DATE_TIME_FORMATTER.print(timestamp)); }

    public static String formatSearchRange(TimePoint start, TimePoint end) {
        return start.getInstant() + ApiConstants.RANGE_SEP + end.getInstant();
    }

    public TimePoint (String date) {
        final String[] parts = date.split(TP_SEP);
        if (parts.length == 0) die("TimePoint: too short: "+date);

        setYear(Integer.parseInt(parts[0]));
        long multiplier = YEAR_MULTIPLIER;
        long instant = multiplier * getYear();

        if (parts.length > 1) {
            setMonth(Byte.parseByte(parts[1]));
            multiplier /= 100;
            instant += multiplier * getMonth();
        }
        if (parts.length > 2) {
            setDay(Byte.parseByte(parts[2]));
            multiplier /= 100;
            instant += multiplier * getDay();
        }
        if (parts.length > 3) {
            setHour(Byte.parseByte(parts[3]));
            multiplier /= 100;
            instant += multiplier * getHour();
        }
        if (parts.length > 4) {
            setMinute(Byte.parseByte(parts[4]));
            multiplier /= 100;
            instant += multiplier * getMinute();
        }
        if (parts.length > 5) {
            setSecond(Byte.parseByte(parts[5]));
            multiplier /= 100;
            instant += multiplier * getSecond();
        }
        if (parts.length > 6) die("TimePoint: too long: "+date);

        setInstant(instant);
    }

    @Getter @Setter private long instant;

    @Min(value=MIN_YEAR, message="err.year.tooEarly")
    @Max(value=MAX_YEAR, message="err.year.tooLate")
    @Getter @Setter private long year;

    @Getter @Setter private Byte month;
    @Getter @Setter private Byte day;
    @Getter @Setter private Byte hour;
    @Getter @Setter private Byte minute;
    @Getter @Setter private Byte second;

    @Override public String toString () {
        StringBuilder b = new StringBuilder(String.valueOf(year));

        if (month == null) return b.toString();
        b.append(TP_SEP).append(month);

        if (day == null) return b.toString();
        b.append(TP_SEP).append(day);

        if (hour == null) return b.toString();
        b.append(TP_SEP).append(hour);

        if (minute == null) return b.toString();
        b.append(TP_SEP).append(minute);

        if (second == null) return b.toString();
        b.append(TP_SEP).append(second);

        return b.toString();
    }
}
