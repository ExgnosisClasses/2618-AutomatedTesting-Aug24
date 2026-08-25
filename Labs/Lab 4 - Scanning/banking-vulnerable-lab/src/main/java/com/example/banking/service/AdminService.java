package com.example.banking.service;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@Service
public class AdminService {

    /**
     * VULNERABILITY: Hardcoded credentials.
     *
     * The admin password is embedded as a string literal in the source code.
     * Anyone with read access to the source (including anyone who can view
     * the JAR's class files) knows the production admin password. Worse, if
     * this code is committed to source control, the password is permanently
     * in the repository's history even if removed later.
     *
     * In production, secrets must come from environment variables, a secrets
     * manager (AWS Secrets Manager, HashiCorp Vault, etc.), or a configuration
     * service that is not in source control.
     */
    private static final String ADMIN_PASSWORD = "Admin@123!Production";

    /**
     * Authenticate an admin user.
     *
     * Uses the hardcoded password above. This compounds the original
     * vulnerability by spreading its consequences throughout the application.
     */
    public boolean authenticateAdmin(String suppliedPassword) {
        return ADMIN_PASSWORD.equals(suppliedPassword);
    }

    /**
     * Generate a system health report by running a shell command.
     *
     * VULNERABILITY: Command injection. The reportType parameter is
     * concatenated into a shell command, so an attacker supplying
     *   "summary; rm -rf /"
     * can execute arbitrary commands on the server.
     */
    public String generateHealthReport(String reportType) {
        try {
            Process process = Runtime.getRuntime().exec(
                    "/bin/sh -c 'echo Report: " + reportType + "'");

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));

            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            return output.toString();
        } catch (IOException e) {
            // VULNERABILITY (low severity): empty catch block.
            // Failures are silently swallowed instead of being logged or
            // surfaced. This makes problems hard to diagnose in production.
        }
        return "";
    }
}
