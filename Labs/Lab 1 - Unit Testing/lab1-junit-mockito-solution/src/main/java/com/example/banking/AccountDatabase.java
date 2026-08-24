package com.example.banking;

/**
 * Where accounts are stored. In a real application this would talk to a
 * real database. In this lab there is no implementation at all -- Mockito
 * creates a stand-in for it during the tests.
 */
public interface AccountDatabase {

    /**
     * Looks up one account.
     *
     * @return the account, or null if no account has that number
     */
    Account get(String accountNumber);

    /** Writes an account back to storage. */
    void save(Account account);
}
