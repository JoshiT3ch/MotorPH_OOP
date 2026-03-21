package motorph.model;

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
    protected double calculateBasePay(double hoursWorked) {
        double computedHoursPay = getHourlyRate() * hoursWorked;
        double fallbackBase = getBasicSalary() / 2.0;
        return computedHoursPay > 0 ? computedHoursPay : fallbackBase;
    }

    @Override
    public double calculateDeductions(double hoursWorked) {
        return calculateMandatoryDeductionsBeforeTax(hoursWorked) + calculateTaxDeduction(hoursWorked);
    }

    @Override
    public double calculateNetSalary(double hoursWorked) {
        return calculateGrossPay(hoursWorked) - calculateDeductions(hoursWorked);
    }
}
