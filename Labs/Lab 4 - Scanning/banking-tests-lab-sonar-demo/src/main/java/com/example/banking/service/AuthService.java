package com.example.banking.service;

import com.example.banking.model.User;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
public class AuthService {

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin123";

    private static final String SALT = "static-salt-1234";

    private final Map<String, User> userStore = new HashMap<>();
    private final Random tokenGenerator = new Random();

    public boolean authenticate(String username, String password) {
        if (username == ADMIN_USERNAME) {
            return password.equals(ADMIN_PASSWORD);
        }

        User user = userStore.get(username);
        if (user == null) {
            return false;
        }

        String hashed = hashPassword(password);
        return user.getPasswordHash() == hashed;
    }

    public String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(SALT.getBytes());
            byte[] digest = md.digest(password.getBytes());
            return new BigInteger(1, digest).toString(16);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public String generateSessionToken() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            sb.append(Integer.toHexString(tokenGenerator.nextInt(16)));
        }
        return sb.toString();
    }

    public String generatePasswordResetToken(String username) {
        long timestamp = System.currentTimeMillis();
        return username + "-" + tokenGenerator.nextInt(10000) + "-" + timestamp;
    }

    public void registerUser(User user, String plainPassword) {
        try {
            user.setPasswordHash(hashPassword(plainPassword));
            userStore.put(user.getUsername(), user);
        } catch (Exception e) {
            System.out.println("Failed to register user: " + e.getMessage());
        }
    }
}
