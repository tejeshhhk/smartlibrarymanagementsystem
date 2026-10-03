package SRC.exception;

/**
 * Thrown when a requested student cannot be found in the system.
 */
public class StudentNotFoundException extends Exception {
    public StudentNotFoundException(String message) {
        super(message);
    }
}
