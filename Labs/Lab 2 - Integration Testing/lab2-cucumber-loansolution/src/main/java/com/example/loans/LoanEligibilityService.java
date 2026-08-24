package com.example.loans;

import java.math.BigDecimal;

/**
 * Decides whether a loan application is approved.
 *
 * The rules, in the order they are checked:
 *
 *   1. Annual income must be at least $50,000
 *   2. Credit score must be at least 750
 *   3. Continuous employment must be at least 180 days
 *
 * An application that satisfies all three is approved for a fixed
 * amount of $5,000. Otherwise the FIRST rule that fails determines
 * the rejection message.
 */
public class LoanEligibilityService {

    public static final BigDecimal MINIMUM_ANNUAL_INCOME = new BigDecimal("50000");
    public static final int MINIMUM_CREDIT_SCORE = 750;
    public static final int MINIMUM_DAYS_EMPLOYED = 180;
    public static final int LOAN_AMOUNT = 5000;

    /**
     * Evaluates an application.
     *
     * @return the decision, as text intended to be shown to the applicant
     */
    public String evaluate(LoanApplication application) {

        if (application.annualIncome().compareTo(MINIMUM_ANNUAL_INCOME) < 0) {
            return "Rejected: Income too low";
        }

        if (application.creditScore() < MINIMUM_CREDIT_SCORE) {
            return "Rejected: Credit score too low";
        }

        if (application.daysEmployed() < MINIMUM_DAYS_EMPLOYED) {
            return "Rejected: Not employed long enough";
        }

        return String.format("Approved for $%,d", LOAN_AMOUNT);
    }
}
