# Lab 1.1 -- Unit Testing with JUnit 5 and Mockito

Starter project for **Lab 1 - Unit Testing** of the Software Test Automation
Survey course.

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

Both files contain numbered `TODO` comments. Replace each one with the test
method given in the lab instructions.

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

Before you start, `mvn test` will report `Tests run: 0` because both test
classes are still empty. That is expected.

## Project layout

```
lab1-junit-mockito/
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
        ├── AccountTest.java              <- you write Part 1 here
        └── AccountServiceTest.java       <- you write Part 2 here
```

## Dependencies

| Library | Version | Purpose |
|---|---|---|
| JUnit Jupiter | 5.11.4 | Runs the tests |
| AssertJ | 3.26.3 | Fluent assertions |
| Mockito | 5.14.2 | Creates the stand-in `AccountDatabase` |

All are `test` scope, so none of them ship with the application.
