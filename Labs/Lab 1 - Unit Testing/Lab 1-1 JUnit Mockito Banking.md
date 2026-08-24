# Lab 1.1 -- Unit Testing with JUnit 5 and Mockito

> **Course:** Software Test Automation Survey
> **Module:** Lab 1 - Unit Testing
> **Estimated time:** 75-90 minutes
> **Environment:** Windows 11, IntelliJ IDEA, Maven, Java 21

---

## Overview

In this lab you will write unit tests for a small banking application. The application has two classes worth testing:

- **`Account`** -- holds a balance and the rules for depositing and withdrawing. It has no dependencies on anything else.
- **`AccountService`** -- coordinates operations by reading accounts from a database, asking the account to do the work, and writing it back. It depends on an `AccountDatabase`.

The lab is split into two parts, deliberately.

**Part 1** tests `Account` using **JUnit 5** and **AssertJ** only. `Account` has no collaborators, so there is nothing to fake.

**Part 2** tests `AccountService` using JUnit, AssertJ, **and Mockito**. `AccountService` needs a database. We do not have one, we do not want one, and we do not need one -- Mockito will manufacture a convincing stand-in.

Most people who meet JUnit and Mockito at the same time end up unsure which framework is doing what. Doing JUnit alone first, then adding Mockito, makes the boundary obvious: **JUnit runs the tests, AssertJ checks the results, Mockito replaces things we don't want to use for real.**

### You do not need to be a Java programmer

Every piece of code you need is given to you in full. Your job is to copy it into the right place, run it, read the explanation, and understand what happened. There is a Java syntax primer below covering everything unfamiliar you will encounter.

### Learning objectives

By the end of this lab you will be able to:

- Explain the structure of a JUnit 5 test method
- Use the Arrange-Act-Assert pattern
- Write AssertJ assertions, including assertions about thrown exceptions
- Write a parameterized test that runs the same logic against many inputs
- Explain what a mock object is and why one is needed
- Use Mockito's `@Mock`, `@InjectMocks`, `when(...).thenReturn(...)`, and `verify(...)`
- Capture arguments with `ArgumentCaptor`
- Decide whether a class needs a mock in order to be tested

---

## Before You Start

### What you need

- **IntelliJ IDEA** (Community Edition is fine)
- **JDK 21**
- **Maven** -- bundled with IntelliJ, no separate install needed
- The starter project from the course repository

### What you do NOT need

- Spring or Spring Boot
- Docker
- A running database
- Any network access once the project has been imported

### Opening the project

1. Open IntelliJ IDEA.
2. Choose **File -> Open**.
3. Navigate to the `lab1-junit-mockito` folder in the cloned course repository and select it. Select the **folder**, not a file inside it.
4. IntelliJ detects `pom.xml` and imports it as a Maven project. Accept any prompt asking to trust or import the project.
5. Wait for the status bar at the bottom to finish indexing.
6. Open `src/test/java/com/example/banking/AccountTest.java`. You should see a class with TODO comments and no red errors.

> **If you see red errors after import:** right-click `pom.xml` in the Project panel, choose **Maven -> Reload Project**. If errors persist, ask the instructor.

If you need to build the project from scratch instead, **Appendix A** contains every file in full.

### Files you will modify

| File | What goes in it | Part |
|---|---|---|
| `src/test/java/com/example/banking/AccountTest.java` | JUnit + AssertJ tests | Part 1 |
| `src/test/java/com/example/banking/AccountServiceTest.java` | JUnit + AssertJ + Mockito tests | Part 2 |

Both files already exist as skeletons with the imports in place. You will paste test methods into them where the TODO comments are.

**You will not modify any file under `src/main/`.** That is the application being tested.

---

## Java Syntax You Will See

Skip this section if you already write Java. Otherwise, read it once -- it covers everything unfamiliar in this lab.

### Annotations

A word starting with `@` attached to a class, method, or field.

```java
@Test
void myTest() { }
```

An annotation is a label. It does nothing by itself. A framework reads the labels and acts on them. `@Test` tells JUnit "this method is a test, please run it."

### `BigDecimal`

Java's type for exact decimal numbers. Money is never stored in `double`, because `0.1 + 0.2` in floating point is `0.30000000000000004`, and a bank cannot lose fractions of a cent.

```java
BigDecimal amount = new BigDecimal("25.00");   // note: a String in quotes
```

Always construct `BigDecimal` from a **String**, never a decimal number literal.

`BigDecimal` cannot use `+` or `-`. It uses method calls, and each one returns a **new** value:

```java
BigDecimal total = balance.add(amount);        // balance itself is unchanged
BigDecimal left  = balance.subtract(amount);
```

Comparison uses `compareTo`, which returns a negative number, zero, or a positive number:

```java
if (balance.compareTo(amount) < 0) { ... }     // balance is less than amount
```

### Static imports

A normal import lets you write `Assertions.assertThat(...)`. A **static** import lets you drop the class name entirely and write `assertThat(...)`:

```java
import static org.assertj.core.api.Assertions.assertThat;
```

Every static import you need is already in the skeleton files.

### Lambdas

A lambda is a small piece of code stored in a variable and run later:

```java
() -> account.withdraw(new BigDecimal("100.00"))
```

Read `() ->` as "a thing that, when run, does the following." This matters for exception tests: we need to hand AssertJ the *instructions* for the risky call so AssertJ can run it inside a try/catch. If we called the method directly, the exception would escape and the test would fail before checking anything.

### Interfaces

An interface lists method signatures with no code behind them:

```java
public interface AccountDatabase {
    Account get(String accountNumber);
    void save(Account account);
}
```

It is a contract: "anything calling itself an `AccountDatabase` must provide these two methods." Because there is no code, an interface cannot be used directly -- something must implement it. In Part 2, **Mockito implements it for us at runtime.**

### `void`

A method declared `void` returns nothing. `deposit` changes a balance but hands nothing back, so it is `void`.

---

## What's in the Starter Project

Take five minutes to read the application before testing it. Everything lives in `src/main/java/com/example/banking/`.

### The directory layout

```
lab1-junit-mockito/
├── pom.xml
└── src/
    ├── main/java/com/example/banking/
    │   ├── Account.java                      <- Part 1 tests this
    │   ├── AccountDatabase.java              <- Part 2 mocks this
    │   ├── AccountService.java               <- Part 2 tests this
    │   ├── AccountStatus.java
    │   ├── AccountFrozenException.java
    │   ├── AccountNotFoundException.java
    │   └── InsufficientFundsException.java
    └── test/java/com/example/banking/
        ├── AccountTest.java                  <- you write Part 1 here
        └── AccountServiceTest.java           <- you write Part 2 here
```

Maven requires this exact layout. Production code goes under `src/main/java`, test code under `src/test/java`, and both use the same package name so tests can see the classes they test.

### `Account` -- the unit under test in Part 1

```java
public class Account {
    private final String accountNumber;
    private BigDecimal balance;
    private AccountStatus status;

    public void deposit(BigDecimal amount)  { ... }
    public void withdraw(BigDecimal amount) { ... }

    public String getAccountNumber() { ... }
    public BigDecimal getBalance()   { ... }
    public AccountStatus getStatus() { ... }
}
```

The rules enforced by `deposit` and `withdraw`:

| Rule | Result when broken |
|---|---|
| Amount must not be null | `IllegalArgumentException` |
| Amount must be greater than zero | `IllegalArgumentException` |
| Account status must be `ACTIVE` | `AccountFrozenException` |
| Withdrawal must not exceed the balance | `InsufficientFundsException` |

Two constructors are available:

```java
new Account("ACC-001", new BigDecimal("100.00"));                        // ACTIVE
new Account("ACC-001", new BigDecimal("100.00"), AccountStatus.FROZEN);  // FROZEN
```

**`Account` depends on nothing.** That is exactly why Part 1 needs no mocks.

### `AccountDatabase` -- the thing we will mock

```java
public interface AccountDatabase {
    Account get(String accountNumber);   // returns null if not found
    void save(Account account);
}
```

Note carefully: **there is no class in this project that implements this interface.** There is no real database, no in-memory map, nothing. That is not an oversight. In Part 2, Mockito builds an implementation on the fly.

> A real application would return `Optional<Account>` rather than `null`. We use `null` here to keep the lab focused on testing rather than on Java idioms.

### `AccountService` -- the unit under test in Part 2

```java
public class AccountService {

    private final AccountDatabase database;

    public AccountService(AccountDatabase database) {   // <- constructor injection
        this.database = database;
    }

    public void deposit(String accountNumber, BigDecimal amount) {
        Account account = fetch(accountNumber);
        account.deposit(amount);
        database.save(account);
    }

    public void withdraw(String accountNumber, BigDecimal amount) { ... }
    public BigDecimal getBalance(String accountNumber) { ... }
    public void transfer(String from, String to, BigDecimal amount) { ... }

    private Account fetch(String accountNumber) {
        Account account = database.get(accountNumber);
        if (account == null) {
            throw new AccountNotFoundException("No account found with number " + accountNumber);
        }
        return account;
    }
}
```

Three things to notice, because all three matter for testing:

1. **The database arrives through the constructor.** `AccountService` never creates its own database. This is *constructor injection*, and it is the single design decision that makes the class testable. If the class built its own database internally, no test could substitute a fake one.

2. **The money arithmetic lives in `Account`, not here.** `AccountService` fetches, delegates, and saves.

3. **`fetch` turns a `null` from the database into an exception.** This is a behaviour we will test.

---

## Part 1 -- JUnit and AssertJ

**Estimated time:** 30-35 minutes
**File:** `src/test/java/com/example/banking/AccountTest.java`

### The anatomy of a test

Every test in this lab follows the same three-step shape, called **Arrange-Act-Assert**:

```java
@Test
@DisplayName("a sentence describing the behaviour")
void aMethodNameDescribingTheBehaviour() {
    // ARRANGE -- build the starting situation
    Account account = new Account("ACC-001", new BigDecimal("100.00"));

    // ACT -- do the one thing being tested
    account.deposit(new BigDecimal("25.00"));

    // ASSERT -- state what should now be true
    assertThat(account.getBalance()).isEqualByComparingTo("125.00");
}
```

What each part contributes:

- **`@Test`** tells JUnit to run this method.
- **`@DisplayName`** gives the test a readable name in the report. Without it, reports show `depositIncreasesTheBalance`; with it, they show a sentence anyone can read.
- **`void`** and no parameters -- a plain `@Test` method takes no arguments and returns nothing.
- **`assertThat(...)`** is AssertJ. If the claim is false, it throws, and JUnit records a failure.

If a test method finishes without any assertion failing, the test passes. There is no "return true" at the end.

### The starter file

Open `AccountTest.java`. It looks like this:

```java
package com.example.banking;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Account")
class AccountTest {

    // TODO 1.1
    // TODO 1.2
    // TODO 1.3
    // TODO 1.4
    // TODO 1.5
    // TODO 1.6
}
```

Replace each TODO comment with the corresponding test method below. Work through them in order.

---

### Task 1.1 -- deposit increases the balance

Replace `// TODO 1.1` with:

```java
    @Test
    @DisplayName("deposit increases the balance")
    void depositIncreasesTheBalance() {
        Account account = new Account("ACC-001", new BigDecimal("100.00"));

        account.deposit(new BigDecimal("25.00"));

        assertThat(account.getBalance()).isEqualByComparingTo("125.00");
    }
```

**Run it.** Click the small green arrow in the left margin (the "gutter") next to the method name and choose **Run**. A test panel opens at the bottom with a green tick.

**What just happened.** JUnit created an instance of `AccountTest`, called `depositIncreasesTheBalance()`, and watched for exceptions. None were thrown, so the test passed.

**Why `isEqualByComparingTo` and not `isEqualTo`?** This trips up nearly everyone.

`BigDecimal` records not just the value but the number of decimal places, called the *scale*. The values `125.00` and `125` are the same amount of money but are **not equal** under Java's `equals()`, because their scales differ. AssertJ's `isEqualTo` uses `equals()`.

`isEqualByComparingTo` uses `compareTo()` instead, which compares numeric value and ignores scale. For money, that is what you want. **Use `isEqualByComparingTo` for every `BigDecimal` assertion in this lab.**

Note also that the expected value is a plain string, `"125.00"`. AssertJ converts it for you.

---

### Task 1.2 -- withdraw decreases the balance

Replace `// TODO 1.2` with:

```java
    @Test
    @DisplayName("withdraw decreases the balance")
    void withdrawDecreasesTheBalance() {
        Account account = new Account("ACC-001", new BigDecimal("100.00"));

        account.withdraw(new BigDecimal("30.00"));

        assertThat(account.getBalance()).isEqualByComparingTo("70.00");
    }
```

Run it. Green.

**Why is this a separate test rather than two assertions in Task 1.1?** Because each test should be able to fail for exactly one reason. If deposit and withdraw were checked in a single test and it went red, you would not know which one broke without reading the failure detail. Two tests means the report tells you immediately.

This is the "one logical behaviour per test" rule. It is the most consistently useful habit in unit testing.

---

### Task 1.3 -- withdraw rejects an amount larger than the balance

So far we have tested that correct usage works. Now we test that **incorrect** usage fails correctly. This matters more than it sounds -- error handling is the least-exercised code in most applications and therefore where the bugs live.

Replace `// TODO 1.3` with:

```java
    @Test
    @DisplayName("withdraw rejects an amount larger than the balance")
    void withdrawRejectsAmountLargerThanBalance() {
        Account account = new Account("ACC-001", new BigDecimal("50.00"));

        assertThatThrownBy(() -> account.withdraw(new BigDecimal("100.00")))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("ACC-001");

        assertThat(account.getBalance()).isEqualByComparingTo("50.00");
    }
```

Run it. Green.

**Reading `assertThatThrownBy`.** It takes a lambda -- the deferred instructions from the primer. AssertJ runs it inside a try/catch and captures whatever comes out. Then:

- `.isInstanceOf(InsufficientFundsException.class)` -- the right *type* of exception was thrown
- `.hasMessageContaining("ACC-001")` -- the message mentions the account, so a human reading a log can tell which account failed

If nothing is thrown at all, `assertThatThrownBy` fails. That is important: a test that expects an exception must fail when the exception does not arrive.

**Why the lambda is required.** Without it you would write:

```java
account.withdraw(new BigDecimal("100.00"));   // throws immediately, test errors out
```

The exception escapes before any assertion runs. The lambda hands AssertJ the instructions rather than the result, so AssertJ controls when they execute.

**The final assertion is the interesting one.** Confirming the right exception was thrown does not confirm that nothing else happened. The last line checks the balance is untouched at $50.00. If a future change subtracted first and validated second, the exception assertion would still pass while money silently vanished. Only the balance check catches that.

> **Principle:** an error-path test should assert both what *did* happen and what *did not*.

---

### Task 1.4 -- deposit rejects a negative amount

Replace `// TODO 1.4` with:

```java
    @Test
    @DisplayName("deposit rejects a negative amount")
    void depositRejectsNegativeAmount() {
        Account account = new Account("ACC-001", new BigDecimal("100.00"));

        assertThatThrownBy(() -> account.deposit(new BigDecimal("-10.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("greater than zero");

        assertThat(account.getBalance()).isEqualByComparingTo("100.00");
    }
```

Run it. Green.

`IllegalArgumentException` is built into Java. It is the conventional choice for "you called this method with a value that makes no sense," as distinct from the custom exceptions (`InsufficientFundsException`, `AccountFrozenException`) which describe *business* rule violations. Both kinds are tested the same way.

Depositing a negative amount is a withdrawal wearing a disguise. Without this rule, an attacker could drain an account through the deposit endpoint.

---

### Task 1.5 -- withdraw rejects a frozen account

Replace `// TODO 1.5` with:

```java
    @Test
    @DisplayName("withdraw rejects a frozen account")
    void withdrawRejectsFrozenAccount() {
        Account account = new Account("ACC-001", new BigDecimal("100.00"), AccountStatus.FROZEN);

        assertThatThrownBy(() -> account.withdraw(new BigDecimal("10.00")))
                .isInstanceOf(AccountFrozenException.class)
                .hasMessageContaining("ACC-001")
                .hasMessageContaining("FROZEN");
    }
```

Run it. Green.

Two things worth noticing.

**The three-argument constructor.** Task 1.1 through 1.4 used the two-argument version, which defaults the status to `ACTIVE`. Here we need a frozen account, so we pass the status explicitly. Being able to construct awkward states directly is a large part of why unit tests are cheap -- freezing an account through a real user interface would take considerably longer.

**Chained assertions all have to pass.** Both `.hasMessageContaining(...)` calls must be satisfied. If the message said "FROZEN" but omitted the account number, the test would fail. Chaining lets one assertion express several requirements.

---

### Task 1.6 -- a parameterized test for deposit

Tasks 1.1 through 1.5 each test one scenario. Sometimes you want the same logic checked against many inputs. Copy-pasting the method four times with different numbers works but is tedious and hard to read.

JUnit's answer is the **parameterized test**.

Replace `// TODO 1.6` with:

```java
    @ParameterizedTest
    @CsvSource({
            "100.00,   25.00,  125.00",
            "0.00,     10.00,   10.00",
            "50.50,    49.50,  100.00",
            "999.99,    0.01, 1000.00"
    })
    @DisplayName("deposit adds the amount to the starting balance")
    void depositAddsAmountToStartingBalance(BigDecimal startingBalance,
                                            BigDecimal depositAmount,
                                            BigDecimal expectedBalance) {
        Account account = new Account("ACC-001", startingBalance);

        account.deposit(depositAmount);

        assertThat(account.getBalance()).isEqualByComparingTo(expectedBalance);
    }
```

Run it.

**What changed structurally.** This method has `@ParameterizedTest` instead of `@Test`, and unlike every previous test it **takes parameters**. `@CsvSource` supplies the values: each string is one row, columns separated by commas, and the columns map onto the parameters in order.

JUnit runs the method **four times**, once per row. The test panel shows one parent entry with four children. The strings are converted to `BigDecimal` automatically -- JUnit knows how to build one from text.

The extra spacing inside the strings is only for readability; JUnit trims it.

**Why this beats a loop.** You could write one `@Test` containing a loop over the same four cases. The parameterized version is better because:

- If row three fails, the report names row three. The other rows still report as passing.
- A loop stops at the first failure, so you learn about one broken case at a time.
- You can re-run a single failing row from IntelliJ.
- Reading `@CsvSource` shows every case at a glance.

Try it: change the last row's expected value from `1000.00` to `1000.01` and re-run. Three children stay green, one goes red, and the failure message names the offending inputs. **Change it back before continuing.**

---

### Verify Part 1

Right-click the `AccountTest` class name in the editor and choose **Run 'AccountTest'**.

You should see **6 test methods, 9 total invocations**, all green (tasks 1.1-1.5 are one invocation each, task 1.6 is four).

The runner displays the `@DisplayName` text, producing a readable specification of what an `Account` does:

```
Account
├── deposit increases the balance
├── withdraw decreases the balance
├── withdraw rejects an amount larger than the balance
├── deposit rejects a negative amount
├── withdraw rejects a frozen account
└── deposit adds the amount to the starting balance
    ├── [1] 100.00, 25.00, 125.00
    ├── [2] 0.00, 10.00, 10.00
    ├── [3] 50.50, 49.50, 100.00
    └── [4] 999.99, 0.01, 1000.00
```

That tree is a genuine deliverable. A colleague can read it and learn how the class behaves without opening the source.

**Note what Part 1 did not require:** no database, no configuration, no network, no Mockito. `Account` has no collaborators, so testing it needs nothing but the class itself. This is why the testing pyramid puts unit tests at the bottom -- they are fast and cheap precisely because there is nothing to set up.

---

## Part 2 -- Adding Mockito

**Estimated time:** 40-45 minutes
**File:** `src/test/java/com/example/banking/AccountServiceTest.java`

### The problem Mockito solves

`AccountService` needs an `AccountDatabase`:

```java
public AccountService(AccountDatabase database) { ... }
```

To construct one in a test, we must pass something. But `AccountDatabase` is an interface with no implementation anywhere in the project.

We have three options.

**Option 1: build a real database.** Install a database, create a schema, write an implementation, start it before tests. Slow, fragile, and it tests the database as much as our class.

**Option 2: hand-write a fake.** Perhaps forty lines backed by a `HashMap`. Workable, and sometimes the right answer -- but it needs writing, maintaining, and its own tests. It also cannot easily simulate a database that throws a connection error.

**Option 3: let Mockito generate one.** One annotation. Mockito produces an object that implements the interface, lets you dictate what each method returns, and records every call made to it.

Option 3 is what this part is about.

### What a mock actually is

A **mock** is an object that pretends to be another type. Mockito generates it at runtime.

Two properties make it useful:

1. **It answers however you tell it to.** `when(database.get("ACC-001")).thenReturn(someAccount)` means "if anyone asks this mock for ACC-001, hand back this account."

2. **It remembers everything.** Every call, with every argument, is recorded. Afterwards you can ask "was `save` called? how many times? with what?"

Property 1 lets you **control the situation**. Property 2 lets you **inspect the behaviour**.

**Unstubbed methods return a harmless default:** `null` for objects, `0` for numbers, `false` for booleans. Nothing happens for `void` methods. This default matters in Task 2.3.

### The starter file

Open `AccountServiceTest.java`:

```java
package com.example.banking;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Account service")
class AccountServiceTest {

    @Mock
    private AccountDatabase database;

    @InjectMocks
    private AccountService accountService;

    // TODO 2.1
    // TODO 2.2
    // TODO 2.3
    // TODO 2.4
    // TODO 2.5
    // TODO 2.6
}
```

Three annotations are doing the setup work:

**`@ExtendWith(MockitoExtension.class)`** switches Mockito on for this class. Without it, `@Mock` and `@InjectMocks` are inert labels and every field stays `null`. A `NullPointerException` in a Mockito test is very often this annotation missing.

**`@Mock`** on a field tells Mockito to create a fresh mock of that type **before every test**. Freshness matters: stubbing and recorded calls from one test never leak into the next, so tests cannot influence each other.

**`@InjectMocks`** tells Mockito to construct the real object under test and pass the mocks into its constructor. `AccountService`'s constructor needs an `AccountDatabase`; Mockito supplies the `@Mock` field above.

There is no `@BeforeEach` method. Mockito rebuilds everything before each test automatically.

> Notice that `@InjectMocks` only works because `AccountService` accepts its database through the constructor. Testability is a property of the production code's design, not of the test framework.

---

### Task 2.1 -- deposit fetches the account from the database

Replace `// TODO 2.1` with:

```java
    @Test
    @DisplayName("deposit fetches the account from the database")
    void depositFetchesTheAccountFromTheDatabase() {
        Account account = new Account("ACC-001", new BigDecimal("100.00"));
        when(database.get("ACC-001")).thenReturn(account);

        accountService.deposit("ACC-001", new BigDecimal("25.00"));

        verify(database).get("ACC-001");
    }
```

Run it. Green.

Mockito tests have **four** steps rather than three:

```
ARRANGE  ->  build the data
STUB     ->  tell the mock how to answer
ACT      ->  call the method under test
VERIFY   ->  check what the mock was asked to do
```

**The stub line.** `when(database.get("ACC-001")).thenReturn(account)` reads as an English sentence: *when someone calls `get` with "ACC-001", then return this account.*

This line is not optional. Without it the mock returns `null`, `fetch` sees the null, and the test dies with `AccountNotFoundException`. Try deleting the line and re-running to see it -- then put it back.

**The verify line.** `verify(database).get("ACC-001")` asserts that `get` was called **exactly once** with exactly that argument. Not "at least once" -- exactly once. It is an assertion like `assertThat`, but about an interaction rather than a value.

**Verify checks something assertions cannot reach.** `deposit` returns nothing. There is no value to assert on. The only observable consequence is what `AccountService` did to its collaborator, and `verify` is how we see it.

---

### Task 2.2 -- deposit saves the account with the updated balance

Task 2.1 proved the account was fetched. It said nothing about what happened next. Now we check what was *written back*.

Replace `// TODO 2.2` with:

```java
    @Test
    @DisplayName("deposit saves the account with the updated balance")
    void depositSavesTheAccountWithTheUpdatedBalance() {
        Account account = new Account("ACC-001", new BigDecimal("100.00"));
        when(database.get("ACC-001")).thenReturn(account);

        accountService.deposit("ACC-001", new BigDecimal("25.00"));

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(database).save(captor.capture());

        Account savedAccount = captor.getValue();
        assertThat(savedAccount.getAccountNumber()).isEqualTo("ACC-001");
        assertThat(savedAccount.getBalance()).isEqualByComparingTo("125.00");
    }
```

Run it. Green.

**`ArgumentCaptor` explained.** `verify(database).save(...)` can confirm that `save` happened, but not *what was inside* the object passed to it. A captor is a net: you place it in the verify call, and it keeps whatever was passed.

Three lines, three jobs:

```java
ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);  // build the net
verify(database).save(captor.capture());                                  // verify AND capture
Account savedAccount = captor.getValue();                                 // retrieve the catch
```

Then assert on the captured object like any ordinary value.

**What this test actually proves.** It closes the loop on the whole operation: the right account was fetched, the deposit was applied to it, and the updated version -- balance $125.00, not the original $100.00 -- was handed to the database. A bug that saved the account before applying the deposit would pass Task 2.1 and fail here.

Notice we assert on the account number too. If the service saved the wrong account with a correct-looking balance, that assertion catches it.

---

### Task 2.3 -- withdraw throws when the account does not exist

Replace `// TODO 2.3` with:

```java
    @Test
    @DisplayName("withdraw throws AccountNotFoundException when the account does not exist")
    void withdrawThrowsWhenTheAccountDoesNotExist() {
        when(database.get("ACC-999")).thenReturn(null);

        assertThatThrownBy(() -> accountService.withdraw("ACC-999", new BigDecimal("10.00")))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining("ACC-999");

        verify(database, never()).save(any(Account.class));
    }
```

Run it. Green.

**Simulating a missing row is trivial with a mock.** With a real database you would have to guarantee the row is absent -- delete it, or hope no other test created it. Here, one line declares it. This is the strongest practical argument for mocks: **unusual situations become as easy to arrange as ordinary ones.**

**The stub is technically redundant, and that is deliberate.** Mocks return `null` for unstubbed object methods, so `get("ACC-999")` would return `null` anyway. Writing it out makes the intent explicit to the next reader: *this account is deliberately absent.* Silence would leave them guessing.

**`verify(database, never()).save(any(Account.class))`** is the important line. It reads: *check that `save` was never called with any `Account` whatsoever.*

- `never()` is a **verification mode** in the same family as the implicit "exactly once" from Task 2.1.
- `any(Account.class)` is an **argument matcher** meaning "any value of this type." We are asserting `save` never happened at all, so we cannot name a specific argument.

**Why bother?** The exception assertion confirms the correct failure. It says nothing about what happened *before* the failure. Suppose a later refactor reordered the method to save first and validate second -- the exception would still be thrown, the first assertion would still pass, and a phantom write would reach the database on every failed lookup. Only `never()` catches it.

This is the same principle as the balance check in Task 1.3, applied to interactions instead of state.

---

### Task 2.4 -- withdraw does not save when funds are insufficient

Replace `// TODO 2.4` with:

```java
    @Test
    @DisplayName("withdraw does not save the account when funds are insufficient")
    void withdrawDoesNotSaveWhenFundsAreInsufficient() {
        Account account = new Account("ACC-001", new BigDecimal("50.00"));
        when(database.get("ACC-001")).thenReturn(account);

        assertThatThrownBy(() -> accountService.withdraw("ACC-001", new BigDecimal("100.00")))
                .isInstanceOf(InsufficientFundsException.class);

        verify(database, never()).save(any(Account.class));
        assertThat(account.getBalance()).isEqualByComparingTo("50.00");
    }
```

Run it. Green.

This test sits at a different **level** from its Part 1 counterpart, and the distinction is worth pausing on.

Task 1.3 tested that `Account.withdraw` **throws** when the balance is too low. That is a rule about one object.

This test asks a different question: given that `Account` throws, **does `AccountService` handle it correctly?** Specifically, does the failure prevent a write? A service that caught the exception and saved anyway would leave the database in a state the business rules forbid.

The last two lines check both halves:

- `verify(database, never()).save(...)` -- nothing was persisted
- `assertThat(account.getBalance())` -- the in-memory object was not partially modified either

Both matter. A partially-applied withdrawal that is never saved is still a bug if that object is used again later in the same request.

---

### Task 2.5 -- transfer moves money and saves both accounts

Replace `// TODO 2.5` with:

```java
    @Test
    @DisplayName("transfer moves money between accounts and saves both")
    void transferMovesMoneyAndSavesBothAccounts() {
        Account from = new Account("ACC-001", new BigDecimal("100.00"));
        Account to = new Account("ACC-002", new BigDecimal("20.00"));
        when(database.get("ACC-001")).thenReturn(from);
        when(database.get("ACC-002")).thenReturn(to);

        accountService.transfer("ACC-001", "ACC-002", new BigDecimal("30.00"));

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(database, times(2)).save(captor.capture());

        List<Account> savedAccounts = captor.getAllValues();
        assertThat(savedAccounts).hasSize(2);

        assertThat(savedAccounts.get(0).getAccountNumber()).isEqualTo("ACC-001");
        assertThat(savedAccounts.get(0).getBalance()).isEqualByComparingTo("70.00");

        assertThat(savedAccounts.get(1).getAccountNumber()).isEqualTo("ACC-002");
        assertThat(savedAccounts.get(1).getBalance()).isEqualByComparingTo("50.00");
    }
```

Run it. Green.

**Two stubs, because two different lookups happen.** Mockito matches stubs by argument, so `get("ACC-001")` and `get("ACC-002")` return different accounts. One mock can be programmed with as many rules as you need.

**`times(2)`** replaces the implicit "exactly once" with "exactly twice."

**`getAllValues()` instead of `getValue()`.** `getValue()` returns only the most recent capture. With two saves, that would give us only the second. `getAllValues()` returns a `List` of every captured argument, in the order the calls happened.

**A caution about `get(0)` and `get(1)`.** This test depends on the order in which `transfer` saves -- source first, destination second. That order is real, and asserting on it means the test detects a change to it. But it also means a harmless reordering would break the test.

If you wanted order-independence, you would search the list by account number rather than index. The trade-off is a genuine judgment call: strict tests catch more changes but require more updating. Here, saving the source before the destination is arguably part of the contract, so asserting it is defensible.

**The arithmetic proves real work happened.** $100 - $30 = $70, and $20 + $30 = $50. Both real `Account` objects were mutated by real `Account` logic. Only the database is fake.

> This is worth dwelling on. We are **not** mocking everything in sight. `Account` is real, because its behaviour is exactly what we want to exercise. `AccountDatabase` is mocked, because it is an inconvenience we want out of the way. Deciding which collaborators to fake and which to keep real is the central skill in mock-based testing.

---

### Task 2.6 -- getBalance returns the balance without saving

Replace `// TODO 2.6` with:

```java
    @Test
    @DisplayName("getBalance returns the balance of the requested account")
    void getBalanceReturnsTheBalanceOfTheRequestedAccount() {
        when(database.get("ACC-001"))
                .thenReturn(new Account("ACC-001", new BigDecimal("42.50")));

        BigDecimal balance = accountService.getBalance("ACC-001");

        assertThat(balance).isEqualByComparingTo("42.50");
        verify(database, never()).save(any(Account.class));
    }
```

Run it. Green.

The first test in Part 2 where the method under test **returns a value**, so we can assert on it directly.

We still add a `verify`. Reading a balance must not write anything -- a read operation that quietly saves would cause needless database load and, worse, could overwrite a concurrent update from elsewhere. The `never()` check makes "this operation is read-only" an enforced property rather than an assumption.

---

### Verify Part 2

Right-click `AccountServiceTest` and choose **Run 'AccountServiceTest'**. All six tests should be green.

Now run everything: right-click the `src/test/java` folder and choose **Run 'All Tests'**.

**Expected result: 12 test methods, 15 invocations, all passing.**

---

## Running the Tests from the Command Line

IntelliJ is convenient, but CI servers have no IDE. Every project must be runnable from a terminal.

Open a terminal in the project folder. In IntelliJ, **View -> Tool Windows -> Terminal** opens one already in the right directory.

```
mvn test
```

Maven compiles `src/main`, compiles `src/test`, and runs every test class through the Surefire plugin. Output ends with:

```
[INFO] Tests run: 15, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

Fifteen, not twelve -- Maven counts invocations, so the parameterized test contributes four.

Other useful commands:

| Command | Effect |
|---|---|
| `mvn test` | Compile and run all tests |
| `mvn test -Dtest=AccountTest` | Run one test class |
| `mvn clean test` | Delete previous output, then test |
| `mvn -q test` | Quieter output |

**`BUILD SUCCESS` is the line a CI pipeline reads.** If any test fails, Maven exits with a non-zero status code, and the pipeline stops. That is the entire mechanism by which unit tests gate a build -- there is nothing more sophisticated happening.

Try it: break one assertion deliberately, run `mvn test`, and observe `BUILD FAILURE` plus a summary of which test failed. Then fix it.

---

## Troubleshooting

**Every field is null / `NullPointerException` in a Part 2 test**
`@ExtendWith(MockitoExtension.class)` is missing from the class. Without it, `@Mock` and `@InjectMocks` do nothing.

**`AccountNotFoundException` in a test you did not expect it in**
A `when(database.get(...)).thenReturn(...)` stub is missing, or the account number in the stub does not exactly match the one passed to the service. `"ACC-001"` and `"ACC-1"` are different strings.

**`UnnecessaryStubbingException`**
You stubbed something the code never called. Usually a typo in the account number, or a stub left over from editing. Remove the unused stub or correct the argument.

**Assertion fails with `expected: 125.00 but was: 125.00`**
You used `isEqualTo` instead of `isEqualByComparingTo` on a `BigDecimal`. The values differ only in scale.

**Red squiggles under `@Test` or `assertThat`**
Maven has not downloaded the dependencies. Right-click `pom.xml` -> **Maven -> Reload Project**.

**`WARNING: A Java agent has been loaded dynamically`**
Harmless on Java 21. Mockito attaches an agent to itself at startup. To silence it, add this to the `maven-surefire-plugin` configuration in `pom.xml`:

```xml
<configuration>
    <argLine>
        -javaagent:${settings.localRepository}/org/mockito/mockito-core/${mockito.version}/mockito-core-${mockito.version}.jar
    </argLine>
</configuration>
```

**Tests pass in IntelliJ but `mvn test` finds none**
Check that test class names end in `Test`. Maven Surefire only picks up `*Test`, `Test*`, and `*Tests`.

---

## Reflection Questions

Create a file `lab1-notes.md` in the project root and answer these.

1. Part 1 needed no mocks; Part 2 did. In your own words, what property of a class determines whether testing it requires a mock?

2. In Task 2.1 the test asserts nothing about a return value -- it only calls `verify`. What is `verify` checking that an ordinary assertion could not reach?

3. Tasks 1.3, 2.3 and 2.4 each assert that something did **not** happen, alongside asserting that the right exception was thrown. Describe a specific bug that the "did not happen" assertion would catch and the exception assertion would miss.

4. Task 2.5 mocks the `AccountDatabase` but uses a real `Account`. Why not mock `Account` as well? What would the test still prove if you did, and what would it stop proving?

5. `AccountService` receives its `AccountDatabase` through its constructor. Suppose instead it created one internally with `this.database = new RealAccountDatabase();`. What would that do to the tests in Part 2, and what does that tell you about the relationship between a class's design and its testability?

---

## What You Have Built

Twelve tests across two classes, covering both fundamental styles of unit test:

**Pure unit tests (Part 1).** The unit under test has no collaborators. Arrange, act, assert. Only JUnit and AssertJ are involved. Fast, simple, and where the majority of your tests should live.

**Mock-collaborator tests (Part 2).** The unit under test depends on something you do not want in a test. Mockito supplies a stand-in you can program and interrogate. The four-step shape -- arrange, stub, act, verify -- appears in every such test.

You also practised three habits that separate useful test suites from decorative ones:

- **One behaviour per test**, so a failure names its own cause
- **Testing the unhappy paths**, where the bugs actually are
- **Asserting on what did not happen**, not only on what did

The frameworks change between languages -- pytest in Python, `go test` in Go -- but every idea here transfers unchanged. Arrange-Act-Assert, test doubles, and interaction verification are universal.


---

## Appendix A -- Complete Project Source

Use this if you need to build the project from scratch. Create the directory structure exactly as shown in *What's in the Starter Project*.

### `pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                             https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>banking-unit-tests</artifactId>
    <version>1.0-SNAPSHOT</version>
    <packaging>jar</packaging>

    <properties>
        <maven.compiler.release>21</maven.compiler.release>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <junit.version>5.11.4</junit.version>
        <assertj.version>3.26.3</assertj.version>
        <mockito.version>5.14.2</mockito.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>${junit.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.assertj</groupId>
            <artifactId>assertj-core</artifactId>
            <version>${assertj.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-core</artifactId>
            <version>${mockito.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-junit-jupiter</artifactId>
            <version>${mockito.version}</version>
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
        </plugins>
    </build>
</project>
```

`<scope>test</scope>` on every dependency means these libraries are available to test code only and are never packaged into the application. Test frameworks should never ship to production.

### `AccountStatus.java`

```java
package com.example.banking;

/**
 * The state an account is in. Only ACTIVE accounts may be used
 * for deposits and withdrawals.
 */
public enum AccountStatus {
    ACTIVE,
    FROZEN
}
```

### `AccountNotFoundException.java`

```java
package com.example.banking;

/** Thrown when no account exists for a given account number. */
public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(String message) {
        super(message);
    }
}
```

### `InsufficientFundsException.java`

```java
package com.example.banking;

/** Thrown when a withdrawal would take an account below zero. */
public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(String message) {
        super(message);
    }
}
```

### `AccountFrozenException.java`

```java
package com.example.banking;

/** Thrown when an operation is attempted on an account that is not ACTIVE. */
public class AccountFrozenException extends RuntimeException {
    public AccountFrozenException(String message) {
        super(message);
    }
}
```

### `Account.java`

```java
package com.example.banking;

import java.math.BigDecimal;

/**
 * A single bank account. This class holds the money rules:
 * what a valid deposit is, what a valid withdrawal is, and
 * what happens when those rules are broken.
 */
public class Account {

    private final String accountNumber;
    private BigDecimal balance;
    private AccountStatus status;

    public Account(String accountNumber, BigDecimal balance) {
        this(accountNumber, balance, AccountStatus.ACTIVE);
    }

    public Account(String accountNumber, BigDecimal balance, AccountStatus status) {
        this.accountNumber = accountNumber;
        this.balance = balance;
        this.status = status;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public AccountStatus getStatus() {
        return status;
    }

    /** Adds money to this account. */
    public void deposit(BigDecimal amount) {
        requirePositiveAmount(amount);
        requireActiveAccount("deposit to");
        balance = balance.add(amount);
    }

    /** Removes money from this account. */
    public void withdraw(BigDecimal amount) {
        requirePositiveAmount(amount);
        requireActiveAccount("withdraw from");
        if (balance.compareTo(amount) < 0) {
            throw new InsufficientFundsException(
                    "Account " + accountNumber + " has a balance of " + balance
                            + " which is less than the requested amount of " + amount);
        }
        balance = balance.subtract(amount);
    }

    private void requirePositiveAmount(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount must not be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Amount must be greater than zero, but was " + amount);
        }
    }

    private void requireActiveAccount(String operation) {
        if (status != AccountStatus.ACTIVE) {
            throw new AccountFrozenException(
                    "Cannot " + operation + " account " + accountNumber
                            + " because its status is " + status);
        }
    }

    @Override
    public String toString() {
        return "Account[" + accountNumber + ", balance=" + balance + ", status=" + status + "]";
    }
}
```

### `AccountDatabase.java`

```java
package com.example.banking;

/**
 * Where accounts are stored. In a real application this would talk to a
 * real database. In this lab there is no implementation at all -- Mockito
 * creates a stand-in for it during the tests.
 */
public interface AccountDatabase {

    /**
     * Looks up one account.
     *
     * @return the account, or null if no account has that number
     */
    Account get(String accountNumber);

    /** Writes an account back to storage. */
    void save(Account account);
}
```

### `AccountService.java`

```java
package com.example.banking;

import java.math.BigDecimal;

/**
 * Coordinates banking operations. This class does not do the money
 * arithmetic itself -- it fetches accounts from the database, asks the
 * Account to do the work, and saves the result back.
 */
public class AccountService {

    private final AccountDatabase database;

    public AccountService(AccountDatabase database) {
        this.database = database;
    }

    public void deposit(String accountNumber, BigDecimal amount) {
        Account account = fetch(accountNumber);
        account.deposit(amount);
        database.save(account);
    }

    public void withdraw(String accountNumber, BigDecimal amount) {
        Account account = fetch(accountNumber);
        account.withdraw(amount);
        database.save(account);
    }

    public BigDecimal getBalance(String accountNumber) {
        return fetch(accountNumber).getBalance();
    }

    public void transfer(String fromAccountNumber, String toAccountNumber, BigDecimal amount) {
        Account from = fetch(fromAccountNumber);
        Account to = fetch(toAccountNumber);
        from.withdraw(amount);
        to.deposit(amount);
        database.save(from);
        database.save(to);
    }

    private Account fetch(String accountNumber) {
        Account account = database.get(accountNumber);
        if (account == null) {
            throw new AccountNotFoundException("No account found with number " + accountNumber);
        }
        return account;
    }
}
```

### Test skeletons

`src/test/java/com/example/banking/AccountTest.java`

```java
package com.example.banking;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Account")
class AccountTest {

    // TODO 1.1 -- deposit increases the balance

    // TODO 1.2 -- withdraw decreases the balance

    // TODO 1.3 -- withdraw rejects an amount larger than the balance

    // TODO 1.4 -- deposit rejects a negative amount

    // TODO 1.5 -- withdraw rejects a frozen account

    // TODO 1.6 -- parameterized deposit test
}
```

`src/test/java/com/example/banking/AccountServiceTest.java`

```java
package com.example.banking;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Account service")
class AccountServiceTest {

    @Mock
    private AccountDatabase database;

    @InjectMocks
    private AccountService accountService;

    // TODO 2.1 -- deposit fetches the account from the database

    // TODO 2.2 -- deposit saves the account with the updated balance

    // TODO 2.3 -- withdraw throws when the account does not exist

    // TODO 2.4 -- withdraw does not save when funds are insufficient

    // TODO 2.5 -- transfer moves money and saves both accounts

    // TODO 2.6 -- getBalance returns the balance without saving
}
```
