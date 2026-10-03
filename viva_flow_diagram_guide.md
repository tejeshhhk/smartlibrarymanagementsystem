# Smart Library Management System — Viva & Architecture Guide (v1.4)

> [!NOTE]
> This guide provides a complete architectural breakdown, module-by-module execution flows, sequence diagrams, data persistence specs, and viva/interview preparation Q&As for the **Smart Library Management System**.

---

## 1. System Architecture Overview

The system follows a classic **4-Layer Architecture** decoupled into a lightweight HTML/JS frontend and a pure Java HTTP server backend.

```mermaid
graph TD
    UI["Web Frontend (HTML / CSS / JS Vanilla)<br/>web/index.html & web/app.js"]
    
    subgraph "Java Backend Engine (Port 8080)"
        HTTP["HTTP Server & Controller Layer<br/>LibraryServer.java"]
        
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

## 2. Module-by-Module Flow Diagrams

### A. Authentication Module Flow

This module handles admin login and session token generation using memory-bound tokens stored in `ConcurrentHashMap<String, String>`.

```mermaid
sequenceDiagram
    autonumber
    actor Admin as Admin User
    participant JS as Frontend (app.js)
    participant Server as LibraryServer (/api/auth/login)
    participant Auth as AuthService
    participant Repo as AdminRepository
    participant File as admins.txt

    Admin->>JS: Enters username & password
    JS->>Server: POST /api/auth/login {username, password}
    Server->>Auth: login(username, password)
    Auth->>Repo: findByUsername(username)
    Repo->>File: Reads & parses admins.txt
    File-->>Repo: List of Admin records
    Repo-->>Auth: Optional<Admin>
    Auth->>Auth: Validate plaintext password against stored password
    alt Credentials Valid
        Auth->>Auth: Generate UUID Token & store in activeSessions Map
        Auth-->>Server: Return token string
        Server-->>JS: HTTP 200 { "token": "..." }
        JS->>JS: Save token in localStorage & show Admin Dashboard
    else Credentials Invalid
        Auth-->>Server: Return null
        Server-->>JS: HTTP 401 { "error": "Invalid username or password" }
        JS-->>Admin: Display error toast alert
    end
```

---

### B. Book Issue Module Flow (Borrow Operation)

Handles student book borrowing, enforcing maximum borrow limit (3 books), duplicate borrow checks, and stock availability.

```mermaid
sequenceDiagram
    autonumber
    actor Admin as Admin User
    participant JS as Frontend (app.js)
    participant Server as LibraryServer (/api/issues/borrow)
    participant IssueS as IssueService
    participant BookR as BookRepository
    participant StudR as StudentRepository
    participant IssueR as IssueRecordRepository

    Admin->>JS: Enter Student ID & Book ID -> Click Issue Book
    JS->>Server: POST /api/issues/borrow {studentId, bookId} (Header: Bearer Token)
    Server->>Server: Validate Bearer Auth Token via AuthService
    Server->>IssueS: borrowBook(studentId, bookId)
    
    IssueS->>StudR: findById(studentId)
    alt Student Not Found
        StudR-->>IssueS: Optional.empty()
        IssueS-->>Server: Throws StudentNotFoundException
    end
    
    IssueS->>BookR: findById(bookId)
    alt Book Not Found or Available Copies < 1
        BookR-->>IssueS: Optional.empty() / 0 copies
        IssueS-->>Server: Throws BookNotFoundException / BookUnavailableException
    end
    
    IssueS->>IssueR: findByStudentId(studentId)
    alt Active borrows >= 3 (Max limit) or Duplicate active borrow exists
        IssueR-->>IssueS: Active list size >= 3 or duplicate book match
        IssueS-->>Server: Throws BorrowLimitExceededException
    end

    IssueS->>IssueS: Calculate Issue Date (Today) & Due Date (Today + 14 Days)
    IssueS->>IssueR: save(new IssueRecord)
    IssueR->>IssueR: Write to issues.txt (Atomic move via .tmp)
    IssueS->>BookR: save(updated Book with availableCopies - 1)
    BookR->>BookR: Write to books.txt (Atomic move via .tmp)
    IssueS->>StudR: save(updated Student with borrowedBookIds)
    StudR->>StudR: Write to students.txt (Atomic move via .tmp)
    IssueS-->>Server: IssueRecord object
    Server-->>JS: HTTP 201 { "issueId": "IS001", ... }
    JS-->>Admin: Show success notification & refresh tables
```

---

### C. Book Return & Fine Calculation Module Flow

Handles book return, checks overdue status, calculates fines dynamically (Rs. 10.00/day), and restores available copy counts.

```mermaid
sequenceDiagram
    autonumber
    actor Admin as Admin User
    participant JS as Frontend (app.js)
    participant Server as LibraryServer (/api/issues/return)
    participant IssueS as IssueService
    participant DateU as DateUtil
    participant IssueR as IssueRecordRepository
    participant BookR as BookRepository
    participant StudR as StudentRepository

    Admin->>JS: Select active issue -> Click Return Book
    JS->>Server: POST /api/issues/return {issueId}
    Server->>IssueS: returnBook(issueId)
    IssueS->>IssueR: findById(issueId)
    
    alt Issue Not Found or Already Returned
        IssueS-->>Server: Throws IllegalStateException ("Book has already been returned.")
    end

    IssueS->>DateU: calculateFine(dueDate, returnDate, 10.0)
    alt Overdue Days > 0
        DateU-->>IssueS: Fine amount (overdueDays * 10.0)
    else On-time Return
        DateU-->>IssueS: Fine amount 0.0
    end
    
    IssueS->>IssueR: save(updated IssueRecord with returnDate & fineAmount)
    IssueR->>IssueR: Update issues.txt (Atomic move via .tmp)
    IssueS->>BookR: save(updated Book with availableCopies + 1)
    BookR->>BookR: Update books.txt (Atomic move via .tmp)
    IssueS->>StudR: save(updated Student with removed borrowedBookId)
    StudR->>StudR: Update students.txt (Atomic move via .tmp)
    IssueS-->>Server: IssueRecord summary object
    Server-->>JS: HTTP 200 { "issueId": "IS001", "fineAmount": 0.0, ... }
    JS-->>Admin: Display return summary toast notification
```

---

### D. Undo Mechanism Module Flow (LIFO Stack Pattern)

Allows reversing administrative mutations (adding/updating/deleting books or registering students) using an in-memory stack.

```mermaid
graph LR
    subgraph "Admin Action Triggered"
        A[Admin Mutates Book/Student] --> B[Execute Action in Service]
        B --> C[Push UndoOperation to Stack]
    end

    subgraph "Undo Triggered"
        D[Admin Clicks Undo] --> E[Pop UndoOperation from Stack]
        E --> F{Operation Type?}
        F -->|ADD_BOOK| G[Delete added book by ID]
        F -->|UPDATE_BOOK| H[Restore snapshot of previous book]
        F -->|DELETE_BOOK| I[Re-insert deleted book]
        F -->|REGISTER_STUDENT| J[Delete registered student by ID]
        G --> K[Persist updated state to file via atomic .tmp move]
        H --> K
        I --> K
        J --> K
    end
```

---

### E. Public Student Portal Lookup Flow

Unauthenticated endpoint allowing students to view their profile and borrowing history by Student ID.

```mermaid
sequenceDiagram
    autonumber
    actor Student as Student
    participant JS as Frontend (app.js)
    participant Server as LibraryServer (/api/public/student?id=ST001)
    participant StudS as StudentService
    participant IssueS as IssueService

    Student->>JS: Enters Student ID (e.g. ST001) in Public Portal
    JS->>Server: GET /api/public/student?id=ST001
    Server->>StudS: getStudentById("ST001")
    alt Student Not Found
        StudS-->>Server: Optional.empty()
        Server-->>JS: HTTP 404 { "error": "Student not found." }
    else Student Found
        StudS-->>Server: Student object
        Server->>IssueS: getAllIssues() (filtered by studentId)
        IssueS-->>Server: List of IssueRecord objects
        Server-->>JS: HTTP 200 { "student": {...}, "issues": [...] }
        JS-->>Student: Render student profile card and issues table
    end
```

---

## 3. Data Persistence & File Schema Specifications

The application uses plain text `.txt` files with line-separated, pipe-delimited (`|`) values stored in `SRC/data/`.

| Entity File | File Path | Field Schema Format | Sample Real Data Row |
| :--- | :--- | :--- | :--- |
| **Books** | `SRC/data/books.txt` | `BookID\|ISBN\|Title\|Author\|Category\|Publisher\|Year\|TotalCopies\|AvailableCopies\|ShelfNumber` | `BK001\|978-0134685991\|Effective Java\|Joshua Bloch\|TECHNOLOGY\|Addison-Wesley\|2018\|5\|5\|A-12` |
| **Students** | `SRC/data/students.txt` | `StudentID\|Name\|Department\|PhoneNumber\|EmailAddress\|BorrowedBookIds` | `ST001\|Tejesh Reddy\|Computer Science\|9876543210\|tejesh.reddy@example.com\|BK001` |
| **Issues (Active)** | `SRC/data/issues.txt` | `IssueID\|StudentID\|BookID\|IssueDate\|DueDate\|ReturnDate\|FineAmount` | `IS001\|ST001\|BK001\|2026-09-01\|2026-09-15\|\|0.0` |
| **Issues (Returned)** | `SRC/data/issues.txt` | `IssueID\|StudentID\|BookID\|IssueDate\|DueDate\|ReturnDate\|FineAmount` | `IS001\|ST001\|BK001\|2026-09-01\|2026-09-15\|2026-09-14\|0.0` |
| **Admins** | `SRC/data/admins.txt` | `AdminID\|Username\|Password` | `AD001\|admin\|admin123` |
| **Logs** | `SRC/data/logs.txt` | `[Timestamp] [LOG_LEVEL] Log Message` | `[2026-10-03 20:40:12] [INFO] Server started on http://localhost:8080` |

---

## 4. Key Business Rules Reference

1. **Borrow Limit**: Maximum **3 active (unreturned) books** per student (`MAX_BORROW_LIMIT = 3`).
2. **Duplicate Borrow Guard**: A student cannot borrow a second copy of a book they already currently hold.
3. **Loan Duration**: Fixed **14 calendar days** from the issue date (`DEFAULT_BORROW_DURATION_DAYS = 14`).
4. **Overdue Fine**: **₹10.00 per day** past the due date (`FINE_PER_DAY = 10.0`).
5. **Book Deletion Rule**: A book cannot be deleted if any copies are currently borrowed (`availableCopies != totalCopies`).
6. **Session Management**: Session tokens are stored in a `ConcurrentHashMap<String, String>`. Tokens do not expire automatically and there is no `/api/auth/logout` HTTP endpoint. Server restarts reset active sessions.
7. **Input Validation Rules**:
   - Phone number: Exactly 10 digits (`^\d{10}$`).
   - Email: Standard email regex (`ValidationUtil.isValidEmail`).
   - Publication year: Between 1000 and current year.

---

## 5. Comprehensive Viva & Interview Q&A Guide

### Section A: Architecture & Core Design

#### Q1: What architectural pattern does this project follow?
> **Answer:** The project follows a **4-Layer Architecture**:
> 1. **Presentation Layer**: HTML5, CSS3, and JavaScript (Vanilla SPA style).
> 2. **Controller/HTTP Layer**: Java's built-in `com.sun.net.httpserver.HttpServer` listening on port 8080 (`StaticFileHandler` and `ApiHandler`).
> 3. **Service Layer**: Business logic enforcement (`BookService`, `StudentService`, `IssueService`, `AuthService`, `UndoService`).
> 4. **Repository Layer**: Data persistence (`BookRepository`, `StudentRepository`, `AdminRepository`, `IssueRecordRepository`).

#### Q2: How is data written safely to flat files to prevent file corruption during writes?
> **Answer:** Repositories use an **Atomic Single-File Swap** strategy: records are serialized into a temporary file (`.tmp.<timestamp>`), and once fully written, `Files.move(..., REPLACE_EXISTING, ATOMIC_MOVE)` atomically replaces the original target file. This prevents half-written or corrupted files if an error occurs mid-write.

#### Q3: How is REST authentication handled without external libraries like Spring Security?
> **Answer:** Admin login requests sent to `/api/auth/login` authenticate against `admins.txt`. Upon success, `AuthService` generates a UUID token and stores it in an in-memory `ConcurrentHashMap<String, String>` mapping token to username. Protected API endpoints verify the token via the `Authorization: Bearer <token>` request header.

---

### Section B: Data Structures & Algorithms

#### Q4: How is the Undo feature implemented?
> **Answer:** The Undo feature uses a **LIFO (Last-In, First-Out) Stack** (`java.util.Stack<UndoOperation>`). When an administrator adds, updates, or deletes a book or registers a student, an `UndoOperation` storing operation metadata and state snapshots is pushed onto the stack. Clicking "Undo" pops the top operation and restores previous state in memory and text files.

#### Q5: What is the ID format and generation mechanism for entities?
> **Answer:** `IdGenerator.generateId(existingIds, prefix)` parses numeric suffixes across existing entity IDs, identifies the maximum numeric value, increments it by 1, and formats it with a 3-digit zero-padded prefix (e.g. `BK001`, `ST001`, `IS001`, `AD001`).

---

### Section C: Concurrency & Error Handling

#### Q6: How does the backend map domain exceptions to HTTP responses?
> **Answer:** `LibraryServer.ApiHandler` catches domain exceptions (`StudentNotFoundException`, `BookNotFoundException`, `BookUnavailableException`, `BorrowLimitExceededException`, `IllegalStateException`) and converts them into standard HTTP status codes (HTTP 400 Bad Request or HTTP 404 Not Found) with a structured JSON error body: `{"error":"<message>"}`.

#### Q7: How is Path Traversal security handled?
> **Answer:** `StaticFileHandler` resolves incoming path strings against `webRoot.getCanonicalFile()` and verifies that `requestedFile.getPath().startsWith(webRootPath)`. Requests attempting relative directory traversal (such as `/../SRC/data/admins.txt`) or containing null bytes are rejected with **HTTP 403 Forbidden**.

---

## 6. Quick Reference Summary

| Metric / Parameter | Value / Detail |
| :--- | :--- |
| **Backend Port** | `http://localhost:8080` |
| **Default Admin Credentials** | Username: `admin` \| Password: `admin123` |
| **JDK Requirement** | Java 8 or higher (No third-party libraries needed) |
| **Data Directory** | `SRC/data/` |
| **Max Borrow Limit** | 3 books per student |
| **Loan Period** | 14 days |
| **Late Fine Rate** | ₹10.00 per day |
