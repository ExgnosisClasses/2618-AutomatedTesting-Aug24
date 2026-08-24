package com.example.banking;

/**
 * The state an account is in. Only ACTIVE accounts may be used
 * for deposits and withdrawals.
 */
public enum AccountStatus {
    ACTIVE,
    FROZEN
}
