package com.example.loans;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The glue between the Gherkin steps and the application.
 *
 * Cucumber creates a NEW instance of this class for every scenario, so the
 * fields below start empty each time and no scenario can affect another.
 */
public class LoanEligibilitySteps {

    private final LoanEligibilityService service = new LoanEligibilityService();

    private BigDecimal annualIncome;
    private int creditScore;
    private int daysEmployed;
    private String decision;

    @Given("an applicant with an annual income of {int}")
    public void anApplicantWithAnAnnualIncomeOf(int income) {
        // {int} keeps the Gherkin simple and locale-independent.
        // The step definition is the boundary where a plain number from the
        // feature file becomes the BigDecimal the domain works in.
        this.annualIncome = BigDecimal.valueOf(income);
    }

    @Given("a credit score of {int}")
    public void aCreditScoreOf(int score) {
        this.creditScore = score;
    }

    @Given("{int} days of continuous employment")
    public void daysOfContinuousEmployment(int days) {
        this.daysEmployed = days;
    }

    @When("the applicant applies for a loan")
    public void theApplicantAppliesForALoan() {
        LoanApplication application =
                new LoanApplication(annualIncome, creditScore, daysEmployed);
        this.decision = service.evaluate(application);
    }

    @Then("the decision should be {string}")
    public void theDecisionShouldBe(String expectedDecision) {
        assertThat(decision).isEqualTo(expectedDecision);
    }
}
