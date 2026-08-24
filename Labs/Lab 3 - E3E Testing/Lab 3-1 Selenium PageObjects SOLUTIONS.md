# Lab 3.1 -- Solutions and Answers

> **Course:** Software Test Automation Survey
> **Purpose:** Complete code for every task in Lab 3.1, plus written answers to
> every reflection question. Use this to check your work after attempting each
> task. Reading the solution before attempting the task defeats the purpose.

---

## How to Use This File

The complete final contents of all four files you edit are shown below,
followed by notes going further than the lab text. The reflection answers are
written to work as debrief prompts.

### What should exist when you are done

| File | Contents |
|---|---|
| `pages/BasePage.java` | provided, unchanged |
| `pages/HomePage.java` | 1 locator, 2 methods |
| `pages/CalculatorPage.java` | 4 locators, 6 methods |
| `pages/ResultsPage.java` | 1 locator, 2 methods |
| `LoanCalculatorIT.java` | 2 parameterized tests, 6 invocations |

`mvn verify` should report:

```
Tests run: 6, Failures: 0, Errors: 0, Skipped: 0

[INFO] BUILD SUCCESS
```

The Part 1 raw tests and the Part 2 page-object tests are **deleted** by the
end. They are scaffolding for the argument, not part of the finished suite.

---

## Complete `HomePage.java`

```java
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
```

---

## Complete `CalculatorPage.java`

```java
package com.example.cashflow.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * The Line of Credit Calculator form at /cashflow/calc.html.
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

    /** Fills all three fields in one call. */
    public CalculatorPage enterApplication(String income, String creditRating, String daysEmployed) {
        return enterIncome(income)
                .enterCreditRating(creditRating)
                .enterDaysEmployed(daysEmployed);
    }

    /**
     * Clicks "Do Me!" and waits for the browser to land on the result page.
     */
    public ResultsPage submit() {
        waitForClickable(SUBMIT).click();
        wait.until(d -> !d.getCurrentUrl().contains("calc.html"));
        return new ResultsPage(driver);
    }
}
```

---

## Complete `ResultsPage.java`

```java
package com.example.cashflow.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * Whichever result page the calculator sent us to.
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
```

---

## Complete `LoanCalculatorIT.java`

```java
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

@DisplayName("Line of Credit Calculator")
class LoanCalculatorIT {

    private WebDriver driver;

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
}
```

---

## Notes on the Solution

**Why the button uses `By.name("Calc")` and not the label.**
The markup is:

```html
<input type="submit" name="Calc" value="Do Me!" />
```

Three options exist: the name, the value, or a structural CSS selector.
`name` wins. It is an identifier -- someone changing it knows they are
changing an identifier. `value` is a caption; a designer reworking the copy
would change "Do Me!" without a thought and break every test. A structural
selector like `#login_form input[type=submit]` breaks on any layout change.

Note also the exact casing is `Do Me!`, not `DO ME!`. Had we matched on the
visible text, that difference alone would have failed every test -- a small
illustration of why captions make poor locators.

**Why `By.id("employment")` catches people out.**
The visible label reads "Days Employed" and the id is `employment`. Students
who guess from the label write `By.id("days")` and get a
`NoSuchElementException`. There is no substitute for reading the markup.

**Why one `ResultsPage` covers four pages.**
`good.html`, `income.html`, `credit.html` and `emp.html` share a template and
differ only in their message. Model pages by structure, not by URL. Four
classes would be four times the maintenance for no gain. The corollary: if two
URLs render genuinely different structures, they need different page objects
even if they feel like "the same page".

**Why `submit()` waits on the URL rather than on an element.**
The condition is `!d.getCurrentUrl().contains("calc.html")`. Two alternatives
were rejected:

- *Waiting for a specific URL* — `urlToBe("good.html")` — would require the
  page object to know which outcome to expect. Which outcome occurs is the
  thing under test; a page object that knows the answer in advance cannot
  detect a wrong one.
- *Waiting for the result text* — same problem, and it would move an
  assertion into a navigation method.

Waiting for "we left the form" is exactly as much as the page object should
know.

**Why the assertion uses `contains` rather than `isEqualTo`.**
`getText()` on `<body>` returns everything visible: the CashFlow heading, five
nav links, the decision, the copyright line. An exact-match assertion would
have to reproduce all of it and would break on any unrelated change to the
footer. `contains` asserts the thing we care about and ignores the rest.

This is the right instinct generally: **assert on the contract with the user,
not on incidental rendering detail.**

**Why `type()` calls `clear()` first.**
Without it, `sendKeys` appends. In this lab each test gets a fresh browser so
the fields are always empty and the bug would not show. It would surface the
moment someone wrote a test that fills the form twice — and it would look like
an application bug, not a test bug. Putting `clear()` in the shared helper
means no page object can forget it.

**Why a fresh browser per test rather than per class.**
`@BeforeEach` costs roughly 1-2 seconds per invocation, so six invocations pay
about ten seconds. In exchange every test starts from a genuinely clean state:
no cookies, no session, no cached form values, no leftover scroll position.

A shared browser (`@BeforeAll`) is faster and is what teams reach for when
suites get slow. It also introduces order dependence, which is the single
hardest category of test defect to diagnose. If the suite is slow enough to
matter, the answer is parallelism, not shared state.

**Why `quit()` and not `close()`.**
`close()` closes the current window; if it is the last one the driver process
may linger. `quit()` ends the session and the driver process. Using `close()`
in a loop is a reliable way to fill a machine with orphaned Chrome processes.

**Why `@BeforeEach` sets an explicit window size.**
Headless Chrome's default viewport is not the same as a real window. Elements
that are visible at 1280x900 can be off-screen or hidden by a responsive
breakpoint at the default. Pinning the size makes headed and headless runs
agree, which removes the largest single cause of "passes locally, fails in CI".

**On the `headless` Maven property.**
The `default-headless` profile activates when the `headless` property is
*absent* and sets it to `false`. Without it, `${headless}` would pass the
literal string `${headless}` into the JVM and `Boolean.parseBoolean` would
return false — which happens to be the desired result, but by accident. The
profile makes the default explicit.

---

## Reflection Question Answers

### Question 1
*In Part 1 the five locators appeared in every test. In Part 2 each appears once. Describe a specific change to the website that would have required four edits under Part 1 and requires one under Part 2.*

Any change to an element's identifying attribute or text. Concretely:

**The link is reworded.** Suppose marketing renames "Line of Credit Calculator"
to "Credit Calculator". `By.linkText("Line of Credit Calculator")` matches
nothing and every test fails with `NoSuchElementException` on the first click.

Under Part 1 that string appears in every test method. Four tests, four edits,
and a fifth the day someone adds another test — with the near-certainty that
one gets missed and fails in CI a week later.

Under Part 2 it appears once, in `HomePage`:

```java
private static final By CALCULATOR_LINK = By.linkText("Credit Calculator");
```

One line, one file, every test fixed.

Other changes in the same category: the income field's id changing from
`income` to `applicantIncome`; the submit button's `name` changing from `Calc`
to `submit`; the calculator moving from `calc.html` to `calculator.html`.

The general shape: **duplication converts a one-line change into an N-line
change, where N grows every time someone adds a test.** The cost is not the
typing, it is that the number is unbounded and nobody knows what it is.

---

### Question 2
*`openCalculator()` returns a `CalculatorPage` rather than `void`. Give two distinct benefits, one about how tests read and one about what the compiler can catch.*

**Readability.** The return value lets calls chain into a sentence that mirrors
the user's journey:

```java
HomePage.open(driver)
        .openCalculator()
        .enterApplication("50000", "750", "180")
        .submit();
```

With `void` methods, the test becomes a sequence of statements against
intermediate variables:

```java
HomePage home = HomePage.open(driver);
home.openCalculator();
CalculatorPage calc = new CalculatorPage(driver);   // constructed by hand
calc.enterApplication("50000", "750", "180");
calc.submit();
ResultsPage results = new ResultsPage(driver);      // and again
```

Notice what is worse: the test now has to *know* which page it lands on and
construct that page object itself. Knowledge of the site's navigation graph has
leaked out of the page objects and into every test.

**Compile-time safety.** The return type encodes the navigation graph, so the
compiler enforces the order of operations. This does not compile:

```java
HomePage.open(driver).enterIncome("50000");
```

`HomePage` has no `enterIncome`. You cannot fill a form you have not navigated
to, and you find out while typing rather than from a runtime
`NoSuchElementException` thirty seconds into a browser run.

The same protection applies at the other end: `submit()` returns a
`ResultsPage`, so `resultText()` is only reachable after actually submitting.

This is sometimes called a *fluent* page object model, and the compile-time
guarantee is the larger of the two benefits — readability is pleasant, but
catching an invalid journey at compile time is a class of bug eliminated.

---

### Question 3
*`submit()` waits for the URL to stop containing `calc.html` rather than sleeping for two seconds. Describe what goes wrong with a two-second sleep in each of two situations: when the site is unusually slow, and when the site is fast.*

**When the site is slow — the test fails for no reason.**

On a congested network, a loaded CI agent, or a morning when the host is
serving badly, navigation takes 2.5 seconds. The sleep expires at 2.0, the code
proceeds, and `getText()` runs against whatever is on screen — the old form
page, or a partially rendered result. The assertion fails.

Nothing is wrong with the application. The failure is not reproducible: rerun
it and it passes. This is exactly how a suite becomes flaky, and flakiness is
corrosive in a specific way — once people learn that red sometimes means
nothing, they stop reading red, and the next real failure ships.

The instinctive fix makes it worse. Someone raises the sleep to five seconds.
It passes for a month, then fails again on a worse day. Now the sleep is ten.
The suite has been slowed permanently to accommodate its worst observed case.

**When the site is fast — the test wastes time.**

Navigation completes in 200ms. The sleep waits the remaining 1.8 seconds doing
nothing.

Trivial once. This project has six invocations, so 10.8 seconds. A real suite
with 200 tests and several sleeps each wastes 20 minutes per run. Multiply by
every developer, every commit, every day.

And slow feedback has a second-order cost. A pipeline nobody waits for stops
providing feedback: developers batch changes to avoid it, batches make failures
harder to localise, and the suite's value drops further.

**Why the explicit wait has neither problem.** `wait.until(...)` polls the
condition roughly every 500ms and returns *the moment it is true*. Fast site:
returns in ~200ms. Slow site: keeps waiting up to 10 seconds. Genuinely broken:
fails with a `TimeoutException` that says what was being waited for.

The deeper point: **there is no correct sleep value.** Any constant is
simultaneously too long for the common case and too short for the worst case.
That is not a tuning problem, it is a sign the mechanism is wrong.

---

### Question 4
*`ResultsPage` asserts against the whole `<body>` text because the decision message has no element of its own. What kind of bug could slip past that assertion but be caught if the message were in an element with an id?*

The assertion is "this text appears *somewhere* on the page". Any bug that
places the right words in the wrong place survives it.

**The message renders in the wrong region.** Suppose a template change causes
the decision to render into the left-hand nav list instead of the content area,
so the applicant sees their loan decision squeezed into the sidebar under
"Links". `contains` passes — the text is on the page. An assertion against
`By.id("decision")` fails, because the content area is now empty.

**Both a stale and a fresh message are shown.** A caching or template bug leaves
the previous decision on the page alongside the new one. The applicant sees
"Your Income is too low" *and* "You would qualify for a line of credit of
$5000". Whole-body `contains` passes for either. A single-element assertion
using `isEqualTo` fails, because that element does not hold exactly one
expected message.

**The text appears somewhere invisible or incidental.** A hidden debug block, a
commented-out template fragment that got rendered, an HTML comment turned into
text, or a `<title>` change could all put the words on the page without showing
them to the user in the right place.

**The message is right but the surrounding content is wrong.** A results page
that renders the correct decision plus another applicant's details would pass.

The general principle: **the broader the element you assert against, the weaker
the assertion.** Body-level `contains` verifies presence. An id-scoped
`isEqualTo` verifies presence, position, exclusivity and exact content.

This is why the lab tells you to assert against the narrowest element you can
rely on, and why the comment in `ResultsPage` records that we could not. It is
also a concrete thing to take to the developers: *give the decision an id and
our tests get stronger for one line of markup.* That conversation is the same
design-for-testability conversation as asking for `data-testid` attributes.

---

### Question 5
*All six tests pass. Look again at `OnSubmitForm()`. What does this suite actually prove, and what does it not prove? Which of Labs 1.1, 2.1 and 3.1 would catch a genuine error in the eligibility rules?*

**What the application actually does.**

```javascript
if (income == '49999')      { target = "income.html"; }
else if (credit == '749')   { target = "credit.html"; }
else if (emp == '179')      { target = "emp.html";    }
else if ((income == '50000') && (credit == '750') && (emp == '180'))
                            { target = "good.html";   }
```

There is no arithmetic and no comparison against a threshold. The code compares
the raw input strings against the four known test values and hard-codes a
destination for each. Anything else falls through to `404.html`.

So an applicant with $60,000 income, an 800 credit rating and 365 days of
employment — comfortably qualified on every stated rule — gets a 404 page.

**What the suite does prove.** Real things, and things no unit test can reach:

- The home page loads and its link points somewhere valid
- The calculator page loads and its three fields accept input
- The submit button is wired and the page's JavaScript executes
- Submission routes to the correct URL for each of the four inputs
- The result page renders and the expected message is visible to a user
- The whole stack — server, HTML, CSS, JavaScript, browser — hangs together

**What it does not prove.** That the eligibility rules are correct. Or that
they exist. The suite is entirely compatible with an application containing no
business logic whatsoever, which is precisely the situation.

**Which lab would catch a rules error.**

**Lab 1.1 would catch it immediately.** Those tests called
`LoanEligibilityService.evaluate(...)` directly and asserted on the returned
decision. Against an implementation that string-matches four inputs, the
parameterized boundary tests would fail on almost every row.

**Lab 2.1 would too, for the same reason.** The Cucumber steps drive the real
service. The Gherkin is business-readable, but underneath, the step definitions
call the same code path Lab 1.1 tested.

**Lab 3.1 would not.** Every check happens through the browser and can only
observe what the page renders. Given inputs that the application happens to
special-case, the observable behaviour is indistinguishable from a correct
implementation.

**The general lesson.** This is the testing pyramid argument in one concrete
example. E2E tests answer *"can a user complete this journey?"* — integration,
routing, rendering, configuration, deployment. Unit tests answer *"are the
rules right?"*

Neither substitutes for the other. A suite made only of E2E tests can be
entirely green over an application whose core logic is absent, and it will look
healthier than a suite that actually checks the rules. That is worse than
having no tests, because it manufactures confidence.

It is also why "we have 95% E2E coverage" is not the reassurance it sounds
like. Coverage of journeys is not coverage of logic.

---

## Common Student Errors and How to Diagnose Them

| Symptom | Cause | Fix |
|---|---|---|
| `NoSuchElementException` on the days field | Guessed `By.id("days")` from the label | The id is `employment` |
| `SessionNotCreatedException`, Chrome version mismatch | Stale cached driver after a Chrome update | Delete `~/.cache/selenium` and rerun |
| First run hangs then fails | Selenium Manager cannot reach the network | Run once with connectivity to warm the cache |
| `TimeoutException` waiting for URL change | Click missed, or page JS did not run | Screenshot at failure; check the button locator |
| `ElementNotInteractableException` | Waited for visible, not clickable | Use `waitForClickable` |
| `StaleElementReferenceException` | A `WebElement` was cached in a field | Locate fresh inside each method |
| Second value appended to the first | `clear()` missing before `sendKeys` | Use the inherited `type(...)` helper |
| `mvn test` says BUILD SUCCESS, nothing ran | Class is `*IT`; Surefire only runs `*Test` | Use `mvn verify` |
| Chrome windows accumulating | `quit()` not reached | Check `@AfterEach`; close via Task Manager |
| Passes headed, fails headless | Viewport difference | Window size is pinned in `@BeforeEach`; check it is present |

---
