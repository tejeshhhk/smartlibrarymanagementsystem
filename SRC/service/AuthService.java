package SRC.service;

import SRC.model.Admin;
import SRC.repository.AdminRepository;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AuthService {
    private final AdminRepository adminRepository;
    private final ConcurrentHashMap<String, String> activeSessions;

    public AuthService(AdminRepository adminRepository) {
        this.adminRepository = adminRepository;
        this.activeSessions = new ConcurrentHashMap<>();
    }

    public synchronized String login(String username, String password) {
        Optional<Admin> adminOpt = adminRepository.findByUsername(username);
        if (adminOpt.isPresent() && adminOpt.get().getPassword().equals(password)) {
            String token = UUID.randomUUID().toString();
            activeSessions.put(token, username);
            return token;
        }
        return null;
    }

    public synchronized boolean logout(String token) {
        return activeSessions.remove(token) != null;
    }

    public synchronized boolean isValidToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        return activeSessions.containsKey(token);
    }

    public synchronized String getUsernameByToken(String token) {
        return activeSessions.get(token);
    }
}
