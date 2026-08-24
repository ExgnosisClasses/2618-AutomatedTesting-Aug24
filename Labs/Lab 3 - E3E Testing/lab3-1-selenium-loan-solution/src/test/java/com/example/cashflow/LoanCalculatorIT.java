package com.example.cashflow;

import com.example.cashflow.pages.CalculatorPage;
import com.example.cashflow.pages.HomePage;
import com.example.cashflow.pages.ResultsPage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end tests for the CashFlow Line of Credit Calculator.
 *
 * The four cases are the same acceptance criteria used in Lab 1.1 (JUnit)
 * and Lab 2.1 (Cucumber). Here they are driven through a real browser
 * against the deployed site.
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

        // Headless on CI, headed locally so students can watch it work.
        if (Boolean.parseBoolean(System.getProperty("headless", "false"))) {
            options.addArguments("--headless=new");
        }

        // Set the window size explicitly. The default headless viewport is
        // not the same as a real window, and layout differences between the
        // two are a classic source of "passes locally, fails in CI".
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

    @ParameterizedTest(name = "{0} / {1} / {2}  ->  {4}")
    @CsvSource({
        "50000, 750, 180, good.html,   You would qualify for a line of credit of $5000",
        "49999, 750, 180, income.html, Your Income is too low",
        "50000, 749, 180, credit.html, Your credit rating is not good enough",
        "50000, 750, 179, emp.html,    You have not worked long enough"
    })
    @DisplayName("returns the correct decision for each application")
    void returnsCorrectDecision(String income,
                                String creditRating,
                                String daysEmployed,
                                String expectedPage,
                                String expectedMessage) {

        ResultsPage results = HomePage.open(driver)
                .openCalculator()
                .enterApplication(income, creditRating, daysEmployed)
                .submit();

        // Two assertions, checking two different things:
        //   - the user sees the right message   (what the customer cares about)
        //   - we landed on the right page       (what the developer cares about)
        // If only the page assertion failed, routing broke. If only the text
        // assertion failed, the right page is showing the wrong content.
        assertThat(results.resultText()).contains(expectedMessage);
        assertThat(results.landedOn()).isEqualTo(expectedPage);
    }

    @ParameterizedTest(name = "direct to calculator: {0} / {1} / {2}")
    @CsvSource({
        "50000, 750, 180, You would qualify for a line of credit of $5000",
        "49999, 750, 180, Your Income is too low"
    })
    @DisplayName("can be driven from the calculator page directly")
    void canStartAtTheCalculator(String income,
                                 String creditRating,
                                 String daysEmployed,
                                 String expectedMessage) {

        // Same journey, skipping the home page. Testing navigation once is
        // enough -- every other test can start where the work actually is.
        // This is the "take shortcuts through the UI" principle: drive the
        // UI only for the thing under test.
        ResultsPage results = CalculatorPage.open(driver)
                .enterApplication(income, creditRating, daysEmployed)
                .submit();

        assertThat(results.resultText()).contains(expectedMessage);
    }
}
