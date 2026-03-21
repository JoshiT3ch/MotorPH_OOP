package motorph.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

public final class InputValidators {

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("M/d/yyyy"),
            DateTimeFormatter.ofPattern("M/d/yy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"));

    private InputValidators() {
    }

    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static boolean isNonNegative(double value) {
        return value >= 0.0;
    }

    public static boolean isValidName(String value) {
        return !isBlank(value) && value.trim().matches("[A-Za-z][A-Za-z '\\-]*");
    }

    public static boolean isValidBirthday(String value) {
        if (isBlank(value)) {
            return false;
        }
        String trimmed = value.trim();
        return trimmed.matches("\\d{1,2}/\\d{1,2}/\\d{4}") && parseDate(trimmed).isPresent();
    }

    public static boolean isValidPhone(String value) {
        return isBlank(value) || value.trim().matches("09\\d{2}(-?\\d{3})(-?\\d{4})");
    }

    public static boolean isValidSssNumber(String value) {
        return isBlank(value) || value.trim().matches("\\d{2}-\\d{7}-\\d");
    }

    public static boolean isValidPhilhealthNumber(String value) {
        return isBlank(value) || value.trim().matches("\\d{12}");
    }

    public static boolean isValidTinNumber(String value) {
        return isBlank(value) || value.trim().matches("\\d{3}-\\d{3}-\\d{3}-\\d{3}");
    }

    public static boolean isValidPagibigNumber(String value) {
        return isBlank(value) || value.trim().matches("\\d{12}");
    }

    public static boolean isValidGovernmentId(String value) {
        return isBlank(value) || value.trim().matches("[0-9-]{8,20}");
    }

    public static boolean isValidMoney(String value) {
        return isBlank(value) || value.trim().matches("\\d+(\\.\\d{1,2})?");
    }

    public static Optional<LocalDate> parseDate(String value) {
        if (isBlank(value)) {
            return Optional.empty();
        }
        String trimmed = value.trim();
        for (DateTimeFormatter formatter : DATE_FORMATS) {
            try {
                return Optional.of(LocalDate.parse(trimmed, formatter));
            } catch (DateTimeParseException ignored) {
            }
        }
        return Optional.empty();
    }
}
