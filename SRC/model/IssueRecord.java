package SRC.model;

import SRC.util.DateUtil;
import java.util.Date;

/**
 * Acts as a bridge connecting Students and Books, tracking borrowing duration and fines.
 */
public class IssueRecord {
    private String issueId;
    private String studentId;
    private String bookId;
    private Date issueDate;
    private Date dueDate;
    private Date returnDate; // Will be null if the book has not been returned yet
    private double fineAmount;

    // Default constructor
    public IssueRecord() {}

    // Parameterized constructor
    public IssueRecord(String issueId, String studentId, String bookId, Date issueDate, Date dueDate) {
        this.issueId = issueId;
        this.studentId = studentId;
        this.bookId = bookId;
        this.issueDate = issueDate;
        this.dueDate = dueDate;
        this.returnDate = null;
        this.fineAmount = 0.0;
    }

    // Copy constructor (for undo history state snapshotting)
    public IssueRecord(IssueRecord other) {
        this.issueId = other.issueId;
        this.studentId = other.studentId;
        this.bookId = other.bookId;
        this.issueDate = other.issueDate != null ? new Date(other.issueDate.getTime()) : null;
        this.dueDate = other.dueDate != null ? new Date(other.dueDate.getTime()) : null;
        this.returnDate = other.returnDate != null ? new Date(other.returnDate.getTime()) : null;
        this.fineAmount = other.fineAmount;
    }

    // Business Logic Methods

    /**
     * Checks if the issue record is currently active (book not returned yet).
     */
    public boolean isActive() {
        return returnDate == null;
    }

    /**
     * Checks if the return of the book is overdue compared to the current date.
     */
    public boolean isOverdue(Date compareDate) {
        if (!isActive()) {
            return returnDate.after(dueDate);
        }
        return compareDate.after(dueDate);
    }

    /**
     * Updates the return date and calculates any outstanding fine based on daily rates.
     */
    public void returnBook(Date actualReturnDate, double finePerDay) {
        this.returnDate = actualReturnDate;
        this.fineAmount = DateUtil.calculateFine(this.dueDate, actualReturnDate, finePerDay);
    }

    // Getters and Setters

    public String getIssueId() {
        return issueId;
    }

    public void setIssueId(String issueId) {
        this.issueId = issueId;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getBookId() {
        return bookId;
    }

    public void setBookId(String bookId) {
        this.bookId = bookId;
    }

    public Date getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(Date issueDate) {
        this.issueDate = issueDate;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    public Date getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(Date returnDate) {
        this.returnDate = returnDate;
    }

    public double getFineAmount() {
        return fineAmount;
    }

    public void setFineAmount(double fineAmount) {
        this.fineAmount = fineAmount;
    }
}
