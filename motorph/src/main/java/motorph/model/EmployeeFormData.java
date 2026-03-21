package motorph.model;

public record EmployeeFormData(
        String id,
        String lastName,
        String firstName,
        String birthdate,
        String address,
        String phoneNumber,
        String sssNumber,
        String philhealthNumber,
        String tinNumber,
        String pagibigNumber,
        String employmentType,
        String position,
        String department,
        double basicSalary,
        double riceSubsidy,
        double phoneAllowance,
        double clothingAllowance,
        double hourlyRate
) {

    public static final String FULL_TIME = "Full Time";
    public static final String CONTRACT = "Contract";

    public Employee toEmployee() {
        String normalizedEmploymentType = normalizeEmploymentType(employmentType);
        double computedHourlyRate = hourlyRate > 0.0 ? hourlyRate : deriveHourlyRate(basicSalary, normalizedEmploymentType);
        double grossSemiMonthlyRate = "contract".equals(normalizedEmploymentType) ? 0.0 : (basicSalary / 2.0);
        if ("contract".equals(normalizedEmploymentType)) {
            return new ContractEmployee(
                    id, lastName, firstName, birthdate, address, phoneNumber,
                    sssNumber, philhealthNumber, tinNumber, pagibigNumber,
                    normalizedEmploymentType, position, "", department, basicSalary,
                    riceSubsidy, phoneAllowance, clothingAllowance, grossSemiMonthlyRate, computedHourlyRate);
        }
        return new FullTimeEmployee(
                id, lastName, firstName, birthdate, address, phoneNumber,
                sssNumber, philhealthNumber, tinNumber, pagibigNumber,
                normalizedEmploymentType, position, "", department, basicSalary,
                riceSubsidy, phoneAllowance, clothingAllowance, grossSemiMonthlyRate, computedHourlyRate);
    }

    public static EmployeeFormData from(Employee emp) {
        return new EmployeeFormData(
                emp.getId(),
                emp.getLastName(),
                emp.getFirstName(),
                emp.getBirthdate(),
                emp.getAddress(),
                emp.getPhoneNumber(),
                emp.getSssNo(),
                emp.getPhilhealthNo(),
                emp.getTinNo(),
                emp.getPagibigNo(),
                displayEmploymentType(emp.getEmploymentType()),
                emp.getPosition(),
                emp.getDepartment(),
                emp.getBasicSalary(),
                emp.getRiceSubsidy(),
                emp.getPhoneAllowance(),
                emp.getClothingAllowance(),
                emp.getHourlyRate()
        );
    }

    public static String normalizeEmploymentType(String rawValue) {
        return CONTRACT.equalsIgnoreCase(rawValue) || "contract".equalsIgnoreCase(rawValue) ? "contract" : "fulltime";
    }

    public static String displayEmploymentType(String rawValue) {
        return "contract".equalsIgnoreCase(rawValue) ? CONTRACT : FULL_TIME;
    }

    public double grossSemiMonthlyRate() {
        return "contract".equals(normalizeEmploymentType(employmentType)) ? 0.0 : basicSalary / 2.0;
    }

    private static double deriveHourlyRate(double basicSalary, String employmentType) {
        if ("contract".equals(employmentType)) {
            return 0.0;
        }
        return basicSalary <= 0.0 ? 0.0 : basicSalary / 168.0;
    }
}
