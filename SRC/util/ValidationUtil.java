package SRC.util;

import java.util.regex.Pattern;

/**
 * Utility class for validating user input and data consistency.
 */
public final class ValidationUtil {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\d{10}$");

    private ValidationUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated.");
    }

    /**
     * Checks if a string is null or empty (after trimming).
     */
    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * Validates if an email address matches the standard email regex.
     */
    public static boolean isValidEmail(String email) {
        if (isEmpty(email)) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Validates if a phone number consists of exactly 10 digits.
     */
    public static boolean isValidPhone(String phone) {
        if (isEmpty(phone)) {
            return false;
        }
        return PHONE_PATTERN.matcher(phone).matches();
    }

    /**
     * Validates if a string represents a positive integer (>= 0).
     */
    public static boolean isPositiveInteger(String str) {
        if (isEmpty(str)) {
            return false;
        }
        try {
            int value = Integer.parseInt(str);
            return value >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Validates if a publication year is within a realistic range.
     */
    public static boolean isValidPublicationYear(int year) {
        int currentYear = java.time.Year.now().getValue();
        return year >= 1000 && year <= currentYear;
    }

    /**
     * Checks if the format of a generated ID is valid (e.g., BK001, ST001, IS001).
     */
    public static boolean isValidId(String id, String prefix) {
        if (isEmpty(id) || prefix == null) {
            return false;
        }
        return id.startsWith(prefix) && id.substring(prefix.length()).matches("\\d+");
    }
}
