public class PayrollProcessor {

    public static boolean isContractEmployee(Employee employee) {
        return employee instanceof ContractEmployee
                || "contract".equalsIgnoreCase(employee.getEmploymentType());
    }

    public record PayrollComputation(
            Employee employee,
            double hoursWorked,
            double hourlyRate,
            double basePay,
            double grossPay,
            double sssDeduction,
            double philhealthDeduction,
            double pagibigDeduction,
            double taxDeduction,
            double totalDeductions,
            double netPay) {
    }

    /**
     * Keeps the existing CLI entrypoint working by using the employee's current hours.
     */
    public void processPayroll(Employee employee) {
        PayrollComputation payroll = computePayroll(employee, employee.getHoursWorked());

        System.out.println("Processing payroll for " + employee.getFullName());
        System.out.println("ID: " + employee.getId());
        System.out.println("Position: " + employee.getPosition());
        System.out.println("Hours worked: " + String.format("%.2f", payroll.hoursWorked()));
        if (isContractEmployee(employee)) {
            System.out.println("Hourly rate: PHP " + String.format("%.2f", payroll.hourlyRate()));
            System.out.println("Hourly pay: PHP " + String.format("%.2f", payroll.basePay()));
        } else {
            System.out.println("Basic salary: PHP " + String.format("%.2f", payroll.basePay()));
        }
        System.out.println("Gross salary: PHP " + String.format("%.2f", payroll.grossPay()));
        System.out.println("SSS deduction: PHP " + String.format("%.2f", payroll.sssDeduction()));
        System.out.println("Deductions: PHP " + String.format("%.2f", payroll.totalDeductions()));
        System.out.println("Net pay: PHP " + String.format("%.2f", payroll.netPay()));
        System.out.println("-----------------------------------------");
    }

    public PayrollComputation computePayroll(Employee employee, double hoursWorked) {
        try {
            Employee.loadContributionRates();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load contribution tables for payroll processing.", e);
        }

        double hourlyRate = employee.getHourlyRate();
        double basePay = isContractEmployee(employee)
                ? hourlyRate * hoursWorked
                : employee.getBasicSalary();
        double grossPay = employee.calculateGrossPay(hoursWorked);
        double sssDeduction = employee.calculateSssDeduction(hoursWorked);
        double philhealthDeduction = employee.calculatePhilhealthDeduction(hoursWorked);
        double pagibigDeduction = employee.calculatePagibigDeduction(hoursWorked);
        double taxDeduction = employee.calculateTaxDeduction(hoursWorked);
        double totalDeductions = employee.calculateDeductions(hoursWorked);
        double netPay = employee.calculateNetSalary(hoursWorked);

        return new PayrollComputation(
                employee,
                hoursWorked,
                hourlyRate,
                basePay,
                grossPay,
                sssDeduction,
                philhealthDeduction,
                pagibigDeduction,
                taxDeduction,
                totalDeductions,
                netPay);
    }
}
