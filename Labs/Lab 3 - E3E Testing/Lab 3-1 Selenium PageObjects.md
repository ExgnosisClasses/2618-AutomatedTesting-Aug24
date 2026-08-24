# Lab 3.1 -- End-to-End Testing with Selenium and the Page Object Model

> **Course:** Software Test Automation Survey
> **Module:** Lab 3 - End-to-End Testing
> **Estimated time:** 80-95 minutes
> **Environment:** Windows 11, IntelliJ IDEA, Maven, Java 21, Google Chrome

---

## Overview

In this lab you will drive a real browser against a live website and check that a loan calculator gives the right answer.

The application under test is the **CashFlow Line of Credit Calculator** at `https://exgnosis.org/cashflow/`. It asks for three values -- yearly income, credit rating, and days employed -- and tells the applicant whether they qualify.

The four cases are the same ones you tested in Lab 2.1 with Cucumber. What changes is the *level*. There is no calling a Java method directly here. Everything goes through the browser: click the link, fill the form, press the button, read the page.

The lab has three parts, and the order is deliberate.

**Part 1** writes two tests using raw Selenium calls, with locators and waits inline. They work. They are also unmaintainable, and you will see exactly why.

**Part 2** introduces the **Page Object Model** and rewrites the same two tests. Same behaviour, radically better structure.

**Part 3** collapses everything into a single parameterized test covering all four cases.

### You do not need to be a Java programmer

Every piece of code is given in full. A syntax primer below covers anything unfamiliar.

### Learning objectives

By the end of this lab you will be able to:

- Launch and quit a browser from a JUnit test
- Choose locators, and explain why some are more durable than others
- Use explicit waits, and explain why `Thread.sleep` is never the answer
- Structure a suite with the Page Object Model
- Return page objects from navigation methods to model a user journey
- Parameterize an end-to-end test across several data sets
- Explain what an E2E test proves, and what it does not

---

## Before You Start

### What you need

- **IntelliJ IDEA** (Community Edition is fine)
- **JDK 21**
- **Maven** -- bundled with IntelliJ
- **Google Chrome**, installed and working
- Network access to `exgnosis.org`
- The starter project from the course repository

### What you do NOT need

- To download chromedriver. Selenium Manager, built into Selenium 4.6 and later, detects your Chrome version and fetches the matching driver automatically the first time you run.
- Selenium IDE, or any browser extension
- A local web server. The site under test is already deployed.

### Opening the project

1. **File -> Open**
2. Select the `lab3-1-selenium-loan` **folder**
3. Accept the prompt to import as a Maven project
4. Wait for indexing

> **If you see red errors:** right-click `pom.xml` -> **Maven -> Reload Project**.

### Files you will modify

| File | What goes in it | Part |
|---|---|---|
| `LoanCalculatorIT.java` | The tests | 1, 2, 3 |
| `pages/HomePage.java` | Page object | 2 |
| `pages/CalculatorPage.java` | Page object | 2 |
| `pages/ResultsPage.java` | Page object | 2 |

`pages/BasePage.java` is written for you. Read it, don't change it.

---

## The Application Under Test

Spend five minutes here. You cannot write good locators for a page you have not looked at.

### The journey

```
  https://exgnosis.org/cashflow/          the index page
              |
              |  click "Line of Credit Calculator"
              v
  https://exgnosis.org/cashflow/calc.html  the form
              |
              |  fill three fields, click "Do Me!"
              v
  good.html | income.html | credit.html | emp.html    the decision
```

**Submitting the form navigates to a different page.** That is not obvious from looking at it, and it shapes the whole design of your page objects.

### The form markup

```html
<form name="f1" method="post" action="#" id="f1" onsubmit="return OnSubmitForm();">
  <td class="f1_label">Yearly Income :</td>
  <td><input type="text" id="income" name="income" value="" /></td>

  <td class="f1_label">Credit Rating :</td>
  <td><input type="text" id="credit" value="" /></td>

  <td class="f1_label">Days Employed :</td>
  <td><input type="text" id="employment" value="" /></td>

  <td><input type="submit" name="Calc" value="Do Me!" /></td>
</form>
```

What matters for testing:

| Element | Available handles |
|---|---|
| Income field | `id="income"` |
| Credit field | `id="credit"` |
| Days field | `id="employment"` -- note the id does **not** match the label |
| Submit button | `name="Calc"`, `value="Do Me!"`, no id |

The third field is a good example of why you read the markup rather than guessing. The label says "Days Employed" and the id is `employment`.

### The script behind the button

```javascript
function OnSubmitForm(){
    var target = "404.html";
    var income = document.getElementById('income').value;
    var credit = document.getElementById('credit').value;
    var emp    = document.getElementById('employment').value;

    if (income == '49999')      { target = "income.html"; }
    else if (credit == '749')   { target = "credit.html"; }
    else if (emp == '179')      { target = "emp.html";    }
    else if ((income == '50000') && (credit == '750') && (emp == '180'))
                                { target = "good.html";   }

    document.f1.action = target;
    return true;
}
```

Read this carefully, because two things follow from it.

**First: the form's destination is decided at submit time.** The `action` starts as `#` and JavaScript rewrites it. So clicking the button causes a real page navigation, and which page you land on *is* the test result. Your page object will have to wait for that navigation.

**Second, and worth pausing on: this application does not implement the loan rules at all.** It compares the raw input strings against the four test values and hard-codes a destination for each. An applicant with an income of $60,000, a credit rating of 800 and two years of employment -- obviously qualified -- lands on `404.html`.

We will come back to what that means at the end of the lab. For now, note that all four of your tests will pass anyway.

### The four cases

| # | Income | Credit | Days | Lands on | Message on the page |
|---|---|---|---|---|---|
| 1 | 50000 | 750 | 180 | `good.html` | You would qualify for a line of credit of $5000 |
| 2 | 49999 | 750 | 180 | `income.html` | Your Income is too low |
| 3 | 50000 | 749 | 180 | `credit.html` | Your credit rating is not good enough |
| 4 | 50000 | 750 | 179 | `emp.html` | You have not worked long enough |

Each varies one input by one unit from the passing case. These are boundary values, the same set you used in Labs 1.1 and 2.1.

---

## Syntax You Will See

Skip if you already write Java.

### The Selenium objects

| Thing | What it is |
|---|---|
| `WebDriver` | Your handle on the browser. `driver.get(url)` navigates. |
| `WebElement` | One element on the page. `.click()`, `.sendKeys(...)`, `.getText()` |
| `By` | A locator -- a way of describing which element you mean |
| `WebDriverWait` | Waits for a condition, up to a timeout |
| `ExpectedConditions` | A library of conditions to wait for |

### Locators

```java
By.id("income")                              // id="income"
By.name("Calc")                              // name="Calc"
By.linkText("Line of Credit Calculator")     // an <a> with exactly this text
By.tagName("body")                           // the <body> element
By.cssSelector("[data-testid='submit']")     // any CSS selector
```

### `static` methods and fields

```java
private static final By INCOME = By.id("income");
```

`static` means the value belongs to the class rather than to any one instance -- there is one `INCOME` locator shared by every `CalculatorPage`. `final` means it cannot be reassigned. Together they say "a constant".

```java
public static HomePage open(WebDriver driver) { ... }
```

A `static` method is called on the class, not on an object: `HomePage.open(driver)`. Useful when the method's job is to *create* the object.

### `this` and method chaining

```java
public CalculatorPage enterIncome(String income) {
    type(INCOME, income);
    return this;      // hand back the same object
}
```

Returning `this` lets calls be strung together:

```java
page.enterIncome("50000").enterCreditRating("750").enterDaysEmployed("180");
```

### `extends`

```java
public class HomePage extends BasePage { ... }
```

`HomePage` inherits everything `BasePage` has -- the `driver` field, the `wait` field, and helper methods like `type(...)`. Write it once, use it in every page object.

### Lambdas

```java
wait.until(d -> !d.getCurrentUrl().contains("calc.html"));
```

Read `d -> ...` as "given a driver `d`, evaluate this". Selenium calls it repeatedly until it returns true or the timeout expires.

---

## Part 1 -- Raw Selenium

**Estimated time:** 25-30 minutes
**File:** `LoanCalculatorIT.java`

### What is already there

Open `LoanCalculatorIT.java`. The browser lifecycle is written for you:

```java
@BeforeEach
void startBrowser() {
    ChromeOptions options = new ChromeOptions();
    if (Boolean.parseBoolean(System.getProperty("headless", "false"))) {
        options.addArguments("--headless=new");
    }
    options.addArguments("--window-size=1280,900");
    driver = new ChromeDriver(options);
}

@AfterEach
void quitBrowser() {
    if (driver != null) {
        driver.quit();
    }
}
```

Three things worth understanding:

**A fresh browser per test.** `@BeforeEach` runs before every test method, so each test gets a clean browser with no cookies, no session, and no leftover form values. It costs a couple of seconds per test and buys complete isolation. Sharing one browser across tests is a common shortcut and a common source of tests that pass alone and fail in a suite.

**`quit()` in `@AfterEach`, with a null check.** `quit()` closes every window and ends the driver process. Skip this and you accumulate orphaned Chrome processes until the machine runs out of memory. The null check matters because if `startBrowser` itself failed, `driver` is still null and `quit()` would throw, masking the real error.

**The window size is set explicitly.** The default headless viewport differs from a real window. Responsive layouts can hide elements at one size and show them at another, which is the classic "passes on my machine, fails in CI" failure.

### Task 1.1 -- The approved applicant

Replace the `TODO 1.1` block with:

```java
    @Test
    @DisplayName("an applicant who meets every requirement is approved")
    void approvedApplicantSeesQualificationMessage() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        driver.get("https://exgnosis.org/cashflow/");

        wait.until(ExpectedConditions.elementToBeClickable(
                By.linkText("Line of Credit Calculator"))).click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("income")))
                .sendKeys("50000");
        driver.findElement(By.id("credit")).sendKeys("750");
        driver.findElement(By.id("employment")).sendKeys("180");

        driver.findElement(By.name("Calc")).click();

        wait.until(d -> !d.getCurrentUrl().contains("calc.html"));

        String pageText = driver.findElement(By.tagName("body")).getText();
        assertThat(pageText).contains("You would qualify for a line of credit of $5000");
    }
```

**Run it.** Click the green arrow in the gutter. A Chrome window opens, navigates, fills the form, clicks, and closes. The first run may pause for a few seconds while Selenium Manager downloads chromedriver.

Watching it work is worth doing at least once. It is also the last time you should rely on watching -- from here the assertions do the checking.

### Walking through what you wrote

**`driver.get(...)`** navigates and blocks until the page load event fires.

**`elementToBeClickable` before the click.** Not just present in the DOM, and not just visible -- also enabled. Clicking an element that is present but not yet interactive is one of the most common causes of intermittent E2E failures.

**`visibilityOfElementLocated` before the first `sendKeys`.** After clicking the link we are on a new page, and the browser needs a moment. We wait for the first field. Once that is visible the rest of the form is too, so the next two fields use a plain `findElement`.

**No `Thread.sleep` anywhere.** This is the rule that matters most in this lab. A sleep is a guess: too short and the test is flaky, too long and every run wastes the difference. An explicit wait returns the instant the condition is true and fails cleanly at the deadline. There is no correct sleep value, which is precisely the problem with sleeps.

**`wait.until(d -> !d.getCurrentUrl().contains("calc.html"))`** waits for the navigation caused by the button. We do not wait for a fixed URL, because which page we land on is what the test is checking.

**The assertion uses `contains`, not `isEqualTo`.** `getText()` on the body returns the whole visible page -- heading, navigation links, footer, copyright. We check that the decision is present somewhere in it.

### Task 1.2 -- Income too low

Replace the `TODO 1.2` block with:

```java
    @Test
    @DisplayName("an applicant earning below the threshold is rejected")
    void lowIncomeApplicantSeesRejectionMessage() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        driver.get("https://exgnosis.org/cashflow/");

        wait.until(ExpectedConditions.elementToBeClickable(
                By.linkText("Line of Credit Calculator"))).click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("income")))
                .sendKeys("49999");
        driver.findElement(By.id("credit")).sendKeys("750");
        driver.findElement(By.id("employment")).sendKeys("180");

        driver.findElement(By.name("Calc")).click();

        wait.until(d -> !d.getCurrentUrl().contains("calc.html"));

        String pageText = driver.findElement(By.tagName("body")).getText();
        assertThat(pageText).contains("Your Income is too low");
    }
```

Run both. Two green tests.

### Now count the damage

Put the two methods side by side. They differ in **three** places: the income value, the expected message, and the method name. Everything else is identical.

Count what is duplicated:

- 5 locators, each written twice: `linkText`, `income`, `credit`, `employment`, `Calc`
- The navigation URL, twice
- The wait strategy, twice
- The submit-and-wait sequence, twice

You have two of the four cases. Adding the other two gives **20 copies of the same five locators**.

Now imagine the developers rename the link to "Credit Calculator". Four tests break, and you fix the same string in four places. Or the income field's id changes. Four more edits. Every change to the page costs you an edit per test, forever.

There is a second problem, subtler and worse. Read the test again and try to say what it does *in business terms*. You can work it out, but you have to decode `By.id("employment")` and `By.name("Calc")` to get there. The test describes **how the browser is driven**, not **what the business rule is**.

Part 2 fixes both.

---

## Part 2 -- The Page Object Model

**Estimated time:** 35-40 minutes

### The idea

A **page object** is a class that represents one page of the application. It holds:

- the **locators** for that page's elements, in one place
- **methods** describing what a user can do there, in business language

Tests then call those methods instead of touching the driver. When the page changes, you edit one class. When you read a test, you read intent.

```
   Without page objects              With page objects

   Test ──> driver.findElement       Test ──> HomePage.openCalculator()
   Test ──> driver.findElement                   |
   Test ──> driver.findElement                   v
   Test ──> driver.findElement       CalculatorPage ──> driver.findElement
                                             |
        locators everywhere            locators in one place
```

### Task 2.1 -- Read `BasePage`

This one is written for you. Open `pages/BasePage.java`:

```java
public abstract class BasePage {

    protected static final Duration TIMEOUT = Duration.ofSeconds(10);

    protected final WebDriver driver;
    protected final WebDriverWait wait;

    protected BasePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, TIMEOUT);
    }

    protected WebElement waitForVisible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    protected WebElement waitForClickable(By locator) {
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }

    protected void type(By locator, String text) {
        WebElement field = waitForVisible(locator);
        field.clear();
        field.sendKeys(text);
    }

    public String currentUrl() {
        return driver.getCurrentUrl();
    }
}
```

Every page object extends this, so every page object gets a driver, a wait, and these helpers without repeating them.

Two details worth noticing.

**`abstract`** means you cannot create a `BasePage` directly. It exists only to be extended. There is no such thing as "the base page" in the application.

**`type(...)` calls `clear()` first.** Without it, typing into a field that already has a value appends rather than replaces. That bites people constantly, and putting it in the shared helper means no page object can forget it.

### Task 2.2 -- `HomePage`

Open `pages/HomePage.java`. Replace `TODO 2.2a` with the locator:

```java
    // The nav links carry no id or class, so there is nothing more stable
    // to anchor on than the link text. In a codebase we controlled, this is
    // where we would ask for a data-testid attribute.
    private static final By CALCULATOR_LINK = By.linkText("Line of Credit Calculator");
```

Replace `TODO 2.2b`:

```java
    /** Navigates to the home page and returns the page object for it. */
    public static HomePage open(WebDriver driver) {
        driver.get(URL);
        return new HomePage(driver);
    }
```

Replace `TODO 2.2c`:

```java
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
```

**The return type is the point.** `openCalculator()` does not return `void` -- it returns the page you are now on. That single convention is what turns a test into a readable journey, and it means the compiler helps you: you cannot call a calculator method before you have navigated to the calculator.

### Task 2.3 -- `CalculatorPage`

Open `pages/CalculatorPage.java`. Replace `TODO 2.3a`:

```java
    // All three fields carry ids, which is the most stable locator available.
    private static final By INCOME     = By.id("income");
    private static final By CREDIT     = By.id("credit");
    private static final By EMPLOYMENT = By.id("employment");

    // The button has a name and a value but no id.
    // name=Calc is more stable than matching on the visible label "Do Me!",
    // which would break the moment someone reworded the button.
    private static final By SUBMIT = By.name("Calc");
```

Replace `TODO 2.3b`:

```java
    /** Navigates straight to the calculator, skipping the home page. */
    public static CalculatorPage open(WebDriver driver) {
        driver.get(URL);
        return new CalculatorPage(driver);
    }
```

Replace `TODO 2.3c`:

```java
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
```

Replace `TODO 2.3d`:

```java
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
```

Replace `TODO 2.3e`:

```java
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
```

Note the method names. They are `enterIncome` and `submit`, not `getIncomeField` or `clickCalcButton`. **A page object exposes what a user can do, not what widgets exist.** Exposing widgets just moves the driver calls into a different file and keeps the tests written in browser language.

### Task 2.4 -- `ResultsPage`

Open `pages/ResultsPage.java`. Replace `TODO 2.4a`:

```java
    // We assert against the whole page text.
    //
    // The decision message on these pages is not wrapped in an element with
    // an id or class of its own, so there is nothing narrower to target. If
    // the markup is later given a dedicated element, change this one line to
    // By.id("...") and every test tightens with it.
    private static final By PAGE_TEXT = By.tagName("body");
```

Replace `TODO 2.4b`:

```java
    /** The visible text of the result page. */
    public String resultText() {
        return waitForVisible(PAGE_TEXT).getText();
    }
```

Replace `TODO 2.4c`:

```java
    /** The file name of the page we landed on, e.g. "good.html". */
    public String landedOn() {
        String url = currentUrl();
        return url.substring(url.lastIndexOf('/') + 1);
    }
```

**One page object covers four pages.** `good.html`, `income.html`, `credit.html` and `emp.html` share a template and differ only in their message, so one class serves all of them. Model pages by *structure*, not by URL. Four near-identical classes would be four times the maintenance for no benefit.

Note also that `PAGE_TEXT` is a compromise, and the comment says so. Asserting on the whole body is broader than ideal -- a well-built page would give the message its own element with an id. The habit worth taking away: **assert against the narrowest element you can rely on, and leave a note when you cannot.**

### Task 2.5 -- Rewrite the tests

Back in `LoanCalculatorIT.java`. **Delete both raw tests from Part 1**, and replace the `TODO 2.5` block with:

```java
    @Test
    @DisplayName("an applicant who meets every requirement is approved")
    void approvedApplicantSeesQualificationMessage() {
        ResultsPage results = HomePage.open(driver)
                .openCalculator()
                .enterApplication("50000", "750", "180")
                .submit();

        assertThat(results.resultText())
                .contains("You would qualify for a line of credit of $5000");
    }

    @Test
    @DisplayName("an applicant earning below the threshold is rejected")
    void lowIncomeApplicantSeesRejectionMessage() {
        ResultsPage results = HomePage.open(driver)
                .openCalculator()
                .enterApplication("49999", "750", "180")
                .submit();

        assertThat(results.resultText())
                .contains("Your Income is too low");
    }
```

Run both. Still green.

### What changed

Compare against Part 1.

Twenty-odd lines became five. But the line count is the least of it:

**Zero locators in the test.** They live in exactly one place each. Rename the link and you edit one line in `HomePage`, and all four tests keep working.

**The test reads as the journey.** Open the home page, open the calculator, enter an application, submit. Someone who has never seen Selenium can follow it. Compare that to `driver.findElement(By.name("Calc")).click()`.

**The waits vanished from view.** They still happen -- every `type` and every `click` goes through one. But they are infrastructure, and infrastructure belongs behind an abstraction.

**The compiler enforces the journey.** `HomePage` has no `enterIncome`. You cannot fill the form before navigating to it, because the type system will not let you.

---

## Part 3 -- All Four Cases

**Estimated time:** 20 minutes

The two tests you just wrote differ in three values. That is what a parameterized test is for -- exactly as in Lab 1.1 with `@CsvSource`, and Lab 2.1 with `Scenario Outline`.

### Task 3.1 -- Parameterize

**Delete both tests from Task 2.5** and replace the `TODO 3.1` block with:

```java
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

        assertThat(results.resultText()).contains(expectedMessage);
        assertThat(results.landedOn()).isEqualTo(expectedPage);
    }
```

Run it. Four browser windows open and close in turn, and you get four green invocations.

**Two assertions, checking two different things.** The message is what the customer sees. The landing page is how the application got there. If only the page assertion fails, routing broke. If only the text assertion fails, the right page is showing the wrong content. Separating them means a failure tells you where to look.

**`@ParameterizedTest(name = ...)`** customises the label for each invocation. `{0}` through `{4}` are the arguments, so the report reads `50000 / 750 / 180 -> You would qualify...` instead of `[1]`. On a four-case suite that is a nicety; on a forty-case suite it is the difference between a readable report and a wall of numbers.

**`@BeforeEach` still runs per invocation.** Each of the four gets its own browser. They are fully independent and could run in parallel.

### Task 3.2 -- Skip the navigation

Replace `TODO 3.2` with:

```java
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

        ResultsPage results = CalculatorPage.open(driver)
                .enterApplication(income, creditRating, daysEmployed)
                .submit();

        assertThat(results.resultText()).contains(expectedMessage);
    }
```

This is a small illustration of a large idea: **drive the UI only for the thing under test.**

The first test proves the link works. Repeating that navigation in every other test re-tests it for free and costs a page load each time. Here we skip straight to the calculator.

On this tiny site the saving is a second. On a real application the equivalent is logging in through the login form once, and having every other test authenticate by injecting a session cookie. That single change routinely halves the runtime of an E2E suite, and it is the highest-value optimisation most suites never make.

### Verify

Run the whole class. **Six invocations, all green** -- four from Task 3.1, two from Task 3.2.

---

## Running from the Command Line

```
mvn verify
```

> ### Use `mvn verify`, not `mvn test`
>
> The class is `LoanCalculatorIT`. Surefire runs `*Test` during `mvn test`;
> Failsafe runs `*IT` during `mvn verify`. On this project `mvn test` reports
> `Tests run: 0` and `BUILD SUCCESS` -- which is not a pass, it is nothing
> having run. Same trap as Lab 2.1.

| Command | Effect |
|---|---|
| `mvn verify` | Run all tests, browser visible |
| `mvn verify -Dheadless=true` | Run headless, as CI would |
| `mvn clean verify` | Clean first |

Try the headless run. No window appears, and it finishes noticeably faster. That is how these run on a build server, where there is no display at all.

**Run headless at least once before pushing.** The overwhelming majority of "passes locally, fails in CI" E2E failures are the difference between a headed and a headless browser.

---

## Troubleshooting

**`SessionNotCreatedException` mentioning Chrome version**
Chrome updated and the cached driver is stale. Delete `C:\Users\<you>\.cache\selenium` and re-run; Selenium Manager will fetch the right one.

**First run hangs for a long time, then fails**
Selenium Manager is trying to download chromedriver without network access. Run once with connectivity to populate the cache.

**`NoSuchElementException` on `By.id("employment")`**
Check the spelling. The label reads "Days Employed" but the id is `employment`.

**`TimeoutException` waiting for the URL to change**
The form did not navigate. Either the click missed, or the page's JavaScript did not run. Take a screenshot at the point of failure to see what the browser was actually showing.

**`ElementNotInteractableException`**
The element exists but cannot be clicked yet. Use `waitForClickable` rather than `waitForVisible`.

**`StaleElementReferenceException`**
A `WebElement` was stored, then the page changed underneath it. Never cache a `WebElement` in a field -- locate it fresh inside each method, which is what the page objects here do.

**Chrome windows piling up**
`quit()` is not running, usually because a test crashed hard. Close them from Task Manager and check `@AfterEach` is present.

**Tests pass in IntelliJ but `mvn test` finds none**
Expected. Use `mvn verify`.

**Assertion fails and the message looks identical**
`getText()` collapses whitespace and returns visible text only. Print the actual value and compare carefully.

---

## Reflection Questions

Create `lab3-notes.md` in the project root.

1. In Part 1 the five locators appeared in every test. In Part 2 each appears once. Describe a specific change to the website that would have required four edits under Part 1 and requires one under Part 2.

2. `openCalculator()` returns a `CalculatorPage` rather than `void`. Give two distinct benefits of that, one about how tests read and one about what the compiler can catch.

3. `submit()` waits for the URL to stop containing `calc.html` rather than sleeping for two seconds. Describe what goes wrong with a two-second sleep in each of two situations: when the site is unusually slow, and when the site is fast.

4. `ResultsPage` asserts against the whole `<body>` text because the decision message has no element of its own. What kind of bug could slip past that assertion but be caught if the message were in an element with an id?

5. All six of your tests pass. Look again at `OnSubmitForm()`. What does this suite actually prove about the application, and what does it not prove? Which of Labs 1.1, 2.1 and 3.1 would catch a genuine error in the eligibility rules?

---

## What You Have Built

Six passing end-to-end tests over four page objects, driving a real browser.

More importantly, three habits:

**Locators live in page objects, never in tests.** One place to change per element.

**Page objects expose behaviour, not widgets.** `enterApplication(...)`, not `getIncomeField()`. Navigation methods return the next page object, so tests read as journeys and the compiler enforces the order.

**Every wait is explicit.** Not one `Thread.sleep` in the project. Waits are conditions with deadlines, and they live in `BasePage` where no page object can forget them.

### One last thing, and it matters

All six tests pass. Now re-read the application's JavaScript.

It never calculates anything. It compares the input strings against the four known test values and hard-codes a destination for each. An applicant with $60,000 income, an 800 credit rating and a year of employment gets `404.html`.

**Your green suite is entirely compatible with an application that has no business logic in it whatsoever.**

That is not a flaw in your tests. They did their job: they proved the link works, the form accepts input, submission routes correctly, and the right message renders. Those are real things, and no unit test can verify them.

But it is a precise demonstration of what E2E testing is *for*, and what it cannot do. E2E answers "can a user get through this journey?" It does not answer "are the rules right?" For that you need the tests from Lab 1.1, which called the rules directly and would have failed instantly on an implementation like this one.

This is the argument for the testing pyramid, in one concrete example. Not that E2E tests are bad -- they caught things the other labs could not -- but that a suite built only from them tells you far less than it appears to.

---

## Appendix A -- Complete Source

### Directory layout

```
lab3-1-selenium-loan/
├── pom.xml
└── src/test/java/com/example/cashflow/
    ├── pages/
    │   ├── BasePage.java
    │   ├── HomePage.java
    │   ├── CalculatorPage.java
    │   └── ResultsPage.java
    └── LoanCalculatorIT.java
```

There is no `src/main`. The application under test is a deployed website, not code in this project.

### `pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                             https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>cashflow-selenium</artifactId>
    <version>1.0-SNAPSHOT</version>
    <packaging>jar</packaging>

    <name>Lab 3.1 - End-to-End Testing with Selenium</name>

    <properties>
        <maven.compiler.release>21</maven.compiler.release>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <selenium.version>4.27.0</selenium.version>
        <junit.version>5.11.4</junit.version>
        <assertj.version>3.26.3</assertj.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.junit</groupId>
                <artifactId>junit-bom</artifactId>
                <version>${junit.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
        <dependency>
            <groupId>org.seleniumhq.selenium</groupId>
            <artifactId>selenium-java</artifactId>
            <version>${selenium.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.assertj</groupId>
            <artifactId>assertj-core</artifactId>
            <version>${assertj.version}</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.13.0</version>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.5.2</version>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-failsafe-plugin</artifactId>
                <version>3.5.2</version>
                <configuration>
                    <systemPropertyVariables>
                        <headless>${headless}</headless>
                    </systemPropertyVariables>
                </configuration>
                <executions>
                    <execution>
                        <goals>
                            <goal>integration-test</goal>
                            <goal>verify</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>

    <profiles>
        <profile>
            <id>default-headless</id>
            <activation><property><name>!headless</name></property></activation>
            <properties><headless>false</headless></properties>
        </profile>
    </profiles>
</project>
```

The `profiles` block gives `headless` a default of `false` so `mvn verify` works without the flag, while `-Dheadless=true` still overrides it.

### `BasePage.java`

```java
package com.example.cashflow.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public abstract class BasePage {

    protected static final Duration TIMEOUT = Duration.ofSeconds(10);

    protected final WebDriver driver;
    protected final WebDriverWait wait;

    protected BasePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, TIMEOUT);
    }

    protected WebElement waitForVisible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    protected WebElement waitForClickable(By locator) {
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }

    protected void type(By locator, String text) {
        WebElement field = waitForVisible(locator);
        field.clear();
        field.sendKeys(text);
    }

    public String currentUrl() {
        return driver.getCurrentUrl();
    }
}
```

The completed `HomePage`, `CalculatorPage`, `ResultsPage` and `LoanCalculatorIT` are in the solutions document.
