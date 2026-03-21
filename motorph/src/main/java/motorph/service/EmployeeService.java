package motorph.service;

import motorph.model.Employee;
import motorph.model.EmployeeFormData;
import motorph.repository.EmployeeRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final ValidationService validationService;

    public EmployeeService(EmployeeRepository employeeRepository, ValidationService validationService) {
        this.employeeRepository = employeeRepository;
        this.validationService = validationService;
    }

    public List<Employee> getAllEmployees() {
        return new ArrayList<>(employeeRepository.findAll());
    }

    public List<Employee> searchEmployees(String keyword) {
        String normalized = keyword == null ? "" : keyword.trim().toLowerCase();
        return getAllEmployees().stream()
                .filter(employee -> normalized.isEmpty()
                        || employee.getId().toLowerCase().contains(normalized)
                        || employee.getFirstName().toLowerCase().contains(normalized)
                        || employee.getLastName().toLowerCase().contains(normalized)
                        || employee.getPosition().toLowerCase().contains(normalized)
                        || employee.getDepartment().toLowerCase().contains(normalized))
                .toList();
    }

    public List<String> addEmployee(EmployeeFormData data) {
        List<Employee> employees = getAllEmployees();
        List<String> errors = validationService.validateEmployee(data, employees, false);
        if (!errors.isEmpty()) {
            return errors;
        }
        employees.add(data.toEmployee());
        employeeRepository.saveAll(employees);
        return List.of();
    }

    public List<String> updateEmployee(EmployeeFormData data) {
        List<Employee> employees = getAllEmployees();
        Optional<Employee> existing = employees.stream().filter(employee -> employee.getId().equals(data.id())).findFirst();
        if (existing.isEmpty()) {
            return List.of("Employee not found.");
        }
        List<String> errors = validationService.validateEmployee(data, List.of(), true);
        if (!errors.isEmpty()) {
            return errors;
        }
        int index = employees.indexOf(existing.get());
        employees.set(index, data.toEmployee());
        employeeRepository.saveAll(employees);
        return List.of();
    }

    public boolean archiveEmployee(String employeeId) {
        List<Employee> employees = getAllEmployees();
        Optional<Employee> existing = employees.stream().filter(employee -> employee.getId().equals(employeeId)).findFirst();
        if (existing.isEmpty()) {
            return false;
        }
        employees.remove(existing.get());
        employeeRepository.saveAll(employees);
        return true;
    }
}
