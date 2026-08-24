package com.example.banking;

/** Thrown when a withdrawal would take an account below zero. */
public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(String message) {
        super(message);
    }
}
