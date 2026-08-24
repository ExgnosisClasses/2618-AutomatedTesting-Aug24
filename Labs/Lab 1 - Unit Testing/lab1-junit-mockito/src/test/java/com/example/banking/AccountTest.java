package com.example.banking;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Part 1 of Lab 1.1 -- JUnit 5 and AssertJ.
 *
 * The class under test is Account, which has no collaborators.
 * No mocks are needed anywhere in this file.
 *
 * Replace each TODO comment below with the test method from the lab
 * instructions. Work through them in order.
 */
@DisplayName("Account")
class AccountTest {

    // ---------------------------------------------------------------
    // TODO 1.1 -- deposit increases the balance
    //
    // Arrange: an ACTIVE account "ACC-001" with a balance of 100.00
    // Act:     deposit 25.00
    // Assert:  the balance is now 125.00
    // ---------------------------------------------------------------


    // ---------------------------------------------------------------
    // TODO 1.2 -- withdraw decreases the balance
    //
    // Arrange: an ACTIVE account "ACC-001" with a balance of 100.00
    // Act:     withdraw 30.00
    // Assert:  the balance is now 70.00
    // ---------------------------------------------------------------


    // ---------------------------------------------------------------
    // TODO 1.3 -- withdraw rejects an amount larger than the balance
    //
    // Arrange: an ACTIVE account "ACC-001" with a balance of 50.00
    // Act +    withdrawing 100.00 throws InsufficientFundsException
    // Assert:  and the message contains "ACC-001"
    // Assert:  the balance is still 50.00
    // ---------------------------------------------------------------


    // ---------------------------------------------------------------
    // TODO 1.4 -- deposit rejects a negative amount
    //
    // Arrange: an ACTIVE account "ACC-001" with a balance of 100.00
    // Act +    depositing -10.00 throws IllegalArgumentException
    // Assert:  and the message contains "greater than zero"
    // Assert:  the balance is still 100.00
    // ---------------------------------------------------------------


    // ---------------------------------------------------------------
    // TODO 1.5 -- withdraw rejects a frozen account
    //
    // Arrange: account "ACC-001", balance 100.00, AccountStatus.FROZEN
    // Act +    withdrawing 10.00 throws AccountFrozenException
    // Assert:  and the message contains both "ACC-001" and "FROZEN"
    // ---------------------------------------------------------------


    // ---------------------------------------------------------------
    // TODO 1.6 -- parameterized test: deposit adds to the starting balance
    //
    // Use @ParameterizedTest and @CsvSource with these four rows:
    //     starting balance | deposit amount | expected balance
    //     100.00           | 25.00          | 125.00
    //     0.00             | 10.00          | 10.00
    //     50.50            | 49.50          | 100.00
    //     999.99           | 0.01           | 1000.00
    // ---------------------------------------------------------------

}
