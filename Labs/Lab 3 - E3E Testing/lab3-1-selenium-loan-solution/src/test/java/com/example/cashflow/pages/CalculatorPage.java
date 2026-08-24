package com.example.cashflow.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * The Line of Credit Calculator form at /cashflow/calc.html.
 *
 * Three text fields and a submit button. Submitting runs a small piece of
 * JavaScript on the page that rewrites the form's action, so the browser
 * navigates to a different result page depending on the values entered.
 * That is why submit() returns a ResultsPage.
 */
public class CalculatorPage extends BasePage {

    public static final String URL = "https://exgnosis.org/cashflow/calc.html";

    // All three fields carry ids, which is the most stable locator available.
    private static final By INCOME     = By.id("income");
    private static final By CREDIT     = By.id("credit");
    private static final By EMPLOYMENT = By.id("employment");

    // The button has a name and a value but no id.
    // name=Calc is more stable than matching on the visible label "Do Me!",
    // which would break the moment someone reworded the button.
    private static final By SUBMIT = By.name("Calc");

    public CalculatorPage(WebDriver driver) {
        super(driver);
    }

    /** Navigates straight to the calculator, skipping the home page. */
    public static CalculatorPage open(WebDriver driver) {
        driver.get(URL);
        return new CalculatorPage(driver);
    }

    public CalculatorPage enterIncome(String income) {
        type(INCOME, income);
        return this;
    }

    public CalculatorPage enterCreditRating(String creditRating) {
        type(CREDIT, creditRating);
        return this;
    }

    public CalculatorPage enterDaysEmployed(String daysEmployed) {
        type(EMPLOYMENT, daysEmployed);
        return this;
    }

    /**
     * Fills all three fields in one call.
     *
     * A convenience for the tests, which always supply all three. The
     * individual setters remain available for scenarios that need to leave
     * a field blank.
     */
    public CalculatorPage enterApplication(String income, String creditRating, String daysEmployed) {
        return enterIncome(income)
                .enterCreditRating(creditRating)
                .enterDaysEmployed(daysEmployed);
    }

    /**
     * Clicks "Do Me!" and waits for the browser to land on the result page.
     *
     * The wait condition is "we are no longer on calc.html" rather than a
     * fixed pause. It returns the instant navigation completes, and fails
     * with a clear timeout if it never does.
     */
    public ResultsPage submit() {
        waitForClickable(SUBMIT).click();
        wait.until(d -> !d.getCurrentUrl().contains("calc.html"));
        return new ResultsPage(driver);
    }
}
