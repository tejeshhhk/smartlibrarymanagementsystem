package SRC.exception;

/**
 * Thrown when a requested book cannot be found in the system.
 */
public class BookNotFoundException extends Exception {
    public BookNotFoundException(String message) {
        super(message);
    }
}
