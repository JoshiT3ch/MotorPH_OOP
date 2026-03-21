package motorph.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

public record AttendanceRecord(
        String employeeId,
        String lastName,
        String firstName,
        String dateText,
        String logInText,
        String logOutText,
        LocalDate date,
        LocalTime logIn,
        LocalTime logOut) {

    public int monthValue() {
        return date == null ? -1 : date.getMonthValue();
    }

    public int yearValue() {
        return date == null ? -1 : date.getYear();
    }

    public double hoursWorked() {
        if (logIn == null || logOut == null) {
            return 0.0;
        }
        long minutes = ChronoUnit.MINUTES.between(logIn, logOut);
        return minutes <= 0 ? 0.0 : minutes / 60.0;
    }

    public boolean isComplete() {
        return logIn != null && logOut != null && hoursWorked() > 0;
    }
}
