import com.opencsv.CSVReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Employee {

    // Encapsulated fields
    private String id;
    private String lastName;
    private String firstName;
    private double basicSalary;
    private double riceSubsidy;
    private double phoneAllowance;
    private double clothingAllowance;
    private double hoursWorked;

    // Extra fields for profile data
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

    // Maps for holding contribution data (loaded from CSV files)
    private static Map<String, Double> sssRates = new HashMap<>();
    private static Map<String, Double> philhealthRates = new HashMap<>();
    private static Map<String, Double> pagibigRates = new HashMap<>();
    private static Map<String, Double> taxRates = new HashMap<>();
    private static final List<SssContributionBracket> sssContributionBrackets = new ArrayList<>();

    private static final class SssContributionBracket {
        private final Double minSalary;
        private final Double maxSalary;
        private final double contribution;

        private SssContributionBracket(Double minSalary, Double maxSalary, double contribution) {
            this.minSalary = minSalary;
            this.maxSalary = maxSalary;
            this.contribution = contribution;
        }

        private boolean matches(double salary) {
            boolean aboveOrEqualMin = minSalary == null || salary >= minSalary;
            boolean belowMax = maxSalary == null || salary < maxSalary;
            return aboveOrEqualMin && belowMax;
        }
    }

    // Full constructor for Employee (used by the system)
    public Employee(
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
        this.hoursWorked = 0;
    }

    // Method to load CSV data (SSS, Pag-Ibig, PhilHealth, Tax rates)
    public static void loadContributionRates() throws IOException, com.opencsv.exceptions.CsvValidationException {
        // Load SSS Rates
        loadSSSRates("data/SSS_Contribution.csv");
        loadPhilhealthRates("data/Philhealth_Contribution.csv");
        loadPagibigRates("data/PagIbig_Contribution.csv");
        loadTaxRates("data/WithholdingTax.csv");
    }

    // Helper method for loading SSS rates from CSV
    public static void loadSSSRates(String filePath) throws IOException, com.opencsv.exceptions.CsvValidationException {
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            sssRates.clear();
            sssContributionBrackets.clear();
            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                if (nextLine.length < 4 || "COMPENSATION RANGE".equalsIgnoreCase(safeTrim(nextLine[0]))) {
                    continue;
                }

                String firstColumn = safeTrim(nextLine[0]);
                String contributionColumn = safeTrim(nextLine[3]);
                if (firstColumn.isEmpty() || contributionColumn.isEmpty()) {
                    continue;
                }

                double contribution = parseAmount(contributionColumn);
                sssRates.put(firstColumn, contribution);
                sssContributionBrackets.add(parseSssBracket(nextLine, contribution));
            }
        }
    }

    // Similar methods for PhilHealth, Pag-IBIG, and Tax (define these as needed)
    public static void loadPhilhealthRates(String filePath) throws IOException, com.opencsv.exceptions.CsvValidationException {
        // Example for PhilHealth rates loading
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            philhealthRates.clear();
            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                if (nextLine.length < 2) {
                    continue;
                }
                String range = safeTrim(nextLine[0]);
                String rateText = safeTrim(nextLine[1]);
                if (range.isEmpty()) {
                    continue;
                }
                try {
                    Double rate = parseAmount(rateText.replace("%", ""));
                    philhealthRates.put(range, rate);
                } catch (NumberFormatException ignored) {
                }
            }
        }
    }

    public static void loadPagibigRates(String filePath) throws IOException, com.opencsv.exceptions.CsvValidationException {
        // Example for PagIbig rates loading
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            pagibigRates.clear();
            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                if (nextLine.length < 2) {
                    continue;
                }
                String range = safeTrim(nextLine[0]);
                String rateText = safeTrim(nextLine[1]);
                if (range.isEmpty()) {
                    continue;
                }
                try {
                    Double rate = parseAmount(rateText.replace("%", ""));
                    pagibigRates.put(range, rate);
                } catch (NumberFormatException ignored) {
                }
            }
        }
    }

    public static void loadTaxRates(String filePath) throws IOException, com.opencsv.exceptions.CsvValidationException {
        // Example for Withholding Tax rates loading
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            taxRates.clear();
            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                if (nextLine.length < 2) {
                    continue;
                }
                String range = safeTrim(nextLine[0]);
                String rateText = safeTrim(nextLine[1]);
                if (range.isEmpty()) {
                    continue;
                }
                try {
                    Double rate = parseAmount(rateText.replace("%", ""));
                    taxRates.put(range, rate);
                } catch (NumberFormatException ignored) {
                }
            }
        }
    }

    // Methods to calculate deductions (protected helper for subclasses)
    protected double sssDeduction() {
        ensureSssRatesLoaded();
        for (SssContributionBracket bracket : sssContributionBrackets) {
            if (bracket.matches(basicSalary)) {
                return bracket.contribution;
            }
        }
        return 0.0;
    }

    protected double philhealthDeduction() {
        // Implement logic for PhilHealth deduction
        return 0.02 * basicSalary;  // Example, replace with actual logic
    }

    protected double pagibigDeduction() {
        // Implement logic for Pag-IBIG deduction
        return 0.01 * basicSalary;  // Example, replace with actual logic
    }

    protected double taxDeduction() {
        // Implement logic for Tax deduction
        return 0.15 * basicSalary;  // Example, replace with actual logic
    }

    public double getSssDeduction() {
        return sssDeduction();
    }

    public double getPhilhealthDeduction() {
        return philhealthDeduction();
    }

    public double getPagibigDeduction() {
        return pagibigDeduction();
    }

    public double getTaxDeduction() {
        return taxDeduction();
    }

    // Calculate total deductions using helpers
    protected double totalDeductions() {
        return sssDeduction() + philhealthDeduction() + pagibigDeduction() + taxDeduction();
    }

    // Calculate net salary (after deductions)
    protected double netSalary() {
        return grossSalary() - totalDeductions();
    }

    // Calculate gross salary (sum of basic salary and allowances)
    public double grossSalary() {
        return basicSalary + riceSubsidy + phoneAllowance + clothingAllowance;
    }

    // Method to display payroll info
    public void printPayroll() {
        System.out.println("=== Payroll Details ===");
        System.out.println("Employee #: " + id);
        System.out.println("Name: " + getFullName());
        System.out.println("Position: " + position);
        System.out.println("Gross Salary: PHP " + grossSalary());
        System.out.println("Total Deductions: PHP " + calculateDeductions());
        System.out.println("Net Salary: PHP " + calculateNetSalary());
    }

    // Helper method to get full name
    public String getFullName() {
        return firstName + " " + lastName;
    }

    // Other getter and setter methods for employee details
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getBirthdate() { return birthdate == null ? "" : birthdate; }
    public void setBirthdate(String birthdate) { this.birthdate = birthdate; }

    public String getAddress() { return address == null ? "" : address; }
    public void setAddress(String address) { this.address = address; }

    public String getPhoneNumber() { return phoneNumber == null ? "" : phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getSssNo() { return sssNo == null ? "" : sssNo; }
    public void setSssNo(String sssNo) { this.sssNo = sssNo; }

    public String getPhilhealthNo() { return philhealthNo == null ? "" : philhealthNo; }
    public void setPhilhealthNo(String philhealthNo) { this.philhealthNo = philhealthNo; }

    public String getTinNo() { return tinNo == null ? "" : tinNo; }
    public void setTinNo(String tinNo) { this.tinNo = tinNo; }

    public String getPagibigNo() { return pagibigNo == null ? "" : pagibigNo; }
    public void setPagibigNo(String pagibigNo) { this.pagibigNo = pagibigNo; }

    public String getStatus() { return status == null ? "" : status; }
    public void setStatus(String status) { this.status = status; }

    public String getPosition() { return position == null ? "" : position; }
    public void setPosition(String position) { this.position = position; }

    public String getSupervisor() { return supervisor == null ? "" : supervisor; }
    public void setSupervisor(String supervisor) { this.supervisor = supervisor; }

    public String getDepartment() { return department == null ? "" : department; }
    public void setDepartment(String department) { this.department = department; }

    public double getBasicSalary() { return basicSalary; }
    public void setBasicSalary(double basicSalary) { this.basicSalary = basicSalary; }

    public double getRiceSubsidy() { return riceSubsidy; }
    public void setRiceSubsidy(double riceSubsidy) { this.riceSubsidy = riceSubsidy; }

    public double getPhoneAllowance() { return phoneAllowance; }
    public void setPhoneAllowance(double phoneAllowance) { this.phoneAllowance = phoneAllowance; }

    public double getClothingAllowance() { return clothingAllowance; }
    public void setClothingAllowance(double clothingAllowance) { this.clothingAllowance = clothingAllowance; }

    public double getHoursWorked() { return hoursWorked; }
    public void setHoursWorked(double hoursWorked) { this.hoursWorked = hoursWorked; }

    public double getGrossSemiMonthlyRate() { return grossSemiMonthlyRate; }
    public void setGrossSemiMonthlyRate(double rate) { this.grossSemiMonthlyRate = rate; }

    public double getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(double hourlyRate) { this.hourlyRate = hourlyRate; }

    // abstract methods forcing subclasses to provide their own implementations
    public abstract double calculateDeductions();
    public abstract double calculateNetSalary();

    private static SssContributionBracket parseSssBracket(String[] row, double contribution) {
        String firstColumn = safeTrim(row[0]);
        String thirdColumn = row.length > 2 ? safeTrim(row[2]) : "";

        if (firstColumn.toLowerCase().startsWith("below ")) {
            double upperBound = parseAmount(firstColumn.substring("Below ".length()));
            return new SssContributionBracket(null, upperBound, contribution);
        }

        Double lowerBound = firstColumn.isEmpty() ? null : parseAmount(firstColumn);
        if ("Over".equalsIgnoreCase(thirdColumn)) {
            return new SssContributionBracket(lowerBound, null, contribution);
        }

        Double upperBound = thirdColumn.isEmpty() ? null : parseAmount(thirdColumn);
        return new SssContributionBracket(lowerBound, upperBound, contribution);
    }

    private static void ensureSssRatesLoaded() {
        if (!sssContributionBrackets.isEmpty()) {
            return;
        }
        try {
            loadSSSRates("data/SSS_Contribution.csv");
        } catch (Exception ignored) {
        }
    }

    private static double parseAmount(String value) {
        String normalized = safeTrim(value).replace(",", "").replace("\"", "");
        return normalized.isEmpty() ? 0.0 : Double.parseDouble(normalized);
    }

    private static String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

}

