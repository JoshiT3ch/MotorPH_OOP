import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Contribution {
    private static Map<String, Double> sssRates = new HashMap<>();
    private static Map<String, Double> philhealthRates = new HashMap<>();
    private static Map<String, Double> pagibigRates = new HashMap<>();
    private static Map<String, Double> taxRates = new HashMap<>();

    // Load rates from the CSV files (Example for SSS)
    public static void loadSSSRates(String filePath) throws IOException, CsvValidationException {
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                // Assuming the first column is the salary range, and second is the SSS rate
                String range = nextLine[0];
                Double rate = Double.parseDouble(nextLine[1]);
                sssRates.put(range, rate);
            }
        }
    }

    // Load PhilHealth rates
    public static void loadPhilhealthRates(String filePath) throws IOException, CsvValidationException {
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                String range = nextLine[0];
                Double rate = Double.parseDouble(nextLine[1]);
                philhealthRates.put(range, rate);
            }
        }
    }

    // Load Pag-IBIG rates
    public static void loadPagibigRates(String filePath) throws IOException, CsvValidationException {
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                String range = nextLine[0];
                Double rate = Double.parseDouble(nextLine[1]);
                pagibigRates.put(range, rate);
            }
        }
    }

    // Load Tax rates
    public static void loadTaxRates(String filePath) throws IOException, CsvValidationException {
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                String range = nextLine[0];
                Double rate = Double.parseDouble(nextLine[1]);
                taxRates.put(range, rate);
            }
        }
    }

    // Get SSS contribution
    public static Double getSSSContribution(double salary) {
        // Retrieve the correct SSS rate based on salary
        return sssRates.getOrDefault(String.valueOf(salary), 0.0); // Default rate if no match
    }

    // Get PhilHealth contribution
    public static Double getPhilhealthContribution(double salary) {
        return philhealthRates.getOrDefault(String.valueOf(salary), 0.0);
    }

    // Get Pag-IBIG contribution
    public static Double getPagibigContribution(double salary) {
        return pagibigRates.getOrDefault(String.valueOf(salary), 0.0);
    }

    // Get Tax contribution
    public static Double getTaxContribution(double salary) {
        return taxRates.getOrDefault(String.valueOf(salary), 0.0);
    }
}