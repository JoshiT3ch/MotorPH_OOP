package motorph.service;

import motorph.model.Employee;
import motorph.model.UserAccount;
import motorph.repository.EmployeeRepository;
import motorph.repository.UserRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class AuthenticationService {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final ValidationService validationService;

    public AuthenticationService(UserRepository userRepository, EmployeeRepository employeeRepository, ValidationService validationService) {
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
        this.validationService = validationService;
    }

    public Optional<UserAccount> authenticate(String username, String password) {
        return userRepository.findAll().stream()
                .filter(account -> account.matchesCredentials(username.trim(), password.trim()))
                .findFirst();
    }

    public List<Employee> findEmployeesMissingAccounts() {
        List<Employee> employees = employeeRepository.findAll();
        List<UserAccount> accounts = userRepository.findAll();
        return employees.stream()
                .filter(employee -> accounts.stream().noneMatch(account -> account.isLinkedTo(employee.getId())))
                .sorted(Comparator.comparing(Employee::getId))
                .toList();
    }

    public List<UserAccount> generateDefaultAccountsForMissingEmployees(String defaultPasswordPrefix) {
        List<UserAccount> existingAccounts = new ArrayList<>(userRepository.findAll());
        List<Employee> employees = employeeRepository.findAll();
        List<UserAccount> created = new ArrayList<>();
        for (Employee employee : findEmployeesMissingAccounts()) {
            UserAccount account = new UserAccount(employee.getId(), defaultPasswordPrefix + employee.getId(), false, employee.getId());
            List<String> errors = validationService.validateUserAccount(account, existingAccounts, employees, false);
            if (errors.isEmpty()) {
                existingAccounts.add(account);
                created.add(account);
            }
        }
        if (!created.isEmpty()) {
            userRepository.saveAll(existingAccounts);
        }
        return created;
    }
}
