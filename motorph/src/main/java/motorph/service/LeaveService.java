package motorph.service;

import motorph.model.Employee;
import motorph.model.LeaveRequest;
import motorph.repository.EmployeeRepository;
import motorph.repository.LeaveRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class LeaveService {

    public static final List<String> VALID_STATUSES = List.of("Pending", "Approved", "Rejected");

    private final LeaveRepository leaveRepository;
    private final EmployeeRepository employeeRepository;
    private final ValidationService validationService;

    public LeaveService(LeaveRepository leaveRepository, EmployeeRepository employeeRepository, ValidationService validationService) {
        this.leaveRepository = leaveRepository;
        this.employeeRepository = employeeRepository;
        this.validationService = validationService;
    }

    public List<LeaveRequest> getAllRequests() {
        return new ArrayList<>(leaveRepository.findAll());
    }

    public List<LeaveRequest> getRequestsForEmployee(String employeeId) {
        return getAllRequests().stream()
                .filter(request -> request.belongsTo(employeeId))
                .sorted(Comparator.comparing(LeaveRequest::filedDate, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();
    }

    public List<String> submitLeave(Employee employee, String leaveType, LocalDate startDate, LocalDate endDate, String reason) {
        List<LeaveRequest> requests = getAllRequests();
        LeaveRequest request = new LeaveRequest(
                "LR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                employee.getId(),
                employee.getFullName(),
                leaveType,
                startDate,
                endDate,
                reason,
                "Pending",
                "",
                "",
                LocalDate.now());
        List<String> errors = validationService.validateLeaveRequest(request, employeeRepository.findAll());
        if (!errors.isEmpty()) {
            return errors;
        }
        requests.add(request);
        leaveRepository.saveAll(requests);
        return List.of();
    }

    public List<String> updateLeaveStatus(String requestId, String status, String approver, String remarks) {
        List<LeaveRequest> requests = getAllRequests();
        Optional<LeaveRequest> existing = requests.stream().filter(request -> request.requestId().equals(requestId)).findFirst();
        if (existing.isEmpty()) {
            return List.of("Leave request not found.");
        }
        String normalizedStatus = status == null ? "" : status.trim();
        if (!VALID_STATUSES.contains(normalizedStatus)) {
            return List.of("Choose a valid leave decision before updating.");
        }
        String normalizedRemarks = remarks == null ? "" : remarks.trim();
        if ("Rejected".equalsIgnoreCase(normalizedStatus) && normalizedRemarks.isBlank()) {
            return List.of("Please provide remarks when rejecting a leave request.");
        }
        LeaveRequest updated = existing.get().withDecision(normalizedStatus, approver, normalizedRemarks);
        List<String> errors = validationService.validateLeaveRequest(updated, employeeRepository.findAll());
        if (!errors.isEmpty()) {
            return errors;
        }
        int index = requests.indexOf(existing.get());
        requests.set(index, updated);
        leaveRepository.saveAll(requests);
        return List.of();
    }
}
