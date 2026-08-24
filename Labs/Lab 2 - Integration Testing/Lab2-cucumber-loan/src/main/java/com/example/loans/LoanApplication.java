package com.example.loans;

import java.math.BigDecimal;

/**
 * The three facts a loan decision is based on.
 *
 * This is a Java "record" -- a short way of declaring a class whose only
 * job is to hold data. The compiler generates the constructor, the
 * accessor methods, equals, hashCode and toString for you.
 *
 * The accessors are named after the fields, with no "get" prefix:
 *     application.annualIncome()
 *     application.creditScore()
 *     application.daysEmployed()
 */
public record LoanApplication(BigDecimal annualIncome, int creditScore, int daysEmployed) {
}
