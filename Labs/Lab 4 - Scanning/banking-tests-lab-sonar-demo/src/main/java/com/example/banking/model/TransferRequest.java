package com.example.banking.model;

import java.math.BigDecimal;

public record TransferRequest(
        String fromAccountId,
        String toAccountId,
        BigDecimal amount,
        String reference
) {}
