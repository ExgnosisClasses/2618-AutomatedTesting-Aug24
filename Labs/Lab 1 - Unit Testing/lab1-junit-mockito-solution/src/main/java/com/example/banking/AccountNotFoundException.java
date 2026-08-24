package com.example.banking;

/** Thrown when no account exists for a given account number. */
public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(String message) {
        super(message);
    }
}
