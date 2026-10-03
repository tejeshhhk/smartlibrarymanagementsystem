package SRC.model;

/**
 * Represents an Administrator entity in the system.
 */
public class Admin {
    private String adminId;
    private String username;
    private String password;

    // Default constructor
    public Admin() {}

    // Parameterized constructor
    public Admin(String adminId, String username, String password) {
        this.adminId = adminId;
        this.username = username;
        this.password = password;
    }

    // Copy constructor
    public Admin(Admin other) {
        this.adminId = other.adminId;
        this.username = other.username;
        this.password = other.password;
    }

    // Getters and Setters

    public String getAdminId() {
        return adminId;
    }

    public void setAdminId(String adminId) {
        this.adminId = adminId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
