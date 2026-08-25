package com.example.banking.model;

import java.math.BigDecimal;

public record Account(
        String id,
        String customerId,
        String accountType,
        BigDecimal balance
) {}
