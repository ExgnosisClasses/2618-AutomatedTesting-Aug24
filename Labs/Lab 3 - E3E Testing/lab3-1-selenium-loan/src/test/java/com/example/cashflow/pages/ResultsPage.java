package com.example.cashflow.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * Whichever result page the calculator sent us to.
 *
 * The application has four -- good.html, income.html, credit.html and
 * emp.html -- plus 404.html for any combination it does not recognise.
 * They share one template, so a single page object covers all of them.
 *
 * Replace each TODO with the code from the lab instructions.
 */
public class ResultsPage extends BasePage {

    public ResultsPage(WebDriver driver) {
        super(driver);
    }

    // ---------------------------------------------------------------
    // TODO 2.4a -- the locator
    //
    // The decision message is not wrapped in an element with an id or a
    // class of its own, so there is nothing narrower to target than the
    // page body.
    // ---------------------------------------------------------------


    // ---------------------------------------------------------------
    // TODO 2.4b -- String resultText()
    //
    // Return the visible text of the page.
    // ---------------------------------------------------------------


    // ---------------------------------------------------------------
    // TODO 2.4c -- String landedOn()
    //
    // Return just the file name from the current URL, e.g. "good.html".
    // Hint: currentUrl() is inherited from BasePage.
    // ---------------------------------------------------------------

}
