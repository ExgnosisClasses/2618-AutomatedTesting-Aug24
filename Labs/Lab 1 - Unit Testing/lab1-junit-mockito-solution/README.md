# Lab 1.1 -- Unit Testing with JUnit 5 and Mockito (SOLUTION)

**COMPLETED SOLUTION** for Lab 1 - Unit Testing of the Software Test Automation
Survey course.

All 12 tests are already written. Use this to verify student work or to run the
full suite yourself. Students should receive `lab1-junit-mockito` instead.

A small banking application. You will write unit tests for it in two stages:
first with JUnit and AssertJ alone, then adding Mockito.

## Requirements

- JDK 21
- Maven (bundled with IntelliJ IDEA -- no separate install needed)
- IntelliJ IDEA (Community Edition is fine)

## Importing into IntelliJ

1. **File -> Open**
2. Select this `lab1-junit-mockito` **folder** (not a file inside it)
3. Accept the prompt to import as a Maven project
4. Wait for indexing to finish

If you see red errors after import, right-click `pom.xml` and choose
**Maven -> Reload Project**.

## What you edit

| File | Part |
|---|---|
| `src/test/java/com/example/banking/AccountTest.java` | Part 1 -- JUnit + AssertJ |
| `src/test/java/com/example/banking/AccountServiceTest.java` | Part 2 -- adds Mockito |

In this solution copy, both files are complete.

**Do not modify anything under `src/main/`.** That is the application under test.

## Running the tests

In IntelliJ, click the green arrow in the gutter beside a test method or class.

From a terminal in this folder:

```
mvn test
```

## Expected result when the lab is complete

```
[INFO] Tests run: 15, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

That is 12 test methods; the parameterized test in Part 1 contributes four
invocations rather than one.

## Project layout

```
lab1-junit-mockito-solution/
├── pom.xml
├── README.md
└── src/
    ├── main/java/com/example/banking/
    │   ├── Account.java                  <- Part 1 tests this
    │   ├── AccountDatabase.java          <- Part 2 mocks this (no implementation exists)
    │   ├── AccountService.java           <- Part 2 tests this
    │   ├── AccountStatus.java
    │   ├── AccountFrozenException.java
    │   ├── AccountNotFoundException.java
    │   └── InsufficientFundsException.java
    └── test/java/com/example/banking/
        ├── AccountTest.java              <- Part 1, completed
        └── AccountServiceTest.java       <- Part 2, completed
```

## Dependencies

| Library | Version | Purpose |
|---|---|---|
| JUnit Jupiter | 5.11.4 | Runs the tests |
| AssertJ | 3.26.3 | Fluent assertions |
| Mockito | 5.14.2 | Creates the stand-in `AccountDatabase` |

All are `test` scope, so none of them ship with the application.
