package SRC.util;

import java.util.Collection;

/**
 * Utility responsible for generating unique structured IDs for entity instances
 * (e.g., BK001, ST001, IS001, AD001).
 */
public final class IdGenerator {

    private IdGenerator() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated.");
    }

    /**
     * Generates a new ID with a specific prefix by scanning existing IDs in a collection,
     * identifying the maximum numeric suffix, and incrementing it.
     *
     * @param existingIds The collection of IDs already present in the database.
     * @param prefix      The prefix for the ID (e.g., "BK", "ST", "IS", "AD").
     * @return A newly generated ID (e.g., "BK004").
     */
    public static String generateId(Collection<String> existingIds, String prefix) {
        int maxNum = 0;
        
        if (existingIds != null) {
            for (String id : existingIds) {
                if (id != null && id.startsWith(prefix)) {
                    String numPart = id.substring(prefix.length());
                    try {
                        int num = Integer.parseInt(numPart);
                        if (num > maxNum) {
                            maxNum = num;
                        }
                    } catch (NumberFormatException e) {
                        // Ignore IDs that don't match the expected numerical suffix format
                    }
                }
            }
        }
        
        // Formats the number as at least 3 digits, e.g., 1 -> 001, 12 -> 012
        return String.format("%s%03d", prefix, maxNum + 1);
    }
}
