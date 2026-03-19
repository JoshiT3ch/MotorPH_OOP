import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class Contribution {

    private static final String SSS_FILE = "data/SSS_Contribution.csv";
    private static final String PHILHEALTH_FILE = "data/Philhealth_Contribution.csv";
    private static final String PAGIBIG_FILE = "data/PagIbig_Contribution.csv";
    private static final String TAX_FILE = "data/WithholdingTax.csv";

    private static final List<SssContributionBracket> sssBrackets = new ArrayList<>();
    private static final List<TaxBracket> taxBrackets = new ArrayList<>();

    private static boolean initialized;
    private static double philhealthPremiumRate = 0.03;
    private static double philhealthSalaryFloor = 10_000.00;
    private static double philhealthSalaryCeiling = 60_000.00;
    private static double pagibigLowerRate = 0.01;
    private static double pagibigHigherRate = 0.02;
    private static double pagibigThreshold = 1_500.00;
    private static double pagibigContributionCap = 100.00;

    private Contribution() {
    }

    public static synchronized void initializeDefaultTables() throws IOException, CsvValidationException {
        if (initialized) {
            return;
        }
        loadSssRates(SSS_FILE);
        loadPhilhealthRates(PHILHEALTH_FILE);
        loadPagibigRates(PAGIBIG_FILE);
        loadTaxRates(TAX_FILE);
        initialized = true;
    }

    public static double calculateSss(double salaryBasis) {
        ensureInitializedQuietly();
        for (SssContributionBracket bracket : sssBrackets) {
            if (bracket.matches(salaryBasis)) {
                return bracket.contribution();
            }
        }
        return 0.0;
    }

    public static double calculatePhilhealth(double salaryBasis) {
        ensureInitializedQuietly();
        double clampedSalary = Math.max(philhealthSalaryFloor, Math.min(salaryBasis, philhealthSalaryCeiling));
        double monthlyPremium = clampedSalary * philhealthPremiumRate;
        return monthlyPremium / 2.0;
    }

    public static double calculatePagibig(double salaryBasis) {
        ensureInitializedQuietly();
        double rate = salaryBasis <= pagibigThreshold ? pagibigLowerRate : pagibigHigherRate;
        return Math.min(salaryBasis * rate, pagibigContributionCap);
    }

    public static double calculateWithholdingTax(double taxableIncome) {
        ensureInitializedQuietly();
        double normalizedTaxableIncome = Math.max(0.0, taxableIncome);
        for (TaxBracket bracket : taxBrackets) {
            if (bracket.matches(normalizedTaxableIncome)) {
                return bracket.calculate(normalizedTaxableIncome);
            }
        }
        return 0.0;
    }

    private static void loadSssRates(String filePath) throws IOException, CsvValidationException {
        sssBrackets.clear();
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            String[] row;
            while ((row = reader.readNext()) != null) {
                if (row.length < 4 || "COMPENSATION RANGE".equalsIgnoreCase(safeTrim(row[0]))) {
                    continue;
                }

                String firstColumn = safeTrim(row[0]);
                String contributionColumn = safeTrim(row[3]);
                if (firstColumn.isEmpty() || contributionColumn.isEmpty()) {
                    continue;
                }

                double contribution = parseAmount(contributionColumn);
                sssBrackets.add(parseSssBracket(row, contribution));
            }
        }
    }

    private static void loadPhilhealthRates(String filePath) throws IOException, CsvValidationException {
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            String[] row;
            while ((row = reader.readNext()) != null) {
                if (row.length < 2) {
                    continue;
                }

                String salaryRange = safeTrim(row[0]);
                String premiumRateColumn = safeTrim(row[1]);
                if (salaryRange.isEmpty() || premiumRateColumn.isEmpty() || !premiumRateColumn.contains("%")) {
                    continue;
                }

                philhealthPremiumRate = parseAmount(premiumRateColumn.replace("%", "")) / 100.0;
                if (salaryRange.contains("to")) {
                    String[] bounds = salaryRange.split("to");
                    philhealthSalaryFloor = parseAmount(bounds[0]);
                    philhealthSalaryCeiling = parseAmount(bounds[1]);
                } else {
                    double value = parseAmount(salaryRange);
                    if (philhealthSalaryFloor == 10_000.00) {
                        philhealthSalaryFloor = value;
                    }
                    philhealthSalaryCeiling = Math.max(philhealthSalaryCeiling, value);
                }
            }
        }
    }

    private static void loadPagibigRates(String filePath) throws IOException, CsvValidationException {
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            String[] row;
            while ((row = reader.readNext()) != null) {
                if (row.length < 2) {
                    continue;
                }

                String salaryRange = safeTrim(row[0]);
                String employeeRate = safeTrim(row[1]);
                if (salaryRange.isEmpty() || employeeRate.isEmpty() || !employeeRate.contains("%")) {
                    continue;
                }

                double parsedRate = parseAmount(employeeRate.replace("%", "")) / 100.0;
                if (salaryRange.toLowerCase().contains("at least")) {
                    pagibigLowerRate = parsedRate;
                } else if (salaryRange.toLowerCase().contains("over")) {
                    pagibigHigherRate = parsedRate;
                    pagibigThreshold = parseAmount(salaryRange.replace("Over", ""));
                }

                if (row.length > 3) {
                    String totalColumn = safeTrim(row[3]);
                    if (totalColumn.toLowerCase().contains("maximum")) {
                        pagibigContributionCap = parseAmount(totalColumn);
                    }
                }
            }
        }
    }

    private static void loadTaxRates(String filePath) throws IOException, CsvValidationException {
        taxBrackets.clear();
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            String[] row;
            while ((row = reader.readNext()) != null) {
                if (row.length < 2) {
                    continue;
                }

                String monthlyRate = safeTrim(row[0]);
                String taxRule = safeTrim(row[1]);
                if (monthlyRate.isEmpty() || taxRule.isEmpty() || "Monthly Rate".equalsIgnoreCase(monthlyRate)) {
                    continue;
                }
                if (!monthlyRate.contains("below") && !monthlyRate.contains("above") && !monthlyRate.contains("and below")) {
                    continue;
                }

                taxBrackets.add(parseTaxBracket(monthlyRate, taxRule));
            }
        }
    }

    private static TaxBracket parseTaxBracket(String range, String formula) {
        String normalizedRange = range.toLowerCase();
        if (normalizedRange.contains("and below")) {
            double upper = parseAmount(range.replace("and below", ""));
            return new TaxBracket(null, upper, 0.0, 0.0);
        }

        if (normalizedRange.contains("to below")) {
            String[] parts = range.split("to below");
            double lower = parseAmount(parts[0]);
            double upper = parseAmount(parts[1]);
            return parseBracketFormula(lower, upper, formula);
        }

        if (normalizedRange.contains("and above")) {
            double lower = parseAmount(range.replace("and above", ""));
            return parseBracketFormula(lower, null, formula);
        }

        return new TaxBracket(null, null, 0.0, 0.0);
    }

    private static TaxBracket parseBracketFormula(double lowerBound, Double upperBound, String formula) {
        String normalizedFormula = formula.toLowerCase();
        if (normalizedFormula.contains("no withholding tax")) {
            return new TaxBracket(lowerBound, upperBound, 0.0, 0.0);
        }

        if (normalizedFormula.contains("plus")) {
            String[] parts = normalizedFormula.split("plus");
            double baseTax = parseAmount(parts[0]);
            String variablePart = parts[1];
            double rate = parseAmount(variablePart.substring(0, variablePart.indexOf('%'))) / 100.0;
            return new TaxBracket(lowerBound, upperBound, baseTax, rate);
        }

        double rate = parseAmount(normalizedFormula.substring(0, normalizedFormula.indexOf('%'))) / 100.0;
        return new TaxBracket(lowerBound, upperBound, 0.0, rate);
    }

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

    private static void ensureInitializedQuietly() {
        if (initialized) {
            return;
        }
        try {
            initializeDefaultTables();
        } catch (IOException | CsvValidationException ignored) {
        }
    }

    private static double parseAmount(String value) {
        String normalized = safeTrim(value)
                .replace(",", "")
                .replace("\"", "")
                .replace("at least", "")
                .replace("below", "")
                .replace("and below", "")
                .replace("and above", "")
                .trim();
        return normalized.isEmpty() ? 0.0 : Double.parseDouble(normalized);
    }

    private static String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private record SssContributionBracket(Double minSalary, Double maxSalary, double contribution) {
        private boolean matches(double salary) {
            boolean aboveOrEqualMin = minSalary == null || salary >= minSalary;
            boolean belowMax = maxSalary == null || salary < maxSalary;
            return aboveOrEqualMin && belowMax;
        }
    }

    private record TaxBracket(Double minSalary, Double maxSalary, double baseTax, double rate) {
        private boolean matches(double salary) {
            boolean aboveOrEqualMin = minSalary == null || salary >= minSalary;
            boolean belowMax = maxSalary == null || salary < maxSalary;
            return aboveOrEqualMin && belowMax;
        }

        private double calculate(double taxableIncome) {
            if (minSalary == null) {
                return 0.0;
            }
            return baseTax + Math.max(0.0, taxableIncome - minSalary) * rate;
        }
    }
}
