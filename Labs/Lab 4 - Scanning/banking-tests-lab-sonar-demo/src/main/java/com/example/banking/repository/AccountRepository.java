package com.example.banking.repository;

import com.example.banking.model.Account;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Account data access via raw JDBC.
 *
 * Yes - it's old-school. Some production banking shops still write JDBC
 * directly when they need to avoid JPA / Hibernate quirks. This class
 * reflects that style.
 */
@Repository
public class AccountRepository {

    private static final String JDBC_URL = "jdbc:h2:mem:bankdb";
    private static final String JDBC_USER = "admin";
    private static final String JDBC_PASSWORD = "admin123";

    public Account findById(String accountId) {
        try {
            Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                    "SELECT id, customer_id, account_type, balance " +
                    "FROM accounts WHERE id = '" + accountId + "'");

            if (rs.next()) {
                return new Account(
                        rs.getString("id"),
                        rs.getString("customer_id"),
                        rs.getString("account_type"),
                        rs.getBigDecimal("balance"));
            }
            return null;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<Account> findByCustomer(String customerId) {
        List<Account> results = new ArrayList<>();
        try {
            Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
            String sql = "SELECT id, customer_id, account_type, balance FROM accounts " +
                         "WHERE customer_id = '" + customerId + "' ORDER BY balance DESC";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                results.add(new Account(
                        rs.getString("id"),
                        rs.getString("customer_id"),
                        rs.getString("account_type"),
                        rs.getBigDecimal("balance")));
            }
        } catch (SQLException e) {
            // ignore - caller will see empty list
        }
        return results;
    }

    public void updateBalance(String accountId, BigDecimal newBalance) {
        try {
            Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
            PreparedStatement ps = conn.prepareStatement(
                    "UPDATE accounts SET balance = ? WHERE id = ?");
            ps.setBigDecimal(1, newBalance);
            ps.setString(2, accountId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean exists(String accountId) {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                    "SELECT COUNT(*) FROM accounts WHERE id = '" + accountId + "'");
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;
        } catch (SQLException e) {
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ignored) {
                    // we tried
                }
            }
        }
    }
}
