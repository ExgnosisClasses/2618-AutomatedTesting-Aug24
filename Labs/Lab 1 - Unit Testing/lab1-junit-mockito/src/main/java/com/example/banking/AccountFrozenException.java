package com.example.banking;

/** Thrown when an operation is attempted on an account that is not ACTIVE. */
public class AccountFrozenException extends RuntimeException {
    public AccountFrozenException(String message) {
        super(message);
    }
}
