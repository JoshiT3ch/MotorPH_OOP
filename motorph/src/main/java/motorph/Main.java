package motorph;

import motorph.model.Employee;
import motorph.repository.EmployeeCsvRepository;
import motorph.service.PayrollProcessor;

import java.util.List;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);
        List<Employee> employees = new EmployeeCsvRepository().findAll();
        PayrollProcessor processor = new PayrollProcessor();

        while (true) {
            System.out.println("\n=== MotorPH Payroll System ===");
            System.out.println("1. View Employee Details");
            System.out.println("2. Compute Payroll");
            System.out.println("3. Exit");
            System.out.print("Choose an option: ");
            String choice = input.nextLine().trim();

            switch (choice) {
                case "1" -> viewEmployeeDetails(employees, input);
                case "2" -> computePayroll(employees, processor, input);
                case "3" -> {
                    input.close();
                    return;
                }
                default -> System.out.println("Invalid option!");
            }
        }
    }

    private static void viewEmployeeDetails(List<Employee> employees, Scanner input) {
        System.out.print("Enter Employee ID: ");
        String id = input.nextLine().trim();
        employees.stream()
                .filter(employee -> employee.getId().equals(id))
                .findFirst()
                .ifPresentOrElse(employee -> {
                    System.out.println("\nEmployee Details:");
                    System.out.println("ID: " + employee.getId());
                    System.out.println("Name: " + employee.getFullName());
                    System.out.println("Basic Salary: PHP " + employee.getBasicSalary());
                }, () -> System.out.println("Employee not found!"));
    }

    private static void computePayroll(List<Employee> employees, PayrollProcessor processor, Scanner input) {
        System.out.print("Enter Employee ID: ");
        String id = input.nextLine().trim();
        employees.stream()
                .filter(employee -> employee.getId().equals(id))
                .findFirst()
                .ifPresentOrElse(processor::processPayroll, () -> System.out.println("Employee not found!"));
    }
}
