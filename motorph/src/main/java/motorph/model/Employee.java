package motorph.model;

public abstract class Employee {

    private String id;
    private String lastName;
    private String firstName;
    private double basicSalary;
    private double riceSubsidy;
    private double phoneAllowance;
    private double clothingAllowance;
    private double hoursWorked;
    private String birthdate;
    private String address;
    private String phoneNumber;
    private String sssNo;
    private String philhealthNo;
    private String tinNo;
    private String pagibigNo;
    private String status;
    private String position;
    private String department;
    private String supervisor;
    private double grossSemiMonthlyRate;
    private double hourlyRate;
    private boolean archived;

    protected Employee(
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
        this.id = id;
        this.lastName = lastName;
        this.firstName = firstName;
        this.birthdate = birthdate;
        this.address = address;
        this.phoneNumber = phoneNumber;
        this.sssNo = sssNo;
        this.philhealthNo = philhealthNo;
        this.tinNo = tinNo;
        this.pagibigNo = pagibigNo;
        this.status = status;
        this.position = position;
        this.supervisor = supervisor;
        this.department = department;
        this.basicSalary = basicSalary;
        this.riceSubsidy = riceSubsidy;
        this.phoneAllowance = phoneAllowance;
        this.clothingAllowance = clothingAllowance;
        this.grossSemiMonthlyRate = grossSemiMonthlyRate;
        this.hourlyRate = hourlyRate;
    }

    public static void loadContributionRates() throws Exception {
        Contribution.initializeDefaultTables();
    }

    protected abstract double calculateBasePay(double hoursWorked);

    protected double calculateAllowancePay(double hoursWorked) {
        return riceSubsidy + phoneAllowance + clothingAllowance;
    }

    protected double calculateContributionBasis(double hoursWorked) {
        return calculateBasePay(hoursWorked);
    }

    protected double calculateTaxableIncome(double hoursWorked) {
        return Math.max(0.0, calculateContributionBasis(hoursWorked) - calculateMandatoryDeductionsBeforeTax(hoursWorked));
    }

    protected final double calculateMandatoryDeductionsBeforeTax(double hoursWorked) {
        return calculateSssDeduction(hoursWorked)
                + calculatePhilhealthDeduction(hoursWorked)
                + calculatePagibigDeduction(hoursWorked);
    }

    public double calculateGrossPay(double hoursWorked) {
        return calculateBasePay(hoursWorked) + calculateAllowancePay(hoursWorked);
    }

    public double calculateSssDeduction(double hoursWorked) {
        return Contribution.calculateSss(calculateContributionBasis(hoursWorked));
    }

    public double calculatePhilhealthDeduction(double hoursWorked) {
        return Contribution.calculatePhilhealth(calculateContributionBasis(hoursWorked));
    }

    public double calculatePagibigDeduction(double hoursWorked) {
        return Contribution.calculatePagibig(calculateContributionBasis(hoursWorked));
    }

    public double calculateTaxDeduction(double hoursWorked) {
        return Contribution.calculateWithholdingTax(calculateTaxableIncome(hoursWorked));
    }

    public double calculateDeductions() {
        return calculateDeductions(hoursWorked);
    }

    public double calculateNetSalary() {
        return calculateNetSalary(hoursWorked);
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public abstract double calculateDeductions(double hoursWorked);

    public abstract double calculateNetSalary(double hoursWorked);

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLastName() {
        return safeText(lastName);
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFirstName() {
        return safeText(firstName);
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public double getBasicSalary() {
        return basicSalary;
    }

    public void setBasicSalary(double basicSalary) {
        this.basicSalary = basicSalary;
    }

    public double getRiceSubsidy() {
        return riceSubsidy;
    }

    public void setRiceSubsidy(double riceSubsidy) {
        this.riceSubsidy = riceSubsidy;
    }

    public double getPhoneAllowance() {
        return phoneAllowance;
    }

    public void setPhoneAllowance(double phoneAllowance) {
        this.phoneAllowance = phoneAllowance;
    }

    public double getClothingAllowance() {
        return clothingAllowance;
    }

    public void setClothingAllowance(double clothingAllowance) {
        this.clothingAllowance = clothingAllowance;
    }

    public double getHoursWorked() {
        return hoursWorked;
    }

    public void setHoursWorked(double hoursWorked) {
        this.hoursWorked = hoursWorked;
    }

    public String getBirthdate() {
        return safeText(birthdate);
    }

    public void setBirthdate(String birthdate) {
        this.birthdate = birthdate;
    }

    public String getAddress() {
        return safeText(address);
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhoneNumber() {
        return safeText(phoneNumber);
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getSssNo() {
        return safeText(sssNo);
    }

    public void setSssNo(String sssNo) {
        this.sssNo = sssNo;
    }

    public String getPhilhealthNo() {
        return safeText(philhealthNo);
    }

    public void setPhilhealthNo(String philhealthNo) {
        this.philhealthNo = philhealthNo;
    }

    public String getTinNo() {
        return safeText(tinNo);
    }

    public void setTinNo(String tinNo) {
        this.tinNo = tinNo;
    }

    public String getPagibigNo() {
        return safeText(pagibigNo);
    }

    public void setPagibigNo(String pagibigNo) {
        this.pagibigNo = pagibigNo;
    }

    public String getStatus() {
        return safeText(status);
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getEmploymentType() {
        return safeText(status);
    }

    public String getPosition() {
        return safeText(position);
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getDepartment() {
        return safeText(department);
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getSupervisor() {
        return safeText(supervisor);
    }

    public void setSupervisor(String supervisor) {
        this.supervisor = supervisor;
    }

    public double getGrossSemiMonthlyRate() {
        return grossSemiMonthlyRate;
    }

    public void setGrossSemiMonthlyRate(double grossSemiMonthlyRate) {
        this.grossSemiMonthlyRate = grossSemiMonthlyRate;
    }

    public double getHourlyRate() {
        return hourlyRate;
    }

    public void setHourlyRate(double hourlyRate) {
        this.hourlyRate = hourlyRate;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }
}
