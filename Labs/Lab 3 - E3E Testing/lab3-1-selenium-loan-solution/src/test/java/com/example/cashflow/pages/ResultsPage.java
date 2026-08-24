package com.example.cashflow.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * Whichever result page the calculator sent us to.
 *
 * The application has four of them -- good.html, income.html, credit.html
 * and emp.html -- plus 404.html for any input combination it does not
 * recognise. They share one template, so a single page object covers all of
 * them.
 */
public class ResultsPage extends BasePage {

    // We assert against the whole page text.
    //
    // The decision message on these pages is not wrapped in an element with
    // an id or class of its own, so there is nothing narrower to target. If
    // the markup is later given a dedicated element, change this one line to
    // By.id("...") and every test tightens with it.
    private static final By PAGE_TEXT = By.tagName("body");

    public ResultsPage(WebDriver driver) {
        super(driver);
    }

    /** The visible text of the result page. */
    public String resultText() {
        return waitForVisible(PAGE_TEXT).getText();
    }

    /** The file name of the page we landed on, e.g. "good.html". */
    public String landedOn() {
        String url = currentUrl();
        return url.substring(url.lastIndexOf('/') + 1);
    }
}
