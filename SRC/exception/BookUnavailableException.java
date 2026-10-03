package SRC.exception;

/**
 * Thrown when trying to borrow a book that is currently out of stock (available copies = 0).
 */
public class BookUnavailableException extends Exception {
    public BookUnavailableException(String message) {
        super(message);
    }
}
