# Lab 3.1 -- End-to-End Testing with Selenium (SOLUTION)

**COMPLETED SOLUTION.** All page objects and tests are written. Use this to
verify student work or run the full suite. Students should receive
`lab3-1-selenium-loan` instead.

Selenium WebDriver tests for the CashFlow Line of Credit Calculator at
<https://exgnosis.org/cashflow/>, built with the Page Object Model.

## Requirements

- JDK 21
- Maven
- **Google Chrome** installed. The chromedriver binary is resolved
  automatically by Selenium Manager -- there is nothing to download.
- Network access to exgnosis.org

## Running

```
mvn verify                      # headed, watch it work
mvn verify -Dheadless=true      # headless, for CI
```

> **Use `mvn verify`, not `mvn test`.** The test class is `LoanCalculatorIT`.
> Surefire runs `*Test` during `mvn test`; Failsafe runs `*IT` during
> `mvn verify`. `mvn test` reports `Tests run: 0` and `BUILD SUCCESS`.

Expected: **6 tests passing** (4 full-journey cases + 2 shortcut cases).

## The journey under test

```
  /cashflow/            HomePage       click "Line of Credit Calculator"
        |
        v
  /cashflow/calc.html   CalculatorPage enter income, credit, days; click "Do Me!"
        |
        v
  good.html | income.html | credit.html | emp.html    ResultsPage   read the decision
```

Submitting runs JavaScript on the page that rewrites the form's `action`, so
the browser navigates to a different result page for each outcome. That is
why `CalculatorPage.submit()` returns a `ResultsPage`.

## The four cases

| Income | Credit | Days | Lands on | Message |
|---|---|---|---|---|
| 50000 | 750 | 180 | `good.html` | You would qualify for a line of credit of $5000 |
| 49999 | 750 | 180 | `income.html` | Your Income is too low |
| 50000 | 749 | 180 | `credit.html` | Your credit rating is not good enough |
| 50000 | 750 | 179 | `emp.html` | You have not worked long enough |

Any other combination lands on `404.html`.

## Layout

```
src/test/java/com/example/cashflow/
├── pages/
│   ├── BasePage.java        driver, wait, shared helpers
│   ├── HomePage.java        the index; navigates to the calculator
│   ├── CalculatorPage.java  the form; submit() returns a ResultsPage
│   └── ResultsPage.java     reads the decision text and the landing URL
└── LoanCalculatorIT.java    the four cases, parameterized
```

## Locators used

| Element | Locator | Why |
|---|---|---|
| Calculator link | `By.linkText` | Links have no id or class |
| Income / Credit / Days | `By.id` | All three fields have ids |
| Submit button | `By.name("Calc")` | Has a name but no id; more stable than the label |
| Result text | `By.tagName("body")` | No dedicated element wraps the message |
