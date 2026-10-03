# Smart Library Management System

A full-stack, lightweight web application built in Java 8+ to demonstrate core software design patterns, custom HTTP request handling, file-based persistence, and a responsive single-page frontend.

---

## 1. Executive Summary & Core Stack

The **Smart Library Management System** provides catalog management, student registration, borrowing and returning workflows, automated overdue fine calculations, persistent activity logging, and administrative undo operations.

- **Zero External Dependencies**: Built using standard Java JDK libraries (`com.sun.net.httpserver.HttpServer`, `java.util.concurrent`, `java.nio`) without frameworks like Spring, Hibernate, or Jackson.
- **Framework-Free Java Backend**: Runs a light HTTP server listening on port `8080`.
- **Flat-File Storage**: Stores records in pipe-delimited (`|`) text files in `SRC/data/` with single-file atomic writes.
- **Single-Page Application (SPA)**: Dynamic web frontend rendering views via dynamic REST API requests (`fetch()` API) without full page reloads.

---

## 2. Features

### Public Portal
- **Book Search & Catalog Browsing**: Search books by title, author, ISBN, or ID, filtered by category.
- **Student Profile Lookup**: View student details and active borrowed books by Student ID.
- **Dark Glassmorphism UI**: High-contrast, responsive user interface.

### Admin Dashboard
- **Admin Authentication**: Bearer-token authentication for library administrators.
- **Catalog Management**: Add new books, update existing book details, and delete books (guarded against active borrows).
- **Student Management**: Register new students with phone number and email validation.
- **Circulation (Borrow & Return)**: Issue books to students (enforces max 3 books per student & duplicate borrow guard) and process returns with automatic overdue fine calculation (Rs. 10.00/day after 14 days).
- **Undo Operations**: Reverse the last administrative operation (book addition/update/deletion or student registration) via an in-memory LIFO stack.
- **System Activity Log**: View up to the last 100 log entries directly in an embedded terminal component.

### Screenshots
*Screenshots available in the `docs/screenshots/` directory (e.g., `docs/screenshots/public_search.png`, `docs/screenshots/admin_dashboard.png`).*

---

## 3. System Architecture

```mermaid
graph TD
    UI["Web Frontend (HTML / CSS / Vanilla JS)<br/>web/index.html & web/app.js"]
    
    subgraph "Java Backend Engine (Port 8080)"
        HTTP["HTTP Server & Controller Layer<br/>LibraryServer.java (com.sun.net.httpserver.HttpServer)"]
        
        subgraph "Service Layer (Business Logic)"
            AUTH_S["AuthService"]
            BOOK_S["BookService"]
            STUD_S["StudentService"]
            ISSUE_S["IssueService"]
            UNDO_S["UndoService"]
        end
        
        subgraph "Repository Layer (Data Access)"
            ADMIN_R["AdminRepository"]
            BOOK_R["BookRepository"]
            STUD_R["StudentRepository"]
            ISSUE_R["IssueRecordRepository"]
        end
        
        subgraph "Utility & Cross-Cutting Layer"
            VAL["ValidationUtil"]
            DATE["DateUtil"]
            LOG["LogUtil"]
            IDGEN["IdGenerator"]
        end
    end

    subgraph "Persistence Storage (Flat Files in SRC/data/)"
        F_ADMIN["admins.txt"]
        F_BOOK["books.txt"]
        F_STUD["students.txt"]
        F_ISSUE["issues.txt"]
        F_LOG["logs.txt"]
    end

    UI -->|"HTTP REST API (JSON / Bearer Token)"| HTTP
    
    HTTP --> AUTH_S
    HTTP --> BOOK_S
    HTTP --> STUD_S
    HTTP --> ISSUE_S
    HTTP --> UNDO_S
    HTTP --> VAL
    HTTP --> IDGEN
    HTTP --> DATE
    HTTP --> LOG

    AUTH_S --> ADMIN_R
    BOOK_S --> BOOK_R
    BOOK_S --> UNDO_S
    BOOK_S --> VAL
    BOOK_S --> LOG
    
    STUD_S --> STUD_R
    STUD_S --> UNDO_S
    STUD_S --> VAL
    STUD_S --> LOG
    
    ISSUE_S --> ISSUE_R
    ISSUE_S --> BOOK_R
    ISSUE_S --> STUD_R
    ISSUE_S --> DATE
    ISSUE_S --> IDGEN
    ISSUE_S --> LOG

    UNDO_S --> BOOK_R
    UNDO_S --> STUD_R
    UNDO_S --> LOG

    ADMIN_R --> F_ADMIN
    BOOK_R --> F_BOOK
    STUD_R --> F_STUD
    ISSUE_R --> F_ISSUE
    LOG --> F_LOG
```

---

## 4. Directory Structure

```
SmartLibraryManagementSystem/
├── bin/                          # Compiled Java bytecode (.class files)
├── web/                          # Web frontend SPA assets
│   ├── index.html                # Main UI shell (Public Search, Admin Dashboard, Modals)
│   ├── style.css                 # Dark Glassmorphism stylesheet
│   └── app.js                    # SPA logic, HTTP fetch calls, XSS-safe DOM renderer
├── SRC/                          # Java backend source code
│   ├── LibraryServer.java        # Main entry point, HTTP Router, StaticFileHandler, ApiHandler
│   ├── config/
│   │   └── Constants.java        # Configuration constants & file paths
│   ├── exception/                # Custom domain runtime exceptions
│   │   ├── BookNotFoundException.java
│   │   ├── BookUnavailableException.java
│   │   ├── BorrowLimitExceededException.java
│   │   └── StudentNotFoundException.java
│   ├── model/                    # Domain POJOs
│   │   ├── Admin.java
│   │   ├── Book.java
│   │   ├── BookCategory.java
│   │   ├── IssueRecord.java
│   │   ├── Student.java
│   │   └── UndoOperation.java
│   ├── repository/               # File persistence repositories
│   │   ├── AdminRepository.java
│   │   ├── BookRepository.java
│   │   ├── IssueRecordRepository.java
│   │   └── StudentRepository.java
│   ├── service/                  # Business logic services
│   │   ├── AuthService.java
│   │   ├── BookService.java
│   │   ├── IssueService.java
│   │   ├── StudentService.java
│   │   └── UndoService.java
│   ├── util/                     # Utility classes
│   │   ├── DateUtil.java
│   │   ├── IdGenerator.java
│   │   ├── LogUtil.java
│   │   └── ValidationUtil.java
│   ├── data/                     # Live flat-file storage directory (.txt files)
│   │   ├── admins.txt
│   │   ├── books.txt
│   │   ├── issues.txt
│   │   ├── logs.txt
│   │   └── students.txt
│   └── data.sample/              # Clean sample template data files for initialization/reset
├── build.bat / build.sh          # Build scripts (Windows / Linux)
├── run.bat / run.sh              # Execution scripts (Windows / Linux)
├── reset-data.bat / reset-data.sh # Sample data reset scripts
├── .gitignore                    # Version control ignore rules
├── README.md                     # Comprehensive technical overview
└── viva_flow_diagram_guide.md    # Architecture & Viva Defense Preparation Guide
```

---

## 5. Configuration

All global system parameters are defined in `SRC/config/Constants.java`:

| Constant Name | Type | Value | Description |
| :--- | :---: | :---: | :--- |
| `DATA_DIR` | `String` | `"SRC/data/"` | Directory path for live text database files |
| `BOOKS_FILE` | `String` | `"SRC/data/books.txt"` | File path for book catalog records |
| `STUDENTS_FILE` | `String` | `"SRC/data/students.txt"` | File path for student records |
| `ADMINS_FILE` | `String` | `"SRC/data/admins.txt"` | File path for admin credentials |
| `ISSUES_FILE` | `String` | `"SRC/data/issues.txt"` | File path for circulation issue records |
| `LOGS_FILE` | `String` | `"SRC/data/logs.txt"` | File path for persistent log entries |
| `WAITING_LIST_FILE` | `String` | `"SRC/data/waiting_lists.txt"` | Reserved file path for waiting list features |
| `DEFAULT_BORROW_DURATION_DAYS` | `int` | `14` | Default loan period before fines accumulate |
| `FINE_PER_DAY` | `double` | `10.0` | Overdue fine rate (Rs. 10.00 per day past due date) |
| `MAX_BORROW_LIMIT` | `int` | `3` | Maximum active borrowed books permitted per student |
| `DATE_FORMAT` | `String` | `"yyyy-MM-dd"` | Strict ISO-8601 date format pattern |
| `FILE_DELIMITER` | `String` | `"\|"` | Field separator string used in text database |
| `FILE_DELIMITER_REGEX` | `String` | `"\\\|"` | Unescaped regex string for parsing file fields |

*Note: Modifying constant values requires recompiling the project Java source files (`build.bat` or `build.sh`).*

---

## 6. Quick Start Guide

### Prerequisites
- Java Development Kit (JDK) 8 or higher installed and added to your system `PATH`.

### Working Directory Requirement
> [!IMPORTANT]
> The application uses relative file paths (`web/` and `SRC/data/`). You **MUST** run all build and execution commands with your current working directory set to the project root (`SmartLibraryManagementSystem/`).

### Windows (cmd or PowerShell)
1. Open terminal in project root directory:
2. Build the project:
   ```cmd
   build.bat
   ```
3. Start the HTTP server:
   ```cmd
   run.bat
   ```
4. Access the web interface in your browser:
   - **Public Portal**: `http://localhost:8080/`
   - **Admin Dashboard**: Click "Admin Login" (Default Username: `admin`, Password: `admin123`).

### Linux / macOS
1. Open terminal in project root directory:
2. Grant execute permissions:
   ```bash
   chmod +x *.sh
   ```
3. Build the project:
   ```bash
   ./build.sh
   ```
4. Start the HTTP server:
   ```bash
   ./run.sh
   ```

> [!WARNING]
> Default admin credentials (`admin` / `admin123`) are provided strictly for local development and viva evaluation. They must be changed before deploying in any production environment.

---

## 7. Component Breakdown

### A. HTTP & Controller Layer (`SRC/LibraryServer.java`)
- **`StaticFileHandler`**: Resolves files against `web/`. Employs path canonicalization (`webRoot.getCanonicalFile()`) and null byte checks (`\0`) to reject path traversal attempts (returning HTTP 403). Sets CORS header `Access-Control-Allow-Origin: *`.
- **`ApiHandler`**: Handles REST requests under `/api/`. Performs input parsing, authentication token validation, CORS header injection, and parameter validation using `ValidationUtil`. Converts domain runtime exceptions into HTTP error responses.

### B. Business Services Layer (`SRC/service/`)
- **`AuthService`**: Manages active admin sessions in a `ConcurrentHashMap<String, String>` mapping generated UUID session tokens to usernames. *Note: Session tokens are stored in memory and do not expire automatically; there is currently no `/api/auth/logout` HTTP endpoint.*
- **`BookService`**: Executes catalog CRUD operations. Snapshots previous states into `UndoService` prior to mutations.
- **`StudentService`**: Registers new students and updates profiles. Snapshots state into `UndoService`.
- **`IssueService`**: Enforces borrowing business constraints (maximum 3 active borrows per student, duplicate borrow guard, stock availability), generates 14-day due dates, and processes book returns with fine calculations.
- **`UndoService`**: Maintains a LIFO `Stack<UndoOperation>` for administrative undo actions.

### C. Data Access & Persistence Layer (`SRC/repository/`)
- Reads and writes pipe-delimited records in `SRC/data/`.
- **Field Escaping**: Escapes `|` as `\|` and `\n` as `\n` to prevent flat-file structure corruption.
- **Atomic Single-File Writes**: Writes updated content to a temporary file (`.tmp.<timestamp>`) and uses `Files.move(..., REPLACE_EXISTING, ATOMIC_MOVE)` (falling back to standard move if atomic move is unsupported). *Note: This guarantees individual file integrity against half-written files during a write, but does not make multi-file updates (e.g. updating books, students, and issues during a borrow) atomic across files or sync file descriptors to physical disk.*

---

## 8. Error Handling & REST API Reference

### Error JSON Response Format
All API error responses follow a consistent JSON structure:
```json
{
  "error": "Detailed error message describing the failure"
}
```

### Exception to HTTP Status Mapping

| Exception / Event | Mapped HTTP Status | Cause |
| :--- | :---: | :--- |
| `StudentNotFoundException` | `400 Bad Request` | Student ID does not exist during borrow/return |
| `BookNotFoundException` | `400 Bad Request` / `404 Not Found` | Book ID not found during issue (400) or GET/PUT/DELETE (404) |
| `BookUnavailableException` | `400 Bad Request` | Book available copies count is zero during issue |
| `BorrowLimitExceededException` | `400 Bad Request` | Student has 3 active borrows or attempts duplicate borrow |
| `IllegalStateException` | `400 Bad Request` | Book has already been returned |
| Validation Failures | `400 Bad Request` | Missing required fields, invalid phone/email/year format |
| Invalid Bearer Token | `401 Unauthorized` | Missing or invalid `Authorization: Bearer <token>` header |
| Forbidden Path | `403 Forbidden` | Path traversal attempt outside `web/` root directory |
| Unknown Route / File | `404 Not Found` | Non-existent API endpoint or static web file |
| Internal Server Error | `500 Internal Error` | Uncaught server exceptions or file I/O errors |

### REST API Specifications

| Endpoint Path | Method | Auth Required | Request Body / Query | Success Code | Error Codes | Description |
| :--- | :---: | :---: | :--- | :---: | :---: | :--- |
| `/api/auth/login` | `POST` | Public | `{"username":"admin","password":"..."}` | `200 OK` | `400`, `401` | Authenticates admin and returns UUID token |
| `/api/books` | `GET` | Public | `?q=search&category=TECHNOLOGY` | `200 OK` | `500` | Searches & lists catalog books |
| `/api/public/student` | `GET` | Public | `?id=ST001` | `200 OK` | `400`, `404` | Public profile & active issue lookup |
| `/api/books` | `POST` | Admin | Book JSON object | `201 Created` | `400`, `401` | Adds new book to catalog |
| `/api/books` | `PUT` | Admin | Updated Book JSON object | `200 OK` | `400`, `401`, `404` | Modifies existing book details |
| `/api/books` | `DELETE` | Admin | `?id=BK001` | `200 OK` | `400`, `401`, `404` | Deletes book (guarded against active borrows) |
| `/api/students` | `GET` | Admin | None | `200 OK` | `401` | Retrieves all registered students |
| `/api/students` | `POST` | Admin | Student JSON object | `201 Created` | `400`, `401` | Registers new student |
| `/api/issues` | `GET` | Admin | None | `200 OK` | `401` | Retrieves all issue records |
| `/api/issues/borrow` | `POST` | Admin | `{"studentId":"ST001","bookId":"BK001"}` | `201 Created` | `400`, `401` | Issues a book copy to student |
| `/api/issues/return` | `POST` | Admin | `{"issueId":"IS001"}` | `200 OK` | `400`, `401` | Returns book and calculates fine |
| `/api/admin/undo` | `POST` | Admin | None | `200 OK` | `400`, `401` | Reverses last administrative operation |
| `/api/admin/undo/size` | `GET` | Admin | None | `200 OK` | `401` | Returns total remaining undo operations |
| `/api/logs` | `GET` | Admin | None | `200 OK` | `401` | Retrieves last 100 log lines |

---

## 9. API Usage Examples (curl)

### 1. Admin Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"admin\",\"password\":\"admin123\"}"
```
*Response*: `{"token":"550e8400-e29b-41d4-a716-446655440000"}`

### 2. Search Books Catalog
```bash
curl -X GET "http://localhost:8080/api/books?q=Clean&category=TECHNOLOGY"
```

### 3. Register Student (Admin Protected)
```bash
curl -X POST http://localhost:8080/api/students \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer 550e8400-e29b-41d4-a716-446655440000" \
  -d "{\"name\":\"Alice Smith\",\"department\":\"Computer Science\",\"phoneNumber\":\"9876543210\",\"emailAddress\":\"alice@example.com\"}"
```

### 4. Issue Book (Admin Protected)
```bash
curl -X POST http://localhost:8080/api/issues/borrow \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer 550e8400-e29b-41d4-a716-446655440000" \
  -d "{\"studentId\":\"ST001\",\"bookId\":\"BK001\"}"
```

### 5. Return Book (Admin Protected)
```bash
curl -X POST http://localhost:8080/api/issues/return \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer 550e8400-e29b-41d4-a716-446655440000" \
  -d "{\"issueId\":\"IS001\"}"
```

---

## 10. Data Persistence & File Schemas

All data files are stored as pipe-delimited text in `SRC/data/`:

- **`books.txt`**: `BookID|ISBN|Title|Author|Category|Publisher|Year|TotalCopies|AvailableCopies|ShelfNumber`
  ```text
  BK001|978-0134685991|Effective Java|Joshua Bloch|TECHNOLOGY|Addison-Wesley|2018|5|4|A-12
  ```
- **`students.txt`**: `StudentID|Name|Department|PhoneNumber|EmailAddress|BorrowedBookIds`
  ```text
  ST001|Tejesh Reddy|Computer Science|9876543210|tejesh.reddy@example.com|BK001
  ```
- **`issues.txt`**: `IssueID|StudentID|BookID|IssueDate|DueDate|ReturnDate|FineAmount`
  - *Active Issue Row*:
    ```text
    IS001|ST001|BK001|2026-09-01|2026-09-15||0.0
    ```
  - *Returned Issue Row*:
    ```text
    IS001|ST001|BK001|2026-09-01|2026-09-15|2026-09-14|0.0
    ```
- **`admins.txt`**: `AdminID|Username|Password`
  ```text
  AD001|admin|admin123
  ```
- **`logs.txt`**: `[yyyy-MM-dd HH:mm:ss] [LEVEL] Log Message`
  ```text
  [2026-10-03 20:40:12] [INFO] Server started on http://localhost:8080
  ```

---

## 11. Security & Data Integrity Features

### Implemented Controls
1. **Path Traversal Prevention**: `StaticFileHandler` verifies canonical file paths against `webRoot.getCanonicalFile()` and rejects null byte injections (`\0`), returning `403 Forbidden`.
2. **XSS Protection**: Frontend (`web/app.js`) implements `escapeHtml()` sanitization across all dynamically rendered table rows and profile elements.
3. **Single-File Atomic Writes**: Repositories write to temporary files (`.tmp.<timestamp>`) and swap using `Files.move(..., ATOMIC_MOVE)` to prevent file truncation.
4. **Delimiter Escaping**: Embedded pipe (`|`) and newline (`\n`) characters in strings are escaped to preserve schema structure.
5. **Duplicate Borrow Guard**: Prevents students from borrowing duplicate active copies of the same book.

### Not Implemented / Known Security Gaps
1. **Plaintext Passwords**: Admin passwords are stored in plaintext in `admins.txt`.
2. **Session Lifecycle**: Bearer tokens are stored in memory without TTL expiration or an HTTP logout endpoint.
3. **Public Student Lookup**: Unauthenticated access to public student profiles using only a Student ID (`ST001`).
4. **CORS Wildcard**: Global `Access-Control-Allow-Origin: *` header returned on all API responses.
5. **Multi-File Atomicity**: Modifying `books.txt`, `students.txt`, and `issues.txt` during borrow/return involves separate file writes, which are not atomic across all three files together.

---

## 12. Testing

There is currently **no automated unit test or integration test suite** included in the core codebase. Verification is performed manually or via HTTP API tools (`curl` / Postman).

### Manual Verification Matrix

| Test Scenario | Executed Command | Expected Status | Observed Result | Status |
| :--- | :--- | :---: | :---: | :---: |
| Path Traversal (`/../admins.txt`) | `curl.exe --path-as-is -i http://localhost:8080/../SRC/data/admins.txt` | `403 Forbidden` | `403 Forbidden` | PASS |
| Encoded Traversal (`/..%2f`) | `curl.exe --path-as-is -i http://localhost:8080/..%2fSRC/data/admins.txt` | `403 Forbidden` | `403 Forbidden` | PASS |
| Hex Traversal (`/%2e%2e/`) | `curl.exe --path-as-is -i http://localhost:8080/%2e%2e/SRC/data/admins.txt` | `403 Forbidden` | `403 Forbidden` | PASS |
| Source Code Access | `curl.exe --path-as-is -i http://localhost:8080/../SRC/LibraryServer.java` | `403 Forbidden` | `403 Forbidden` | PASS |
| Public Static Index | `curl.exe -i http://localhost:8080/` | `200 OK` | `200 OK` | PASS |
| Missing Auth Token | `curl.exe -i http://localhost:8080/api/students` | `401 Unauthorized` | `401 Unauthorized` | PASS |
| Duplicate Borrow Guard | `POST /api/issues/borrow` (same student & book twice) | `400 Bad Request` | `400 Bad Request` | PASS |
| Double Return Guard | `POST /api/issues/return` (same issue twice) | `400 Bad Request` | `400 Bad Request` | PASS |

---

## 13. Known Limitations & Roadmap

| Limitation | System Impact | Proposed Production Fix |
| :--- | :--- | :--- |
| **Plaintext Admin Passwords** | Credentials visible in `admins.txt` | Hash stored passwords using BCrypt / PBKDF2 |
| **In-Memory Session Storage** | Tokens lost on restart; no automatic timeout or logout endpoint | Implement server session TTL timeouts and `/api/auth/logout` endpoint |
| **Public Student Lookup** | Anyone can view student profiles knowing only `StudentID` | Require Student ID + Phone/Email verification check |
| **Multi-File Non-Atomic Writes** | Server crash mid-operation can cause partial data updates across files | Wrap operations in multi-entity transaction logs or migrate to SQLite |
| **ID Generator Reuse** | Deleting the highest ID book allows ID reuse | Track auto-increment counter sequence separately |
| **Undo Scope Limitations** | Undo only handles book CRUD and student registration, not borrow/returns | Implement full event-sourcing transaction log |
| **$O(N)$ Flat File Scanning** | Performance degrades with large datasets | Add indexing or migrate to an indexed SQL database |
| **Single-Threaded Executor** | Server uses default single-thread handler (`setExecutor(null)`) | Attach thread pool executor (`Executors.newFixedThreadPool(10)`) |

---

## 14. Troubleshooting

- **Port 8080 Already in Use**:
  If another process is using port 8080, terminate it using `netstat -ano | findstr 8080` and `taskkill /F /PID <PID>` on Windows, or `lsof -i :8080` and `kill -9 <PID>` on Linux.
- **"Could not find or load main class SRC.LibraryServer"**:
  Ensure your terminal working directory is set to the project root directory (`SmartLibraryManagementSystem/`) before running Java commands.
- **Java Version Errors**:
  Ensure JDK 8 or higher is installed (`java -version` and `javac -version`).
- **Resetting Data Directory**:
  To restore sample database files, run `reset-data.bat` (Windows) or `./reset-data.sh` (Linux/macOS).
- **Backing Up Live Data**:
  Copy the `SRC/data/` folder to a secure backup directory before performing manual testing or updates.

---

## 15. Documentation & References

- [Technical Overview (README.md)](file:///d:/Users/TEJESH/Documents/SmartLibraryManagementSystem/README.md)
- [Architecture & Viva Preparation Guide (viva_flow_diagram_guide.md)](file:///d:/Users/TEJESH/Documents/SmartLibraryManagementSystem/viva_flow_diagram_guide.md)
