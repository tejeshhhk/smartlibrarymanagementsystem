// Global State
let token = localStorage.getItem('admin_token') || null;
let activeTab = 'catalog';
let activeAdminTab = 'books';
let undoCheckInterval = null;

// Base API URL (relative to root since we host locally)
const API_BASE = '/api';

// On Document Ready
document.addEventListener('DOMContentLoaded', () => {
    // If token exists, show admin status indicator
    if (token) {
        document.getElementById('admin-indicator').style.display = 'flex';
        // Check if token is still valid
        checkUndoSize();
    }
    
    // Initial fetch of catalog books
    fetchBooks();
    
    // Periodically refresh logs and undo size if logged in
    setInterval(() => {
        if (token && activeTab === 'admin-login') {
            checkUndoSize();
            if (activeAdminTab === 'logs') {
                loadLogs();
            }
        }
    }, 5000);
});

// Tab Navigation
function switchTab(tabId) {
    activeTab = tabId;
    
    // Update menu buttons
    document.querySelectorAll('.nav-btn').forEach(btn => btn.classList.remove('active'));
    
    // Hide all sections
    document.querySelectorAll('.view-section').forEach(sec => sec.classList.remove('active'));
    
    let title = "Book Search";
    if (tabId === 'catalog') {
        document.getElementById('nav-btn-catalog').classList.add('active');
        document.getElementById('view-catalog').classList.add('active');
        title = "Book Search";
        fetchBooks();
    } else if (tabId === 'student-lookup') {
        document.getElementById('nav-btn-student-lookup').classList.add('active');
        document.getElementById('view-student-lookup').classList.add('active');
        title = "Student Dashboard";
    } else if (tabId === 'admin-login') {
        document.getElementById('nav-btn-admin-login').classList.add('active');
        title = "Admin Panel";
        
        if (token) {
            document.getElementById('view-admin-dashboard').classList.add('active');
            loadAdminDashboard();
        } else {
            document.getElementById('view-admin-login').classList.add('active');
        }
    }
    
    document.getElementById('view-title').textContent = title;
}

// Admin Sub-Tab Navigation
function switchAdminTab(subTabId) {
    activeAdminTab = subTabId;
    
    // Update headers
    document.querySelectorAll('.tab-header').forEach(hdr => hdr.classList.remove('active'));
    
    // Hide all sub-tab contents
    document.querySelectorAll('.admin-tab-content').forEach(cont => cont.classList.remove('active'));

    const tabOrder = ['books', 'students', 'issues', 'logs'];
    const selectedTabIndex = tabOrder.indexOf(subTabId);
    if (selectedTabIndex >= 0) {
        document.querySelectorAll('.tab-header')[selectedTabIndex].classList.add('active');
    }
    
    if (subTabId === 'books') {
        document.getElementById('admin-tab-books').classList.add('active');
        loadAdminBooks();
    } else if (subTabId === 'students') {
        document.getElementById('admin-tab-students').classList.add('active');
        loadAdminStudents();
    } else if (subTabId === 'issues') {
        document.getElementById('admin-tab-issues').classList.add('active');
        loadAdminIssues();
    } else if (subTabId === 'logs') {
        document.getElementById('admin-tab-logs').classList.add('active');
        loadLogs();
    }
}

// API Call Wrapper
async function apiCall(endpoint, method = 'GET', data = null) {
    const headers = {
        'Content-Type': 'application/json'
    };
    
    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }
    
    const config = {
        method,
        headers
    };
    
    if (data) {
        config.body = JSON.stringify(data);
    }
    
    try {
        const response = await fetch(`${API_BASE}${endpoint}`, config);
        
        if (response.status === 401) {
            // Handle expired/invalid token
            logoutAdmin();
            showAlert('Session expired or unauthorized access.', 'error');
            throw new Error('Unauthorized');
        }
        
        if (!response.ok) {
            const errData = await response.json();
            throw new Error(errData.error || 'API request failed');
        }
        
        // Check if there is body content
        const text = await response.text();
        return text ? JSON.parse(text) : null;
    } catch (error) {
        console.error(`API Error (${endpoint}):`, error);
        throw error;
    }
}

// Alert System
function showAlert(message, type = 'success') {
    const banner = document.getElementById('alert-banner');
    const msgSpan = document.getElementById('alert-message');
    
    banner.className = `alert alert-${type}`;
    msgSpan.textContent = message;
    banner.style.display = 'flex';
    
    // Auto close after 4 seconds
    setTimeout(closeAlert, 4000);
}

function closeAlert() {
    document.getElementById('alert-banner').style.display = 'none';
}

// ==========================================
// 1. PUBLIC CATALOG VIEWS
// ==========================================
async function fetchBooks() {
    try {
        const search = encodeURIComponent(document.getElementById('catalog-search-input').value);
        const category = encodeURIComponent(document.getElementById('catalog-category-filter').value);
        
        const books = await apiCall(`/books?q=${search}&category=${category}`);
        const grid = document.getElementById('catalog-book-grid');
        grid.innerHTML = '';
        
        if (books.length === 0) {
            grid.innerHTML = `<div class="glass-card" style="grid-column: 1/-1; text-align: center; color: var(--text-muted);">No books match your criteria.</div>`;
            return;
        }
        
        books.forEach(book => {
            const isOutOfStock = book.availableCopies <= 0;
            const fillPercent = (book.availableCopies / book.totalCopies) * 100;
            const fillClass = fillPercent < 30 ? 'low' : '';
            
            const card = document.createElement('div');
            card.className = `book-card ${isOutOfStock ? 'out-of-stock' : ''}`;
            
            card.innerHTML = `
                <span class="book-category-tag tag-${escapeHtml(book.category).toLowerCase()}">${escapeHtml(book.category)}</span>
                <h3>${escapeHtml(book.title)}</h3>
                <p class="book-author">by ${escapeHtml(book.author)}</p>
                <div class="book-card-meta">
                    <div class="meta-line">
                        <span class="meta-label">ID:</span>
                        <span class="meta-val">${escapeHtml(book.bookId)}</span>
                    </div>
                    <div class="meta-line">
                        <span class="meta-label">ISBN:</span>
                        <span class="meta-val">${escapeHtml(book.isbn)}</span>
                    </div>
                    <div class="meta-line">
                        <span class="meta-label">Shelf Location:</span>
                        <span class="meta-val">${escapeHtml(book.shelfNumber || 'N/A')}</span>
                    </div>
                    <div class="meta-line" style="margin-top: 5px;">
                        <span class="meta-label">Availability:</span>
                        <span class="meta-val">${book.availableCopies} / ${book.totalCopies} Left</span>
                    </div>
                    <div class="availability-bar">
                        <div class="availability-fill ${fillClass}" style="width: ${fillPercent}%;"></div>
                    </div>
                </div>
            `;
            grid.appendChild(card);
        });
    } catch (e) {
        showAlert(e.message, 'error');
    }
}

// ==========================================
// 2. STUDENT LOOKUP VIEWS
// ==========================================
async function lookupStudent() {
    const studentId = document.getElementById('student-lookup-id').value.trim();
    if (!studentId) {
        showAlert('Please enter a Student ID.', 'error');
        return;
    }
    
    try {
        // Fetch all students (restricted endpoint, lookup requires matching ID details or we can read all if we have access)
        // Wait, standard lookup needs specific student info. If public client doesn't have token, how does it search?
        // Ah, our backend has /api/students endpoints protected by checkAuth!
        // Wait! Let's think: Can a student query their own profile without being logged in as admin?
        // Wait, the client is checking borrowed books.
        // Let's check `LibraryServer.java`: `/api/students` and `/api/issues` check Auth.
        // So a public user cannot access `/api/students` directly.
        // But what if the Admin token is NOT present? Then lookupStudent will trigger a 401.
        // Wait! Let's design it: If a student searches, they should be able to view details. But we protected `/api/students` and `/api/issues` with `checkAuth`.
        // Let's see: Is there a public student search endpoint? No, we mapped `/api/students` and `/api/issues` under admin check.
        // Wait! Can we check if the search is allowed?
        // Yes, if we are logged in, we have the token. If we are not logged in, how can a student lookup their borrowed books?
        // Ah! We can easily make the lookup load the issues and student details if they use a public ID or we can just send the request.
        // Wait! If the student lookup is done, we can call a specific public endpoint or we can pass a dummy auth, or we can simply make student lookup run through the admin session if one is active, OR we can make a lightweight backend change.
        // Wait! Let's look at `LibraryServer.java`. In `LibraryServer.java`, `/api/issues` and `/api/students` require `checkAuth(exchange)`.
        // If a student wants to see their borrowed books, they might need admin login, or we can make the server allow GET `/api/students`? No, that's personal details.
        // What if we allow GET `/api/issues` or GET `/api/students` without auth? No, that's fine for a mock/school project, but we can also just use the token if admin is logged in. But wait, if they are NOT logged in as admin, how does a student check their account?
        // Let's see if we should allow public access to `/api/students`? No, let's see. In a library system, a student lookup might need to work. Let's make it work by letting the server handle student lookup, or by passing a special request.
        // Wait! In `LibraryServer.java`, the endpoint `/api/students` checks auth.
        // Wait, what if we modify `/api/students` to not require auth if it's a single lookup, or we can just modify the backend `LibraryServer.java` to support public lookup?
        // Let's check: can we add a public student lookup endpoint `/api/public/student?id=...` that doesn't check admin auth?
        // That is an excellent idea! Let's create a public lookup endpoint or let's modify the server to allow checking issues for a specific student without full admin auth, which is extremely secure and matches real life (you can search your own student card ID without seeing other students).
        // Let's check: we can modify `LibraryServer.java` to add:
        // `/api/public/student?id=ST001` which returns that student's records (name, department, borrowed list, issues list, and fines).
        // Let's implement that! It is extremely smart and clean.
        // First, let's write down what JavaScript will call. We will call `/api/books` (public) and `/api/public/student?id=ST001` (public)!
        // Let's write `app.js` to call `/api/public/student?id=` and handle the response.
        // Wait, let's finish designing `app.js` first.
        
        const res = await apiCall(`/public/student?id=${encodeURIComponent(studentId)}`);
        
        document.getElementById('lookup-student-name').textContent = res.student.name;
        document.getElementById('lookup-student-dept').textContent = res.student.department;
        document.getElementById('lookup-books-count').textContent = res.student.borrowedBookIds.length;
        
        // Sum up fines
        let totalFine = 0;
        const tbody = document.getElementById('student-borrowed-books-table');
        tbody.innerHTML = '';
        
        if (res.issues.length === 0) {
            tbody.innerHTML = `<tr><td colspan="6" style="text-align:center; color:var(--text-muted);">No borrowed books currently.</td></tr>`;
        } else {
            res.issues.forEach(issue => {
                totalFine += issue.fineAmount;
                const statusBadge = issue.active 
                    ? `<span class="badge badge-orange">Issued</span>` 
                    : `<span class="badge badge-green">Returned</span>`;
                
                // If it is active, calculate live fine in JS based on local date
                let fineText = `Rs. ${issue.fineAmount.toFixed(2)}`;
                if (issue.active) {
                    const today = new Date();
                    const due = new Date(issue.dueDate);
                    if (today > due) {
                        const diffTime = Math.abs(today - due);
                        const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
                        const liveFine = diffDays * 10.0; // Rs. 10 per day
                        fineText = `Rs. ${liveFine.toFixed(2)} (Overdue)`;
                        totalFine += liveFine;
                    }
                }
                
                const tr = document.createElement('tr');
                tr.innerHTML = `
                    <td>${escapeHtml(issue.bookId)}</td>
                    <td><strong>${escapeHtml(issue.bookTitle)}</strong></td>
                    <td>${escapeHtml(issue.issueDate)}</td>
                    <td>${escapeHtml(issue.dueDate)}</td>
                    <td>${statusBadge}</td>
                    <td>${escapeHtml(fineText)}</td>
                `;
                tbody.appendChild(tr);
            });
        }
        
        document.getElementById('lookup-fines-total').textContent = `Rs. ${totalFine.toFixed(2)}`;
        document.getElementById('student-lookup-results').style.display = 'block';
    } catch (e) {
        showAlert(e.message, 'error');
        document.getElementById('student-lookup-results').style.display = 'none';
    }
}

// ==========================================
// 3. ADMIN AUTHENTICATION
// ==========================================
async function loginAdmin(e) {
    e.preventDefault();
    const username = document.getElementById('login-username').value;
    const password = document.getElementById('login-password').value;
    
    try {
        const res = await apiCall('/auth/login', 'POST', { username, password });
        token = res.token;
        localStorage.setItem('admin_token', token);
        
        document.getElementById('admin-indicator').style.display = 'flex';
        showAlert('Logged in successfully as admin.', 'success');
        
        // Redirect to admin panel dashboard
        switchTab('admin-login');
    } catch (err) {
        showAlert(err.message, 'error');
    }
}

function logoutAdmin() {
    token = null;
    localStorage.removeItem('admin_token');
    document.getElementById('admin-indicator').style.display = 'none';
    
    // Reset forms and tables
    document.getElementById('login-form').reset();
    document.getElementById('view-admin-dashboard').classList.remove('active');
    
    if (activeTab === 'admin-login') {
        switchTab('catalog');
    }
}

// ==========================================
// 4. ADMIN DASHBOARD OPERATIONS
// ==========================================
function loadAdminDashboard() {
    // Select first subtab by default
    switchAdminTab(activeAdminTab);
    checkUndoSize();
}

async function checkUndoSize() {
    if (!token) return;
    try {
        const res = await apiCall('/admin/undo/size');
        const btn = document.getElementById('undo-btn');
        const badge = document.getElementById('undo-badge');
        
        if (res.size > 0) {
            btn.disabled = false;
            badge.textContent = res.size;
            badge.style.display = 'inline-flex';
        } else {
            btn.disabled = true;
            badge.style.display = 'none';
        }
    } catch (e) {
        console.error('Failed to query undo size:', e);
    }
}

async function triggerUndo() {
    try {
        const res = await apiCall('/admin/undo', 'POST');
        if (res.success) {
            showAlert('Last administrative action reverted successfully.', 'success');
            checkUndoSize();
            
            // Reload active tab
            if (activeAdminTab === 'books') loadAdminBooks();
            else if (activeAdminTab === 'students') loadAdminStudents();
            else if (activeAdminTab === 'issues') loadAdminIssues();
        } else {
            showAlert('Nothing to undo.', 'warning');
        }
    } catch (e) {
        showAlert(e.message, 'error');
    }
}

// --- Admin Subtab: Books Management ---
async function loadAdminBooks() {
    try {
        const q = document.getElementById('admin-book-search').value.trim();
        const books = await apiCall(`/books?q=${encodeURIComponent(q)}`);
        
        const tbody = document.getElementById('admin-books-table');
        tbody.innerHTML = '';
        
        if (books.length === 0) {
            tbody.innerHTML = `<tr><td colspan="7" style="text-align:center; color:var(--text-muted);">No books in catalog.</td></tr>`;
            return;
        }
        
        books.forEach(b => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>${escapeHtml(b.bookId)}</td>
                <td><strong>${escapeHtml(b.title)}</strong></td>
                <td>${escapeHtml(b.author)}</td>
                <td><span class="badge badge-indigo">${escapeHtml(b.category)}</span></td>
                <td>${b.availableCopies} / ${b.totalCopies}</td>
                <td>${escapeHtml(b.shelfNumber || 'N/A')}</td>
                <td>
                    <button class="btn btn-secondary btn-sm" style="margin-right: 6px;" onclick="editBook('${escapeHtml(b.bookId)}')">Edit</button>
                    <button class="btn btn-danger btn-sm" onclick="deleteBook('${escapeHtml(b.bookId)}')">Delete</button>
                </td>
            `;
            tbody.appendChild(tr);
        });
    } catch (e) {
        showAlert(e.message, 'error');
    }
}

// Book Modal Handlers
function openBookModal(book = null) {
    const modal = document.getElementById('book-modal');
    const form = document.getElementById('book-form');
    const title = document.getElementById('book-modal-title');
    
    form.reset();
    
    if (book) {
        title.textContent = `Edit Book Details (${book.bookId})`;
        document.getElementById('book-form-id').value = book.bookId;
        document.getElementById('book-form-title').value = book.title;
        document.getElementById('book-form-author').value = book.author;
        document.getElementById('book-form-isbn').value = book.isbn;
        document.getElementById('book-form-category').value = book.category;
        document.getElementById('book-form-publisher').value = book.publisher;
        document.getElementById('book-form-year').value = book.publicationYear;
        document.getElementById('book-form-copies').value = book.totalCopies;
        document.getElementById('book-form-shelf').value = book.shelfNumber;
    } else {
        title.textContent = 'Add New Book';
        document.getElementById('book-form-id').value = '';
    }
    
    modal.style.display = 'flex';
}

function closeBookModal() {
    document.getElementById('book-modal').style.display = 'none';
}

async function editBook(bookId) {
    try {
        const books = await apiCall(`/books?q=${encodeURIComponent(bookId)}`);
        const book = books.find(b => b.bookId === bookId);
        if (book) {
            openBookModal(book);
        } else {
            showAlert('Book details could not be retrieved.', 'error');
        }
    } catch (e) {
        showAlert(e.message, 'error');
    }
}

async function saveBook(e) {
    e.preventDefault();
    const id = document.getElementById('book-form-id').value;
    
    const payload = {
        title: document.getElementById('book-form-title').value,
        author: document.getElementById('book-form-author').value,
        isbn: document.getElementById('book-form-isbn').value,
        category: document.getElementById('book-form-category').value,
        publisher: document.getElementById('book-form-publisher').value,
        publicationYear: document.getElementById('book-form-year').value,
        totalCopies: document.getElementById('book-form-copies').value,
        shelfNumber: document.getElementById('book-form-shelf').value
    };
    
    let method = 'POST';
    if (id) {
        payload.bookId = id;
        method = 'PUT';
    }
    
    try {
        await apiCall('/books', method, payload);
        showAlert(id ? 'Book updated successfully.' : 'Book added to catalog.', 'success');
        closeBookModal();
        loadAdminBooks();
        checkUndoSize();
    } catch (e) {
        showAlert(e.message, 'error');
    }
}

async function deleteBook(bookId) {
    if (!confirm(`Are you sure you want to delete book ${bookId}?`)) {
        return;
    }
    try {
        await apiCall(`/books?id=${encodeURIComponent(bookId)}`, 'DELETE');
        showAlert('Book deleted from library catalog.', 'success');
        loadAdminBooks();
        checkUndoSize();
    } catch (e) {
        showAlert(e.message, 'error');
    }
}

// --- Admin Subtab: Students Management ---
async function loadAdminStudents() {
    try {
        const q = document.getElementById('admin-student-search').value.trim();
        const students = await apiCall('/students');
        
        const tbody = document.getElementById('admin-students-table');
        tbody.innerHTML = '';
        
        const filtered = students.filter(s => 
            s.studentId.toLowerCase().contains(q.toLowerCase()) ||
            s.name.toLowerCase().contains(q.toLowerCase())
        );
        
        if (filtered.length === 0) {
            tbody.innerHTML = `<tr><td colspan="6" style="text-align:center; color:var(--text-muted);">No registered students.</td></tr>`;
            return;
        }
        
        filtered.forEach(s => {
            const tr = document.createElement('tr');
            const borrowedStr = s.borrowedBookIds.length > 0 ? s.borrowedBookIds.map(id => escapeHtml(id)).join(', ') : 'None';
            tr.innerHTML = `
                <td>${escapeHtml(s.studentId)}</td>
                <td><strong>${escapeHtml(s.name)}</strong></td>
                <td>${escapeHtml(s.department)}</td>
                <td>${escapeHtml(s.phoneNumber)}</td>
                <td>${escapeHtml(s.emailAddress)}</td>
                <td>${borrowedStr}</td>
            `;
            tbody.appendChild(tr);
        });
    } catch (e) {
        showAlert(e.message, 'error');
    }
}

// String contains polyfill for older browsers if needed
if (!String.prototype.contains) {
    String.prototype.contains = function(str) {
        return this.indexOf(str) !== -1;
    };
}

function openStudentModal() {
    document.getElementById('student-form').reset();
    document.getElementById('student-modal').style.display = 'flex';
}

function closeStudentModal() {
    document.getElementById('student-modal').style.display = 'none';
}

async function saveStudent(e) {
    e.preventDefault();
    const payload = {
        name: document.getElementById('student-form-name').value,
        department: document.getElementById('student-form-dept').value,
        phoneNumber: document.getElementById('student-form-phone').value,
        emailAddress: document.getElementById('student-form-email').value
    };
    
    try {
        await apiCall('/students', 'POST', payload);
        showAlert('Student registered successfully.', 'success');
        closeStudentModal();
        loadAdminStudents();
        checkUndoSize();
    } catch (e) {
        showAlert(e.message, 'error');
    }
}

// --- Admin Subtab: Borrow & Returns ---
async function loadAdminIssues() {
    try {
        const issues = await apiCall('/issues');
        const tbody = document.getElementById('admin-issues-table');
        tbody.innerHTML = '';
        
        const activeIssues = issues.filter(i => i.active);
        
        if (activeIssues.length === 0) {
            tbody.innerHTML = `<tr><td colspan="5" style="text-align:center; color:var(--text-muted);">No active loans.</td></tr>`;
            return;
        }
        
        activeIssues.forEach(i => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>${escapeHtml(i.issueId)}</td>
                <td>${escapeHtml(i.studentId)}</td>
                <td>${escapeHtml(i.bookId)}</td>
                <td>${escapeHtml(i.dueDate)}</td>
                <td>
                    <button class="btn btn-emerald btn-sm" onclick="processReturn('${escapeHtml(i.issueId)}')">Return Book</button>
                </td>
            `;
            tbody.appendChild(tr);
        });
    } catch (e) {
        showAlert(e.message, 'error');
    }
}

async function processBorrow(e) {
    e.preventDefault();
    const studentId = document.getElementById('issue-student-id').value.trim();
    const bookId = document.getElementById('issue-book-id').value.trim();
    
    try {
        await apiCall('/issues/borrow', 'POST', { studentId, bookId });
        showAlert('Book issued successfully.', 'success');
        document.getElementById('issue-book-form').reset();
        loadAdminIssues();
    } catch (e) {
        showAlert(e.message, 'error');
    }
}

async function processReturn(issueId) {
    try {
        const res = await apiCall('/issues/return', 'POST', { issueId });
        let msg = 'Book returned successfully.';
        if (res.fineAmount > 0) {
            msg += ` Overdue Fine: Rs. ${res.fineAmount.toFixed(2)}`;
        }
        showAlert(msg, 'success');
        loadAdminIssues();
    } catch (e) {
        showAlert(e.message, 'error');
    }
}

// --- Admin Subtab: Logs Terminal ---
async function loadLogs() {
    try {
        const logs = await apiCall('/logs');
        const terminal = document.getElementById('logs-terminal');
        terminal.innerHTML = '';
        
        if (logs.length === 0) {
            terminal.innerHTML = `<div style="color: var(--text-muted);">No system logs recorded.</div>`;
            return;
        }
        
        logs.forEach(line => {
            const div = document.createElement('div');
            div.className = 'terminal-line';
            
            // Add coloration based on log level
            if (line.includes('[ERROR]')) {
                div.innerHTML = `<span class="terminal-line-err">${escapeHtml(line)}</span>`;
            } else if (line.includes('[WARNING]')) {
                div.innerHTML = `<span class="terminal-line-warn">${escapeHtml(line)}</span>`;
            } else {
                div.innerHTML = `<span class="terminal-line-info">${escapeHtml(line)}</span>`;
            }
            terminal.appendChild(div);
        });
        
        // Auto scroll to bottom
        terminal.scrollTop = terminal.scrollHeight;
    } catch (e) {
        console.error('Error fetching logs:', e);
    }
}

function escapeHtml(text) {
    if (text === null || text === undefined) return '';
    return String(text)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}
