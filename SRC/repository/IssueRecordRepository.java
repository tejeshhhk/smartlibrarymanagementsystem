package SRC.repository;

import SRC.config.Constants;
import SRC.model.IssueRecord;
import SRC.util.DateUtil;
import java.io.*;
import java.text.ParseException;
import java.util.*;

public class IssueRecordRepository {
    private final File file;

    public IssueRecordRepository() {
        this.file = new File(Constants.ISSUES_FILE);
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
                System.err.println("Failed to create issues file: " + e.getMessage());
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

    public synchronized List<IssueRecord> findAll() {
        List<IssueRecord> records = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split("(?<!\\\\)\\|", -1);
                if (parts.length >= 7) {
                    IssueRecord record = new IssueRecord();
                    record.setIssueId(unescapeField(parts[0]));
                    record.setStudentId(unescapeField(parts[1]));
                    record.setBookId(unescapeField(parts[2]));
                    
                    try {
                        record.setIssueDate(DateUtil.parseDate(unescapeField(parts[3])));
                        record.setDueDate(DateUtil.parseDate(unescapeField(parts[4])));
                        
                        String returnStr = unescapeField(parts[5]);
                        if (returnStr != null && !returnStr.trim().isEmpty() && !"null".equalsIgnoreCase(returnStr)) {
                            record.setReturnDate(DateUtil.parseDate(returnStr));
                        } else {
                            record.setReturnDate(null);
                        }
                        
                        record.setFineAmount(Double.parseDouble(unescapeField(parts[6])));
                    } catch (ParseException | NumberFormatException e) {
                        System.err.println("Error parsing issue record fields: " + e.getMessage());
                        continue;
                    }
                    
                    records.add(record);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading issues file: " + e.getMessage());
        }
        return records;
    }

    public synchronized Optional<IssueRecord> findById(String issueId) {
        return findAll().stream()
                .filter(r -> r.getIssueId().equals(issueId))
                .findFirst();
    }

    public synchronized List<IssueRecord> findByStudentId(String studentId) {
        List<IssueRecord> studentRecords = new ArrayList<>();
        for (IssueRecord r : findAll()) {
            if (r.getStudentId().equals(studentId)) {
                studentRecords.add(r);
            }
        }
        return studentRecords;
    }

    public synchronized void saveAll(List<IssueRecord> records) {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        File tempFile = new File(parent, file.getName() + ".tmp." + System.currentTimeMillis());
        try {
            try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(tempFile), "UTF-8"))) {
                for (IssueRecord record : records) {
                    String issueDateStr = DateUtil.formatDate(record.getIssueDate());
                    String dueDateStr = DateUtil.formatDate(record.getDueDate());
                    String returnDateStr = record.getReturnDate() != null ? DateUtil.formatDate(record.getReturnDate()) : "";
                    
                    pw.println(String.join(Constants.FILE_DELIMITER,
                            escapeField(record.getIssueId()),
                            escapeField(record.getStudentId()),
                            escapeField(record.getBookId()),
                            escapeField(issueDateStr),
                            escapeField(dueDateStr),
                            escapeField(returnDateStr),
                            String.valueOf(record.getFineAmount())
                    ));
                }
            }
            try {
                java.nio.file.Files.move(tempFile.toPath(), file.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                java.nio.file.Files.move(tempFile.toPath(), file.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            System.err.println("Error writing to issues file: " + e.getMessage());
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    public synchronized void save(IssueRecord record) {
        List<IssueRecord> records = findAll();
        int index = -1;
        for (int i = 0; i < records.size(); i++) {
            if (records.get(i).getIssueId().equals(record.getIssueId())) {
                index = i;
                break;
            }
        }
        if (index >= 0) {
            records.set(index, record);
        } else {
            records.add(record);
        }
        saveAll(records);
    }
}
