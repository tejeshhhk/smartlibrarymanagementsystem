package SRC.model;

/**
 * Represents a Book entity in the library system.
 * This class encapsulates only book metadata and inventory status.
 */
public class Book {
    private String bookId;
    private String isbn;
    private String title;
    private String author;
    private BookCategory category;
    private String publisher;
    private int publicationYear;
    private int totalCopies;
    private int availableCopies;
    private String shelfNumber;

    // Default constructor
    public Book() {}

    // Parameterized constructor
    public Book(String bookId, String isbn, String title, String author, BookCategory category,
                String publisher, int publicationYear, int totalCopies, int availableCopies, String shelfNumber) {
        this.bookId = bookId;
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.category = category;
        this.publisher = publisher;
        this.publicationYear = publicationYear;
        this.totalCopies = totalCopies;
        this.availableCopies = availableCopies;
        this.shelfNumber = shelfNumber;
    }

    // Copy constructor (useful for undo state snapshotting)
    public Book(Book other) {
        this.bookId = other.bookId;
        this.isbn = other.isbn;
        this.title = other.title;
        this.author = other.author;
        this.category = other.category;
        this.publisher = other.publisher;
        this.publicationYear = other.publicationYear;
        this.totalCopies = other.totalCopies;
        this.availableCopies = other.availableCopies;
        this.shelfNumber = other.shelfNumber;
    }

    // Business Logic Methods

    /**
     * Checks if a copy of the book is currently available to be issued.
     */
    public boolean isAvailable() {
        return availableCopies > 0;
    }

    /**
     * Decrements the count of available copies when a book is issued.
     * Returns true if successful, false if copies are unavailable.
     */
    public boolean issueCopy() {
        if (isAvailable()) {
            availableCopies--;
            return true;
        }
        return false;
    }

    /**
     * Increments the count of available copies when a book is returned.
     * Ensures we don't exceed totalCopies.
     */
    public boolean returnCopy() {
        if (availableCopies < totalCopies) {
            availableCopies++;
            return true;
        }
        return false;
    }

    /**
     * Formats book details for display in the console.
     */
    public void displayInfo() {
        System.out.println("----------------------------------------------");
        System.out.println("Book ID:          " + bookId);
        System.out.println("Title:            " + title);
        System.out.println("Author:           " + author);
        System.out.println("ISBN:             " + isbn);
        System.out.println("Category:         " + (category != null ? category.getDisplayName() : "N/A"));
        System.out.println("Publisher:        " + publisher);
        System.out.println("Publication Year: " + publicationYear);
        System.out.println("Total Copies:     " + totalCopies);
        System.out.println("Available Copies: " + availableCopies);
        System.out.println("Shelf Number:     " + shelfNumber);
        System.out.println("----------------------------------------------");
    }

    // Getters and Setters

    public String getBookId() {
        return bookId;
    }

    public void setBookId(String bookId) {
        this.bookId = bookId;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public BookCategory getCategory() {
        return category;
    }

    public void setCategory(BookCategory category) {
        this.category = category;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public int getPublicationYear() {
        return publicationYear;
    }

    public void setPublicationYear(int publicationYear) {
        this.publicationYear = publicationYear;
    }

    public int getTotalCopies() {
        return totalCopies;
    }

    public void setTotalCopies(int totalCopies) {
        this.totalCopies = totalCopies;
    }

    public int getAvailableCopies() {
        return availableCopies;
    }

    public void setAvailableCopies(int availableCopies) {
        this.availableCopies = availableCopies;
    }

    public String getShelfNumber() {
        return shelfNumber;
    }

    public void setShelfNumber(String shelfNumber) {
        this.shelfNumber = shelfNumber;
    }
}
