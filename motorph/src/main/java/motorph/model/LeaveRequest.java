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

    public LeaveRequest {
        requestId = normalizeText(requestId);
        employeeId = normalizeText(employeeId);
        employeeName = normalizeText(employeeName);
        leaveType = normalizeText(leaveType);
        reason = normalizeText(reason);
        status = normalizeStatus(status);
        approver = normalizeText(approver);
        remarks = normalizeText(remarks);
    }

    public boolean belongsTo(String candidateEmployeeId) {
        return employeeId.equals(normalizeText(candidateEmployeeId));
    }

    public LeaveRequest withDecision(String updatedStatus, String updatedApprover, String updatedRemarks) {
        return new LeaveRequest(
                requestId,
                employeeId,
                employeeName,
                leaveType,
                startDate,
                endDate,
                reason,
                updatedStatus,
                updatedApprover,
                updatedRemarks,
                filedDate);
    }

    private static String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalizeStatus(String value) {
        String normalized = normalizeText(value);
        return normalized.isEmpty() ? "Pending" : normalized;
    }
}
