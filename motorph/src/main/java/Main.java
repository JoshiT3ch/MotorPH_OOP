

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
                if (nextLine.length < 9) continue;

                String employmentType = resolveEmploymentType(nextLine, headerIndexes);
                double basicSalary = readAmount(nextLine, headerIndexes, 11, "Basic Salary", 0.0);
                double hourlyRate = readAmount(nextLine, headerIndexes, -1, "Hourly Rate", deriveHourlyRate(basicSalary, employmentType));
                EmployeeFormData formData = new EmployeeFormData(
                        readText(nextLine, headerIndexes, 0, "Employee #"),
                        readText(nextLine, headerIndexes, 1, "Last Name"),
                        readText(nextLine, headerIndexes, 2, "First Name"),
                        readText(nextLine, headerIndexes, 3, "Birthday"),
                        readText(nextLine, headerIndexes, 4, "Address"),
                        readText(nextLine, headerIndexes, 5, "Phone Number"),
                        readText(nextLine, headerIndexes, 6, "SSS Number"),
                        readText(nextLine, headerIndexes, 7, "PhilHealth Number"),
                        readText(nextLine, headerIndexes, -1, "TIN Number"),
                        readText(nextLine, headerIndexes, 8, "PAG-IBIG Number"),
                        "contract".equals(employmentType) ? EmployeeFormData.CONTRACT : EmployeeFormData.FULL_TIME,
                        readText(nextLine, headerIndexes, 9, "Position"),
                        readText(nextLine, headerIndexes, 10, "Department"),
                        basicSalary,
                        readAmount(nextLine, headerIndexes, 12, "Rice Subsidy", 0.0),
                        readAmount(nextLine, headerIndexes, 13, "Phone Allowance", 0.0),
                        readAmount(nextLine, headerIndexes, 14, "Clothing Allowance", 0.0),
                        hourlyRate
                );
                Employee emp = formData.toEmployee();
                emp.setGrossSemiMonthlyRate(readAmount(nextLine, headerIndexes, -1, "Gross Semi-Monthly Rate", basicSalary / 2.0));
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

    private static String readText(String[] row, Map<String, Integer> headerIndexes, int legacyIndex, String... columnNames) {
        for (String columnName : columnNames) {
            Integer index = headerIndexes.get(columnName.toLowerCase());
            if (index != null && index >= 0 && index < row.length) {
                return safeTrim(row[index]);
            }
        }
        if (legacyIndex >= 0 && legacyIndex < row.length) {
            return safeTrim(row[legacyIndex]);
        }
        return "";
    }

    private static double readAmount(String[] row, Map<String, Integer> headerIndexes, int legacyIndex, String columnName, double fallbackValue) {
        String value = readText(row, headerIndexes, legacyIndex, columnName);
        return value.isEmpty() ? fallbackValue : parseDouble(value);
    }

    private static String resolveEmploymentType(String[] row, Map<String, Integer> headerIndexes) {
        String explicitEmploymentType = readText(row, headerIndexes, -1, "Employment Type");
        if (!explicitEmploymentType.isEmpty()) {
            return EmployeeFormData.normalizeEmploymentType(explicitEmploymentType);
        }

        String status = readText(row, headerIndexes, -1, "Status");
        if (!status.isEmpty()) {
            return EmployeeFormData.normalizeEmploymentType(status);
        }

        String position = readText(row, headerIndexes, 9, "Position");
        return position.toLowerCase().contains("contract") ? "contract" : "fulltime";
    }

    private static double deriveHourlyRate(double basicSalary, String employmentType) {
        if ("contract".equals(employmentType)) {
            return 0.0;
        }
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
