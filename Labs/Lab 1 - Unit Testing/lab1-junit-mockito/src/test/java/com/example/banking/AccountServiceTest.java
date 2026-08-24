package com.example.banking;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Part 2 of Lab 1.1 -- JUnit 5, AssertJ and Mockito.
 *
 * The class under test is AccountService, which depends on AccountDatabase.
 * There is no implementation of AccountDatabase anywhere in this project --
 * Mockito creates one at runtime.
 *
 * The three annotations below do all the setup:
 *
 *   @ExtendWith(MockitoExtension.class)
 *        Switches Mockito on. Without it, @Mock and @InjectMocks do nothing
 *        and every field below stays null.
 *
 *   @Mock
 *        Creates a fresh stand-in AccountDatabase before EVERY test.
 *
 *   @InjectMocks
 *        Constructs the real AccountService and passes the mock into its
 *        constructor. This works because AccountService accepts its
 *        database as a constructor parameter.
 *
 * There is no @BeforeEach method. Mockito rebuilds both fields before
 * each test automatically.
 *
 * Replace each TODO comment below with the test method from the lab
 * instructions. Work through them in order.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Account service")
class AccountServiceTest {

    @Mock
    private AccountDatabase database;

    @InjectMocks
    private AccountService accountService;

    // ---------------------------------------------------------------
    // TODO 2.1 -- deposit fetches the account from the database
    //
    // Arrange: an account "ACC-001" with a balance of 100.00
    // Stub:    when(database.get("ACC-001")) returns that account
    // Act:     accountService.deposit("ACC-001", 25.00)
    // Verify:  database.get("ACC-001") was called
    // ---------------------------------------------------------------


    // ---------------------------------------------------------------
    // TODO 2.2 -- deposit saves the account with the updated balance
    //
    // Arrange: an account "ACC-001" with a balance of 100.00
    // Stub:    when(database.get("ACC-001")) returns that account
    // Act:     accountService.deposit("ACC-001", 25.00)
    // Verify:  capture the argument passed to database.save(...)
    // Assert:  the captured account is "ACC-001" with a balance of 125.00
    //
    // Hint: ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
    // ---------------------------------------------------------------


    // ---------------------------------------------------------------
    // TODO 2.3 -- withdraw throws when the account does not exist
    //
    // Stub:    when(database.get("ACC-999")) returns null
    // Act +    withdrawing from "ACC-999" throws AccountNotFoundException
    // Assert:  and the message contains "ACC-999"
    // Verify:  database.save(...) was NEVER called
    // ---------------------------------------------------------------


    // ---------------------------------------------------------------
    // TODO 2.4 -- withdraw does not save when funds are insufficient
    //
    // Arrange: an account "ACC-001" with a balance of 50.00
    // Stub:    when(database.get("ACC-001")) returns that account
    // Act +    withdrawing 100.00 throws InsufficientFundsException
    // Verify:  database.save(...) was NEVER called
    // Assert:  the account balance is still 50.00
    // ---------------------------------------------------------------


    // ---------------------------------------------------------------
    // TODO 2.5 -- transfer moves money and saves both accounts
    //
    // Arrange: "ACC-001" with 100.00 and "ACC-002" with 20.00
    // Stub:    both lookups
    // Act:     accountService.transfer("ACC-001", "ACC-002", 30.00)
    // Verify:  database.save(...) was called exactly twice, capturing both
    // Assert:  first saved is "ACC-001" with 70.00
    // Assert:  second saved is "ACC-002" with 50.00
    //
    // Hint: captor.getAllValues() returns every captured argument in order
    // ---------------------------------------------------------------


    // ---------------------------------------------------------------
    // TODO 2.6 -- getBalance returns the balance without saving
    //
    // Stub:    when(database.get("ACC-001")) returns an account with 42.50
    // Act:     BigDecimal balance = accountService.getBalance("ACC-001")
    // Assert:  the returned balance is 42.50
    // Verify:  database.save(...) was NEVER called
    // ---------------------------------------------------------------

}
