package motorph.repository;

import com.opencsv.exceptions.CsvValidationException;
import motorph.model.UserAccount;
import motorph.util.CsvUtils;
import motorph.util.FilePathResolver;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserCsvRepository implements UserRepository {

    private static final String FILE_NAME = "Users.csv";
    private static final String[] HEADER = {"username", "password", "role", "employeeId"};
    private final Path path;

    public UserCsvRepository() {
        this.path = FilePathResolver.resolveDataPath(FILE_NAME);
    }

    @Override
    public List<UserAccount> findAll() {
        try {
            List<String[]> rows = CsvUtils.readAll(path);
            List<UserAccount> accounts = new ArrayList<>();
            for (int i = rows.isEmpty() ? 0 : 1; i < rows.size(); i++) {
                String[] row = rows.get(i);
                if (row.length < 3) {
                    continue;
                }
                String username = CsvUtils.safeTrim(row[0]);
                String password = row.length > 1 ? CsvUtils.safeTrim(row[1]) : "";
                String role = row.length > 2 ? CsvUtils.safeTrim(row[2]) : "";
                String employeeId = row.length > 3 ? CsvUtils.safeTrim(row[3]) : "";
                boolean isAdmin = "admin".equalsIgnoreCase(role) || "admin".equalsIgnoreCase(username);
                if (!isAdmin && employeeId.isEmpty()) {
                    employeeId = username;
                }
                accounts.add(new UserAccount(username, password, isAdmin, employeeId));
            }
            return accounts;
        } catch (IOException | CsvValidationException e) {
            throw new IllegalStateException("Error reading users CSV: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<UserAccount> findByUsername(String username) {
        return findAll().stream().filter(account -> account.username().equalsIgnoreCase(username)).findFirst();
    }

    @Override
    public void saveAll(List<UserAccount> accounts) {
        List<String[]> rows = new ArrayList<>();
        rows.add(HEADER);
        for (UserAccount account : accounts) {
            rows.add(new String[] {
                    account.username(),
                    account.password(),
                    account.isAdmin() ? "admin" : "employee",
                    account.employeeId() == null ? "" : account.employeeId()
            });
        }
        try {
            CsvUtils.writeAll(path, rows);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save users CSV: " + e.getMessage(), e);
        }
    }
}
