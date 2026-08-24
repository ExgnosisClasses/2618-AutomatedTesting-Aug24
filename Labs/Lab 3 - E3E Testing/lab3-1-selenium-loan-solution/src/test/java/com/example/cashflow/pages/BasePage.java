package com.example.cashflow.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Shared behaviour for every page object.
 *
 * Holds the driver and a single WebDriverWait, and provides the small number
 * of helpers the page objects need. Every interaction goes through an explicit
 * wait -- there is no Thread.sleep anywhere in this project.
 */
public abstract class BasePage {

    protected static final Duration TIMEOUT = Duration.ofSeconds(10);

    protected final WebDriver driver;
    protected final WebDriverWait wait;

    protected BasePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, TIMEOUT);
    }

    /** Waits until the element is present and visible, then returns it. */
    protected WebElement waitForVisible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    /** Waits until the element is visible AND enabled, then returns it. */
    protected WebElement waitForClickable(By locator) {
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }

    /** Clears a text field and types into it. */
    protected void type(By locator, String text) {
        WebElement field = waitForVisible(locator);
        field.clear();
        field.sendKeys(text);
    }

    public String currentUrl() {
        return driver.getCurrentUrl();
    }
}
