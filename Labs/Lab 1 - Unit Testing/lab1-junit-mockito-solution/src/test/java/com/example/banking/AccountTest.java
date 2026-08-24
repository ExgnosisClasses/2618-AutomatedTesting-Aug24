package com.example.banking;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Account")
class AccountTest {

    @Test
    @DisplayName("deposit increases the balance")
    void depositIncreasesTheBalance() {
        Account account = new Account("ACC-001", new BigDecimal("100.00"));
        account.deposit(new BigDecimal("25.00"));
        assertThat(account.getBalance()).isEqualByComparingTo("125.00");
    }

    @Test
    @DisplayName("withdraw decreases the balance")
    void withdrawDecreasesTheBalance() {
        Account account = new Account("ACC-001", new BigDecimal("100.00"));
        account.withdraw(new BigDecimal("30.00"));
        assertThat(account.getBalance()).isEqualByComparingTo("70.00");
    }

    @Test
    @DisplayName("withdraw rejects an amount larger than the balance")
    void withdrawRejectsAmountLargerThanBalance() {
        Account account = new Account("ACC-001", new BigDecimal("50.00"));
        assertThatThrownBy(() -> account.withdraw(new BigDecimal("100.00")))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("ACC-001");
        assertThat(account.getBalance()).isEqualByComparingTo("50.00");
    }

    @Test
    @DisplayName("deposit rejects a negative amount")
    void depositRejectsNegativeAmount() {
        Account account = new Account("ACC-001", new BigDecimal("100.00"));
        assertThatThrownBy(() -> account.deposit(new BigDecimal("-10.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("greater than zero");
        assertThat(account.getBalance()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("withdraw rejects a frozen account")
    void withdrawRejectsFrozenAccount() {
        Account account = new Account("ACC-001", new BigDecimal("100.00"), AccountStatus.FROZEN);
        assertThatThrownBy(() -> account.withdraw(new BigDecimal("10.00")))
                .isInstanceOf(AccountFrozenException.class)
                .hasMessageContaining("ACC-001")
                .hasMessageContaining("FROZEN");
    }

    @ParameterizedTest
    @CsvSource({
            "100.00,   25.00,  125.00",
            "0.00,     10.00,   10.00",
            "50.50,    49.50,  100.00",
            "999.99,    0.01, 1000.00"
    })
    @DisplayName("deposit adds the amount to the starting balance")
    void depositAddsAmountToStartingBalance(BigDecimal startingBalance,
                                            BigDecimal depositAmount,
                                            BigDecimal expectedBalance) {
        Account account = new Account("ACC-001", startingBalance);
        account.deposit(depositAmount);
        assertThat(account.getBalance()).isEqualByComparingTo(expectedBalance);
    }
}
