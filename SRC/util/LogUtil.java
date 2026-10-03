package SRC.util;

import SRC.config.Constants;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Utility for logging system events both to the console and to a persistent file.
 */
public final class LogUtil {

    private LogUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated.");
    }

    private static String getCurrentTimestamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

    /**
     * Logs an informational message.
     */
    public static void info(String message) {
        log("INFO", message);
    }

    /**
     * Logs a warning message.
     */
    public static void warning(String message) {
        log("WARNING", message);
    }

    /**
     * Logs an error message.
     */
    public static void error(String message) {
        log("ERROR", message);
    }

    private static void log(String level, String message) {
        String logEntry = String.format("[%s] [%s] %s", getCurrentTimestamp(), level, message);
        
        // Print to standard out/err depending on the level
        if ("ERROR".equalsIgnoreCase(level)) {
            System.err.println(logEntry);
        } else {
            System.out.println(logEntry);
        }

        // Write to log file
        writeToLogFile(logEntry);
    }

    private static void writeToLogFile(String entry) {
        File logFile = new File(Constants.LOGS_FILE);
        
        // Ensure parent directories exist
        File parentDir = logFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
            writer.write(entry);
            writer.newLine();
        } catch (IOException e) {
            System.err.println("CRITICAL: Failed to write to log file: " + e.getMessage());
        }
    }
}
