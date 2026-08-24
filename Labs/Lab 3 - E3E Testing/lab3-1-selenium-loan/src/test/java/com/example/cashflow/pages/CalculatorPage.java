package com.example.cashflow.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * The Line of Credit Calculator form at /cashflow/calc.html.
 *
 * Three text fields and a submit button. Submitting runs JavaScript on the
 * page that rewrites the form's action, so the browser navigates to a
 * different result page depending on the values entered.
 *
 * Replace each TODO with the code from the lab instructions.
 */
public class CalculatorPage extends BasePage {

    public static final String URL = "https://exgnosis.org/cashflow/calc.html";

    public CalculatorPage(WebDriver driver) {
        super(driver);
    }

    // ---------------------------------------------------------------
    // TODO 2.3a -- the four locators
    //
    //   income field   id = income
    //   credit field   id = credit
    //   days field     id = employment
    //   submit button  name = Calc      (it has no id)
    // ---------------------------------------------------------------


    // ---------------------------------------------------------------
    // TODO 2.3b -- static CalculatorPage open(WebDriver driver)
    //
    // Navigate straight to URL, skipping the home page.
    // ---------------------------------------------------------------


    // ---------------------------------------------------------------
    // TODO 2.3c -- the three field setters
    //
    //   enterIncome(String)        enterCreditRating(String)
    //   enterDaysEmployed(String)
    //
    // Each uses the inherited type(...) helper and returns `this`, so
    // calls can be chained.
    // ---------------------------------------------------------------


    // ---------------------------------------------------------------
    // TODO 2.3d -- enterApplication(income, creditRating, daysEmployed)
    //
    // Fill all three fields by chaining the setters above.
    // ---------------------------------------------------------------


    // ---------------------------------------------------------------
    // TODO 2.3e -- ResultsPage submit()
    //
    // Click the button, wait until the URL no longer contains "calc.html",
    // then return a new ResultsPage.
    // ---------------------------------------------------------------

}
