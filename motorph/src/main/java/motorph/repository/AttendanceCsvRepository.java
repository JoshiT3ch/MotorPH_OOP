package motorph.repository;

import com.opencsv.exceptions.CsvValidationException;
import motorph.model.AttendanceRecord;
import motorph.util.CsvUtils;
import motorph.util.DateUtils;
import motorph.util.FilePathResolver;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class AttendanceCsvRepository implements AttendanceRepository {

    private static final String FILE_NAME = "employee_attendance.csv";
    private static final String[] HEADER = {"Employee #", "Last Name", "First Name", "Date", "Log In", "Log Out"};
    private final Path path;

    public AttendanceCsvRepository() {
        this.path = FilePathResolver.resolveDataPath(FILE_NAME);
    }

    @Override
    public List<AttendanceRecord> findAll() {
        try {
            List<String[]> rows = CsvUtils.readAll(path);
            List<AttendanceRecord> records = new ArrayList<>();
            for (int i = rows.isEmpty() ? 0 : 1; i < rows.size(); i++) {
                String[] row = rows.get(i);
                if (row.length < 4) {
                    continue;
                }
                String employeeId = CsvUtils.safeTrim(row[0]);
                String lastName = row.length > 1 ? CsvUtils.safeTrim(row[1]) : "";
                String firstName = row.length > 2 ? CsvUtils.safeTrim(row[2]) : "";
                String dateText = row.length > 3 ? CsvUtils.safeTrim(row[3]) : "";
                String logInText = row.length > 4 ? CsvUtils.safeTrim(row[4]) : "";
                String logOutText = row.length > 5 ? CsvUtils.safeTrim(row[5]) : "";
                records.add(new AttendanceRecord(
                        employeeId,
                        lastName,
                        firstName,
                        dateText,
                        logInText,
                        logOutText,
                        DateUtils.parseAttendanceDate(dateText).orElse(null),
                        DateUtils.parseAttendanceTime(logInText).orElse(null),
                        DateUtils.parseAttendanceTime(logOutText).orElse(null)
                ));
            }
            return records;
        } catch (IOException | CsvValidationException e) {
            throw new IllegalStateException("Error reading attendance CSV: " + e.getMessage(), e);
        }
    }

    @Override
    public void saveAll(List<AttendanceRecord> records) {
        List<String[]> rows = new ArrayList<>();
        rows.add(HEADER);
        for (AttendanceRecord record : records) {
            rows.add(new String[] {
                    record.employeeId(),
                    record.lastName(),
                    record.firstName(),
                    record.dateText(),
                    record.logInText(),
                    record.logOutText()
            });
        }
        try {
            CsvUtils.writeAll(path, rows);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save attendance CSV: " + e.getMessage(), e);
        }
    }
}
