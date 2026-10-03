package SRC.repository;

import SRC.config.Constants;
import SRC.model.Admin;
import java.io.*;
import java.util.*;

public class AdminRepository {
    private final File file;

    public AdminRepository() {
        this.file = new File(Constants.ADMINS_FILE);
        ensureFileExists();
    }

    private void ensureFileExists() {
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        if (!file.exists() || file.length() == 0) {
            try {
                file.createNewFile();
                seedDefaultAdmin();
            } catch (IOException e) {
                System.err.println("Failed to create admins file: " + e.getMessage());
            }
        }
    }

    private void seedDefaultAdmin() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            pw.println("AD001|admin|admin123");
        } catch (IOException e) {
            System.err.println("Error seeding default admin: " + e.getMessage());
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

    public synchronized List<Admin> findAll() {
        List<Admin> admins = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split("(?<!\\\\)\\|", -1);
                if (parts.length >= 3) {
                    Admin admin = new Admin();
                    admin.setAdminId(unescapeField(parts[0]));
                    admin.setUsername(unescapeField(parts[1]));
                    admin.setPassword(unescapeField(parts[2]));
                    admins.add(admin);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading admins file: " + e.getMessage());
        }
        
        // If file was somehow empty, return a defaulted list and save it
        if (admins.isEmpty()) {
            Admin defaultAdmin = new Admin("AD001", "admin", "admin123");
            admins.add(defaultAdmin);
            saveAll(admins);
        }
        
        return admins;
    }

    public synchronized Optional<Admin> findByUsername(String username) {
        return findAll().stream()
                .filter(a -> a.getUsername().equalsIgnoreCase(username))
                .findFirst();
    }

    public synchronized void saveAll(List<Admin> admins) {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        File tempFile = new File(parent, file.getName() + ".tmp." + System.currentTimeMillis());
        try {
            try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(tempFile), "UTF-8"))) {
                for (Admin admin : admins) {
                    pw.println(String.join(Constants.FILE_DELIMITER,
                            escapeField(admin.getAdminId()),
                            escapeField(admin.getUsername()),
                            escapeField(admin.getPassword())
                    ));
                }
            }
            try {
                java.nio.file.Files.move(tempFile.toPath(), file.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                java.nio.file.Files.move(tempFile.toPath(), file.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            System.err.println("Error writing to admins file: " + e.getMessage());
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    public synchronized void save(Admin admin) {
        List<Admin> admins = findAll();
        int index = -1;
        for (int i = 0; i < admins.size(); i++) {
            if (admins.get(i).getAdminId().equals(admin.getAdminId())) {
                index = i;
                break;
            }
        }
        if (index >= 0) {
            admins.set(index, admin);
        } else {
            admins.add(admin);
        }
        saveAll(admins);
    }
}
