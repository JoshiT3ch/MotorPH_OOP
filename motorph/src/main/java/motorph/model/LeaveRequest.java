package motorph.model;

import java.time.LocalDate;

public record LeaveRequest(
        String requestId,
        String employeeId,
        String employeeName,
        String leaveType,
        LocalDate startDate,
        LocalDate endDate,
        String reason,
        String status,
        String approver,
        String remarks,
        LocalDate filedDate) {
}
