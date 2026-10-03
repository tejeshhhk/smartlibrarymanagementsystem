package SRC.service;

import SRC.config.Constants;
import SRC.exception.BookNotFoundException;
import SRC.exception.BookUnavailableException;
import SRC.exception.BorrowLimitExceededException;
import SRC.exception.StudentNotFoundException;
import SRC.model.Book;
import SRC.model.IssueRecord;
import SRC.model.Student;
import SRC.repository.BookRepository;
import SRC.repository.IssueRecordRepository;
import SRC.repository.StudentRepository;
import SRC.util.DateUtil;
import SRC.util.IdGenerator;
import SRC.util.LogUtil;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class IssueService {
    private final IssueRecordRepository issueRecordRepository;
    private final BookRepository bookRepository;
    private final StudentRepository studentRepository;

    public IssueService(IssueRecordRepository issueRecordRepository, BookRepository bookRepository, StudentRepository studentRepository) {
        this.issueRecordRepository = issueRecordRepository;
        this.bookRepository = bookRepository;
        this.studentRepository = studentRepository;
    }

    public synchronized List<IssueRecord> getAllIssues() {
        return issueRecordRepository.findAll();
    }

    public synchronized IssueRecord borrowBook(String studentId, String bookId)
            throws StudentNotFoundException, BookNotFoundException, BookUnavailableException, BorrowLimitExceededException {
        
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new StudentNotFoundException("Student with ID " + studentId + " not found."));
        
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BookNotFoundException("Book with ID " + bookId + " not found."));

        // Business Rule: Check student borrow limit
        if (student.getBorrowedBookIds().size() >= Constants.MAX_BORROW_LIMIT) {
            throw new BorrowLimitExceededException("Student has reached the maximum borrowing limit of " + Constants.MAX_BORROW_LIMIT + " books.");
        }

        // Business Rule: Check duplicate borrow
        if (student.getBorrowedBookIds().contains(bookId)) {
            throw new BorrowLimitExceededException("Student with ID " + studentId + " has already borrowed book ID " + bookId + ".");
        }

        // Business Rule: Check book availability
        if (!book.isAvailable()) {
            throw new BookUnavailableException("No copies of the book '" + book.getTitle() + "' are currently available.");
        }

        // Apply changes
        book.issueCopy();
        student.borrowBook(bookId);

        // Generate issue record
        List<String> existingIds = issueRecordRepository.findAll().stream()
                .map(IssueRecord::getIssueId)
                .collect(Collectors.toList());
        String issueId = IdGenerator.generateId(existingIds, "IS");
        
        Date issueDate = new Date();
        Date dueDate = DateUtil.calculateDueDate(issueDate, Constants.DEFAULT_BORROW_DURATION_DAYS);
        
        IssueRecord record = new IssueRecord(issueId, studentId, bookId, issueDate, dueDate);

        // Persist
        bookRepository.save(book);
        studentRepository.save(student);
        issueRecordRepository.save(record);

        LogUtil.info("Book '" + book.getTitle() + "' (ID: " + bookId + ") issued to student '" + student.getName() + "' (ID: " + studentId + "). Due Date: " + DateUtil.formatDate(dueDate));
        
        return record;
    }

    public synchronized IssueRecord returnBook(String issueId) throws Exception {
        IssueRecord record = issueRecordRepository.findById(issueId)
                .orElseThrow(() -> new IllegalArgumentException("Issue record with ID " + issueId + " not found."));

        if (!record.isActive()) {
            throw new IllegalStateException("Book has already been returned for this issue record.");
        }

        Student student = studentRepository.findById(record.getStudentId())
                .orElseThrow(() -> new StudentNotFoundException("Student with ID " + record.getStudentId() + " not found."));
        
        Book book = bookRepository.findById(record.getBookId())
                .orElseThrow(() -> new BookNotFoundException("Book with ID " + record.getBookId() + " not found."));

        // Return book and calculate fine
        Date returnDate = new Date();
        record.returnBook(returnDate, Constants.FINE_PER_DAY);
        book.returnCopy();
        student.returnBook(record.getBookId());

        // Persist
        bookRepository.save(book);
        studentRepository.save(student);
        issueRecordRepository.save(record);

        String fineInfo = record.getFineAmount() > 0 ? " (Overdue Fine: Rs. " + record.getFineAmount() + ")" : "";
        LogUtil.info("Book '" + book.getTitle() + "' returned by student '" + student.getName() + "'" + fineInfo);

        return record;
    }
}
