package SRC.service;

import SRC.exception.BookNotFoundException;
import SRC.model.Book;
import SRC.model.UndoOperation;
import SRC.repository.BookRepository;
import SRC.util.LogUtil;
import java.util.List;
import java.util.Optional;

public class BookService {
    private final BookRepository bookRepository;
    private final UndoService undoService;

    public BookService(BookRepository bookRepository, UndoService undoService) {
        this.bookRepository = bookRepository;
        this.undoService = undoService;
    }

    public synchronized List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    public synchronized Optional<Book> getBookById(String bookId) {
        return bookRepository.findById(bookId);
    }

    public synchronized Book addBook(Book book) {
        bookRepository.save(book);
        LogUtil.info("Added new book: " + book.getTitle() + " (ID: " + book.getBookId() + ")");
        
        // Save undo operation snapshot
        Book bookSnapshot = new Book(book);
        undoService.push(new UndoOperation(UndoOperation.Type.ADD_BOOK, book.getBookId(), null, bookSnapshot));
        
        return book;
    }

    public synchronized Book updateBook(Book updatedBook) throws BookNotFoundException {
        Book oldBook = bookRepository.findById(updatedBook.getBookId())
                .orElseThrow(() -> new BookNotFoundException("Book with ID " + updatedBook.getBookId() + " not found."));
        
        Book oldBookSnapshot = new Book(oldBook);
        Book newBookSnapshot = new Book(updatedBook);
        
        bookRepository.save(updatedBook);
        LogUtil.info("Updated book: " + updatedBook.getTitle() + " (ID: " + updatedBook.getBookId() + ")");
        
        // Save undo operation snapshot
        undoService.push(new UndoOperation(UndoOperation.Type.UPDATE_BOOK, updatedBook.getBookId(), oldBookSnapshot, newBookSnapshot));
        
        return updatedBook;
    }

    public synchronized void deleteBook(String bookId) throws BookNotFoundException {
        Book oldBook = bookRepository.findById(bookId)
                .orElseThrow(() -> new BookNotFoundException("Book with ID " + bookId + " not found."));
        
        Book oldBookSnapshot = new Book(oldBook);
        bookRepository.deleteById(bookId);
        LogUtil.info("Deleted book: " + oldBook.getTitle() + " (ID: " + bookId + ")");
        
        // Save undo operation snapshot
        undoService.push(new UndoOperation(UndoOperation.Type.DELETE_BOOK, bookId, oldBookSnapshot, null));
    }
}
