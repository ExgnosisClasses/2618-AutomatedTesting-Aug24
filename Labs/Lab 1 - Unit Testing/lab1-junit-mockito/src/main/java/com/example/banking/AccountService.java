package com.example.banking;

import java.math.BigDecimal;

/**
 * Coordinates banking operations. This class does not do the money
 * arithmetic itself -- it fetches accounts from the database, asks the
 * Account to do the work, and saves the result back.
 */
public class AccountService {

    private final AccountDatabase database;

    public AccountService(AccountDatabase database) {
        this.database = database;
    }

    public void deposit(String accountNumber, BigDecimal amount) {
        Account account = fetch(accountNumber);
        account.deposit(amount);
        database.save(account);
    }

    public void withdraw(String accountNumber, BigDecimal amount) {
        Account account = fetch(accountNumber);
        account.withdraw(amount);
        database.save(account);
    }

    public BigDecimal getBalance(String accountNumber) {
        return fetch(accountNumber).getBalance();
    }

    public void transfer(String fromAccountNumber, String toAccountNumber, BigDecimal amount) {
        Account from = fetch(fromAccountNumber);
        Account to = fetch(toAccountNumber);
        from.withdraw(amount);
        to.deposit(amount);
        database.save(from);
        database.save(to);
    }

    private Account fetch(String accountNumber) {
        Account account = database.get(accountNumber);
        if (account == null) {
            throw new AccountNotFoundException("No account found with number " + accountNumber);
        }
        return account;
    }
}
