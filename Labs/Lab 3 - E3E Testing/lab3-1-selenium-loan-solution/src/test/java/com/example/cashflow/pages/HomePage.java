package com.example.cashflow.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * The CashFlow site index at /cashflow/.
 *
 * Its only job in these tests is to get us to the calculator.
 */
public class HomePage extends BasePage {

    public static final String URL = "https://exgnosis.org/cashflow/";

    // The nav links carry no id or class, so there is nothing more stable to
    // anchor on than the link text. In a codebase we controlled, this is where
    // we would ask for a data-testid attribute.
    private static final By CALCULATOR_LINK = By.linkText("Line of Credit Calculator");

    public HomePage(WebDriver driver) {
        super(driver);
    }

    /** Navigates to the home page and returns the page object for it. */
    public static HomePage open(WebDriver driver) {
        driver.get(URL);
        return new HomePage(driver);
    }

    /**
     * Clicks through to the calculator.
     *
     * Returns the NEXT page object, which is what lets a test read as a
     * chain of business steps rather than a sequence of clicks.
     */
    public CalculatorPage openCalculator() {
        waitForClickable(CALCULATOR_LINK).click();
        return new CalculatorPage(driver);
    }
}
