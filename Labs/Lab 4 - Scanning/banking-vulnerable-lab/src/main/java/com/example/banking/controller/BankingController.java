package com.example.banking.controller;

import com.example.banking.domain.Account;
import com.example.banking.service.AccountRepository;
import com.example.banking.service.AdminService;
import com.example.banking.service.DocumentService;
import com.example.banking.util.CryptoUtil;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

/**
 * Controller exposing the vulnerable services.
 *
 * Each endpoint exercises one or more of the deliberately-introduced
 * vulnerabilities. The lab does not require running the application;
 * SonarQube analyzes the source code statically.
 */
@RestController
@RequestMapping("/api")
public class BankingController {

    private final AccountRepository accountRepository;
    private final AdminService adminService;
    private final DocumentService documentService;
    private final CryptoUtil cryptoUtil;

    public BankingController(
            AccountRepository accountRepository,
            AdminService adminService,
            DocumentService documentService,
            CryptoUtil cryptoUtil) {
        this.accountRepository = accountRepository;
        this.adminService = adminService;
        this.documentService = documentService;
        this.cryptoUtil = cryptoUtil;
    }

    @GetMapping("/accounts/{accountNumber}")
    public Account getAccount(@PathVariable String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber);
    }

    @GetMapping("/accounts")
    public List<Account> getAccountsByStatus(@RequestParam String status) {
        return accountRepository.findByStatus(status);
    }

    @PostMapping("/admin/login")
    public String adminLogin(@RequestParam String password) {
        return adminService.authenticateAdmin(password) ? "OK" : "DENIED";
    }

    @GetMapping("/admin/health")
    public String getHealth(@RequestParam String reportType) {
        return adminService.generateHealthReport(reportType);
    }

    @GetMapping("/documents/{filename}")
    public byte[] getDocument(@PathVariable String filename) throws IOException {
        return documentService.readCustomerDocument(filename);
    }

    @PostMapping("/users/hash")
    public String hashPassword(@RequestParam String password) {
        return cryptoUtil.hashPassword(password);
    }

    @GetMapping("/users/token")
    public String getSessionToken() {
        return cryptoUtil.generateSessionToken();
    }
}
