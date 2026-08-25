package com.example.banking.domain;

import java.math.BigDecimal;

public record Account(
        String accountNumber,
        String accountType,
        BigDecimal balance,
        String status) {
}
