public class ContractEmployee extends Employee {

    public ContractEmployee(
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
        // contract employees may only pay tax, for example
        return taxDeduction();
    }

    @Override
    public double calculateNetSalary() {
        // calculate based on hours worked and hourly rate instead of basic salary
        double gross = getHourlyRate() * getHoursWorked();
        return gross - calculateDeductions();
    }
}
