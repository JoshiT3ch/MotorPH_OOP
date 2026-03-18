public class PayrollSssSmokeTest {
    public static void main(String[] args) throws Exception {
        Employee.loadContributionRates();
        Employee employee = new FullTimeEmployee(
            "10001", "Doe", "Jane", "1990-01-01", "Address", "0900",
            "SSS", "PH", "TIN", "PAGIBIG", "fulltime", "Developer", "Boss", "IT",
            3000.0, 0.0, 0.0, 0.0, 0.0, 0.0
        );
        System.out.println("SSS@3000=" + employee.getSssDeduction());
        employee.setBasicSalary(3250.0);
        System.out.println("SSS@3250=" + employee.getSssDeduction());
        employee.setBasicSalary(24750.0);
        System.out.println("SSS@24750=" + employee.getSssDeduction());
    }
}
