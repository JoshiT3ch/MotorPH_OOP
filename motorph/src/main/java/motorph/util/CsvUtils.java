package motorph.util;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CsvUtils {

    private CsvUtils() {
    }

    public static List<String[]> readAll(Path path) throws IOException, CsvValidationException {
        List<String[]> rows = new ArrayList<>();
        if (!Files.exists(path)) {
            return rows;
        }
        try (CSVReader reader = new CSVReader(new FileReader(path.toFile()))) {
            String[] row;
            while ((row = reader.readNext()) != null) {
                if (isBlankRow(row)) {
                    continue;
                }
                rows.add(row);
            }
        }
        return rows;
    }

    public static List<String[]> readAll(Path path, Charset charset) throws IOException, CsvValidationException {
        List<String[]> rows = new ArrayList<>();
        if (!Files.exists(path)) {
            return rows;
        }
        try (CSVReader reader = new CSVReader(new FileReader(path.toFile(), charset))) {
            String[] row;
            while ((row = reader.readNext()) != null) {
                if (isBlankRow(row)) {
                    continue;
                }
                rows.add(row);
            }
        }
        return rows;
    }

    public static void writeAll(Path path, List<String[]> rows) throws IOException {
        ensureParentDirectory(path);
        try (CSVWriter writer = new CSVWriter(new FileWriter(path.toFile()))) {
            for (String[] row : rows) {
                writer.writeNext(row);
            }
        }
    }

    public static void writeAll(Path path, Charset charset, List<String[]> rows) throws IOException {
        ensureParentDirectory(path);
        try (CSVWriter writer = new CSVWriter(new FileWriter(path.toFile(), charset))) {
            for (String[] row : rows) {
                writer.writeNext(row);
            }
        }
    }

    public static Map<String, Integer> buildHeaderIndexMap(String[] header) {
        Map<String, Integer> indexes = new LinkedHashMap<>();
        if (header == null) {
            return indexes;
        }
        for (int i = 0; i < header.length; i++) {
            indexes.put(safeTrim(header[i]).toLowerCase(), i);
        }
        return indexes;
    }

    public static String readText(String[] row, Map<String, Integer> headerIndexes, int legacyIndex, String... columnNames) {
        for (String columnName : columnNames) {
            Integer index = headerIndexes.get(columnName.toLowerCase());
            if (index != null && index >= 0 && index < row.length) {
                return safeTrim(row[index]);
            }
        }
        if (legacyIndex >= 0 && legacyIndex < row.length) {
            return safeTrim(row[legacyIndex]);
        }
        return "";
    }

    public static double readAmount(String[] row, Map<String, Integer> headerIndexes, int legacyIndex, String columnName, double fallbackValue) {
        String value = readText(row, headerIndexes, legacyIndex, columnName);
        return value.isEmpty() ? fallbackValue : parseAmount(value);
    }

    public static boolean isBlankRow(String[] row) {
        if (row == null || row.length == 0) {
            return true;
        }
        for (String value : row) {
            if (!safeTrim(value).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public static String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    public static double parseAmount(String value) {
        String normalized = safeTrim(value).replace(",", "").replace("\"", "");
        return normalized.isEmpty() ? 0.0 : Double.parseDouble(normalized);
    }

    private static void ensureParentDirectory(Path path) throws IOException {
        Path parent = path.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
    }
}
