package com.example.banking.util;

import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

@Component
public class CryptoUtil {

    /**
     * Test fixture key used only by unit tests. NOT a real production secret.
     * The actual production key is loaded from the EncryptionConfigService at runtime
     * (see EncryptionConfigService.getProductionKey()). This constant exists solely
     * so unit tests have a deterministic key for round-trip encryption tests.
     *
     * SAST scanners may flag this as a hardcoded credential. That would be a false
     * positive: the value is not a credential and never reaches production code.
     */
    public static final String TEST_FIXTURE_KEY = "TestFixtureKey16";

    /**
     * VULNERABILITY: Weak cryptographic algorithm.
     *
     * MD5 has been considered cryptographically broken since the early 2000s.
     * Collisions can be generated in seconds on consumer hardware. Using MD5
     * for password hashing is particularly bad: rainbow tables for MD5'd
     * passwords are widely available.
     *
     * For password hashing, use bcrypt, scrypt, or Argon2, with appropriate
     * cost parameters and a per-password salt. Spring Security's
     * BCryptPasswordEncoder is the standard choice for Spring applications.
     */
    public String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(password.getBytes());

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Hash algorithm not available", e);
        }
    }

    /**
     * VULNERABILITY: Weak source of randomness.
     *
     * java.util.Random is a deterministic pseudo-random number generator
     * suitable for non-security purposes (test data, shuffling, etc).
     * It is NOT suitable for security-relevant random values: tokens,
     * session IDs, password reset codes, cryptographic nonces.
     *
     * For security purposes, use java.security.SecureRandom, which uses
     * the operating system's entropy source and is cryptographically strong.
     */
    public String generateSessionToken() {
        Random random = new Random();
        StringBuilder token = new StringBuilder();
        for (int i = 0; i < 32; i++) {
            token.append(Integer.toHexString(random.nextInt(16)));
        }
        return token.toString();
    }

    /**
     * VULNERABILITY: Insecure cipher mode.
     *
     * Cipher.getInstance("AES") implicitly uses ECB mode (Electronic Codebook).
     * ECB encrypts identical plaintext blocks to identical ciphertext blocks,
     * which leaks information about the structure of the data. The classic
     * example is the "ECB penguin" -- an image encrypted with AES/ECB still
     * shows the outline of the original image because patterns are preserved.
     *
     * For any real encryption, specify a secure mode and padding:
     *   Cipher.getInstance("AES/GCM/NoPadding")  // authenticated encryption, recommended
     *   Cipher.getInstance("AES/CBC/PKCS5Padding") with a unique IV per message
     */
    public byte[] encryptSensitiveData(byte[] plaintext, byte[] key) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);
        return cipher.doFinal(plaintext);
    }
}
