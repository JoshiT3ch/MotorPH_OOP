package motorph.model;

public record UserAccount(String username, String password, boolean isAdmin, String employeeId) {

    public boolean matchesCredentials(String rawUsername, String rawPassword) {
        return username.equals(rawUsername) && password.equals(rawPassword);
    }

    public String roleName() {
        return isAdmin ? "admin" : "employee";
    }
}
