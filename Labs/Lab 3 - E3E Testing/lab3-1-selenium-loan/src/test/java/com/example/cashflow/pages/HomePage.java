package com.example.cashflow.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * The CashFlow site index at /cashflow/.
 *
 * Its only job in these tests is to get us to the calculator.
 *
 * Replace each TODO with the code from the lab instructions.
 */
public class HomePage extends BasePage {

    public static final String URL = "https://exgnosis.org/cashflow/";

    public HomePage(WebDriver driver) {
        super(driver);
    }

    // ---------------------------------------------------------------
    // TODO 2.2a -- the locator
    //
    // The nav links have no id and no class, so the only thing to
    // anchor on is the visible link text: "Line of Credit Calculator"
    //
    //   private static final By CALCULATOR_LINK = ...
    // ---------------------------------------------------------------


    // ---------------------------------------------------------------
    // TODO 2.2b -- static HomePage open(WebDriver driver)
    //
    // Navigate the browser to URL and return a new HomePage.
    // ---------------------------------------------------------------


    // ---------------------------------------------------------------
    // TODO 2.2c -- CalculatorPage openCalculator()
    //
    // Wait for the link to be clickable, click it, and return a NEW
    // CalculatorPage. Returning the next page object is what lets a test
    // read as a chain of steps.
    // ---------------------------------------------------------------

}
