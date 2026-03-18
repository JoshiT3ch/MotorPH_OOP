

import com.opencsv.CSVReader;
import java.io.File;
import com.opencsv.CSVWriter;

import com.opencsv.exceptions.CsvValidationException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;


public class Main {

    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);

        List<Employee> employees = loadEmployees("data/EmployeeData.csv");
        System.out.println("Employee data loaded successfully!");

        // polymorphism demo: create a contract employee and process payroll via processor
        PayrollProcessor demoProc = new PayrollProcessor();
        ContractEmployee contractor = new ContractEmployee(
                "C001","Doe","Jane","01/01/1990","Some Address","123-456","","", "", "",
                "contract","Freelancer","","IT", 0.0,0.0,0.0,0.0,0.0,500.0);
        contractor.setHoursWorked(120); // e.g. 120 hours
        System.out.println("\n--- Demo payroll for a contract employee ---");
        demoProc.processPayroll(contractor);

        while (true) {
            System.out.println("\n=== MotorPH Payroll System ===");
            System.out.println("1. View Employee Details");
            System.out.println("2. Compute Payroll");
            System.out.println("3. Exit");
            System.out.print("Choose an option: ");
            String choice = input.nextLine().trim();

            switch (choice) {
                case "1":
                    viewEmployeeDetails(employees, input);
                    break;
                case "2":
                    computePayroll(employees, input);
                    break;
                case "3":
                    System.out.println("Exiting...");
                    input.close();
                    return;
                default:
                    System.out.println("Invalid option!");
            }
        }
    }

    // ---------------- Load Employees ----------------
    public static List<Employee> loadEmployees(String filePath) {
        List<Employee> list = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            String[] header = reader.readNext();
            if (header == null) {
                return list;
            }
            Map<String, Integer> headerIndexes = buildHeaderIndexMap(header);
            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                if (nextLine.length < 15) continue; // ensure core columns exist

                // choose subclass based on the position string; this makes it easy to
                // switch to contract employees without changing the CSV format.
                String position = safeTrim(nextLine[9]);
                boolean isContract = position.toLowerCase().contains("contract");
                double basicSalary = parseDouble(nextLine[11]);
                double grossSemiMonthlyRate = readOptionalAmount(nextLine, headerIndexes, "Gross Semi-Monthly Rate", basicSalary / 2.0);
                double hourlyRate = readOptionalAmount(nextLine, headerIndexes, "Hourly Rate", deriveHourlyRate(basicSalary));
                Employee emp;
                if (isContract) {
                    emp = new ContractEmployee(
                            safeTrim(nextLine[0]), // ID
                            safeTrim(nextLine[1]), // Last Name
                            safeTrim(nextLine[2]), // First Name
                            safeTrim(nextLine[3]), // Birthday
                            safeTrim(nextLine[4]), // Address
                            safeTrim(nextLine[5]), // Phone Number
                            safeTrim(nextLine[6]), // SSS #
                            safeTrim(nextLine[7]), // Philhealth #
                            "",               // TIN #
                            safeTrim(nextLine[8]), // Pag-ibig #
                            "contract",        // Status
                            position,
                            "",               // Supervisor
                            safeTrim(nextLine[10]), // Department
                            basicSalary,
                            parseDouble(nextLine[12]),
                            parseDouble(nextLine[13]),
                            parseDouble(nextLine[14]),
                            grossSemiMonthlyRate,
                            hourlyRate
                    );
                } else {
                    emp = new FullTimeEmployee(
                            safeTrim(nextLine[0]), // ID
                            safeTrim(nextLine[1]), // Last Name
                            safeTrim(nextLine[2]), // First Name
                            safeTrim(nextLine[3]), // Birthday
                            safeTrim(nextLine[4]), // Address
                            safeTrim(nextLine[5]), // Phone Number
                            safeTrim(nextLine[6]), // SSS #
                            safeTrim(nextLine[7]), // Philhealth #
                            "",               // TIN #
                            safeTrim(nextLine[8]), // Pag-ibig #
                            "fulltime",        // Status
                            position,
                            "",               // Supervisor
                            safeTrim(nextLine[10]), // Department
                            basicSalary,
                            parseDouble(nextLine[12]),
                            parseDouble(nextLine[13]),
                            parseDouble(nextLine[14]),
                            grossSemiMonthlyRate,
                            hourlyRate
                    );
                }
                list.add(emp);
            }
        } catch (IOException | CsvValidationException e) {
            System.out.println("Error reading CSV: " + e.getMessage());
        }
        return list;
    }

    private static Map<String, Integer> buildHeaderIndexMap(String[] header) {
        Map<String, Integer> indexes = new HashMap<>();
        for (int i = 0; i < header.length; i++) {
            indexes.put(safeTrim(header[i]).toLowerCase(), i);
        }
        return indexes;
    }

    private static double readOptionalAmount(String[] row, Map<String, Integer> headerIndexes, String columnName, double fallbackValue) {
        Integer index = headerIndexes.get(columnName.toLowerCase());
        if (index == null || index < 0 || index >= row.length) {
            return fallbackValue;
        }

        String value = safeTrim(row[index]);
        return value.isEmpty() ? fallbackValue : parseDouble(value);
    }

    private static double deriveHourlyRate(double basicSalary) {
        return basicSalary <= 0 ? 0.0 : basicSalary / 168.0;
    }

    private static double parseDouble(String value) {
        String normalized = safeTrim(value).replace(",", "");
        return normalized.isEmpty() ? 0.0 : Double.parseDouble(normalized);
    }

    private static String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    // ---------------- View Employee ----------------
    public static void viewEmployeeDetails(List<Employee> employees, Scanner input) {
        System.out.print("Enter Employee ID: ");
        String id = input.nextLine().trim();
        for (Employee emp : employees) {
            if (emp.getId().equals(id)) {
                System.out.println("\nEmployee Details:");
                System.out.println("ID: " + emp.getId());
                System.out.println("Name: " + emp.getFullName());
                System.out.println("Basic Salary: PHP " + emp.getBasicSalary());
                System.out.println("Hours Worked: " + emp.getHoursWorked());
                return;
            }
        }
        System.out.println("Employee not found!");
    }

    // ---------------- Compute Payroll ----------------
    public static void computePayroll(List<Employee> employees, Scanner input) {
        System.out.print("Enter Employee ID or 'all' for all employees: ");
        String choice = input.nextLine().trim();

        PayrollProcessor processor = new PayrollProcessor();
        if (choice.equalsIgnoreCase("all")) {
            for (Employee emp : employees) {
                processor.processPayroll(emp);
            }
        } else {
            for (Employee emp : employees) {
                if (emp.getId().equals(choice)) {
                    processor.processPayroll(emp);
                    return;
                }
            }
            System.out.println("Employee not found!");
        }
    }
}
