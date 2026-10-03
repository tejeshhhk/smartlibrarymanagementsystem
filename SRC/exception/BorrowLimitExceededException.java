package SRC.exception;

/**
 * Thrown when a student attempts to borrow more books than their allowed limit.
 */
public class BorrowLimitExceededException extends Exception {
    public BorrowLimitExceededException(String message) {
        super(message);
    }
}
