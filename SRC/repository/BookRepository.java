package SRC.repository;

import SRC.config.Constants;
import SRC.model.Book;
import SRC.model.BookCategory;
import java.io.*;
import java.util.*;

public class BookRepository {
    private final File file;

    public BookRepository() {
        this.file = new File(Constants.BOOKS_FILE);
        ensureFileExists();
    }

    private void ensureFileExists() {
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                System.err.println("Failed to create books file: " + e.getMessage());
            }
        }
    }

    private static String escapeField(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("|", "\\|")
                  .replace("\r", "\\r")
                  .replace("\n", "\\n");
    }

    private static String unescapeField(String str) {
        if (str == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '\\' && i + 1 < str.length()) {
                char next = str.charAt(i + 1);
                if (next == '|') { sb.append('|'); i++; }
                else if (next == '\\') { sb.append('\\'); i++; }
                else if (next == 'n') { sb.append('\n'); i++; }
                else if (next == 'r') { sb.append('\r'); i++; }
                else { sb.append(c); }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public synchronized List<Book> findAll() {
        List<Book> books = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split("(?<!\\\\)\\|", -1);
                if (parts.length >= 10) {
                    Book book = new Book();
                    book.setBookId(unescapeField(parts[0]));
                    book.setIsbn(unescapeField(parts[1]));
                    book.setTitle(unescapeField(parts[2]));
                    book.setAuthor(unescapeField(parts[3]));
                    book.setCategory(BookCategory.fromString(unescapeField(parts[4])));
                    book.setPublisher(unescapeField(parts[5]));
                    book.setPublicationYear(Integer.parseInt(unescapeField(parts[6])));
                    book.setTotalCopies(Integer.parseInt(unescapeField(parts[7])));
                    book.setAvailableCopies(Integer.parseInt(unescapeField(parts[8])));
                    book.setShelfNumber(unescapeField(parts[9]));
                    books.add(book);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading books file: " + e.getMessage());
        }
        return books;
    }

    public synchronized Optional<Book> findById(String bookId) {
        return findAll().stream()
                .filter(b -> b.getBookId().equals(bookId))
                .findFirst();
    }

    public synchronized void saveAll(List<Book> books) {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        File tempFile = new File(parent, file.getName() + ".tmp." + System.currentTimeMillis());
        try {
            try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(tempFile), "UTF-8"))) {
                for (Book book : books) {
                    pw.println(String.join(Constants.FILE_DELIMITER,
                            escapeField(book.getBookId()),
                            escapeField(book.getIsbn()),
                            escapeField(book.getTitle()),
                            escapeField(book.getAuthor()),
                            book.getCategory() != null ? book.getCategory().name() : "",
                            escapeField(book.getPublisher()),
                            String.valueOf(book.getPublicationYear()),
                            String.valueOf(book.getTotalCopies()),
                            String.valueOf(book.getAvailableCopies()),
                            escapeField(book.getShelfNumber())
                    ));
                }
            }
            try {
                java.nio.file.Files.move(tempFile.toPath(), file.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                java.nio.file.Files.move(tempFile.toPath(), file.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            System.err.println("Error writing to books file: " + e.getMessage());
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    public synchronized void save(Book book) {
        List<Book> books = findAll();
        int index = -1;
        for (int i = 0; i < books.size(); i++) {
            if (books.get(i).getBookId().equals(book.getBookId())) {
                index = i;
                break;
            }
        }
        if (index >= 0) {
            books.set(index, book);
        } else {
            books.add(book);
        }
        saveAll(books);
    }

    public synchronized boolean deleteById(String bookId) {
        List<Book> books = findAll();
        boolean removed = books.removeIf(b -> b.getBookId().equals(bookId));
        if (removed) {
            saveAll(books);
        }
        return removed;
    }
}
