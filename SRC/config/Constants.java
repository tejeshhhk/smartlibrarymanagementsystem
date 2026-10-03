package SRC.config;

/**
 * Global application constants to eliminate magic numbers and hardcoded strings.
 */
public final class Constants {
    // Suppress default constructor for non-instantiability
    private Constants() {
        throw new UnsupportedOperationException("Constants class cannot be instantiated.");
    }

    // Storage File Paths
    public static final String DATA_DIR = "SRC/data/";
    public static final String BOOKS_FILE = DATA_DIR + "books.txt";
    public static final String STUDENTS_FILE = DATA_DIR + "students.txt";
    public static final String ADMINS_FILE = DATA_DIR + "admins.txt";
    public static final String ISSUES_FILE = DATA_DIR + "issues.txt";
    public static final String LOGS_FILE = DATA_DIR + "logs.txt";
    public static final String WAITING_LIST_FILE = DATA_DIR + "waiting_lists.txt";

    // Business Rules
    public static final int DEFAULT_BORROW_DURATION_DAYS = 14;
    public static final double FINE_PER_DAY = 10.0; // E.g., Rs. 10 per day overdue
    public static final int MAX_BORROW_LIMIT = 3;

    // Formatting & Delimiters
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String FILE_DELIMITER = "|";
    public static final String FILE_DELIMITER_REGEX = "\\|";
}
