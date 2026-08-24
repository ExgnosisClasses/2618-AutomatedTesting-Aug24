# Lab 2.1 -- Solutions and Answers

> **Course:** Software Test Automation Survey
> **Purpose:** Complete code for every task in Lab 2.1, plus written answers to
> every reflection question. Use this to check your work after attempting each
> task. Reading the solution before attempting the task defeats the purpose.

---

## How to Use This File

The complete final contents of both files you edit are shown below, followed by
notes explaining the decisions behind them. The notes go further than the lab
text and are intended to support the class debrief.

### Summary of what should exist when you are done

| File | Contents |
|---|---|
| `src/test/resources/features/loan_eligibility.feature` | 4 scenarios + 1 outline with 2 Examples blocks |
| `src/test/java/com/example/loans/LoanEligibilitySteps.java` | 5 step definitions |

`mvn verify` should report:

```
10 Scenarios (10 passed)
50 Steps (50 passed)

[INFO] BUILD SUCCESS
```

Ten scenario executions: 4 written-out scenarios, plus 4 rows in the first
`Examples` block, plus 2 rows in the second.

---

## Complete `loan_eligibility.feature`

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

  Scenario: An applicant who meets every requirement is approved
    Given an applicant with an annual income of 50000
    And a credit score of 750
    And 180 days of continuous employment
    When the applicant applies for a loan
    Then the decision should be "Approved for $5,000"

  Scenario: An applicant earning below the income threshold is rejected
    Given an applicant with an annual income of 49999
    And a credit score of 750
    And 180 days of continuous employment
    When the applicant applies for a loan
    Then the decision should be "Rejected: Income too low"

  Scenario: An applicant below the credit score threshold is rejected
    Given an applicant with an annual income of 50000
    And a credit score of 749
    And 180 days of continuous employment
    When the applicant applies for a loan
    Then the decision should be "Rejected: Credit score too low"

  Scenario: An applicant employed for too short a time is rejected
    Given an applicant with an annual income of 50000
    And a credit score of 750
    And 179 days of continuous employment
    When the applicant applies for a loan
    Then the decision should be "Rejected: Not employed long enough"

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

    Examples: Comfortably inside the thresholds
      | income | score | days | decision            |
      | 50001  | 751   | 181  | Approved for $5,000 |
      | 99999  | 850   | 365  | Approved for $5,000 |
```

---

## Complete `LoanEligibilitySteps.java`

```java
package com.example.loans;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The glue between the Gherkin steps and the application.
 *
 * Cucumber creates a NEW instance of this class for every scenario, so the
 * fields below start empty each time and no scenario can affect another.
 */
public class LoanEligibilitySteps {

    private final LoanEligibilityService service = new LoanEligibilityService();

    private BigDecimal annualIncome;
    private int creditScore;
    private int daysEmployed;
    private String decision;

    @Given("an applicant with an annual income of {int}")
    public void anApplicantWithAnAnnualIncomeOf(int income) {
        // {int} keeps the Gherkin simple and locale-independent.
        // The step definition is the boundary where a plain number from the
        // feature file becomes the BigDecimal the domain works in.
        this.annualIncome = BigDecimal.valueOf(income);
    }

    @Given("a credit score of {int}")
    public void aCreditScoreOf(int score) {
        this.creditScore = score;
    }

    @Given("{int} days of continuous employment")
    public void daysOfContinuousEmployment(int days) {
        this.daysEmployed = days;
    }

    @When("the applicant applies for a loan")
    public void theApplicantAppliesForALoan() {
        LoanApplication application =
                new LoanApplication(annualIncome, creditScore, daysEmployed);
        this.decision = service.evaluate(application);
    }

    @Then("the decision should be {string}")
    public void theDecisionShouldBe(String expectedDecision) {
        assertThat(decision).isEqualTo(expectedDecision);
    }
}
```

---

## Notes on the Solution

**Why fields work as scenario state, and why that is safe.**
Cucumber instantiates the glue class once **per scenario**, not once per run.
Every scenario therefore begins with `annualIncome`, `creditScore`,
`daysEmployed` and `decision` at their defaults, and nothing one scenario does
can reach another.

This is the same guarantee Mockito gives with fresh mocks per test in Lab 1.1,
and it exists for the same reason: shared mutable state between tests produces
order-dependent failures, which are among the hardest defects to diagnose.

Worth stating explicitly to the class: **never use `static` fields for scenario
state.** A `static` field is shared across every instance, so it survives from
one scenario to the next and destroys the isolation Cucumber is providing for
free. It is a common mistake and the resulting failures look like application
bugs rather than test bugs.

**Why all three preconditions are annotated `@Given`.**
The Gherkin reads `Given ... And ... And ...`, but `And` is not a keyword in its
own right -- it inherits whatever preceded it. Three `Given` steps.

`io.cucumber.java.en.And` does exist, and annotating the second and third steps
with `@And` would work. It is nonetheless a bad idea: the annotations would then
record how the step happened to be phrased at one call site rather than what the
step *is*. A step that establishes a precondition is a `Given` wherever it
appears. If a later scenario writes `Given a credit score of 750` as its first
line, an `@And`-annotated method would still match, and the annotation would
then be actively misleading.

**Why `{int}` for all three, with the income converted in the step.**

The tempting choice for income is Cucumber's `{bigdecimal}` parameter type:
money belongs in a `BigDecimal`, and the conversion would be free. **Do not use
it here.**

The regular expression behind `{bigdecimal}` (and `{float}` and `{double}`) is
constructed from the **locale of the machine running the tests**, because
locales disagree about thousands and decimal separators. Cucumber's own
maintainers describe this as a known cross-platform wrinkle in these
Java-specific parameter types. Two consequences follow:

- A step can match on one machine and fail to match on another, purely because
  of a regional setting. In a classroom of twenty differently-configured
  laptops, that is a guaranteed support call.
- Some IntelliJ Cucumber plugin versions do not resolve `{bigdecimal}` at all,
  so the step shows as undefined in the editor even when `mvn verify` passes.

`{int}` is a plain `-?\d+` in every locale on every machine, and every IDE
plugin understands it. Since all three values in this feature are whole
numbers, `{int}` matches all three.

The conversion then happens explicitly in the step body:

```java
this.annualIncome = BigDecimal.valueOf(income);
```

This is worth drawing out as a principle rather than a workaround. **The feature
file should carry the simplest values that express the business rule; the step
definition is the boundary where those values become domain types.** Gherkin is
read by people who do not care that Java has a `BigDecimal`. Pushing the type
conversion into the glue keeps the specification readable and keeps the matching
deterministic. The cost is one line.

`BigDecimal.valueOf(int)` produces a value with scale 0, and
`BigDecimal.valueOf(50000).compareTo(new BigDecimal("50000"))` returns 0, so the
boundary comparisons behave exactly as before. (Note the contrast with Lab 1.1's
warning about `equals` and scale -- `compareTo` ignores scale, which is why the
service uses it.)

**Why the placeholder position varies.**
Two of the patterns end with a placeholder; the third begins with one:

```java
@Given("a credit score of {int}")
@Given("{int} days of continuous employment")
```

A Cucumber Expression describes the **entire step text**, not a prefix. The
placeholder can sit anywhere, and the phrasing should follow whatever reads
naturally in English rather than whatever is convenient for the pattern.

**Why `When` stores and `Then` asserts.**
The `When` step calls `evaluate` and puts the answer in a field. It makes no
claim about whether the answer is right.

This separation is what allowed Part 2 to add three scenarios with no new Java.
The same `When` serves the approval case and all three rejection cases, because
it does not care what the decision was. Had the assertion been folded into
`When`, the step would only have suited scenarios expecting approval, and each
rejection case would have needed its own `When`.

It also keeps Gherkin honest. `Then` is where an expectation belongs; a `When`
that can fail an assertion means the scenario can go red before reaching its
`Then`, and the report will point at the wrong line.

**Why the decision text is quoted in Gherkin.**
`{string}` matches a double-quoted run of text and strips the quotes before
passing it to the method. The quotes serve two purposes: they delimit a value
containing spaces, and they visually mark it as data rather than prose.

The alternative, `Then the decision should be Approved for $5,000`, would
require a much looser pattern such as `{}` (the anonymous type) or a regular
expression, and would be ambiguous the moment another step began with the same
words.

**Why the quotes live in the outline template, not the Examples table.**
In the Scenario Outline the step reads:

```gherkin
    Then the decision should be "<decision>"
```

and the table cell contains `Approved for $5,000` with no quotes. After
substitution the line becomes `Then the decision should be "Approved for
$5,000"`, which `{string}` matches.

Quoting in both places is a common error. The cell value would become
`"Approved for $5,000"` including quote characters, substitution would yield
`""Approved for $5,000""`, and `{string}` would capture an empty string followed
by unmatched text. The failure message is confusing enough that students rarely
spot the cause unaided.

**Why two Examples blocks rather than one.**
`Examples` blocks can be named, and the names document intent. The first block is
the agreed acceptance criteria and should not change without a conversation with
the business. The second is engineering judgment -- confirmation that applicants
comfortably over the line are not rejected.

Keeping them separate means a later reader can tell which rows are contractual
and which are defensive.

**On the boundary values.**
The first `Examples` block tests one unit **below** each threshold; the row
`50001 | 751 | 181` in the second tests one unit **above**. Together they pin
each boundary from both sides.

This pair is what catches the classic off-by-one. If someone changed
`compareTo(MINIMUM) < 0` to `<= 0`, the `50000 | 750 | 180` row would fail
immediately. Testing only with an income of 10000 and an income of 90000 would
let that defect through indefinitely.

**Why the lab keeps both the written-out scenarios and the outline.**
It is deliberate duplication for teaching. Students write four near-identical
scenarios, feel the repetition, and then see the outline remove it. Discovering
*why* a feature exists is more durable than being told.

In a real project you would keep one form. For this feature the outline is
better: four scenarios differing only in numbers is precisely the case outlines
exist for. Ask the class which they would keep and why -- there is a defensible
argument for retaining the approval case as a named scenario for readability and
putting the rejections in a table.

---

## Reflection Question Answers

### Question 1
*In Part 2 you added three scenarios and wrote no Java. What property of the Part 1 step definitions made that possible, and what would have had to be different for each new scenario to require new Java?*

The step definitions were written as **parameterised patterns** rather than fixed
sentences. Each contains a placeholder that matches a range of values:

```java
@Given("a credit score of {int}")           // matches 750, 749, 300, any integer
@Then("the decision should be {string}")    // matches any quoted text
```

Because `{int}` matches every integer and `{string}` matches every quoted
string, the three rejection scenarios were already covered by the vocabulary
built in Part 1. Cucumber found a match for every line without new code.

For each new scenario to have required new Java, the steps would have had to be
**literal** -- containing the specific values instead of placeholders:

```java
@Given("a credit score of 750")     // matches exactly one line
@Given("a credit score of 749")     // a second method, for one more line
```

Twenty scenarios would then need up to a hundred step definitions, nearly all
of them near-identical.

A subtler version of the same failure is the **conjunction step**, which packs
several facts into one sentence:

```java
@Given("an applicant with an income of 50000, a score of 750, and 180 days employed")
```

Even with placeholders this reuses badly, because it can only ever describe an
applicant with all three facts stated together in that order. Splitting the
precondition into three independent steps means a future scenario can supply
them in a different order, or introduce a `Background` for the common ones, or
add a fourth fact without rewriting the first three.

The principle: **step definitions are a vocabulary, not a script.** A small set
of parameterised, single-purpose steps composes into an unlimited number of
scenarios. This is what makes a BDD suite scale, and its absence is the usual
reason teams conclude that Cucumber does not.

---

### Question 2
*The `When` step stores the decision in a field instead of asserting on it, and the `Then` step does the asserting. What would break if the assertion were moved into the `When` step?*

Several things, in increasing order of seriousness.

**The `When` step would need to know the expected answer.** It currently takes no
parameters. To assert, it would have to receive the expected decision, which
means the Gherkin would have to supply it:

```gherkin
When the applicant applies for a loan and is told "Approved for $5,000"
```

That sentence conflates an action with its outcome, which is exactly what the
Given/When/Then structure exists to separate.

**Reuse collapses.** The current `When` serves all ten scenario executions
precisely because it is indifferent to the result. An asserting `When` would need
a variant per expected outcome, and Part 2's "no new Java" result would not have
happened.

**The `Then` step becomes redundant or dishonest.** Either it is deleted, leaving
scenarios with no `Then` -- which is not really Gherkin any more -- or it stays
and asserts something already asserted, so a reader cannot tell where the real
check lives.

**Failure reporting points at the wrong line.** Cucumber reports which step
failed. With the assertion in `When`, a wrong decision is reported as a failure
of "the applicant applies for a loan" -- which sounds like the application threw
an error. The `Then` line, which states the expectation that was actually
violated, would be marked skipped. The report would actively mislead.

**Scenarios could not distinguish action failures from wrong answers.** If
`evaluate` threw an exception, that would also surface as a `When` failure. Right
now, an exception fails `When` and a wrong answer fails `Then`, and those are
usefully different problems.

The general rule: `Given` arranges, `When` acts and records, `Then` asserts. It
is the Arrange-Act-Assert pattern from Lab 1.1 with Gherkin keywords, and it
holds for the same reasons.

---

### Question 3
*`LoanEligibilityService` checks income, then credit score, then days employed, returning at the first failure. None of the four acceptance criteria reveals this order. Write a test case that would, and explain why the business might care.*

Any application failing **more than one** rule exposes the order. For example:

| Income | Score | Days | Decision |
|---|---|---|---|
| 49999 | 749 | 179 | `Rejected: Income too low` |

All three rules fail, but only income is reported, because income is checked
first and the method returns immediately.

A second case isolating a different pair:

| Income | Score | Days | Decision |
|---|---|---|---|
| 50000 | 700 | 10 | `Rejected: Credit score too low` |

Income passes; score and days both fail; score is reported because it is checked
before days.

As Gherkin:

```gherkin
  Scenario: An applicant failing several rules is told about income first
    Given an applicant with an annual income of 49999
    And a credit score of 749
    And 179 days of continuous employment
    When the applicant applies for a loan
    Then the decision should be "Rejected: Income too low"
```

**Why the business might care.**

*Applicant experience.* Someone told "income too low" fixes their income,
reapplies, and is then told "credit score too low." Serial rejection is a poor
experience and generates support calls. The business may prefer to report every
failing rule at once, or to report the one most easily remedied.

*Fair lending and regulatory obligations.* In several jurisdictions a lender
must give specific principal reasons for an adverse decision. In the United
States, Regulation B under the Equal Credit Opportunity Act requires adverse
action notices to state the principal reasons for denial. Reporting only the
first rule checked may not satisfy that, and the order itself becomes a
compliance-relevant decision rather than an implementation detail.

*Analytics.* If rejection reasons drive reporting, the ordering silently biases
the numbers. Income will appear to be the dominant cause of rejection simply
because it is evaluated first, and product decisions made on that data would be
built on an artefact of the code.

**The testing lesson.** Four well-chosen acceptance criteria still left a real
behaviour completely unspecified. Each criterion varies one input, which is
exactly right for pinning boundaries -- but boundary tests say nothing about
interaction between rules. When the specification is silent, the implementation
decides, and nobody notices until it matters. Asking "what does this suite *not*
cover?" is a habit worth building.

---

### Question 4
*Compare the Gherkin you wrote with a hypothetical version whose steps are `Given I enter 50000 in the income field` and `When I click Submit`. Both could test the same rules. What does the version you wrote survive that the other would not?*

The version in this lab is **declarative** -- it describes intent. The
alternative is **imperative** -- it describes mechanics.

What the declarative version survives:

**A user interface redesign.** "The applicant applies for a loan" stays true
whether the application arrives via a web form, a mobile app, a REST call, or a
branch officer's terminal. "I click Submit" is false the moment the button is
renamed, replaced with a keyboard shortcut, or split across a two-page wizard.

**Being read by the business.** A lending officer can confirm or correct "an
applicant with an annual income of 50000." They have no view on which field
receives the value. If the scenarios cannot be validated by the people who own
the rules, the main benefit of Gherkin is gone and only its costs remain.

**Testing at a different level.** These same steps could be re-pointed at a REST
endpoint or a UI by rewriting the step definitions only -- the feature file would
not change. Imperative steps are welded to one delivery mechanism.

**Scenario count.** Imperative steps tempt authors to write a scenario per UI
path, and the count grows with the interface rather than with the rules.

**Diagnosis.** A failure in "the applicant applies for a loan" points at the
business operation. A failure in "I click Submit" could be the button, the form,
the network, the session, or the rule -- the step name tells you nothing.

The trade-off worth acknowledging: imperative steps are easier to write, because
they are a transcription of what the author just did by hand. That is exactly why
the anti-pattern is so common. Declarative steps require deciding what the
business operation *is*, which is real work -- and is the work BDD is asking you
to do.

A practical marker: if a step mentions a UI element -- a field, a button, a page,
a click -- it is probably imperative. Those details belong inside the step
definition, where changing them is a one-line fix.

---

### Question 5
*Lab 1.1 tested the same kind of business rules with plain JUnit and Mockito. This lab used Cucumber. Cucumber adds a translation layer. Under what circumstances is that layer worth its cost, and under what circumstances would you tell a team to use plain JUnit instead?*

**What the layer costs.** A second artefact to keep in step with the code; a
matching mechanism that fails in ways plain Java does not (undefined steps,
ambiguous steps, expression typos); an extra abstraction for newcomers to learn;
slower navigation from a failure to the code that caused it; and a suite that can
degrade into hundreds of near-duplicate step definitions if the vocabulary is not
curated.

**When it is worth paying.**

*Non-technical people actually read the scenarios.* This is the decisive test. If
a product owner, business analyst, compliance officer or auditor reads
`.feature` files and comments on them, the translation layer is buying something
Java cannot: a specification the business can verify.

*The rules are contested or complex.* Where eligibility, pricing or entitlement
rules are argued over, having examples in shared language is worth real effort.
Loan eligibility is a decent example -- thresholds get negotiated.

*Regulated environments needing readable evidence.* An HTML Cucumber report in
business language is far better audit evidence than a JUnit XML file.

*The scenarios genuinely precede the code.* BDD's value is largely in the
conversation. Writing examples together, before implementation, surfaces
ambiguity while it is still cheap. Cucumber then keeps those examples honest.

**When to use plain JUnit instead.**

*Nobody outside the team reads the feature files.* If they are written by
testers, reviewed by testers and executed by CI, the layer is pure overhead.
Recommend JUnit and parameterized tests -- Lab 1.1's `@CsvSource` covers the same
four cases in a dozen lines with no glue code at all.

*The scenarios are written after the code.* Then they are tests wearing a
costume, not a specification. The collaboration benefit has already been missed.

*The subject matter is not business-facing.* Retry policies, cache eviction,
serialisation, connection pooling -- nobody wants Gherkin for these, and forcing
it produces technical Gherkin like `Given the SQL table users contains 3 rows`,
which is imperative and unreadable at the same time.

*The team is drowning in step maintenance.* Sometimes the honest advice is to
delete the Gherkin layer and keep the assertions.

**The summary the module offers:**

> BDD without the conversation is just an expensive test syntax.

Cucumber is a collaboration tool that happens to run tests. Judge it on whether
the collaboration is happening. If it is, the layer pays for itself. If it is
not, the same coverage is available in plain JUnit for less money.

---

## Common Student Errors and How to Diagnose Them

| Symptom | Cause | Fix |
|---|---|---|
| `mvn test` prints BUILD SUCCESS, nothing ran | Runner is `*IT`, Surefire only runs `*Test` | Use `mvn verify` |
| Steps yellow in IntelliJ / `UndefinedStepException` | Gherkin text does not match the annotation | Compare character by character; watch trailing spaces |
| A number is not recognised as a parameter | `{bigdecimal}`/`{float}`/`{double}` used; regex is locale-derived | Use `{int}` and convert in the step body |
| IDE says undefined but `mvn verify` passes | Plugin supports fewer parameter types than the runtime | Trust `mvn verify`; prefer `{int}` and `{string}` |
| Steps undefined despite the method existing | Glue package wrong | Step class must be in `com.example.loans` |
| `AmbiguousStepDefinitionsException` | Two patterns match one line | Remove the duplicate |
| Zero scenarios found | Feature file in the wrong folder | Must be under `src/test/resources/features/` |
| Outline row fails, others pass | Column heading does not match a placeholder | Check `<name>` against the header cell |
| `expected "X" but was ""` in an outline | Quotes duplicated in template and table | Quotes belong in the template only |
| Decision comparison fails on apparently identical text | Curly quote or non-breaking space from pasting | Retype the string by hand |
| Second scenario sees the first scenario's data | `static` fields used for scenario state | Make the fields non-static |
| No colouring, no Ctrl+click | Cucumber for Java plugin not installed | Install from the Marketplace and restart |

--- 