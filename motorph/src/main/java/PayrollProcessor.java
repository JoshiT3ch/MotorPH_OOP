public class PayrollProcessor {

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
        System.out.println("Hourly rate: PHP " + String.format("%.2f", payroll.hourlyRate()));
        System.out.println("Base pay: PHP " + String.format("%.2f", payroll.basePay()));
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
        double basePay = hourlyRate * hoursWorked;

        double originalBasicSalary = employee.getBasicSalary();
        double originalHoursWorked = employee.getHoursWorked();
        try {
            // Reuse the existing deduction logic by evaluating it against the hourly-derived monthly pay.
            employee.setBasicSalary(basePay);
            employee.setHoursWorked(hoursWorked);

            double grossPay = employee.grossSalary();
            double sssDeduction = employee.getSssDeduction();
            double philhealthDeduction = employee.getPhilhealthDeduction();
            double pagibigDeduction = employee.getPagibigDeduction();
            double taxDeduction = employee.getTaxDeduction();
            double totalDeductions = employee.calculateDeductions();
            double netPay = grossPay - totalDeductions;

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
        } finally {
            employee.setBasicSalary(originalBasicSalary);
            employee.setHoursWorked(originalHoursWorked);
        }
    }
}
