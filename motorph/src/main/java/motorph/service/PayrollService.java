package motorph.service;

import motorph.model.Employee;

public class PayrollService {

    public boolean isContractEmployee(Employee employee) {
        return employee != null && employee.isContractEmployee();
    }

    public PayrollComputation computePayroll(Employee employee, double hoursWorked) {
        try {
            Employee.loadContributionRates();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load contribution tables for payroll processing.", e);
        }

        double hourlyRate = employee.getHourlyRate();
        double basicSalary = employee.getBasicSalary();
        double computedHoursPay = hourlyRate * hoursWorked;
        double basePay = employee.calculateBasePayAmount(hoursWorked);
        double grossPay = employee.calculateGrossPay(hoursWorked);
        double allowancePay = employee.calculateAllowancePayAmount(hoursWorked);
        double sssDeduction = employee.calculateSssDeduction(hoursWorked);
        double philhealthDeduction = employee.calculatePhilhealthDeduction(hoursWorked);
        double pagibigDeduction = employee.calculatePagibigDeduction(hoursWorked);
        double taxDeduction = employee.calculateTaxDeduction(hoursWorked);
        double totalDeductions = employee.calculateDeductions(hoursWorked);
        double netPay = employee.calculateNetSalary(hoursWorked);

        return new PayrollComputation(employee, hoursWorked, hourlyRate, basicSalary, computedHoursPay, basePay, grossPay,
                allowancePay, sssDeduction, philhealthDeduction, pagibigDeduction, taxDeduction, totalDeductions, netPay);
    }

    public record PayrollComputation(
            Employee employee,
            double hoursWorked,
            double hourlyRate,
            double basicSalary,
            double computedHoursPay,
            double basePay,
            double grossPay,
            double allowancePay,
            double sssDeduction,
            double philhealthDeduction,
            double pagibigDeduction,
            double taxDeduction,
            double totalDeductions,
            double netPay) {
    }
}
