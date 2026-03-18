public class HourlyPayrollSmokeTest {
    public static void main(String[] args) {
        Employee employee = new FullTimeEmployee(
            "10001", "Doe", "Jane", "1990-01-01", "Address", "0900",
            "SSS", "PH", "TIN", "PAGIBIG", "fulltime", "Developer", "Boss", "IT",
            30000.0, 1500.0, 1000.0, 500.0, 0.0, 200.0
        );
        PayrollProcessor.PayrollComputation payroll = new PayrollProcessor().computePayroll(employee, 80.0);
        System.out.println("hours=" + payroll.hoursWorked());
        System.out.println("rate=" + payroll.hourlyRate());
        System.out.println("base=" + payroll.basePay());
        System.out.println("gross=" + payroll.grossPay());
        System.out.println("sss=" + payroll.sssDeduction());
        System.out.println("net=" + payroll.netPay());
        System.out.println("basicRestored=" + employee.getBasicSalary());
    }
}
