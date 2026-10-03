package SRC.model;

/**
 * Standardized book categories to avoid typos and ensure search consistency.
 */
public enum BookCategory {
    FICTION("Fiction"),
    NON_FICTION("Non-Fiction"),
    SCIENCE("Science"),
    TECHNOLOGY("Technology"),
    HISTORY("History"),
    BIOGRAPHY("Biography"),
    MATHEMATICS("Mathematics"),
    LITERATURE("Literature");

    private final String displayName;

    BookCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Converts a string value (case-insensitive) to its corresponding BookCategory.
     * Returns null if no category matches.
     */
    public static BookCategory fromString(String text) {
        if (text == null) {
            return null;
        }
        for (BookCategory category : BookCategory.values()) {
            if (category.name().equalsIgnoreCase(text.trim()) || 
                category.displayName.equalsIgnoreCase(text.trim())) {
                return category;
            }
        }
        return null;
    }
}
