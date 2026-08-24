package com.example.cashflow;

import com.example.cashflow.pages.CalculatorPage;
import com.example.cashflow.pages.HomePage;
import com.example.cashflow.pages.ResultsPage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end tests for the CashFlow Line of Credit Calculator.
 *
 * The browser lifecycle below is already written for you. Everything else
 * you will add as you work through the lab.
 *
 * Run with:   mvn verify
 * Headless:   mvn verify -Dheadless=true
 */
@DisplayName("Line of Credit Calculator")
class LoanCalculatorIT {

    private WebDriver driver;

    @BeforeEach
    void startBrowser() {
        ChromeOptions options = new ChromeOptions();

        // Headless on CI, headed locally so you can watch it work.
        if (Boolean.parseBoolean(System.getProperty("headless", "false"))) {
            options.addArguments("--headless=new");
        }

        // Set the window size explicitly. The default headless viewport is
        // not the same as a real window, and the difference between the two
        // is a classic source of "passes locally, fails in CI".
        options.addArguments("--window-size=1280,900");

        // Selenium Manager resolves the matching chromedriver automatically.
        driver = new ChromeDriver(options);
    }

    @AfterEach
    void quitBrowser() {
        if (driver != null) {
            driver.quit();
        }
    }

    // ===============================================================
    // PART 1 -- raw Selenium, no page objects
    // ===============================================================

    // ---------------------------------------------------------------
    // TODO 1.1 -- approved applicant, written with raw driver calls
    //
    //   50000 / 750 / 180
    //   expect the page to contain
    //     "You would qualify for a line of credit of $5000"
    // ---------------------------------------------------------------


    // ---------------------------------------------------------------
    // TODO 1.2 -- income too low, also raw
    //
    //   49999 / 750 / 180
    //   expect "Your Income is too low"
    //
    // Copy TODO 1.1 and change the two values. Notice how much is
    // duplicated -- that is the point of this task.
    // ---------------------------------------------------------------


    // ===============================================================
    // PART 2 -- the same two tests, using page objects
    // ===============================================================

    // ---------------------------------------------------------------
    // TODO 2.5 -- rewrite both tests using HomePage / CalculatorPage /
    //             ResultsPage, then DELETE the raw versions above.
    // ---------------------------------------------------------------


    // ===============================================================
    // PART 3 -- all four cases, parameterized
    // ===============================================================

    // ---------------------------------------------------------------
    // TODO 3.1 -- one @ParameterizedTest covering all four cases
    //
    //   income  credit  days  lands on      message
    //   50000   750     180   good.html     You would qualify for a line of credit of $5000
    //   49999   750     180   income.html   Your Income is too low
    //   50000   749     180   credit.html   Your credit rating is not good enough
    //   50000   750     179   emp.html      You have not worked long enough
    //
    // Then DELETE the two page-object tests from TODO 2.5.
    // ---------------------------------------------------------------


    // ---------------------------------------------------------------
    // TODO 3.2 -- a test that starts at the calculator, skipping the
    //             home page, for two of the cases
    // ---------------------------------------------------------------

}
