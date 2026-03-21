package motorph.model;

public record UserAccount(String username, String password, boolean isAdmin, String employeeId) {

    public UserAccount {
        username = normalizeText(username);
        password = normalizeText(password);
        employeeId = isAdmin ? normalizeText(employeeId) : normalizeEmployeeId(username, employeeId);
    }

    public boolean matchesCredentials(String rawUsername, String rawPassword) {
        return username.equals(rawUsername) && password.equals(rawPassword);
    }

    public String roleName() {
        return isAdmin ? "admin" : "employee";
    }

    public boolean isLinkedTo(String candidateEmployeeId) {
        return employeeId.equals(normalizeText(candidateEmployeeId));
    }

    private static String normalizeEmployeeId(String usernameValue, String employeeIdValue) {
        String normalizedEmployeeId = normalizeText(employeeIdValue);
        return normalizedEmployeeId.isEmpty() ? normalizeText(usernameValue) : normalizedEmployeeId;
    }

    private static String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }
}
