
public class UserAccount {

    public String username;
    public String password;
    public boolean isAdmin;
    public String employeeId;

    public UserAccount(String u, String p, boolean isAdmin, String employeeId) {
        this.username = u;
        this.password = p;
        this.isAdmin = isAdmin;
        this.employeeId = employeeId;
    }

}
