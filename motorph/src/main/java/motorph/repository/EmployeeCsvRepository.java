package motorph.repository;

import com.opencsv.exceptions.CsvValidationException;
import motorph.model.Employee;
import motorph.model.EmployeeFormData;
import motorph.util.CsvUtils;
import motorph.util.FilePathResolver;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class EmployeeCsvRepository implements EmployeeRepository {

    private static final Charset EMPLOYEE_CSV_CHARSET = Charset.forName("cp1252");
    private static final String FILE_NAME = "EmployeeData.csv";
    private static final String[] DEFAULT_HEADER = new String[] {
            "Employee #", "Last Name", "First Name", "Birthday", "Address",
            "Phone Number", "SSS Number", "PhilHealth Number", "PAG-IBIG Number",
            "Position", "Department", "Basic Salary", "Rice Subsidy", "Phone Allowance",
            "Clothing Allowance", "Gross Semi-Monthly Rate", "Hourly Rate"
    };

    private final Path path;
    private String[] lastKnownHeader = DEFAULT_HEADER.clone();

    public EmployeeCsvRepository() {
        this.path = FilePathResolver.resolveDataPath(FILE_NAME);
    }

    @Override
    public List<Employee> findAll() {
        List<Employee> employees = new ArrayList<>();
        try {
            List<String[]> rows = CsvUtils.readAll(path, EMPLOYEE_CSV_CHARSET);
            if (rows.isEmpty()) {
                return employees;
            }

            String[] header = rows.getFirst();
            lastKnownHeader = header;
            Map<String, Integer> headerIndexes = CsvUtils.buildHeaderIndexMap(header);

            for (int i = 1; i < rows.size(); i++) {
                String[] row = rows.get(i);
                if (row.length < 3) {
                    continue;
                }

                String employmentType = resolveEmploymentType(row, headerIndexes);
                double basicSalary = CsvUtils.readAmount(row, headerIndexes, 11, "Basic Salary", 0.0);
                double hourlyRate = CsvUtils.readAmount(row, headerIndexes, -1, "Hourly Rate", deriveHourlyRate(basicSalary, employmentType));
                EmployeeFormData formData = new EmployeeFormData(
                        CsvUtils.readText(row, headerIndexes, 0, "Employee #"),
                        CsvUtils.readText(row, headerIndexes, 1, "Last Name"),
                        CsvUtils.readText(row, headerIndexes, 2, "First Name"),
                        CsvUtils.readText(row, headerIndexes, 3, "Birthday"),
                        CsvUtils.readText(row, headerIndexes, 4, "Address"),
                        CsvUtils.readText(row, headerIndexes, 5, "Phone Number"),
                        CsvUtils.readText(row, headerIndexes, 6, "SSS Number"),
                        CsvUtils.readText(row, headerIndexes, 7, "PhilHealth Number"),
                        CsvUtils.readText(row, headerIndexes, -1, "TIN Number"),
                        CsvUtils.readText(row, headerIndexes, 8, "PAG-IBIG Number"),
                        "contract".equals(employmentType) ? EmployeeFormData.CONTRACT : EmployeeFormData.FULL_TIME,
                        CsvUtils.readText(row, headerIndexes, 9, "Position"),
                        CsvUtils.readText(row, headerIndexes, 10, "Department"),
                        basicSalary,
                        CsvUtils.readAmount(row, headerIndexes, 12, "Rice Subsidy", 0.0),
                        CsvUtils.readAmount(row, headerIndexes, 13, "Phone Allowance", 0.0),
                        CsvUtils.readAmount(row, headerIndexes, 14, "Clothing Allowance", 0.0),
                        hourlyRate
                );
                Employee employee = formData.toEmployee();
                employee.setGrossSemiMonthlyRate(CsvUtils.readAmount(row, headerIndexes, -1, "Gross Semi-Monthly Rate", basicSalary / 2.0));
                employees.add(employee);
            }
            return employees;
        } catch (IOException | CsvValidationException e) {
            throw new IllegalStateException("Error reading employee CSV: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Employee> findById(String employeeId) {
        return findAll().stream().filter(employee -> employee.getId().equals(employeeId)).findFirst();
    }

    @Override
    public void saveAll(List<Employee> employees) {
        List<String[]> rows = new ArrayList<>();
        String[] header = lastKnownHeader == null || lastKnownHeader.length == 0 ? DEFAULT_HEADER : lastKnownHeader;
        rows.add(header);
        for (Employee employee : employees) {
            rows.add(toRow(employee, header));
        }
        try {
            CsvUtils.writeAll(path, EMPLOYEE_CSV_CHARSET, rows);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save employee CSV: " + e.getMessage(), e);
        }
    }

    private String[] toRow(Employee employee, String[] header) {
        String[] row = new String[header.length];
        Map<String, Integer> headerIndexes = CsvUtils.buildHeaderIndexMap(header);
        writeValue(row, headerIndexes, "employee #", employee.getId());
        writeValue(row, headerIndexes, "last name", employee.getLastName());
        writeValue(row, headerIndexes, "first name", employee.getFirstName());
        writeValue(row, headerIndexes, "birthday", employee.getBirthdate());
        writeValue(row, headerIndexes, "address", employee.getAddress());
        writeValue(row, headerIndexes, "phone number", employee.getPhoneNumber());
        writeValue(row, headerIndexes, "sss number", employee.getSssNo());
        writeValue(row, headerIndexes, "philhealth number", employee.getPhilhealthNo());
        writeValue(row, headerIndexes, "tin number", employee.getTinNo());
        writeValue(row, headerIndexes, "pag-ibig number", employee.getPagibigNo());
        writeValue(row, headerIndexes, "employment type", employee.getEmploymentType());
        writeValue(row, headerIndexes, "status", employee.getEmploymentType());
        writeValue(row, headerIndexes, "position", employee.getPosition());
        writeValue(row, headerIndexes, "department", employee.getDepartment());
        writeValue(row, headerIndexes, "basic salary", String.valueOf(employee.getBasicSalary()));
        writeValue(row, headerIndexes, "rice subsidy", String.valueOf(employee.getRiceSubsidy()));
        writeValue(row, headerIndexes, "phone allowance", String.valueOf(employee.getPhoneAllowance()));
        writeValue(row, headerIndexes, "clothing allowance", String.valueOf(employee.getClothingAllowance()));
        writeValue(row, headerIndexes, "gross semi-monthly rate", String.valueOf(employee.getGrossSemiMonthlyRate()));
        writeValue(row, headerIndexes, "hourly rate", String.valueOf(employee.getHourlyRate()));
        for (int i = 0; i < row.length; i++) {
            if (row[i] == null) {
                row[i] = "";
            }
        }
        return row;
    }

    private void writeValue(String[] row, Map<String, Integer> headerIndexes, String columnName, String value) {
        Integer index = headerIndexes.get(columnName);
        if (index != null && index >= 0 && index < row.length) {
            row[index] = value == null ? "" : value;
        }
    }

    private String resolveEmploymentType(String[] row, Map<String, Integer> headerIndexes) {
        String explicitEmploymentType = CsvUtils.readText(row, headerIndexes, -1, "Employment Type");
        if (!explicitEmploymentType.isEmpty()) {
            return EmployeeFormData.normalizeEmploymentType(explicitEmploymentType);
        }
        String status = CsvUtils.readText(row, headerIndexes, -1, "Status");
        if (!status.isEmpty()) {
            return EmployeeFormData.normalizeEmploymentType(status);
        }
        String position = CsvUtils.readText(row, headerIndexes, 9, "Position");
        return position.toLowerCase().contains("contract") ? "contract" : "fulltime";
    }

    private double deriveHourlyRate(double basicSalary, String employmentType) {
        if ("contract".equals(employmentType)) {
            return 0.0;
        }
        return basicSalary <= 0.0 ? 0.0 : basicSalary / 168.0;
    }
}
