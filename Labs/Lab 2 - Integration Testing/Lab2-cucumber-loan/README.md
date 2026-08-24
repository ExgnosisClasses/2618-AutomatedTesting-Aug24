# Lab 2.1 -- Integration Testing with Cucumber and Gherkin

Starter project for **Lab 2 - Integration Testing** of the Software Test
Automation Survey course.

A loan eligibility calculator. You will write a Gherkin feature file
describing the business rules, then the Java step definitions that make it
executable.

## Requirements

- JDK 21
- Maven (bundled with IntelliJ IDEA)
- IntelliJ IDEA (Community Edition is fine)
- The **Cucumber for Java** plugin (see below)

### Install the IntelliJ Cucumber plugin

**File -> Settings -> Plugins -> Marketplace**, search for
**Cucumber for Java**, install, restart.

Without it the `.feature` file is plain text: no syntax colouring, no
Ctrl+click from a Gherkin step to its Java method, and no green run arrows
beside scenarios. The lab works without the plugin but is much less pleasant.

## Importing into IntelliJ

1. **File -> Open**
2. Select this `lab2-1-cucumber-loan` **folder** (not a file inside it)
3. Accept the prompt to import as a Maven project
4. Wait for indexing to finish

If you see red errors, right-click `pom.xml` -> **Maven -> Reload Project**.

## What you edit

| File | Part |
|---|---|
| `src/test/resources/features/loan_eligibility.feature` | Parts 1, 2, 3 |
| `src/test/java/com/example/loans/LoanEligibilitySteps.java` | Part 1 |

Both contain numbered `TODO` comments. **Do not modify `src/main/`** or
`RunCucumberIT.java`.

## Running the scenarios

In IntelliJ: click the green arrow beside a `Scenario:` line, or run
`RunCucumberIT`.

From a terminal in this folder:

```
mvn verify
```

> ### Use `mvn verify`, not `mvn test`
>
> The runner class is named `RunCucumberIT`, ending in `IT` for
> "integration test". Maven's **Surefire** plugin runs `*Test` classes during
> `mvn test`; the **Failsafe** plugin runs `*IT` classes during `mvn verify`.
>
> `mvn test` on this project reports **`Tests run: 0`** and **`BUILD SUCCESS`**.
> That is not a pass. It means nothing ran.

## Expected result when the lab is complete

```
10 Scenarios (10 passed)
50 Steps (50 passed)
```

followed by `BUILD SUCCESS`. That is 4 written-out scenarios plus 6 rows
across the two `Examples` blocks of the Scenario Outline.

An HTML report is written to `target/cucumber-report.html`.

## Project layout

```
lab2-1-cucumber-loan/
├── pom.xml
├── README.md
└── src/
    ├── main/java/com/example/loans/
    │   ├── LoanApplication.java          <- the three inputs (a Java record)
    │   └── LoanEligibilityService.java   <- the rules under test
    └── test/
        ├── java/com/example/loans/
        │   ├── RunCucumberIT.java        <- the runner; do not edit
        │   └── LoanEligibilitySteps.java <- you write the glue here
        └── resources/features/
            └── loan_eligibility.feature  <- you write the Gherkin here
```

## The rules being tested

An applicant is approved for **$5,000** only if all three hold:

| Rule | Threshold |
|---|---|
| Annual income | at least 50000 |
| Credit score | at least 750 |
| Continuous employment | at least 180 days |

Rules are checked in that order, and the **first** failure determines the
message: `Rejected: Income too low`, `Rejected: Credit score too low`, or
`Rejected: Not employed long enough`.

## Dependencies

| Library | Version | Purpose |
|---|---|---|
| Cucumber JVM | 7.20.1 | Gherkin parsing, step matching |
| JUnit Platform Suite | 5.11.4 (BOM) | Hosts the Cucumber engine |
| AssertJ | 3.26.3 | Assertions inside step definitions |
