package com.example.loans;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The "glue" between the Gherkin steps and the application.
 *
 * Each method below is matched to a line of Gherkin by the text in its
 * annotation. Cucumber creates a NEW instance of this class for every
 * scenario, so the fields start empty each time and no scenario can
 * affect another.
 *
 * The fields are the scenario's memory: the Given steps fill them in,
 * the When step uses them, and the Then step checks the outcome.
 *
 * Replace each TODO comment with the method from the lab instructions.
 */
public class LoanEligibilitySteps {

    private final LoanEligibilityService service = new LoanEligibilityService();

    private BigDecimal annualIncome;
    private int creditScore;
    private int daysEmployed;
    private String decision;

    // ---------------------------------------------------------------
    // TODO 1.2 -- @Given("an applicant with an annual income of {int}")
    //
    // Convert the int to a BigDecimal and store it in the annualIncome field.
    // Hint: BigDecimal.valueOf(income)
    // ---------------------------------------------------------------


    // ---------------------------------------------------------------
    // TODO 1.3 -- @Given("a credit score of {int}")
    //
    // Store the score in the creditScore field.
    // ---------------------------------------------------------------


    // ---------------------------------------------------------------
    // TODO 1.4 -- @Given("{int} days of continuous employment")
    //
    // Store the number of days in the daysEmployed field.
    // ---------------------------------------------------------------


    // ---------------------------------------------------------------
    // TODO 1.5 -- @When("the applicant applies for a loan")
    //
    // Build a LoanApplication from the three fields, pass it to
    // service.evaluate(...), and store the answer in the decision field.
    // ---------------------------------------------------------------


    // ---------------------------------------------------------------
    // TODO 1.6 -- @Then("the decision should be {string}")
    //
    // Assert that the decision field equals the expected text.
    // ---------------------------------------------------------------

}
