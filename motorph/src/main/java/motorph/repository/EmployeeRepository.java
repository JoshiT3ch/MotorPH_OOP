package motorph.repository;

import motorph.model.Employee;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository {
    List<Employee> findAll();
    Optional<Employee> findById(String employeeId);
    void saveAll(List<Employee> employees);
}
