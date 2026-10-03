package SRC.repository;

import SRC.config.Constants;
import SRC.model.Student;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class StudentRepository {
    private final File file;

    public StudentRepository() {
        this.file = new File(Constants.STUDENTS_FILE);
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
                System.err.println("Failed to create students file: " + e.getMessage());
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

    public synchronized List<Student> findAll() {
        List<Student> students = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split("(?<!\\\\)\\|", -1);
                if (parts.length >= 5) {
                    Student student = new Student();
                    student.setStudentId(unescapeField(parts[0]));
                    student.setName(unescapeField(parts[1]));
                    student.setDepartment(unescapeField(parts[2]));
                    student.setPhoneNumber(unescapeField(parts[3]));
                    student.setEmailAddress(unescapeField(parts[4]));
                    
                    if (parts.length >= 6 && !parts[5].trim().isEmpty()) {
                        String[] bookIds = parts[5].split(",");
                        List<String> list = new ArrayList<>();
                        for (String id : bookIds) {
                            list.add(unescapeField(id));
                        }
                        student.setBorrowedBookIds(list);
                    } else {
                        student.setBorrowedBookIds(new ArrayList<>());
                    }
                    students.add(student);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading students file: " + e.getMessage());
        }
        return students;
    }

    public synchronized Optional<Student> findById(String studentId) {
        return findAll().stream()
                .filter(s -> s.getStudentId().equals(studentId))
                .findFirst();
    }

    public synchronized void saveAll(List<Student> students) {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        File tempFile = new File(parent, file.getName() + ".tmp." + System.currentTimeMillis());
        try {
            try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(tempFile), "UTF-8"))) {
                for (Student student : students) {
                    List<String> escapedBookIds = new ArrayList<>();
                    if (student.getBorrowedBookIds() != null) {
                        for (String id : student.getBorrowedBookIds()) {
                            escapedBookIds.add(escapeField(id));
                        }
                    }
                    String borrowedIdsStr = String.join(",", escapedBookIds);
                    pw.println(String.join(Constants.FILE_DELIMITER,
                            escapeField(student.getStudentId()),
                            escapeField(student.getName()),
                            escapeField(student.getDepartment()),
                            escapeField(student.getPhoneNumber()),
                            escapeField(student.getEmailAddress()),
                            borrowedIdsStr
                    ));
                }
            }
            try {
                java.nio.file.Files.move(tempFile.toPath(), file.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                java.nio.file.Files.move(tempFile.toPath(), file.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            System.err.println("Error writing to students file: " + e.getMessage());
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    public synchronized void save(Student student) {
        List<Student> students = findAll();
        int index = -1;
        for (int i = 0; i < students.size(); i++) {
            if (students.get(i).getStudentId().equals(student.getStudentId())) {
                index = i;
                break;
            }
        }
        if (index >= 0) {
            students.set(index, student);
        } else {
            students.add(student);
        }
        saveAll(students);
    }

    public synchronized boolean deleteById(String studentId) {
        List<Student> students = findAll();
        boolean removed = students.removeIf(s -> s.getStudentId().equals(studentId));
        if (removed) {
            saveAll(students);
        }
        return removed;
    }
}
