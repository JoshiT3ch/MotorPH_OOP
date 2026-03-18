public class FullTimeEmployee extends Employee {

    public FullTimeEmployee(
            String id,
            String lastName,
            String firstName,
            String birthdate,
            String address,
            String phoneNumber,
            String sssNo,
            String philhealthNo,
            String tinNo,
            String pagibigNo,
            String status,
            String position,
            String supervisor,
            String department,
            double basicSalary,
            double riceSubsidy,
            double phoneAllowance,
            double clothingAllowance,
            double grossSemiMonthlyRate,
            double hourlyRate
    ) {
        super(id, lastName, firstName, birthdate, address, phoneNumber,
                sssNo, philhealthNo, tinNo, pagibigNo, status, position,
                supervisor, department, basicSalary, riceSubsidy, phoneAllowance,
                clothingAllowance, grossSemiMonthlyRate, hourlyRate);
    }

    @Override
    public double calculateDeductions() {
        // full-time employees pay all standard contributions
        return totalDeductions();
    }

    @Override
    public double calculateNetSalary() {
        // gross minus deductions
        return grossSalary() - calculateDeductions();
    }
}
