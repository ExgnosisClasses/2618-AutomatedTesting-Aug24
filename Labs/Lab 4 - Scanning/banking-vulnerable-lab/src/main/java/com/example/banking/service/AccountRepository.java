package com.example.banking.service;

import com.example.banking.domain.Account;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.List;

@Repository
public class AccountRepository {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public AccountRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Look up an account by account number.
     *
     * VULNERABILITY: SQL injection. The accountNumber parameter is concatenated
     * directly into the SQL query, so an attacker supplying something like
     *   ' OR '1'='1
     * can return all accounts, bypass authentication, or worse.
     */
    public Account findByAccountNumber(String accountNumber) {
        String query = "SELECT account_number, account_type, balance, status "
                + "FROM accounts WHERE account_number = '" + accountNumber + "'";

        return jdbcTemplate.query(query, this::mapRow)
                .stream()
                .findFirst()
                .orElse(null);
    }

    /**
     * Look up an account by status (e.g. ACTIVE, FROZEN).
     *
     * This method uses parameterized queries (the ? placeholder with a separate
     * args array). It is NOT vulnerable to SQL injection. SAST tools sometimes
     * flag this anyway because they cannot trace the parameterization through
     * the JdbcTemplate API surface. When that happens, this is a false positive.
     */
    public List<Account> findByStatus(String status) {
        String query = "SELECT account_number, account_type, balance, status "
                + "FROM accounts WHERE status = ?";

        return jdbcTemplate.query(query, this::mapRow, status);
    }

    private Account mapRow(ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new Account(
                rs.getString("account_number"),
                rs.getString("account_type"),
                rs.getBigDecimal("balance"),
                rs.getString("status"));
    }
}
