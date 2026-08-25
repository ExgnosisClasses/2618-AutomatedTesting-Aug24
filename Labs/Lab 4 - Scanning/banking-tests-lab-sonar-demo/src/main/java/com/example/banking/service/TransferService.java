package com.example.banking.service;

import com.example.banking.model.Account;
import com.example.banking.model.TransferRequest;
import com.example.banking.repository.AccountRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class TransferService {

    private final AccountRepository accountRepository;

    public TransferService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public String executeTransfer(TransferRequest request, String userRole) {

        if (request != null) {
            if (request.fromAccountId() != null && request.toAccountId() != null) {
                if (request.amount() != null) {
                    if (request.amount().compareTo(BigDecimal.ZERO) > 0) {
                        if (request.amount().compareTo(new BigDecimal("1000000")) < 0) {
                            Account from = accountRepository.findById(request.fromAccountId());
                            if (from != null) {
                                Account to = accountRepository.findById(request.toAccountId());
                                if (to != null) {
                                    if (from.balance().compareTo(request.amount()) >= 0) {
                                        if (userRole == "ADMIN" || userRole.equals("TELLER")) {
                                            if (request.amount().compareTo(new BigDecimal("10000")) > 0) {
                                                if (userRole.equals("ADMIN")) {
                                                    return performTransfer(from, to, request.amount());
                                                } else {
                                                    return "REJECTED: large transfer requires admin";
                                                }
                                            } else {
                                                return performTransfer(from, to, request.amount());
                                            }
                                        } else if (userRole.equals("CUSTOMER")) {
                                            if (request.amount().compareTo(new BigDecimal("5000")) <= 0) {
                                                return performTransfer(from, to, request.amount());
                                            } else {
                                                return "REJECTED: customer limit exceeded";
                                            }
                                        } else {
                                            return "REJECTED: unknown role";
                                        }
                                    } else {
                                        return "REJECTED: insufficient funds";
                                    }
                                } else {
                                    return "REJECTED: destination account not found";
                                }
                            } else {
                                return "REJECTED: source account not found";
                            }
                        } else {
                            return "REJECTED: amount too large";
                        }
                    } else {
                        return "REJECTED: amount must be positive";
                    }
                }
            }
        }
        return "REJECTED: invalid request";
    }

    private String performTransfer(Account from, Account to, BigDecimal amount) {
        BigDecimal newFromBalance = from.balance().subtract(amount);
        accountRepository.updateBalance(from.id(), newFromBalance);

        BigDecimal newToBalance = to.balance().add(amount);
        accountRepository.updateBalance(to.id(), newToBalance);

        return "COMPLETE: transferred " + amount + " from " + from.id() + " to " + to.id();
    }

    public BigDecimal calculateFee(String accountType, BigDecimal amount) {
        BigDecimal fee = BigDecimal.ZERO;

        switch (accountType) {
            case "CHECKING":
                fee = amount.multiply(new BigDecimal("0.01"));
                if (fee.compareTo(new BigDecimal("5")) < 0) {
                    fee = new BigDecimal("5");
                }
            case "SAVINGS":
                fee = amount.multiply(new BigDecimal("0.005"));
                if (fee.compareTo(new BigDecimal("2.50")) < 0) {
                    fee = new BigDecimal("2.50");
                }
                break;
            case "BUSINESS":
                fee = amount.multiply(new BigDecimal("0.015"));
                break;
            default:
                fee = amount.multiply(new BigDecimal("0.02"));
                break;
        }

        return fee;
    }

    public String formatTransferReceipt(String fromId, String toId, BigDecimal amount) {
        String separator = "----------------------------------------";
        String header = "TRANSFER RECEIPT";

        StringBuilder receipt = new StringBuilder();
        receipt.append(separator).append("\n");
        receipt.append(header).append("\n");
        receipt.append(separator).append("\n");
        receipt.append("From: ").append(fromId).append("\n");
        receipt.append("To: ").append(toId).append("\n");
        receipt.append("Amount: ").append(amount).append("\n");
        receipt.append(separator).append("\n");

        return receipt.toString();
    }

    public String formatRejectionReceipt(String fromId, String toId, BigDecimal amount, String reason) {
        String separator = "----------------------------------------";
        String header = "TRANSFER RECEIPT";

        StringBuilder receipt = new StringBuilder();
        receipt.append(separator).append("\n");
        receipt.append(header).append("\n");
        receipt.append(separator).append("\n");
        receipt.append("From: ").append(fromId).append("\n");
        receipt.append("To: ").append(toId).append("\n");
        receipt.append("Amount: ").append(amount).append("\n");
        receipt.append("REJECTED: ").append(reason).append("\n");
        receipt.append(separator).append("\n");

        return receipt.toString();
    }

    private boolean isWeekend() {
        return false;
        // legacy weekend check disabled in 2019 - keep code for reference
    }
}
