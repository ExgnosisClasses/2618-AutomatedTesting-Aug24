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

@ExtendWith(MockitoExtension.class)
@DisplayName("Account service")
class AccountServiceTest {

    @Mock
    private AccountDatabase database;

    @InjectMocks
    private AccountService accountService;

    @Test
    @DisplayName("deposit fetches the account from the database")
    void depositFetchesTheAccountFromTheDatabase() {
        Account account = new Account("ACC-001", new BigDecimal("100.00"));
        when(database.get("ACC-001")).thenReturn(account);

        accountService.deposit("ACC-001", new BigDecimal("25.00"));

        verify(database).get("ACC-001");
    }

    @Test
    @DisplayName("deposit saves the account with the updated balance")
    void depositSavesTheAccountWithTheUpdatedBalance() {
        Account account = new Account("ACC-001", new BigDecimal("100.00"));
        when(database.get("ACC-001")).thenReturn(account);

        accountService.deposit("ACC-001", new BigDecimal("25.00"));

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(database).save(captor.capture());

        Account savedAccount = captor.getValue();
        assertThat(savedAccount.getAccountNumber()).isEqualTo("ACC-001");
        assertThat(savedAccount.getBalance()).isEqualByComparingTo("125.00");
    }

    @Test
    @DisplayName("withdraw throws AccountNotFoundException when the account does not exist")
    void withdrawThrowsWhenTheAccountDoesNotExist() {
        when(database.get("ACC-999")).thenReturn(null);

        assertThatThrownBy(() -> accountService.withdraw("ACC-999", new BigDecimal("10.00")))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining("ACC-999");

        verify(database, never()).save(any(Account.class));
    }

    @Test
    @DisplayName("withdraw does not save the account when funds are insufficient")
    void withdrawDoesNotSaveWhenFundsAreInsufficient() {
        Account account = new Account("ACC-001", new BigDecimal("50.00"));
        when(database.get("ACC-001")).thenReturn(account);

        assertThatThrownBy(() -> accountService.withdraw("ACC-001", new BigDecimal("100.00")))
                .isInstanceOf(InsufficientFundsException.class);

        verify(database, never()).save(any(Account.class));
        assertThat(account.getBalance()).isEqualByComparingTo("50.00");
    }

    @Test
    @DisplayName("transfer moves money between accounts and saves both")
    void transferMovesMoneyAndSavesBothAccounts() {
        Account from = new Account("ACC-001", new BigDecimal("100.00"));
        Account to = new Account("ACC-002", new BigDecimal("20.00"));
        when(database.get("ACC-001")).thenReturn(from);
        when(database.get("ACC-002")).thenReturn(to);

        accountService.transfer("ACC-001", "ACC-002", new BigDecimal("30.00"));

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(database, times(2)).save(captor.capture());

        List<Account> savedAccounts = captor.getAllValues();
        assertThat(savedAccounts).hasSize(2);

        assertThat(savedAccounts.get(0).getAccountNumber()).isEqualTo("ACC-001");
        assertThat(savedAccounts.get(0).getBalance()).isEqualByComparingTo("70.00");

        assertThat(savedAccounts.get(1).getAccountNumber()).isEqualTo("ACC-002");
        assertThat(savedAccounts.get(1).getBalance()).isEqualByComparingTo("50.00");
    }

    @Test
    @DisplayName("getBalance returns the balance of the requested account")
    void getBalanceReturnsTheBalanceOfTheRequestedAccount() {
        when(database.get("ACC-001"))
                .thenReturn(new Account("ACC-001", new BigDecimal("42.50")));

        BigDecimal balance = accountService.getBalance("ACC-001");

        assertThat(balance).isEqualByComparingTo("42.50");
        verify(database, never()).save(any(Account.class));
    }
}
