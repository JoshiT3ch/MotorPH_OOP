package motorph.util;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

public final class DateUtils {

    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("M/d/yyyy"),
            DateTimeFormatter.ofPattern("M/d/yy"),
            DateTimeFormatter.ISO_LOCAL_DATE);
    private static final List<DateTimeFormatter> TIME_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("H:mm"),
            DateTimeFormatter.ofPattern("HH:mm"),
            DateTimeFormatter.ofPattern("H:mm:ss"),
            DateTimeFormatter.ofPattern("HH:mm:ss"));

    private DateUtils() {
    }

    public static Optional<LocalDate> parseAttendanceDate(String value) {
        String trimmed = CsvUtils.safeTrim(value);
        if (trimmed.isEmpty()) {
            return Optional.empty();
        }
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return Optional.of(LocalDate.parse(trimmed, formatter));
            } catch (DateTimeParseException ignored) {
            }
        }
        return Optional.empty();
    }

    public static Optional<LocalTime> parseAttendanceTime(String value) {
        String trimmed = CsvUtils.safeTrim(value);
        if (trimmed.isEmpty()) {
            return Optional.empty();
        }
        for (DateTimeFormatter formatter : TIME_FORMATTERS) {
            try {
                return Optional.of(LocalTime.parse(trimmed, formatter));
            } catch (DateTimeParseException ignored) {
            }
        }
        return Optional.empty();
    }
}
