# Lab 2.1 -- Integration Testing with Cucumber and Gherkin

> **Course:** Software Test Automation Survey
> **Module:** Lab 2 - Integration Testing
> **Estimated time:** 75-90 minutes
> **Environment:** Windows 11, IntelliJ IDEA, Maven, Java 21

---

## Overview

In this lab you will write **executable specifications** for a loan eligibility calculator.

The application decides whether an applicant qualifies for a $5,000 loan, based on three facts: annual income, credit score, and how long they have been continuously employed. Four acceptance criteria were agreed with the business, and your job is to turn those criteria into tests that run.

The difference from Lab 1.1 is where the tests live. There, tests were Java methods, readable only by programmers. Here they are written in **Gherkin** -- structured English that a lending officer could read and correct -- and connected to Java by **step definitions**.

The lab has three parts:

**Part 1** builds one complete scenario end to end: the Gherkin, then the five step definitions that make it run.

**Part 2** adds the three rejection scenarios and reveals something important -- they need no new Java code at all.

**Part 3** collapses all four into a single **Scenario Outline** driven by a data table, and adds boundary cases.

### You do not need to be a Java programmer

Every piece of code is given to you in full. There is a syntax primer below covering everything unfamiliar.

### Learning objectives

By the end of this lab you will be able to:

- Write a Gherkin feature file using `Feature`, `Scenario`, `Given`, `When`, `Then` and `And`
- Explain how Cucumber matches a line of Gherkin to a Java method
- Write step definitions using Cucumber Expressions such as `{int}` and `{string}`
- Share state between steps within a scenario
- Convert repetitive scenarios into a `Scenario Outline` with `Examples`
- Explain why Maven's Failsafe plugin runs these tests and Surefire does not
- Recognise the common Gherkin anti-patterns

---

## Before You Start

### What you need

- **IntelliJ IDEA** (Community Edition is fine)
- **JDK 21**
- **Maven** -- bundled with IntelliJ
- The **Cucumber for Java** IntelliJ plugin
- The starter project from the course repository

### Install the Cucumber plugin first

**File -> Settings -> Plugins -> Marketplace**, search for **Cucumber for Java**, click Install, restart IntelliJ.

This matters more than it sounds. With the plugin you get:

- Syntax colouring in `.feature` files
- Green run arrows beside each `Scenario:`
- **Ctrl+click** from a Gherkin step straight to the Java method that implements it
- Undefined steps highlighted in yellow before you run anything

Without it, the feature file is grey text and you are debugging blind.

### What you do NOT need

- Spring or Spring Boot
- Docker
- A database, a web server, or a browser
- Network access once the project has been imported

### Opening the project

1. **File -> Open**
2. Select the `lab2-1-cucumber-loan` **folder** in the cloned course repository
3. Accept the prompt to import as a Maven project
4. Wait for indexing to finish
5. Open `src/test/resources/features/loan_eligibility.feature`

> **If you see red errors:** right-click `pom.xml` -> **Maven -> Reload Project**.

If you need to build the project from scratch, **Appendix A** has every file.

### Files you will modify

| File | What goes in it | Part |
|---|---|---|
| `src/test/resources/features/loan_eligibility.feature` | Gherkin scenarios | 1, 2, 3 |
| `src/test/java/com/example/loans/LoanEligibilitySteps.java` | Java step definitions | 1 |

**Do not modify anything under `src/main/`**, and do not edit `RunCucumberIT.java`.

---

## The Acceptance Criteria

These four cases came from the business. They are the specification.

| # | Income | Credit score | Days employed | Expected decision |
|---|---|---|---|---|
| 1 | 50000 | 750 | 180 | `Approved for $5,000` |
| 2 | 49999 | 750 | 180 | `Rejected: Income too low` |
| 3 | 50000 | 749 | 180 | `Rejected: Credit score too low` |
| 4 | 50000 | 750 | 179 | `Rejected: Not employed long enough` |

Look at what these four have in common: each varies **one** input by **one** unit from a passing application. Case 2 drops income by a single dollar. Case 3 drops the score by a single point. Case 4 removes a single day.

These are **boundary value** tests, and that is not an accident. Off-by-one errors at thresholds -- writing `>` where `>=` belongs -- are among the most common defects in rule-based code, and among the least likely to be caught by testing with obviously-good and obviously-bad data. Testing with an income of 10000 proves very little. Testing with 49999 proves the boundary sits exactly where the business said it does.

---

## Syntax You Will See

Skip the Java section if you already write Java. Read the Gherkin section regardless.

### Gherkin

Gherkin is a small, structured English syntax. A file uses these keywords:

| Keyword | Purpose |
|---|---|
| `Feature` | Names the capability. One per file. |
| `Scenario` | One concrete example. Many per file. |
| `Given` | The starting situation |
| `When` | The action being tested |
| `Then` | The expected outcome |
| `And` | Continues whatever keyword came before it |
| `Scenario Outline` | A scenario template run once per data row |
| `Examples` | The data table feeding a Scenario Outline |
| `#` | A comment |

Indentation is for readability only; Gherkin does not require it. Blank lines are ignored.

A crucial point about `And`: it is **not** a keyword in its own right. `Given ... And ...` means two `Given` steps. In the Java code you annotate both with `@Given`.

### Cucumber Expressions

The text in a step definition annotation is a pattern with typed placeholders:

```java
@Given("a credit score of {int}")
```

The `{int}` matches a whole number in the Gherkin and passes it to the method as an `int`. The built-in types you will use:

| Placeholder | Matches | Java type |
|---|---|---|
| `{int}` | `750` | `int` |
| `{string}` | `"text in quotes"` | `String` (quotes stripped) |
| `{word}` | a single unquoted word | `String` |
| `{float}` | `12.5` | `float` |

> **A note on `{bigdecimal}`.** Cucumber does provide a `{bigdecimal}`
> parameter type, and money in Java belongs in a `BigDecimal`, so it looks
> like the obvious choice for the income. Avoid it.
>
> The regular expression behind `{bigdecimal}` is built from the **locale of
> the machine running the tests**, because different locales use different
> thousands and decimal separators. That makes it the least predictable of
> the numeric types: a step that matches on one machine can fail to match on
> another, and some IDE plugins do not recognise it at all.
>
> `{int}` is a plain `-?\d+` on every machine in every locale. Use `{int}`
> in the Gherkin and convert inside the step definition, where the conversion
> is explicit and testable.

### Java: records

```java
public record LoanApplication(BigDecimal annualIncome, int creditScore, int daysEmployed) { }
```

A **record** is shorthand for a class that only holds data. The compiler writes the constructor and the accessor methods. Accessors have no `get` prefix:

```java
LoanApplication app = new LoanApplication(new BigDecimal("50000"), 750, 180);
app.creditScore();     // 750
```

### Java: `BigDecimal`

Java's exact decimal type, used for money because floating point cannot represent decimal fractions precisely. Build one from a **String**:

```java
BigDecimal income = new BigDecimal("50000");
```

Compare with `compareTo`, which returns negative, zero, or positive:

```java
if (income.compareTo(MINIMUM) < 0) { ... }   // income is below the minimum
```

### Java: annotations and fields

An annotation is a label starting with `@` that a framework reads. `@Given("...")` tells Cucumber "call this method when you see that line of Gherkin."

A **field** is a variable declared in the class body rather than inside a method, so every method in the class can see it. Step definitions use fields to pass information from one step to the next.

---

## What's in the Starter Project

```
lab2-1-cucumber-loan/
├── pom.xml
├── README.md
└── src/
    ├── main/java/com/example/loans/
    │   ├── LoanApplication.java          <- the three inputs
    │   └── LoanEligibilityService.java   <- the rules under test
    └── test/
        ├── java/com/example/loans/
        │   ├── RunCucumberIT.java        <- the runner; do not edit
        │   └── LoanEligibilitySteps.java <- you write the glue here
        └── resources/features/
            └── loan_eligibility.feature  <- you write the Gherkin here
```

Note where feature files live: under `src/test/resources`, not `src/test/java`. They are not Java, so they are resources.

### `LoanApplication`

```java
public record LoanApplication(BigDecimal annualIncome, int creditScore, int daysEmployed) { }
```

### `LoanEligibilityService` -- the class under test

```java
public class LoanEligibilityService {

    public static final BigDecimal MINIMUM_ANNUAL_INCOME = new BigDecimal("50000");
    public static final int MINIMUM_CREDIT_SCORE = 750;
    public static final int MINIMUM_DAYS_EMPLOYED = 180;
    public static final int LOAN_AMOUNT = 5000;

    public String evaluate(LoanApplication application) {

        if (application.annualIncome().compareTo(MINIMUM_ANNUAL_INCOME) < 0) {
            return "Rejected: Income too low";
        }
        if (application.creditScore() < MINIMUM_CREDIT_SCORE) {
            return "Rejected: Credit score too low";
        }
        if (application.daysEmployed() < MINIMUM_DAYS_EMPLOYED) {
            return "Rejected: Not employed long enough";
        }
        return String.format("Approved for $%,d", LOAN_AMOUNT);
    }
}
```

One method, `evaluate`, returning the decision as text.

**Rule order matters.** The checks run top to bottom and return at the first failure. An applicant failing all three is told only about income. Our four acceptance cases each break exactly one rule, so the order never shows -- but it is a real design decision, and reflection question 3 asks about it.

### `RunCucumberIT` -- the runner

```java
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.example.loans")
@ConfigurationParameter(
        key = PLUGIN_PROPERTY_NAME,
        value = "pretty, summary, html:target/cucumber-report.html")
public class RunCucumberIT {
}
```

An empty class. All the work is in the annotations:

- `@Suite` -- this is a JUnit Platform test suite
- `@IncludeEngines("cucumber")` -- run the Cucumber engine
- `@SelectClasspathResource("features")` -- feature files are in the `features` folder under `src/test/resources`
- `GLUE_PROPERTY_NAME` -- step definitions live in package `com.example.loans`
- `PLUGIN_PROPERTY_NAME` -- print readable output and write an HTML report

**The class name ends in `IT`, and that is load-bearing.** More on this below.

---

### Why `IT` and not `Test`

Maven has two plugins for running tests, and they pick classes by name:

| Plugin | Maven phase | Command | Class names |
|---|---|---|---|
| **Surefire** | `test` | `mvn test` | `*Test`, `Test*`, `*Tests` |
| **Failsafe** | `integration-test` | `mvn verify` | `*IT`, `IT*`, `*ITCase` |

Cucumber scenarios are integration tests, so this project uses Failsafe and the runner is called `RunCucumberIT`.

> ### ⚠️ `mvn test` will report success and run nothing
>
> There are no `*Test` classes in this project. Surefire finds nothing, reports
> `Tests run: 0`, and prints **`BUILD SUCCESS`**.
>
> That is not a pass. Use **`mvn verify`**.

**Why Failsafe exists at all.** Surefire fails the build the instant a test fails, which would skip any teardown -- leaving containers running or databases dirty. Failsafe instead records failures, lets the `post-integration-test` phase clean up, and only then fails the build during `verify`. That is why the `pom.xml` declares **both** the `integration-test` and `verify` goals. Declaring only the first would run the scenarios and then ignore whether they passed.

---

## Part 1 -- Your First Scenario

**Estimated time:** 30-35 minutes

### Task 1.1 -- Write the scenario

Open `src/test/resources/features/loan_eligibility.feature`. The `Feature` block and its description are already there. Replace the `TODO 1.1` comment block with:

```gherkin
  Scenario: An applicant who meets every requirement is approved
    Given an applicant with an annual income of 50000
    And a credit score of 750
    And 180 days of continuous employment
    When the applicant applies for a loan
    Then the decision should be "Approved for $5,000"
```

Read it aloud. That is the point of Gherkin -- a lending officer could read this and tell you whether it describes their business.

Structurally:

- Three `Given` steps establish the applicant (`And` continues the `Given`)
- One `When` step performs the action
- One `Then` step states the expected outcome

The decision text is in **double quotes**. That is what lets the step definition capture it with `{string}`.

### Task 1.2 -- Run it before writing any Java

This step is the most instructive in the lab. Do not skip it.

Run the scenario: click the green arrow beside the `Scenario:` line, or run `RunCucumberIT`.

**It fails**, and the output looks like this:

```
Undefined scenario: An applicant who meets every requirement is approved

Step undefined: Given an applicant with an annual income of 50000

You can implement this step using the snippet(s) below:

@Given("an applicant with an annual income of {int}")
public void an_applicant_with_an_annual_income_of(Integer int1) {
    // Write code here that turns the phrase above into concrete actions
    throw new io.cucumber.java.PendingException();
}
```

**What just happened.** Cucumber parsed the Gherkin, tried to match each step to a Java method, found none, and reported the scenario as *undefined* rather than *failed*. It then generated suggested step definitions and printed them.

Three things worth noticing:

1. **The feature file and the step definitions are genuinely separate.** You can write and parse Gherkin before any Java exists. In real BDD the conversation and the scenarios come first, and the code follows.

2. **Undefined is not the same as failed.** A failing step means the application misbehaved. An undefined step means the automation is incomplete. Cucumber distinguishes them.

3. **Cucumber guessed `{int}` for the income, and this time the guess is right.** Do not treat the snippets as authoritative, though. They are inferred from the one sample value in the feature file, so a step whose examples happen to be whole numbers will always be guessed as `{int}` even when the value is really a decimal. Read them, then decide.

### Task 1.3 -- The three Given steps

Open `LoanEligibilitySteps.java`. The fields are already declared:

```java
private final LoanEligibilityService service = new LoanEligibilityService();

private BigDecimal annualIncome;
private int creditScore;
private int daysEmployed;
private String decision;
```

These fields are the scenario's memory. The `Given` steps fill them in, the `When` step consumes them, the `Then` step checks the result.

Replace `TODO 1.2` with:

```java
    @Given("an applicant with an annual income of {int}")
    public void anApplicantWithAnAnnualIncomeOf(int income) {
        this.annualIncome = BigDecimal.valueOf(income);
    }
```

Replace `TODO 1.3` with:

```java
    @Given("a credit score of {int}")
    public void aCreditScoreOf(int score) {
        this.creditScore = score;
    }
```

Replace `TODO 1.4` with:

```java
    @Given("{int} days of continuous employment")
    public void daysOfContinuousEmployment(int days) {
        this.daysEmployed = days;
    }
```

**Three points about these.**

**All three are `@Given`, even though two are written `And` in the Gherkin.** `And` inherits the keyword above it. There is an `@And` annotation, but using it would obscure which steps are preconditions and which are actions.

**`{int}` everywhere, with the income converted inside the step.** All three values are written as whole numbers in the feature file, so `{int}` matches all three. The income is then widened to a `BigDecimal` with `BigDecimal.valueOf(income)` because money belongs in a `BigDecimal` once it reaches the domain.

This division of labour is worth noticing. **Gherkin carries simple values; the step definition converts them into domain types.** Keeping the feature file to plain integers means it stays readable to the business and matches identically on every machine. Cucumber's `{bigdecimal}` would have skipped the conversion line, but at the cost of a locale-dependent pattern -- a poor trade for one saved line.

**The placeholder can appear anywhere in the pattern.** In the third step it comes first: `{int} days of continuous employment` matches `180 days of continuous employment`. The pattern describes the whole line, not a prefix.

### Task 1.4 -- The When step

Replace `TODO 1.5` with:

```java
    @When("the applicant applies for a loan")
    public void theApplicantAppliesForALoan() {
        LoanApplication application =
                new LoanApplication(annualIncome, creditScore, daysEmployed);
        this.decision = service.evaluate(application);
    }
```

This is where the application is actually exercised. It takes no parameters -- everything it needs is already in the fields, put there by the `Given` steps.

Note that it **stores** the decision rather than asserting on it. Assertions belong in `Then`. Keeping `When` free of assertions means the same `When` works for scenarios expecting success and scenarios expecting rejection -- which is exactly what Part 2 relies on.

### Task 1.5 -- The Then step

Replace `TODO 1.6` with:

```java
    @Then("the decision should be {string}")
    public void theDecisionShouldBe(String expectedDecision) {
        assertThat(decision).isEqualTo(expectedDecision);
    }
```

`{string}` matches text in double quotes and hands it over **with the quotes removed**. So `"Approved for $5,000"` in the Gherkin arrives as `Approved for $5,000`.

`assertThat(...).isEqualTo(...)` is AssertJ, the same library as Lab 1.1. If the strings differ, it throws, Cucumber marks the step failed, and the scenario is reported red.

### Verify Part 1

Run the scenario again.

```
1 Scenarios (1 passed)
5 Steps (5 passed)
```

With the IntelliJ plugin installed, the yellow highlighting in the feature file is gone -- every step now resolves. **Ctrl+click** any Gherkin step to jump to its Java method.

**What you have built.** A specification a non-programmer can read, that executes against real code and fails when the code is wrong. That combination is the entire argument for BDD.

---

## Part 2 -- The Rejection Scenarios

**Estimated time:** 15-20 minutes

Three acceptance criteria remain. Add them to the feature file.

### Task 2.1 -- Income too low

Replace the `TODO 2.1` block with:

```gherkin
  Scenario: An applicant earning below the income threshold is rejected
    Given an applicant with an annual income of 49999
    And a credit score of 750
    And 180 days of continuous employment
    When the applicant applies for a loan
    Then the decision should be "Rejected: Income too low"
```

### Task 2.2 -- Credit score too low

Replace `TODO 2.2` with:

```gherkin
  Scenario: An applicant below the credit score threshold is rejected
    Given an applicant with an annual income of 50000
    And a credit score of 749
    And 180 days of continuous employment
    When the applicant applies for a loan
    Then the decision should be "Rejected: Credit score too low"
```

### Task 2.3 -- Not employed long enough

Replace `TODO 2.3` with:

```gherkin
  Scenario: An applicant employed for too short a time is rejected
    Given an applicant with an annual income of 50000
    And a credit score of 750
    And 179 days of continuous employment
    When the applicant applies for a loan
    Then the decision should be "Rejected: Not employed long enough"
```

### Task 2.4 -- Run all four

Run `RunCucumberIT`.

```
4 Scenarios (4 passed)
20 Steps (20 passed)
```

### Now notice what you did not do

**You wrote no Java.** Three new scenarios, twelve new steps, zero new step definitions.

This is the payoff of the design in Part 1. The steps were written as **parameterised patterns**, not fixed sentences. `a credit score of {int}` matches 750, 749, and every other number. `the decision should be {string}` matches every possible decision text.

The general principle: **step definitions are a vocabulary, not a script.** Build a small vocabulary of parameterised steps and the business can compose new scenarios from it indefinitely. This is what makes BDD scale, and its absence is what makes BDD suites collapse.

The failure mode is a step definition per scenario:

```java
@Given("an applicant with an income of 50000, a score of 750, and 180 days employed")
```

That matches exactly one line and can never be reused. Twenty scenarios means twenty methods. This is the **conjunction step** anti-pattern -- one step doing the work of several -- and it is why some teams conclude Cucumber "does not scale." It is the step design that does not scale, not Cucumber.

---

## Part 3 -- Scenario Outline

**Estimated time:** 20-25 minutes

Look at the four scenarios you have written. They are structurally identical -- same five steps, different numbers. Twenty lines of Gherkin to express four rows of data.

Gherkin's answer is the **Scenario Outline**.

### Task 3.1 -- Write the outline

Replace the `TODO 3.1` block with:

```gherkin
  Scenario Outline: Loan decisions at and around the eligibility boundaries
    Given an applicant with an annual income of <income>
    And a credit score of <score>
    And <days> days of continuous employment
    When the applicant applies for a loan
    Then the decision should be "<decision>"

    Examples: The four acceptance criteria
      | income | score | days | decision                           |
      | 50000  | 750   | 180  | Approved for $5,000                |
      | 49999  | 750   | 180  | Rejected: Income too low           |
      | 50000  | 749   | 180  | Rejected: Credit score too low     |
      | 50000  | 750   | 179  | Rejected: Not employed long enough |
```

**How it works.** `Scenario Outline` is a template. Words in angle brackets are placeholders. Each row under `Examples` supplies one set of values, and Cucumber runs the whole template once per row.

Three rules to keep straight:

1. **Every placeholder needs a matching column heading.** `<income>` requires a column called `income`. A typo produces a step Cucumber cannot match.
2. **Every row needs the same number of cells as the header.** A missing `|` is the single most common Gherkin error.
3. **Quoting still applies.** `"<decision>"` keeps the quotes so that `{string}` still matches after substitution.

Note the decision column has no quotes *in the table* -- the quotes are in the step template. Adding them in both places would give you a decision text that literally begins with a quotation mark.

### Task 3.2 -- A second Examples block

A Scenario Outline can have several `Examples` blocks, each with its own name. Replace `TODO 3.2` with:

```gherkin
    Examples: Comfortably inside the thresholds
      | income | score | days | decision            |
      | 50001  | 751   | 181  | Approved for $5,000 |
      | 99999  | 850   | 365  | Approved for $5,000 |
```

Naming the blocks documents *why* each group of rows exists. The first block is the agreed acceptance criteria; the second confirms the rules do not accidentally reject applicants who are clearly qualified.

The first row here is worth a moment: `50001 / 751 / 181` is one unit **above** each threshold, where the previous block tested one unit below. Together they pin the boundary from both sides. If someone changed `>=` to `>`, the `50000 / 750 / 180` row would go red immediately.

### Task 3.3 -- Run everything

Run `RunCucumberIT`.

```
10 Scenarios (10 passed)
50 Steps (50 passed)
```

Ten, because Cucumber counts each Examples row as a scenario: 4 written-out scenarios + 4 rows + 2 rows.

**Again, no new Java.** The outline reuses the same five step definitions.

### Should you keep both forms?

You now have the four acceptance criteria expressed twice -- once as written-out scenarios, once as table rows. In a real project you would pick one.

**Written-out scenarios** read better and give each case a descriptive name. Good when cases differ meaningfully from one another, or when the scenario name carries real information.

**Scenario Outlines** are compact and make the data pattern visible. Good when cases are structurally identical and vary only in values, as here.

For this feature, the outline is the better choice: four scenarios differing only in numbers is exactly what outlines are for. The written-out versions exist in this lab so you can see the difference and feel why the outline is an improvement.

A reasonable compromise, common in practice: keep the single most important case written out as a named scenario for readability, and put the variations in an outline.

---

## Running from the Command Line

IntelliJ is convenient; CI servers have no IDE.

Open a terminal in the project folder (**View -> Tool Windows -> Terminal**):

```
mvn verify
```

Output ends with the Cucumber summary, then:

```
[INFO] BUILD SUCCESS
```

| Command | Effect |
|---|---|
| `mvn verify` | Compile and run all scenarios |
| `mvn clean verify` | Delete previous output first |
| `mvn test` | **Runs nothing.** No `*Test` classes exist |
| `mvn verify -Dcucumber.filter.tags="@smoke"` | Run only tagged scenarios |

### The HTML report

After a run, open `target/cucumber-report.html` in a browser. Every scenario, every step, pass or fail, with timings.

This is a genuine deliverable. Unlike a JUnit report, it is written in business language, so it can go to a product owner or an auditor as evidence that the agreed criteria were checked.

### Tags

Add `@smoke` above any scenario:

```gherkin
  @smoke
  Scenario: An applicant who meets every requirement is approved
```

Then run only tagged scenarios:

```
mvn verify -Dcucumber.filter.tags="@smoke"
```

This is how a pipeline runs a two-minute smoke suite on every commit and the full suite nightly.

---

## Troubleshooting

**`mvn test` says BUILD SUCCESS but nothing ran**
Expected. Use `mvn verify`. The runner is `RunCucumberIT`, which only Failsafe picks up.

**`UndefinedStepException`, or yellow steps in IntelliJ**
No step definition matches. Compare the Gherkin against the annotation text character by character -- Cucumber matching is exact apart from the placeholders. Watch for trailing spaces and curly quotes pasted from a word processor.

**Steps are yellow but the Java method clearly exists**
The glue package is wrong. `RunCucumberIT` declares `com.example.loans`; the step class must be in that package.

**A number in a step is not recognised as a parameter**
Almost always a `{bigdecimal}`, `{double}` or `{float}` placeholder. Those
patterns are built from the machine's locale and are the least portable of the
parameter types; some IDE plugins also fail to resolve them. Switch the Gherkin
to a whole number, use `{int}`, and convert inside the step definition.

**IntelliJ shows a step as unresolved but `mvn verify` passes**
The IDE plugin and the Cucumber runtime parse expressions separately, and the
plugin supports fewer parameter types. Trust `mvn verify` -- it is what CI runs.

**`AmbiguousStepDefinitionsException`**
Two step definitions match the same line. Usually a duplicate left behind while editing.

**Feature file not found / zero scenarios**
The `.feature` file must be under `src/test/resources/features/`. If it is under `src/test/java`, Maven will not copy it to the classpath.

**`Approved for $5,000` fails with what looks like identical text**
Check for a non-breaking space or a curly quote. Retype the string by hand rather than pasting.

**Scenario Outline: a step is undefined only for some rows**
A column heading does not match a placeholder, or a row has the wrong number of `|` separators.

**No syntax colouring, no Ctrl+click**
The Cucumber for Java plugin is not installed. See *Before You Start*.

---

## Reflection Questions

Create `lab2-notes.md` in the project root and answer these.

1. In Part 2 you added three scenarios and wrote no Java. What property of the Part 1 step definitions made that possible, and what would have had to be different for each new scenario to require new Java?

2. The `When` step stores the decision in a field instead of asserting on it, and the `Then` step does the asserting. What would break if the assertion were moved into the `When` step?

3. `LoanEligibilityService` checks income, then credit score, then days employed, returning at the first failure. None of the four acceptance criteria reveals this order. Write a test case that would, and explain why the business might care.

4. Compare the Gherkin you wrote with a hypothetical version whose steps are `Given I enter 50000 in the income field` and `When I click Submit`. Both could test the same rules. What does the version you wrote survive that the other would not?

5. Lab 1.1 tested the same kind of business rules with plain JUnit and Mockito. This lab used Cucumber. Cucumber adds a translation layer -- Gherkin to regex to Java. Under what circumstances is that layer worth its cost, and under what circumstances would you tell a team to use plain JUnit instead?

---

## What You Have Built

Ten scenario executions driven by a specification written in business language, exercising real application code through five reusable step definitions.

The techniques transfer directly:

**A feature file is a specification that runs.** It does not go stale, because a change to the rules that is not reflected in the code turns it red.

**Step definitions are a vocabulary.** Parameterised steps compose into new scenarios without new code. This is the difference between a BDD suite that scales and one that collapses under its own weight.

**Scenario Outlines separate the shape of a test from its data.** The same idea as the parameterized JUnit test in Lab 1.1, expressed in Gherkin.

**Boundary values are where the bugs are.** All four acceptance criteria sit one unit from a threshold. That was a deliberate choice by whoever wrote them, and a good one.

You also met a piece of Maven that catches everyone once: **Surefire runs `*Test` during `mvn test`, Failsafe runs `*IT` during `mvn verify`.** Integration tests belong to the second pair.

---


## Appendix A -- Complete Project Source

### Directory layout

```
lab2-1-cucumber-loan/
├── pom.xml
└── src/
    ├── main/java/com/example/loans/
    │   ├── LoanApplication.java
    │   └── LoanEligibilityService.java
    └── test/
        ├── java/com/example/loans/
        │   ├── RunCucumberIT.java
        │   └── LoanEligibilitySteps.java
        └── resources/features/
            └── loan_eligibility.feature
```

### `pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                             https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>loan-eligibility-cucumber</artifactId>
    <version>1.0-SNAPSHOT</version>
    <packaging>jar</packaging>

    <name>Lab 2.1 - Integration Testing with Cucumber and Gherkin</name>

    <properties>
        <maven.compiler.release>21</maven.compiler.release>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>

        <cucumber.version>7.20.1</cucumber.version>
        <junit.version>5.11.4</junit.version>
        <assertj.version>3.26.3</assertj.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>io.cucumber</groupId>
                <artifactId>cucumber-bom</artifactId>
                <version>${cucumber.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
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
            <groupId>io.cucumber</groupId>
            <artifactId>cucumber-java</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>io.cucumber</groupId>
            <artifactId>cucumber-junit-platform-engine</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.junit.platform</groupId>
            <artifactId>junit-platform-suite</artifactId>
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
</project>
```

### `LoanApplication.java`

```java
package com.example.loans;

import java.math.BigDecimal;

/**
 * The three facts a loan decision is based on.
 *
 * This is a Java "record" -- a short way of declaring a class whose only
 * job is to hold data. The compiler generates the constructor, the
 * accessor methods, equals, hashCode and toString for you.
 *
 * The accessors are named after the fields, with no "get" prefix:
 *     application.annualIncome()
 *     application.creditScore()
 *     application.daysEmployed()
 */
public record LoanApplication(BigDecimal annualIncome, int creditScore, int daysEmployed) {
}
```

### `LoanEligibilityService.java`

```java
package com.example.loans;

import java.math.BigDecimal;

/**
 * Decides whether a loan application is approved.
 *
 * The rules, in the order they are checked:
 *
 *   1. Annual income must be at least $50,000
 *   2. Credit score must be at least 750
 *   3. Continuous employment must be at least 180 days
 *
 * An application that satisfies all three is approved for a fixed
 * amount of $5,000. Otherwise the FIRST rule that fails determines
 * the rejection message.
 */
public class LoanEligibilityService {

    public static final BigDecimal MINIMUM_ANNUAL_INCOME = new BigDecimal("50000");
    public static final int MINIMUM_CREDIT_SCORE = 750;
    public static final int MINIMUM_DAYS_EMPLOYED = 180;
    public static final int LOAN_AMOUNT = 5000;

    /**
     * Evaluates an application.
     *
     * @return the decision, as text intended to be shown to the applicant
     */
    public String evaluate(LoanApplication application) {

        if (application.annualIncome().compareTo(MINIMUM_ANNUAL_INCOME) < 0) {
            return "Rejected: Income too low";
        }

        if (application.creditScore() < MINIMUM_CREDIT_SCORE) {
            return "Rejected: Credit score too low";
        }

        if (application.daysEmployed() < MINIMUM_DAYS_EMPLOYED) {
            return "Rejected: Not employed long enough";
        }

        return String.format("Approved for $%,d", LOAN_AMOUNT);
    }
}
```

### `RunCucumberIT.java`

```java
package com.example.loans;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.example.loans")
@ConfigurationParameter(
        key = PLUGIN_PROPERTY_NAME,
        value = "pretty, summary, html:target/cucumber-report.html")
public class RunCucumberIT {
}
```

### Test skeletons

`src/test/java/com/example/loans/LoanEligibilitySteps.java`

```java
package com.example.loans;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

public class LoanEligibilitySteps {

    private final LoanEligibilityService service = new LoanEligibilityService();

    private BigDecimal annualIncome;
    private int creditScore;
    private int daysEmployed;
    private String decision;

    // TODO 1.2 -- @Given("an applicant with an annual income of {int}")

    // TODO 1.3 -- @Given("a credit score of {int}")

    // TODO 1.4 -- @Given("{int} days of continuous employment")

    // TODO 1.5 -- @When("the applicant applies for a loan")

    // TODO 1.6 -- @Then("the decision should be {string}")
}
```

`src/test/resources/features/loan_eligibility.feature`

```gherkin
Feature: Loan eligibility

  As a lending officer
  I want applications assessed against our eligibility rules
  So that every applicant receives a consistent, explainable decision

  An applicant qualifies for a $5,000 loan only when all three of these
  are true. If more than one fails, the first failure listed wins.

    1. Annual income is at least $50,000
    2. Credit score is at least 750
    3. Continuous employment is at least 180 days

  # TODO 1.1 -- Scenario: meets every requirement -> "Approved for $5,000"

  # TODO 2.1 -- Scenario: income 49999 -> "Rejected: Income too low"

  # TODO 2.2 -- Scenario: score 749 -> "Rejected: Credit score too low"

  # TODO 2.3 -- Scenario: 179 days -> "Rejected: Not employed long enough"

  # TODO 3.1 -- Scenario Outline with Examples for all four cases
  # TODO 3.2 -- a second Examples block, comfortably inside the thresholds
```
