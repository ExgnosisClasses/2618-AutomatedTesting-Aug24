package com.example.banking;

import java.math.BigDecimal;

/**
 * A single bank account. This class holds the money rules:
 * what a valid deposit is, what a valid withdrawal is, and
 * what happens when those rules are broken.
 */
public class Account {

    private final String accountNumber;
    private BigDecimal balance;
    private AccountStatus status;

    public Account(String accountNumber, BigDecimal balance) {
        this(accountNumber, balance, AccountStatus.ACTIVE);
    }

    public Account(String accountNumber, BigDecimal balance, AccountStatus status) {
        this.accountNumber = accountNumber;
        this.balance = balance;
        this.status = status;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public AccountStatus getStatus() {
        return status;
    }

    /** Adds money to this account. */
    public void deposit(BigDecimal amount) {
        requirePositiveAmount(amount);
        requireActiveAccount("deposit to");
        balance = balance.add(amount);
    }

    /** Removes money from this account. */
    public void withdraw(BigDecimal amount) {
        requirePositiveAmount(amount);
        requireActiveAccount("withdraw from");
        if (balance.compareTo(amount) < 0) {
            throw new InsufficientFundsException(
                    "Account " + accountNumber + " has a balance of " + balance
                            + " which is less than the requested amount of " + amount);
        }
        balance = balance.subtract(amount);
    }

    private void requirePositiveAmount(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount must not be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Amount must be greater than zero, but was " + amount);
        }
    }

    private void requireActiveAccount(String operation) {
        if (status != AccountStatus.ACTIVE) {
            throw new AccountFrozenException(
                    "Cannot " + operation + " account " + accountNumber
                            + " because its status is " + status);
        }
    }

    @Override
    public String toString() {
        return "Account[" + accountNumber + ", balance=" + balance + ", status=" + status + "]";
    }
}
