package histori.model.support;

import com.fasterxml.jackson.annotation.JsonIgnore;
import histori.ApiConstants;
import histori.wiki.OrdinalCentury;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import javax.persistence.Embeddable;
import javax.persistence.Transient;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.io.Serializable;
import java.math.BigInteger;

import static org.cobbzilla.util.daemon.ZillaRuntime.bigint;
import static org.cobbzilla.util.daemon.ZillaRuntime.die;

@Embeddable @NoArgsConstructor @EqualsAndHashCode(of={"instant"})
public class TimePoint {

    // allows us to represent any TimePoint as a BigInteger
    // in the form: yearMMddHHmmss
    // "year" is limited by Long.MAX_VALUE, so we are limited to the range -9223372036854775807 BCE to 9223372036854775807 CE
    // This is ~700M x (age of the universe). And we have resolution down to the second for all that time.
    public static final long YEAR_MULTIPLIER = 1_00_00_00_00_00L;
    //                                           MM dd HH mm ss

    public static final String TP_SEP = "-";

    public static final String DATE_TIME_PATTERN = "yyyy" + TP_SEP + "MM" + TP_SEP + "dd" + TP_SEP + "HH" + TP_SEP + "mm" + TP_SEP + "ss";
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormat.forPattern(DATE_TIME_PATTERN);

    public static final long MIN_YEAR = -1_000_000;
    public static final long MAX_YEAR = 10_000;

    public static final TimePoint MIN_VALUE = new TimePoint(String.valueOf(MIN_YEAR));
    public static final BigInteger DATE_INSTANT_DIVISOR = BigInteger.valueOf(1_000_000L);

    public TimePoint (long timestamp) { this(DATE_TIME_FORMATTER.print(timestamp)); }

    public static String formatSearchRange(TimePoint start, TimePoint end) {
        return start.getInstant() + ApiConstants.RANGE_SEP + end.getInstant();
    }

    public TimePoint (String date) { initInstant(date); }

    public void initInstant(String date) {
        final String[] parts = date.split(TP_SEP);
        if (parts.length == 0) die("TimePoint: too short: "+date);

        final int year;
        final int yearIndex;
        if (parts[0].length() == 0) {
            year = -1 * Integer.parseInt(parts[1]);
            yearIndex = 1;
        } else {
            year = Integer.parseInt(parts[0]);
            yearIndex = 0;
        }
        setYear(year);

        long multiplier = YEAR_MULTIPLIER;
        BigInteger instant = bigint(multiplier).multiply(bigint(getYear()));

        if (parts.length > 1 + yearIndex) {
            setMonth(Byte.parseByte(parts[1+yearIndex]));
            multiplier /= 100;
            instant = instant.add(bigint(multiplier).multiply(bigint(getMonth())));
        }
        if (parts.length > 2 + yearIndex) {
            setDay(Byte.parseByte(parts[2+yearIndex]));
            multiplier /= 100;
            instant = instant.add(bigint(multiplier).multiply(bigint(getDay())));
        }
        if (parts.length > 3 + yearIndex) {
            setHour(Byte.parseByte(parts[3+yearIndex]));
            multiplier /= 100;
            instant = instant.add(bigint(multiplier).multiply(bigint(getHour())));
        }
        if (parts.length > 4 + yearIndex) {
            setMinute(Byte.parseByte(parts[4+yearIndex]));
            multiplier /= 100;
            instant = instant.add(bigint(multiplier).multiply(bigint(getMinute())));
        }
        if (parts.length > 5 + yearIndex) {
            setSecond(Byte.parseByte(parts[5+yearIndex]));
            multiplier /= 100;
            instant = instant.add(bigint(multiplier).multiply(bigint(getSecond())));
        }
        if (parts.length > 6 + yearIndex) die("TimePoint: too long: "+date);
        setInstant(instant);
    }

    @Getter @Setter private BigInteger instant;
    public TimePoint initInstant () { if (instant == null) initInstant(toString()); return this; }

    // For indexing in elasticsearch, which has poor support for range queries based in BigInteger
    @Transient public long getDateInstant () {
        return instant == null ? 0 : instant.divide(DATE_INSTANT_DIVISOR).longValue();
    }
    public void setDateInstant (long instant) {
        if (getDateInstant() != instant) this.instant = BigInteger.valueOf(instant).multiply(DATE_INSTANT_DIVISOR);
    }

    @Min(value=MIN_YEAR, message="err.timePoint.year.tooEarly")
    @Max(value=MAX_YEAR, message="err.timePoint.year.tooLate")
    @Getter @Setter private long year;

    @JsonIgnore public String getCentury () { return OrdinalCentury.forYear(getYear()); }
    public void setCentury (String ordinal) { setYear(OrdinalCentury.getYear(ordinal)); }

    public void invertYear() { year *= -1; }

    @Min(value=1, message="err.timePoint.month.tooSmall")
    @Max(value=12, message="err.timePoint.month.tooLarge")
    @Getter @Setter private Byte month;

    @Min(value=1, message="err.timePoint.day.tooSmall")
    @Max(value=31, message="err.timePoint.day.tooLarge")
    @Getter @Setter private Byte day;

    @Min(value=0, message="err.timePoint.hour.tooSmall")
    @Max(value=23, message="err.timePoint.hour.tooLarge")
    @Getter @Setter private Byte hour;

    @Min(value=0, message="err.timePoint.minute.tooSmall")
    @Max(value=59, message="err.timePoint.minute.tooLarge")
    @Getter @Setter private Byte minute;

    @Min(value=0, message="err.timePoint.second.tooSmall")
    @Max(value=59, message="err.timePoint.second.tooLarge")
    @Getter @Setter private Byte second;

    @Override public String toString () {

        StringBuilder b = new StringBuilder(String.valueOf(year));

        if (month == null) return b.toString();
        b.append(TP_SEP).append(pad(month));

        if (day == null) return b.toString();
        b.append(TP_SEP).append(pad(day));

        if (hour == null) return b.toString();
        b.append(TP_SEP).append(pad(hour));

        if (minute == null) return b.toString();
        b.append(TP_SEP).append(pad(minute));

        if (second == null) return b.toString();
        b.append(TP_SEP).append(pad(second));

        return b.toString();
    }

    public Serializable pad(Number val) {
        return val.longValue() < 10L ? "0"+val : val;
    }

}
