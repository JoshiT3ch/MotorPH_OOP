package motorph.service;

import motorph.model.AttendanceRecord;
import motorph.model.Employee;
import motorph.model.EmployeeFormData;
import motorph.model.LeaveRequest;
import motorph.model.UserAccount;
import motorph.util.InputValidators;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ValidationService {

    public List<String> validateEmployee(EmployeeFormData data, Collection<Employee> existingEmployees, boolean isUpdate) {
        List<String> errors = new ArrayList<>();
        if (InputValidators.isBlank(data.id())) {
            errors.add("Employee number is required.");
        }
        if (InputValidators.isBlank(data.firstName())) {
            errors.add("First name is required.");
        } else if (!InputValidators.isValidName(data.firstName())) {
            errors.add("First name may only contain letters, spaces, apostrophe, and hyphen.");
        }
        if (InputValidators.isBlank(data.lastName())) {
            errors.add("Last name is required.");
        } else if (!InputValidators.isValidName(data.lastName())) {
            errors.add("Last name may only contain letters, spaces, apostrophe, and hyphen.");
        }
        if (!InputValidators.isValidBirthday(data.birthdate())) {
            errors.add("Birthday must use MM/DD/YYYY format.");
        }
        if (!InputValidators.isValidPhone(data.phoneNumber())) {
            errors.add("Phone number must use 09XX-XXX-XXXX or 11-digit mobile format.");
        }
        if (!InputValidators.isValidSssNumber(data.sssNumber())) {
            errors.add("SSS number must use XX-XXXXXXX-X format.");
        }
        if (!InputValidators.isValidPhilhealthNumber(data.philhealthNumber())) {
            errors.add("PhilHealth number must contain 12 digits.");
        }
        if (!InputValidators.isValidPagibigNumber(data.pagibigNumber())) {
            errors.add("Pag-IBIG number must contain 12 digits.");
        }
        if (!InputValidators.isValidTinNumber(data.tinNumber())) {
            errors.add("TIN number must use XXX-XXX-XXX-XXX format.");
        }
        if (!InputValidators.isNonNegative(data.basicSalary())) {
            errors.add("Basic salary cannot be negative.");
        }
        if (!InputValidators.isNonNegative(data.riceSubsidy())) {
            errors.add("Rice subsidy cannot be negative.");
        }
        if (!InputValidators.isNonNegative(data.phoneAllowance())) {
            errors.add("Phone allowance cannot be negative.");
        }
        if (!InputValidators.isNonNegative(data.clothingAllowance())) {
            errors.add("Clothing allowance cannot be negative.");
        }
        if (!InputValidators.isNonNegative(data.hourlyRate())) {
            errors.add("Hourly rate cannot be negative.");
        }
        boolean duplicateEmployee = existingEmployees.stream()
                .anyMatch(employee -> employee.getId().equals(data.id()));
        if (!isUpdate && duplicateEmployee) {
            errors.add("Employee number must be unique.");
        }
        return errors;
    }

    public List<String> validateUserAccount(UserAccount account, Collection<UserAccount> existingAccounts, Collection<Employee> employees, boolean isUpdate) {
        List<String> errors = new ArrayList<>();
        if (InputValidators.isBlank(account.username())) {
            errors.add("Username is required.");
        }
        if (InputValidators.isBlank(account.password())) {
            errors.add("Password is required.");
        }
        if (!account.isAdmin() && InputValidators.isBlank(account.employeeId())) {
            errors.add("Employee-linked accounts must have an employee number.");
        }
        if (!account.isAdmin() && employees.stream().noneMatch(employee -> employee.getId().equals(account.employeeId()))) {
            errors.add("Linked employee does not exist.");
        }
        boolean duplicateUser = existingAccounts.stream().anyMatch(existing -> existing.username().equalsIgnoreCase(account.username()));
        if (!isUpdate && duplicateUser) {
            errors.add("Username must be unique.");
        }
        return errors;
    }

    public List<String> validateAttendance(AttendanceRecord record, Collection<Employee> employees) {
        List<String> errors = new ArrayList<>();
        if (employees.stream().noneMatch(employee -> employee.getId().equals(record.employeeId()))) {
            errors.add("Attendance employee does not exist.");
        }
        if (record.date() == null) {
            errors.add("Attendance date is invalid.");
        }
        if (record.logIn() == null && record.logOut() != null) {
            errors.add("Clock in is required before clock out.");
        }
        if (record.hoursWorked() < 0) {
            errors.add("Hours worked cannot be negative.");
        }
        return errors;
    }

    public List<String> validateLeaveRequest(LeaveRequest request, Collection<Employee> employees) {
        List<String> errors = new ArrayList<>();
        if (employees.stream().noneMatch(employee -> employee.getId().equals(request.employeeId()))) {
            errors.add("Leave request employee does not exist.");
        }
        if (InputValidators.isBlank(request.leaveType())) {
            errors.add("Leave type is required.");
        }
        if (request.startDate() == null || request.endDate() == null) {
            errors.add("Leave date range is invalid.");
        } else if (request.endDate().isBefore(request.startDate())) {
            errors.add("Leave end date cannot be before start date.");
        }
        if (InputValidators.isBlank(request.reason())) {
            errors.add("Leave reason is required.");
        }
        if (InputValidators.isBlank(request.status())) {
            errors.add("Leave status is required.");
        } else if (!LeaveService.VALID_STATUSES.contains(request.status().trim())) {
            errors.add("Leave status must be Pending, Approved, or Rejected.");
        }
        if ("Rejected".equalsIgnoreCase(request.status()) && InputValidators.isBlank(request.remarks())) {
            errors.add("Please provide remarks when rejecting a leave request.");
        }
        return errors;
    }
}
