public class EmployeeFormDialog {

    public static EmployeeFormData show(javafx.stage.Stage owner, String title, EmployeeFormData initialData) {
        return EmployeeFormData.show(owner, title, initialData);
    }

    public String id;
    public String lastName;
    public String firstName;
    public String birthdate;
    public String address;
    public String phoneNumber;
    public String sssNumber;
    public String philhealthNumber;
    public String pagibigNumber;
    public String position;
    public String department;

    // New fields for salary and allowances
    public double basicSalary;
    public double riceSubsidy;
    public double phoneAllowance;
    public double clothingAllowance;

    public EmployeeFormDialog(String id, String lastName, String firstName, String birthdate,
                           String address, String phoneNumber, String sssNumber,
                           String philhealthNumber, String pagibigNumber, String position,
                           String department, double basicSalary, double riceSubsidy, 
                           double phoneAllowance, double clothingAllowance) {
        this.id = id;
        this.lastName = lastName;
        this.firstName = firstName;
        this.birthdate = birthdate;
        this.address = address;
        this.phoneNumber = phoneNumber;
        this.sssNumber = sssNumber;
        this.philhealthNumber = philhealthNumber;
        this.pagibigNumber = pagibigNumber;
        this.position = position;
        this.department = department;
        this.basicSalary = basicSalary;
        this.riceSubsidy = riceSubsidy;
        this.phoneAllowance = phoneAllowance;
        this.clothingAllowance = clothingAllowance;
    }

    public Employee toEmployee() {
        // return a concrete subclass; default to full-time
        return new FullTimeEmployee(
            this.id,
            this.lastName,
            this.firstName,
            this.birthdate,
            this.address,
            this.phoneNumber,
            this.sssNumber,
            this.philhealthNumber,
            "", // TIN Number (if needed later)
            this.pagibigNumber,
            "fulltime", // status placeholder
            this.position,
            "", // Supervisor (if needed later)
            this.department,
            this.basicSalary,
            this.riceSubsidy,
            this.phoneAllowance,
            this.clothingAllowance,
            0.0,  // Gross Semi-Monthly Rate (you can calculate this if needed)
            0.0   // Hourly Rate (if needed)
        );
    }

    public static EmployeeFormDialog from(Employee emp) {
        if (emp == null) return null;
        return new EmployeeFormDialog(
            emp.getId(),
            emp.getLastName(),
            emp.getFirstName(),
            emp.getBirthdate(),
            emp.getAddress(),
            emp.getPhoneNumber(),
            emp.getSssNo(),
            emp.getPhilhealthNo(),
            emp.getPagibigNo(),
            emp.getPosition(),
            emp.getDepartment(),
            emp.getBasicSalary(),
            emp.getRiceSubsidy(),
            emp.getPhoneAllowance(),
            emp.getClothingAllowance()
        );
    }
}