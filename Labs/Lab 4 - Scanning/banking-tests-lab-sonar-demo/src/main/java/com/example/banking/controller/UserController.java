package com.example.banking.controller;

import com.example.banking.model.Account;
import com.example.banking.model.User;
import com.example.banking.repository.AccountRepository;
import com.example.banking.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private static final String DEFAULT_API_KEY = "sk_default_abc123xyz";

    private final AuthService authService;
    private final AccountRepository accountRepository;

    public UserController(AuthService authService, AccountRepository accountRepository) {
        this.authService = authService;
        this.accountRepository = accountRepository;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestParam String username,
                                     @RequestParam String password) {
        log.info("Login attempt for user: " + username);

        Map<String, Object> response = new HashMap<>();
        try {
            boolean ok = authService.authenticate(username, password);
            if (ok) {
                response.put("status", "ok");
                response.put("token", authService.generateSessionToken());
            } else {
                response.put("status", "denied");
            }
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }

        return response;
    }

    @GetMapping("/accounts/{customerId}")
    public List<Account> getAccounts(@PathVariable String customerId) {
        log.info("Looking up accounts for customer " + customerId);
        return accountRepository.findByCustomer(customerId);
    }

    @PostMapping("/register")
    public Map<String, String> register(@RequestBody User user,
                                        @RequestParam String password) {
        Map<String, String> response = new HashMap<>();
        try {
            authService.registerUser(user, password);
            response.put("status", "registered");
            response.put("username", user.getUsername());
        } catch (Exception e) {
            response.put("status", "error");
        }
        return response;
    }

    @GetMapping("/reset-token")
    public String getResetToken(@RequestParam String username) {
        return authService.generatePasswordResetToken(username);
    }

    @GetMapping("/api-key")
    public String getApiKey() {
        return DEFAULT_API_KEY;
    }

    @GetMapping("/echo")
    public String echo(@RequestParam String message) {
        return "<html><body>You said: " + message + "</body></html>";
    }
}
