package SRC;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import SRC.config.Constants;
import SRC.exception.BookNotFoundException;
import SRC.exception.BookUnavailableException;
import SRC.exception.BorrowLimitExceededException;
import SRC.exception.StudentNotFoundException;
import SRC.model.*;
import SRC.repository.*;
import SRC.service.*;
import SRC.util.DateUtil;
import SRC.util.IdGenerator;
import SRC.util.LogUtil;
import SRC.util.ValidationUtil;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class LibraryServer {

    private static BookRepository bookRepository;
    private static StudentRepository studentRepository;
    private static AdminRepository adminRepository;
    private static IssueRecordRepository issueRecordRepository;

    private static UndoService undoService;
    private static BookService bookService;
    private static StudentService studentService;
    private static IssueService issueService;
    private static AuthService authService;

    public static void main(String[] args) throws IOException {
        // Initialize Repositories
        bookRepository = new BookRepository();
        studentRepository = new StudentRepository();
        adminRepository = new AdminRepository();
        issueRecordRepository = new IssueRecordRepository();

        // Initialize Services
        undoService = new UndoService();
        bookService = new BookService(bookRepository, undoService);
        studentService = new StudentService(studentRepository, undoService);
        issueService = new IssueService(issueRecordRepository, bookRepository, studentRepository);
        authService = new AuthService(adminRepository);

        // Seed data if database is empty
        seedDataIfEmpty();

        // Start HTTP Server
        int port = 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        // Register Handlers
        server.createContext("/api", new ApiHandler());
        server.createContext("/", new StaticFileHandler());

        server.setExecutor(null); // default executor
        server.start();
        LogUtil.info("Server started on http://localhost:" + port);
    }

    private static void seedDataIfEmpty() {
        if (bookRepository.findAll().isEmpty()) {
            LogUtil.info("Seeding books...");
            bookRepository.save(new Book("BK001", "978-0134685991", "Effective Java", "Joshua Bloch", BookCategory.TECHNOLOGY, "Addison-Wesley", 2018, 5, 5, "A-12"));
            bookRepository.save(new Book("BK002", "978-0132350884", "Clean Code", "Robert C. Martin", BookCategory.TECHNOLOGY, "Prentice Hall", 2008, 3, 3, "B-04"));
            bookRepository.save(new Book("BK003", "978-0596009205", "Head First Design Patterns", "Eric Freeman", BookCategory.TECHNOLOGY, "O'Reilly", 2004, 4, 4, "B-09"));
            bookRepository.save(new Book("BK004", "978-0321356680", "Design Patterns", "Erich Gamma", BookCategory.TECHNOLOGY, "Addison-Wesley", 1994, 2, 2, "A-02"));
        }
        if (studentRepository.findAll().isEmpty()) {
            LogUtil.info("Seeding students...");
            studentRepository.save(new Student("ST001", "Tejesh Reddy", "Computer Science", "9876543210", "tejesh.reddy@example.com"));
            studentRepository.save(new Student("ST002", "Jane Smith", "Electrical Engineering", "8765432109", "jane.smith@example.com"));
            studentRepository.save(new Student("ST003", "Bob Johnson", "Mathematics", "7654321098", "bob.johnson@example.com"));
        }
    }

    private static class StaticFileHandler implements HttpHandler {
        private final File webRoot;

        public StaticFileHandler() {
            File root = new File("web");
            File canonicalRoot;
            try {
                canonicalRoot = root.getCanonicalFile();
            } catch (IOException e) {
                canonicalRoot = root.getAbsoluteFile();
            }
            this.webRoot = canonicalRoot;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path == null || path.contains("\0")) {
                sendResponse(exchange, 400, "{\"error\":\"Invalid request path\"}", "application/json");
                return;
            }

            if (path.equals("/")) {
                path = "/index.html";
            }

            File requestedFile;
            try {
                requestedFile = new File(webRoot, path).getCanonicalFile();
            } catch (IOException e) {
                sendResponse(exchange, 403, "{\"error\":\"Forbidden access\"}", "application/json");
                return;
            }

            String webRootPath = webRoot.getPath();
            if (!webRootPath.endsWith(File.separator)) {
                webRootPath += File.separator;
            }

            if (!requestedFile.getPath().startsWith(webRootPath) && !requestedFile.equals(webRoot)) {
                sendResponse(exchange, 403, "{\"error\":\"Forbidden access\"}", "application/json");
                return;
            }

            if (requestedFile.exists() && requestedFile.isFile()) {
                String contentType = "text/plain";
                String fileName = requestedFile.getName().toLowerCase();
                if (fileName.endsWith(".html")) contentType = "text/html; charset=utf-8";
                else if (fileName.endsWith(".css")) contentType = "text/css; charset=utf-8";
                else if (fileName.endsWith(".js")) contentType = "application/javascript; charset=utf-8";
                else if (fileName.endsWith(".png")) contentType = "image/png";
                else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) contentType = "image/jpeg";
                else if (fileName.endsWith(".svg")) contentType = "image/svg+xml";

                byte[] fileBytes = Files.readAllBytes(requestedFile.toPath());
                exchange.getResponseHeaders().set("Content-Type", contentType);
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.sendResponseHeaders(200, fileBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(fileBytes);
                }
            } else {
                sendResponse(exchange, 404, "{\"error\":\"File not found\"}", "application/json");
            }
        }
    }

    private static class ApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Handle CORS Preflight
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
                exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();

            try {
                if (path.equals("/api/auth/login") && "POST".equalsIgnoreCase(method)) {
                    handleLogin(exchange);
                } else if (path.equals("/api/public/student") && "GET".equalsIgnoreCase(method)) {
                    handlePublicStudentLookup(exchange);
                } else if (path.equals("/api/books") && "GET".equalsIgnoreCase(method)) {
                    handleGetBooks(exchange);
                } else if (path.equals("/api/books") && "POST".equalsIgnoreCase(method)) {
                    if (checkAuth(exchange)) handleAddBook(exchange);
                } else if (path.equals("/api/books") && "PUT".equalsIgnoreCase(method)) {
                    if (checkAuth(exchange)) handleUpdateBook(exchange);
                } else if (path.equals("/api/books") && "DELETE".equalsIgnoreCase(method)) {
                    if (checkAuth(exchange)) handleDeleteBook(exchange);
                } else if (path.equals("/api/students") && "GET".equalsIgnoreCase(method)) {
                    if (checkAuth(exchange)) handleGetStudents(exchange);
                } else if (path.equals("/api/students") && "POST".equalsIgnoreCase(method)) {
                    if (checkAuth(exchange)) handleRegisterStudent(exchange);
                } else if (path.equals("/api/issues") && "GET".equalsIgnoreCase(method)) {
                    if (checkAuth(exchange)) handleGetIssues(exchange);
                } else if (path.equals("/api/issues/borrow") && "POST".equalsIgnoreCase(method)) {
                    if (checkAuth(exchange)) handleBorrow(exchange);
                } else if (path.equals("/api/issues/return") && "POST".equalsIgnoreCase(method)) {
                    if (checkAuth(exchange)) handleReturn(exchange);
                } else if (path.equals("/api/admin/undo") && "POST".equalsIgnoreCase(method)) {
                    if (checkAuth(exchange)) handleUndo(exchange);
                } else if (path.equals("/api/admin/undo/size") && "GET".equalsIgnoreCase(method)) {
                    if (checkAuth(exchange)) handleUndoSize(exchange);
                } else if (path.equals("/api/logs") && "GET".equalsIgnoreCase(method)) {
                    if (checkAuth(exchange)) handleGetLogs(exchange);
                } else {
                    sendResponse(exchange, 404, "{\"error\":\"Endpoint not found\"}", "application/json");
                }
            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}", "application/json");
            }
        }

        private boolean checkAuth(HttpExchange exchange) throws IOException {
            String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7).trim();
                if (authService.isValidToken(token)) {
                    return true;
                }
            }
            sendResponse(exchange, 401, "{\"error\":\"Unauthorized admin access\"}", "application/json");
            return false;
        }

        private void handleLogin(HttpExchange exchange) throws IOException {
            String body = readRequestBody(exchange);
            String user = getJsonField(body, "username");
            String pass = getJsonField(body, "password");

            String token = authService.login(user, pass);
            if (token != null) {
                sendResponse(exchange, 200, "{\"token\":\"" + token + "\"}", "application/json");
            } else {
                sendResponse(exchange, 401, "{\"error\":\"Invalid username or password\"}", "application/json");
            }
        }

        private void handlePublicStudentLookup(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            String studentId = "";
            if (query != null) {
                String[] params = query.split("&");
                for (String param : params) {
                    String[] pair = param.split("=");
                    if (pair.length == 2 && pair[0].equals("id")) {
                        studentId = java.net.URLDecoder.decode(pair[1], "UTF-8").trim();
                    }
                }
            }

            if (ValidationUtil.isEmpty(studentId)) {
                sendResponse(exchange, 400, "{\"error\":\"Student ID is required.\"}", "application/json");
                return;
            }

            Optional<Student> studentOpt = studentService.getStudentById(studentId);
            if (!studentOpt.isPresent()) {
                sendResponse(exchange, 404, "{\"error\":\"Student not found.\"}", "application/json");
                return;
            }

            Student student = studentOpt.get();

            List<IssueRecord> issues = issueService.getAllIssues().stream()
                    .filter(i -> i.getStudentId().equals(student.getStudentId()))
                    .collect(Collectors.toList());

            String studentJson = studentToJson(student);
            
            String issuesJson = issues.stream()
                    .map(r -> {
                        String bookTitle = "Unknown Book";
                        Optional<Book> b = bookService.getBookById(r.getBookId());
                        if (b.isPresent()) {
                            bookTitle = b.get().getTitle();
                        }
                        return String.format(
                            "{\"issueId\":\"%s\",\"studentId\":\"%s\",\"bookId\":\"%s\",\"bookTitle\":\"%s\",\"issueDate\":\"%s\",\"dueDate\":\"%s\",\"returnDate\":\"%s\",\"fineAmount\":%.2f,\"active\":%b}",
                            escapeJson(r.getIssueId()),
                            escapeJson(r.getStudentId()),
                            escapeJson(r.getBookId()),
                            escapeJson(bookTitle),
                            DateUtil.formatDate(r.getIssueDate()),
                            DateUtil.formatDate(r.getDueDate()),
                            r.getReturnDate() != null ? DateUtil.formatDate(r.getReturnDate()) : "",
                            r.getFineAmount(),
                            r.isActive()
                        );
                    })
                    .collect(Collectors.joining(","));

            String responseText = String.format("{\"student\":%s,\"issues\":[%s]}", studentJson, issuesJson);
            sendResponse(exchange, 200, responseText, "application/json");
        }

        private void handleGetBooks(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            String search = "";
            String categoryStr = "";
            
            if (query != null) {
                String[] params = query.split("&");
                for (String param : params) {
                    String[] pair = param.split("=");
                    if (pair.length == 2) {
                        if (pair[0].equals("q")) {
                            search = java.net.URLDecoder.decode(pair[1], "UTF-8").toLowerCase().trim();
                        } else if (pair[0].equals("category")) {
                            categoryStr = java.net.URLDecoder.decode(pair[1], "UTF-8").trim();
                        }
                    }
                }
            }

            final String finalSearch = search;
            final String finalCategory = categoryStr;

            List<Book> books = bookService.getAllBooks().stream()
                    .filter(b -> {
                        boolean matchesSearch = finalSearch.isEmpty() ||
                                b.getTitle().toLowerCase().contains(finalSearch) ||
                                b.getAuthor().toLowerCase().contains(finalSearch) ||
                                b.getIsbn().toLowerCase().contains(finalSearch) ||
                                b.getBookId().toLowerCase().contains(finalSearch);
                        
                        boolean matchesCategory = finalCategory.isEmpty() || "ALL".equalsIgnoreCase(finalCategory) ||
                                (b.getCategory() != null && b.getCategory().name().equalsIgnoreCase(finalCategory));

                        return matchesSearch && matchesCategory;
                    })
                    .collect(Collectors.toList());

            String booksJson = books.stream()
                    .map(LibraryServer::bookToJson)
                    .collect(Collectors.joining(","));
            
            sendResponse(exchange, 200, "[" + booksJson + "]", "application/json");
        }

        private void handleAddBook(HttpExchange exchange) throws IOException {
            String body = readRequestBody(exchange);
            
            String isbn = getJsonField(body, "isbn");
            String title = getJsonField(body, "title");
            String author = getJsonField(body, "author");
            String categoryStr = getJsonField(body, "category");
            String publisher = getJsonField(body, "publisher");
            String publicationYearStr = getJsonField(body, "publicationYear");
            String totalCopiesStr = getJsonField(body, "totalCopies");
            String shelfNumber = getJsonField(body, "shelfNumber");

            // Validations
            if (ValidationUtil.isEmpty(title) || ValidationUtil.isEmpty(author) || ValidationUtil.isEmpty(isbn)) {
                sendResponse(exchange, 400, "{\"error\":\"Title, Author, and ISBN are required.\"}", "application/json");
                return;
            }

            int year = 0;
            int total = 0;
            try {
                year = Integer.parseInt(publicationYearStr);
                total = Integer.parseInt(totalCopiesStr);
            } catch (NumberFormatException e) {
                sendResponse(exchange, 400, "{\"error\":\"Publication Year and Total Copies must be valid integers.\"}", "application/json");
                return;
            }

            if (!ValidationUtil.isValidPublicationYear(year)) {
                sendResponse(exchange, 400, "{\"error\":\"Invalid publication year.\"}", "application/json");
                return;
            }
            if (total < 1) {
                sendResponse(exchange, 400, "{\"error\":\"Total copies must be at least 1.\"}", "application/json");
                return;
            }

            BookCategory category = BookCategory.fromString(categoryStr);
            if (category == null) {
                category = BookCategory.TECHNOLOGY;
            }

            List<String> existingIds = bookService.getAllBooks().stream()
                    .map(Book::getBookId)
                    .collect(Collectors.toList());
            String bookId = IdGenerator.generateId(existingIds, "BK");

            Book book = new Book(bookId, isbn, title, author, category, publisher, year, total, total, shelfNumber);
            Book added = bookService.addBook(book);

            sendResponse(exchange, 201, bookToJson(added), "application/json");
        }

        private void handleUpdateBook(HttpExchange exchange) throws IOException, BookNotFoundException {
            String body = readRequestBody(exchange);
            String bookId = getJsonField(body, "bookId");
            String isbn = getJsonField(body, "isbn");
            String title = getJsonField(body, "title");
            String author = getJsonField(body, "author");
            String categoryStr = getJsonField(body, "category");
            String publisher = getJsonField(body, "publisher");
            String publicationYearStr = getJsonField(body, "publicationYear");
            String totalCopiesStr = getJsonField(body, "totalCopies");
            String shelfNumber = getJsonField(body, "shelfNumber");

            if (ValidationUtil.isEmpty(bookId)) {
                sendResponse(exchange, 400, "{\"error\":\"Book ID is required for updates.\"}", "application/json");
                return;
            }

            Optional<Book> currentBookOpt = bookService.getBookById(bookId);
            if (!currentBookOpt.isPresent()) {
                sendResponse(exchange, 404, "{\"error\":\"Book not found.\"}", "application/json");
                return;
            }

            Book currentBook = currentBookOpt.get();

            int year = currentBook.getPublicationYear();
            int total = currentBook.getTotalCopies();

            if (!ValidationUtil.isEmpty(publicationYearStr)) {
                try {
                    year = Integer.parseInt(publicationYearStr);
                } catch (NumberFormatException e) {
                    sendResponse(exchange, 400, "{\"error\":\"Invalid year format.\"}", "application/json");
                    return;
                }
            }

            if (!ValidationUtil.isEmpty(totalCopiesStr)) {
                try {
                    total = Integer.parseInt(totalCopiesStr);
                } catch (NumberFormatException e) {
                    sendResponse(exchange, 400, "{\"error\":\"Invalid total copies format.\"}", "application/json");
                    return;
                }
            }

            // Adjust available copies based on new total
            int diff = total - currentBook.getTotalCopies();
            int newAvailable = currentBook.getAvailableCopies() + diff;
            if (newAvailable < 0) {
                sendResponse(exchange, 400, "{\"error\":\"Cannot reduce total copies below the count of currently issued copies.\"}", "application/json");
                return;
            }

            Book updated = new Book();
            updated.setBookId(bookId);
            updated.setIsbn(ValidationUtil.isEmpty(isbn) ? currentBook.getIsbn() : isbn);
            updated.setTitle(ValidationUtil.isEmpty(title) ? currentBook.getTitle() : title);
            updated.setAuthor(ValidationUtil.isEmpty(author) ? currentBook.getAuthor() : author);
            updated.setCategory(ValidationUtil.isEmpty(categoryStr) ? currentBook.getCategory() : BookCategory.fromString(categoryStr));
            updated.setPublisher(ValidationUtil.isEmpty(publisher) ? currentBook.getPublisher() : publisher);
            updated.setPublicationYear(year);
            updated.setTotalCopies(total);
            updated.setAvailableCopies(newAvailable);
            updated.setShelfNumber(ValidationUtil.isEmpty(shelfNumber) ? currentBook.getShelfNumber() : shelfNumber);

            Book result = bookService.updateBook(updated);
            sendResponse(exchange, 200, bookToJson(result), "application/json");
        }

        private void handleDeleteBook(HttpExchange exchange) throws IOException, BookNotFoundException {
            String query = exchange.getRequestURI().getQuery();
            String bookId = getQueryParameter(query, "id");

            if (ValidationUtil.isEmpty(bookId)) {
                sendResponse(exchange, 400, "{\"error\":\"Book ID query parameter 'id' is required.\"}", "application/json");
                return;
            }

            Optional<Book> bookOpt = bookService.getBookById(bookId);
            if (!bookOpt.isPresent()) {
                sendResponse(exchange, 404, "{\"error\":\"Book not found.\"}", "application/json");
                return;
            }

            // Verify book is not currently issued
            if (bookOpt.get().getAvailableCopies() != bookOpt.get().getTotalCopies()) {
                sendResponse(exchange, 400, "{\"error\":\"Cannot delete book because one or more copies are currently borrowed.\"}", "application/json");
                return;
            }

            bookService.deleteBook(bookId);
            sendResponse(exchange, 200, "{\"success\":true}", "application/json");
        }

        private void handleGetStudents(HttpExchange exchange) throws IOException {
            List<Student> students = studentService.getAllStudents();
            String json = students.stream()
                    .map(LibraryServer::studentToJson)
                    .collect(Collectors.joining(","));
            sendResponse(exchange, 200, "[" + json + "]", "application/json");
        }

        private void handleRegisterStudent(HttpExchange exchange) throws IOException {
            String body = readRequestBody(exchange);
            String name = getJsonField(body, "name");
            String department = getJsonField(body, "department");
            String phone = getJsonField(body, "phoneNumber");
            String email = getJsonField(body, "emailAddress");

            if (ValidationUtil.isEmpty(name) || ValidationUtil.isEmpty(department)) {
                sendResponse(exchange, 400, "{\"error\":\"Name and department are required.\"}", "application/json");
                return;
            }
            if (!ValidationUtil.isValidEmail(email)) {
                sendResponse(exchange, 400, "{\"error\":\"Invalid email address format.\"}", "application/json");
                return;
            }
            if (!ValidationUtil.isValidPhone(phone)) {
                sendResponse(exchange, 400, "{\"error\":\"Phone number must be exactly 10 digits.\"}", "application/json");
                return;
            }

            List<String> existingIds = studentService.getAllStudents().stream()
                    .map(Student::getStudentId)
                    .collect(Collectors.toList());
            String studentId = IdGenerator.generateId(existingIds, "ST");

            Student student = new Student(studentId, name, department, phone, email);
            Student registered = studentService.registerStudent(student);

            sendResponse(exchange, 201, studentToJson(registered), "application/json");
        }

        private void handleGetIssues(HttpExchange exchange) throws IOException {
            List<IssueRecord> issues = issueService.getAllIssues();
            String json = issues.stream()
                    .map(LibraryServer::recordToJson)
                    .collect(Collectors.joining(","));
            sendResponse(exchange, 200, "[" + json + "]", "application/json");
        }

        private void handleBorrow(HttpExchange exchange) throws IOException {
            String body = readRequestBody(exchange);
            String studentId = getJsonField(body, "studentId");
            String bookId = getJsonField(body, "bookId");

            if (ValidationUtil.isEmpty(studentId) || ValidationUtil.isEmpty(bookId)) {
                sendResponse(exchange, 400, "{\"error\":\"Student ID and Book ID are required.\"}", "application/json");
                return;
            }

            try {
                IssueRecord record = issueService.borrowBook(studentId, bookId);
                sendResponse(exchange, 201, recordToJson(record), "application/json");
            } catch (StudentNotFoundException | BookNotFoundException | BookUnavailableException | BorrowLimitExceededException e) {
                sendResponse(exchange, 400, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}", "application/json");
            }
        }

        private void handleReturn(HttpExchange exchange) throws IOException {
            String body = readRequestBody(exchange);
            String issueId = getJsonField(body, "issueId");

            if (ValidationUtil.isEmpty(issueId)) {
                sendResponse(exchange, 400, "{\"error\":\"Issue ID is required.\"}", "application/json");
                return;
            }

            try {
                IssueRecord record = issueService.returnBook(issueId);
                sendResponse(exchange, 200, recordToJson(record), "application/json");
            } catch (Exception e) {
                sendResponse(exchange, 400, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}", "application/json");
            }
        }

        private void handleUndo(HttpExchange exchange) throws IOException {
            boolean success = undoService.undoLast(bookRepository, studentRepository);
            sendResponse(exchange, 200, "{\"success\":" + success + ",\"remainingUndo\":" + undoService.size() + "}", "application/json");
        }

        private void handleUndoSize(HttpExchange exchange) throws IOException {
            sendResponse(exchange, 200, "{\"size\":" + undoService.size() + "}", "application/json");
        }

        private void handleGetLogs(HttpExchange exchange) throws IOException {
            File logFile = new File(Constants.LOGS_FILE);
            List<String> logLines = new ArrayList<>();
            if (logFile.exists()) {
                try (BufferedReader br = new BufferedReader(new FileReader(logFile))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        logLines.add(line);
                    }
                }
            }
            
            // Limit to last 100 log lines to optimize payload size
            int startIdx = Math.max(0, logLines.size() - 100);
            List<String> slice = logLines.subList(startIdx, logLines.size());

            String logsJson = slice.stream()
                    .map(line -> "\"" + escapeJson(line) + "\"")
                    .collect(Collectors.joining(","));

            sendResponse(exchange, 200, "[" + logsJson + "]", "application/json");
        }
    }

    private static String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody();
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = is.read(buffer)) > -1) {
                bos.write(buffer, 0, len);
            }
            return bos.toString("UTF-8");
        }
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String responseText, String contentType) throws IOException {
        byte[] responseBytes = responseText.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    private static String escapeJson(String str) {
        if (str == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            switch (c) {
                case '\\': sb.append("\\\\"); break;
                case '"': sb.append("\\\""); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < ' ') {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                    break;
            }
        }
        return sb.toString();
    }

    private static String bookToJson(Book book) {
        return String.format(
            "{\"bookId\":\"%s\",\"isbn\":\"%s\",\"title\":\"%s\",\"author\":\"%s\",\"category\":\"%s\",\"publisher\":\"%s\",\"publicationYear\":%d,\"totalCopies\":%d,\"availableCopies\":%d,\"shelfNumber\":\"%s\"}",
            escapeJson(book.getBookId()),
            escapeJson(book.getIsbn()),
            escapeJson(book.getTitle()),
            escapeJson(book.getAuthor()),
            book.getCategory() != null ? book.getCategory().name() : "",
            escapeJson(book.getPublisher()),
            book.getPublicationYear(),
            book.getTotalCopies(),
            book.getAvailableCopies(),
            escapeJson(book.getShelfNumber())
        );
    }

    private static String studentToJson(Student s) {
        String borrowedIdsJson = s.getBorrowedBookIds() != null 
                ? s.getBorrowedBookIds().stream()
                    .map(id -> "\"" + escapeJson(id) + "\"")
                    .collect(Collectors.joining(","))
                : "";
        return String.format(
            "{\"studentId\":\"%s\",\"name\":\"%s\",\"department\":\"%s\",\"phoneNumber\":\"%s\",\"emailAddress\":\"%s\",\"borrowedBookIds\":[%s]}",
            escapeJson(s.getStudentId()),
            escapeJson(s.getName()),
            escapeJson(s.getDepartment()),
            escapeJson(s.getPhoneNumber()),
            escapeJson(s.getEmailAddress()),
            borrowedIdsJson
        );
    }

    private static String recordToJson(IssueRecord r) {
        return String.format(
            "{\"issueId\":\"%s\",\"studentId\":\"%s\",\"bookId\":\"%s\",\"issueDate\":\"%s\",\"dueDate\":\"%s\",\"returnDate\":\"%s\",\"fineAmount\":%.2f,\"active\":%b}",
            escapeJson(r.getIssueId()),
            escapeJson(r.getStudentId()),
            escapeJson(r.getBookId()),
            DateUtil.formatDate(r.getIssueDate()),
            DateUtil.formatDate(r.getDueDate()),
            r.getReturnDate() != null ? DateUtil.formatDate(r.getReturnDate()) : "",
            r.getFineAmount(),
            r.isActive()
        );
    }

    private static String getJsonField(String json, String fieldName) {
        if (json == null || fieldName == null || json.trim().isEmpty()) {
            return "";
        }
        int keyIdx = json.indexOf("\"" + fieldName + "\"");
        if (keyIdx == -1) {
            return "";
        }
        int colonIdx = json.indexOf(':', keyIdx + fieldName.length() + 2);
        if (colonIdx == -1) {
            return "";
        }
        int idx = colonIdx + 1;
        while (idx < json.length() && Character.isWhitespace(json.charAt(idx))) {
            idx++;
        }
        if (idx >= json.length()) {
            return "";
        }
        if (json.charAt(idx) == '"') {
            idx++;
            StringBuilder sb = new StringBuilder();
            while (idx < json.length()) {
                char c = json.charAt(idx);
                if (c == '"') {
                    return sb.toString();
                }
                if (c == '\\' && idx + 1 < json.length()) {
                    char next = json.charAt(idx + 1);
                    if (next == '"') { sb.append('"'); idx += 2; continue; }
                    else if (next == '\\') { sb.append('\\'); idx += 2; continue; }
                    else if (next == '/') { sb.append('/'); idx += 2; continue; }
                    else if (next == 'b') { sb.append('\b'); idx += 2; continue; }
                    else if (next == 'f') { sb.append('\f'); idx += 2; continue; }
                    else if (next == 'n') { sb.append('\n'); idx += 2; continue; }
                    else if (next == 'r') { sb.append('\r'); idx += 2; continue; }
                    else if (next == 't') { sb.append('\t'); idx += 2; continue; }
                    else if (next == 'u' && idx + 5 < json.length()) {
                        try {
                            int hex = Integer.parseInt(json.substring(idx + 2, idx + 6), 16);
                            sb.append((char) hex);
                            idx += 6;
                            continue;
                        } catch (NumberFormatException e) {}
                    }
                }
                sb.append(c);
                idx++;
            }
            return sb.toString();
        } else {
            StringBuilder sb = new StringBuilder();
            while (idx < json.length()) {
                char c = json.charAt(idx);
                if (c == ',' || c == '}' || c == ']' || Character.isWhitespace(c)) {
                    break;
                }
                sb.append(c);
                idx++;
            }
            return sb.toString().trim();
        }
    }

    private static String getQueryParameter(String query, String parameterName) {
        if (query == null || query.trim().isEmpty()) {
            return "";
        }

        for (String parameter : query.split("&")) {
            String[] pair = parameter.split("=", 2);
            if (pair.length == 2 && pair[0].equals(parameterName)) {
                try {
                    return java.net.URLDecoder.decode(pair[1], "UTF-8").trim();
                } catch (UnsupportedEncodingException e) {
                    return pair[1].trim();
                }
            }
        }
        return "";
    }
}
