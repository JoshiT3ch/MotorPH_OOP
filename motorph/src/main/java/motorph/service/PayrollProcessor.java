package motorph.service;

import motorph.model.Employee;

public class PayrollProcessor {

    private final PayrollService payrollService = new PayrollService();

    public PayrollService.PayrollComputation computePayroll(Employee employee, double hoursWorked) {
        return payrollService.computePayroll(employee, hoursWorked);
    }

    public void processPayroll(Employee employee) {
        PayrollService.PayrollComputation payroll = computePayroll(employee, employee.getHoursWorked());
        System.out.println("Processing payroll for " + employee.getFullName());
        System.out.println("ID: " + employee.getId());
        System.out.println("Position: " + employee.getPosition());
        System.out.println("Hours worked: " + String.format("%.2f", payroll.hoursWorked()));
        System.out.println("Gross salary: PHP " + String.format("%.2f", payroll.grossPay()));
        System.out.println("Deductions: PHP " + String.format("%.2f", payroll.totalDeductions()));
        System.out.println("Net pay: PHP " + String.format("%.2f", payroll.netPay()));
        System.out.println("-----------------------------------------");
    }
}
