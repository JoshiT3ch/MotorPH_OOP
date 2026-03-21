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
        setId(id);
        setLastName(lastName);
        setFirstName(firstName);
        setBirthdate(birthdate);
        setAddress(address);
        setPhoneNumber(phoneNumber);
        setSssNo(sssNo);
        setPhilhealthNo(philhealthNo);
        setTinNo(tinNo);
        setPagibigNo(pagibigNo);
        setStatus(status);
        setPosition(position);
        setSupervisor(supervisor);
        setDepartment(department);
        setBasicSalary(basicSalary);
        setRiceSubsidy(riceSubsidy);
        setPhoneAllowance(phoneAllowance);
        setClothingAllowance(clothingAllowance);
        setGrossSemiMonthlyRate(grossSemiMonthlyRate);
        setHourlyRate(hourlyRate);
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

    public double calculateBasePayAmount(double hoursWorked) {
        return calculateBasePay(hoursWorked);
    }

    public double calculateAllowancePayAmount(double hoursWorked) {
        return calculateAllowancePay(hoursWorked);
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

    public boolean isContractEmployee() {
        return false;
    }

    public String getEmploymentCategoryLabel() {
        return isContractEmployee() ? "Contract Employee" : "Full-Time Employee";
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = normalizeText(id);
    }

    public String getLastName() {
        return safeText(lastName);
    }

    public void setLastName(String lastName) {
        this.lastName = normalizeText(lastName);
    }

    public String getFirstName() {
        return safeText(firstName);
    }

    public void setFirstName(String firstName) {
        this.firstName = normalizeText(firstName);
    }

    public double getBasicSalary() {
        return basicSalary;
    }

    public void setBasicSalary(double basicSalary) {
        this.basicSalary = sanitizeNonNegativeAmount(basicSalary);
    }

    public double getRiceSubsidy() {
        return riceSubsidy;
    }

    public void setRiceSubsidy(double riceSubsidy) {
        this.riceSubsidy = sanitizeNonNegativeAmount(riceSubsidy);
    }

    public double getPhoneAllowance() {
        return phoneAllowance;
    }

    public void setPhoneAllowance(double phoneAllowance) {
        this.phoneAllowance = sanitizeNonNegativeAmount(phoneAllowance);
    }

    public double getClothingAllowance() {
        return clothingAllowance;
    }

    public void setClothingAllowance(double clothingAllowance) {
        this.clothingAllowance = sanitizeNonNegativeAmount(clothingAllowance);
    }

    public double getHoursWorked() {
        return hoursWorked;
    }

    public void setHoursWorked(double hoursWorked) {
        this.hoursWorked = sanitizeNonNegativeAmount(hoursWorked);
    }

    public String getBirthdate() {
        return safeText(birthdate);
    }

    public void setBirthdate(String birthdate) {
        this.birthdate = normalizeText(birthdate);
    }

    public String getAddress() {
        return safeText(address);
    }

    public void setAddress(String address) {
        this.address = normalizeText(address);
    }

    public String getPhoneNumber() {
        return safeText(phoneNumber);
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = normalizeText(phoneNumber);
    }

    public String getSssNo() {
        return safeText(sssNo);
    }

    public void setSssNo(String sssNo) {
        this.sssNo = normalizeText(sssNo);
    }

    public String getPhilhealthNo() {
        return safeText(philhealthNo);
    }

    public void setPhilhealthNo(String philhealthNo) {
        this.philhealthNo = normalizeText(philhealthNo);
    }

    public String getTinNo() {
        return safeText(tinNo);
    }

    public void setTinNo(String tinNo) {
        this.tinNo = normalizeText(tinNo);
    }

    public String getPagibigNo() {
        return safeText(pagibigNo);
    }

    public void setPagibigNo(String pagibigNo) {
        this.pagibigNo = normalizeText(pagibigNo);
    }

    public String getStatus() {
        return safeText(status);
    }

    public void setStatus(String status) {
        this.status = normalizeEmploymentType(status);
    }

    public String getEmploymentType() {
        return safeText(status);
    }

    public String getPosition() {
        return safeText(position);
    }

    public void setPosition(String position) {
        this.position = normalizeText(position);
    }

    public String getDepartment() {
        return safeText(department);
    }

    public void setDepartment(String department) {
        this.department = normalizeText(department);
    }

    public String getSupervisor() {
        return safeText(supervisor);
    }

    public void setSupervisor(String supervisor) {
        this.supervisor = normalizeText(supervisor);
    }

    public double getGrossSemiMonthlyRate() {
        return grossSemiMonthlyRate;
    }

    public void setGrossSemiMonthlyRate(double grossSemiMonthlyRate) {
        this.grossSemiMonthlyRate = sanitizeNonNegativeAmount(grossSemiMonthlyRate);
    }

    public double getHourlyRate() {
        return hourlyRate;
    }

    public void setHourlyRate(double hourlyRate) {
        this.hourlyRate = sanitizeNonNegativeAmount(hourlyRate);
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

    private String normalizeText(String value) {
        return safeText(value).trim();
    }

    private String normalizeEmploymentType(String value) {
        String normalized = normalizeText(value);
        return "contract".equalsIgnoreCase(normalized) ? "contract" : "fulltime";
    }

    private double sanitizeNonNegativeAmount(double value) {
        return Double.isFinite(value) && value > 0.0 ? value : 0.0;
    }
}
