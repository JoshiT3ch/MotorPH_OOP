package motorph.repository;

import com.opencsv.exceptions.CsvValidationException;
import motorph.model.LeaveRequest;
import motorph.util.CsvUtils;
import motorph.util.FilePathResolver;
import motorph.util.InputValidators;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class LeaveCsvRepository implements LeaveRepository {

    private static final String FILE_NAME = "LeaveRequests.csv";
    private static final String[] HEADER = {
            "Request ID", "Employee #", "Employee Name", "Leave Type", "Start Date",
            "End Date", "Reason", "Status", "Approver", "Remarks", "Filed Date"
    };
    private final Path path;

    public LeaveCsvRepository() {
        this.path = FilePathResolver.resolveDataPath(FILE_NAME);
    }

    @Override
    public List<LeaveRequest> findAll() {
        try {
            List<String[]> rows = CsvUtils.readAll(path);
            if (rows.isEmpty()) {
                saveAll(List.of());
                return List.of();
            }
            List<LeaveRequest> requests = new ArrayList<>();
            for (int i = 1; i < rows.size(); i++) {
                String[] row = rows.get(i);
                if (row.length < 7) {
                    continue;
                }
                requests.add(new LeaveRequest(
                        safe(row, 0),
                        safe(row, 1),
                        safe(row, 2),
                        safe(row, 3),
                        InputValidators.parseDate(safe(row, 4)).orElse(null),
                        InputValidators.parseDate(safe(row, 5)).orElse(null),
                        safe(row, 6),
                        row.length > 7 ? safe(row, 7) : "Pending",
                        row.length > 8 ? safe(row, 8) : "",
                        row.length > 9 ? safe(row, 9) : "",
                        row.length > 10 ? InputValidators.parseDate(safe(row, 10)).orElse(null) : null
                ));
            }
            return requests;
        } catch (IOException | CsvValidationException e) {
            throw new IllegalStateException("Error reading leave CSV: " + e.getMessage(), e);
        }
    }

    @Override
    public void saveAll(List<LeaveRequest> requests) {
        List<String[]> rows = new ArrayList<>();
        rows.add(HEADER);
        for (LeaveRequest request : requests) {
            rows.add(new String[] {
                    request.requestId(),
                    request.employeeId(),
                    request.employeeName(),
                    request.leaveType(),
                    formatDate(request.startDate()),
                    formatDate(request.endDate()),
                    request.reason(),
                    request.status(),
                    request.approver(),
                    request.remarks(),
                    formatDate(request.filedDate())
            });
        }
        try {
            CsvUtils.writeAll(path, rows);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save leave CSV: " + e.getMessage(), e);
        }
    }

    private String safe(String[] row, int index) {
        return index >= 0 && index < row.length ? CsvUtils.safeTrim(row[index]) : "";
    }

    private String formatDate(LocalDate value) {
        return value == null ? "" : value.toString();
    }
}
