package SRC.util;

import SRC.config.Constants;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for common date operations like formatting, parsing,
 * calculating due dates, and calculating overdue fines.
 */
public final class DateUtil {

    private DateUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated.");
    }

    private static SimpleDateFormat getFormatter() {
        SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_FORMAT);
        sdf.setLenient(false); // Strict date matching
        return sdf;
    }

    /**
     * Parses a string formatted as Constants.DATE_FORMAT into a Date object.
     */
    public static Date parseDate(String dateStr) throws ParseException {
        return getFormatter().parse(dateStr);
    }

    /**
     * Formats a Date object to a string formatted as Constants.DATE_FORMAT.
     */
    public static String formatDate(Date date) {
        if (date == null) {
            return "";
        }
        return getFormatter().format(date);
    }

    /**
     * Calculates the due date by adding a specified number of days to the issue date.
     */
    public static Date calculateDueDate(Date issueDate, int durationDays) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(issueDate);
        calendar.add(Calendar.DAY_OF_YEAR, durationDays);
        return calendar.getTime();
    }

    /**
     * Calculates the difference in days between two dates.
     * Returns 0 if actualDate is on or before dueDate.
     */
    public static int getOverdueDays(Date dueDate, Date actualDate) {
        if (dueDate == null || actualDate == null) {
            return 0;
        }
        
        // Strip time components to compare strictly dates
        Date cleanDueDate = stripTime(dueDate);
        Date cleanActualDate = stripTime(actualDate);

        if (cleanActualDate.before(cleanDueDate)) {
            return 0;
        }

        long diffInMillis = cleanActualDate.getTime() - cleanDueDate.getTime();
        return (int) TimeUnit.MILLISECONDS.toDays(diffInMillis);
    }

    /**
     * Calculates the fine amount based on overdue days and fine per day rate.
     */
    public static double calculateFine(Date dueDate, Date actualDate, double finePerDay) {
        int overdueDays = getOverdueDays(dueDate, actualDate);
        return overdueDays * finePerDay;
    }

    private static Date stripTime(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }
}
