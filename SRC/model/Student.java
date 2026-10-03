package SRC.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Student entity in the library system.
 * Employs a normalized design: stores only references (book IDs) rather than nested Book objects.
 */
public class Student {
    private String studentId;
    private String name;
    private String department;
    private String phoneNumber;
    private String emailAddress;
    private List<String> borrowedBookIds;

    // Default constructor
    public Student() {
        this.borrowedBookIds = new ArrayList<>();
    }

    // Parameterized constructor
    public Student(String studentId, String name, String department, String phoneNumber, String emailAddress) {
        this.studentId = studentId;
        this.name = name;
        this.department = department;
        this.phoneNumber = phoneNumber;
        this.emailAddress = emailAddress;
        this.borrowedBookIds = new ArrayList<>();
    }

    // Copy constructor (useful for undo state snapshotting)
    public Student(Student other) {
        this.studentId = other.studentId;
        this.name = other.name;
        this.department = other.department;
        this.phoneNumber = other.phoneNumber;
        this.emailAddress = other.emailAddress;
        this.borrowedBookIds = new ArrayList<>(other.borrowedBookIds);
    }

    // Business Logic Methods

    /**
     * Adds a borrowed book ID reference.
     */
    public void borrowBook(String bookId) {
        if (bookId != null && !borrowedBookIds.contains(bookId)) {
            borrowedBookIds.add(bookId);
        }
    }

    /**
     * Removes a borrowed book ID reference.
     */
    public boolean returnBook(String bookId) {
        return borrowedBookIds.remove(bookId);
    }

    /**
     * Checks if the student is currently borrowing a specific book.
     */
    public boolean isBorrowing(String bookId) {
        return borrowedBookIds.contains(bookId);
    }

    /**
     * Formats student details for display.
     */
    public void displayInfo() {
        System.out.println("----------------------------------------------");
        System.out.println("Student ID:       " + studentId);
        System.out.println("Name:             " + name);
        System.out.println("Department:       " + department);
        System.out.println("Phone Number:     " + phoneNumber);
        System.out.println("Email Address:    " + emailAddress);
        System.out.println("Borrowed Book IDs: " + borrowedBookIds);
        System.out.println("----------------------------------------------");
    }

    // Getters and Setters

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public List<String> getBorrowedBookIds() {
        return borrowedBookIds;
    }

    public void setBorrowedBookIds(List<String> borrowedBookIds) {
        this.borrowedBookIds = borrowedBookIds != null ? borrowedBookIds : new ArrayList<>();
    }
}
