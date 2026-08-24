# Lab 1.1 -- Solutions and Answers

> **Course:** Software Test Automation Survey
> **Purpose:** Complete code for every test in Lab 1.1, plus written answers to
> every reflection question. Use this to check your work after attempting each
> task. Reading the solution before attempting the task defeats the purpose.

---

## How to Use This File

Each section below corresponds to a part of the lab. The complete final contents
of each test file are shown, so you can compare against your own version in full
rather than task by task.

Following each file is a set of notes explaining the decisions behind the code --
these go further than the lab text and are useful for the class debrief.

### Summary of what should exist when you are done

| File | Test methods | Invocations |
|---|---|---|
| `src/test/java/com/example/banking/AccountTest.java` | 6 | 9 |
| `src/test/java/com/example/banking/AccountServiceTest.java` | 6 | 6 |
| **Total** | **12** | **15** |

`mvn test` should report:

```
[INFO] Tests run: 15, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---

## Part 1 -- Complete `AccountTest.java`

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

    @Test
    @DisplayName("deposit increases the balance")
    void depositIncreasesTheBalance() {
        Account account = new Account("ACC-001", new BigDecimal("100.00"));

        account.deposit(new BigDecimal("25.00"));

        assertThat(account.getBalance()).isEqualByComparingTo("125.00");
    }

    @Test
    @DisplayName("withdraw decreases the balance")
    void withdrawDecreasesTheBalance() {
        Account account = new Account("ACC-001", new BigDecimal("100.00"));

        account.withdraw(new BigDecimal("30.00"));

        assertThat(account.getBalance()).isEqualByComparingTo("70.00");
    }

    @Test
    @DisplayName("withdraw rejects an amount larger than the balance")
    void withdrawRejectsAmountLargerThanBalance() {
        Account account = new Account("ACC-001", new BigDecimal("50.00"));

        assertThatThrownBy(() -> account.withdraw(new BigDecimal("100.00")))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("ACC-001");

        assertThat(account.getBalance()).isEqualByComparingTo("50.00");
    }

    @Test
    @DisplayName("deposit rejects a negative amount")
    void depositRejectsNegativeAmount() {
        Account account = new Account("ACC-001", new BigDecimal("100.00"));

        assertThatThrownBy(() -> account.deposit(new BigDecimal("-10.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("greater than zero");

        assertThat(account.getBalance()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("withdraw rejects a frozen account")
    void withdrawRejectsFrozenAccount() {
        Account account = new Account("ACC-001", new BigDecimal("100.00"), AccountStatus.FROZEN);

        assertThatThrownBy(() -> account.withdraw(new BigDecimal("10.00")))
                .isInstanceOf(AccountFrozenException.class)
                .hasMessageContaining("ACC-001")
                .hasMessageContaining("FROZEN");
    }

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
}
```

---

### Notes on Part 1

**Why `isEqualByComparingTo` rather than `isEqualTo`.**
`BigDecimal` stores a value *and* a scale (the number of decimal places).
`new BigDecimal("125.00")` and `new BigDecimal("125")` represent the same
amount but are not equal under `equals()`, because their scales differ.
`isEqualTo` uses `equals()`; `isEqualByComparingTo` uses `compareTo()`, which
compares numeric value only.

This is not a theoretical concern. `BigDecimal.add` and `BigDecimal.subtract`
produce results whose scale follows from the operands, so arithmetic can
easily yield a scale that differs from the literal you compare against.
Using `isEqualTo` for money produces failures that look impossible --
`expected 125.00 but was 125.00` -- and cost students a great deal of time.
Make `isEqualByComparingTo` the default habit for any decimal comparison.

**Why there is no `@BeforeEach` in this file.**
The template lab used a `@BeforeEach` to construct a shared service instance.
Here each test constructs its own `Account` with the specific starting balance
and status it needs, so there is nothing common to hoist out.

That is a reasonable default: `@BeforeEach` is worth using when every test
genuinely needs the same setup, and a liability when tests need *different*
setups and end up overwriting the shared object anyway. Setup that only some
tests use is a mild test smell -- a reader has to check whether the shared
state is relevant to the test in front of them.

**Why the lambda in `assertThatThrownBy`.**
`assertThatThrownBy` needs to run the risky call inside its own try/catch.
If you passed the *result* of the call rather than instructions for making it,
the exception would be thrown at the point of the call and would propagate out
of the test method before AssertJ ever ran. The lambda `() -> ...` defers
execution until AssertJ is ready.

**Why the extra assertion in tasks 1.3 and 1.4.**
Both tests assert the balance is unchanged after the exception. This is not
redundant. Consider a plausible bad implementation of `withdraw`:

```java
public void withdraw(BigDecimal amount) {
    balance = balance.subtract(amount);          // subtract first
    if (balance.compareTo(BigDecimal.ZERO) < 0) {
        throw new InsufficientFundsException(...);   // then validate
    }
}
```

This throws the correct exception with the correct message. The
`assertThatThrownBy` block passes completely. But the balance is now negative,
and if that object is reused later in the same request the corruption
propagates. Only the balance assertion catches it.

**Why the parameterized test uses three columns rather than two.**
Varying the starting balance as well as the deposit amount exercises the
arithmetic more thoroughly. The row `999.99 + 0.01 -> 1000.00` is deliberately
chosen: it crosses a decimal boundary and produces a carry, which is exactly
the kind of case that would break under floating-point arithmetic. It is worth
pointing out in the debrief that this row would be a coin-flip with `double`.

**On JUnit's automatic string conversion.**
`@CsvSource` supplies strings. JUnit's built-in argument converter turns them
into `BigDecimal` because the method parameters are declared as `BigDecimal`.
It handles the common types (numbers, enums, `LocalDate`, and so on) without
configuration. If a student asks about custom types, the answer is
`@ConvertWith` with an `ArgumentConverter` -- beyond this lab's scope.

---

## Part 2 -- Complete `AccountServiceTest.java`

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

    @Test
    @DisplayName("deposit fetches the account from the database")
    void depositFetchesTheAccountFromTheDatabase() {
        Account account = new Account("ACC-001", new BigDecimal("100.00"));
        when(database.get("ACC-001")).thenReturn(account);

        accountService.deposit("ACC-001", new BigDecimal("25.00"));

        verify(database).get("ACC-001");
    }

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

    @Test
    @DisplayName("withdraw throws AccountNotFoundException when the account does not exist")
    void withdrawThrowsWhenTheAccountDoesNotExist() {
        when(database.get("ACC-999")).thenReturn(null);

        assertThatThrownBy(() -> accountService.withdraw("ACC-999", new BigDecimal("10.00")))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining("ACC-999");

        verify(database, never()).save(any(Account.class));
    }

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

    @Test
    @DisplayName("getBalance returns the balance of the requested account")
    void getBalanceReturnsTheBalanceOfTheRequestedAccount() {
        when(database.get("ACC-001"))
                .thenReturn(new Account("ACC-001", new BigDecimal("42.50")));

        BigDecimal balance = accountService.getBalance("ACC-001");

        assertThat(balance).isEqualByComparingTo("42.50");
        verify(database, never()).save(any(Account.class));
    }
}
```

---

### Notes on Part 2

**What `@ExtendWith(MockitoExtension.class)` actually does.**
It registers a JUnit 5 extension that runs before and after each test method.
Before: it scans the test class for `@Mock` fields, creates a mock for each,
and assigns it; then it finds the `@InjectMocks` field and constructs that
object, passing the mocks into the matching constructor parameters. After: it
validates the mocks -- which is where `UnnecessaryStubbingException` comes from.

Without the annotation, none of this runs. Every annotated field remains
`null`, and the first line of the first test throws `NullPointerException`.
This is the most common single error in Mockito labs, and it is worth writing
on the whiteboard.

**Why mocks are recreated for every test.**
Mockito builds fresh mocks before each test method rather than reusing one
across the class. This guarantees that stubbing and recorded interactions
cannot leak between tests. Leakage would make tests order-dependent -- passing
alone, failing in a suite, or vice versa -- which is one of the hardest
categories of test defect to diagnose.

Connect this to the broader principle from the best-practices module: tests
must be independent and order-independent. Mockito enforces it for you here;
in other contexts you have to enforce it yourself.

**Why `@InjectMocks` works at all.**
Because `AccountService` takes its dependency through the constructor:

```java
public AccountService(AccountDatabase database) {
    this.database = database;
}
```

Mockito matches the constructor parameter type against the available `@Mock`
fields and wires them in. If the class had instead written
`this.database = new SomeRealDatabase();`, there would be no seam to inject
through, and the class would be untestable without changing it.

This is worth stating plainly to the class: **`@InjectMocks` is not what makes
the class testable. Constructor injection is. Mockito just takes advantage of
it.** Reflection question 5 covers this.

**Why Task 2.3 stubs a return of `null` when `null` is already the default.**
It is redundant to the compiler and meaningful to the reader. A mock returns
`null` for any unstubbed method that returns an object, so removing the line
leaves the test passing. But then a reader has to know Mockito's defaults to
understand why the test works, and has to guess whether the absence of a stub
was deliberate or an oversight.

Writing the stub states the scenario: *this account does not exist.* Tests are
read far more often than they are written, and explicit beats clever.

Note that this stub does **not** trigger `UnnecessaryStubbingException`,
because the stubbed method genuinely is called during the test. Mockito's
strictness flags stubs that are never *used*, not stubs whose value happens to
match the default.

**Why `verify(...)` and not just assertions.**
`deposit`, `withdraw` and `transfer` all return `void`. There is no value to
assert on. The only observable evidence that the method did anything is what it
did to its collaborator.

This is the conceptual core of interaction-based testing: for a method whose
whole purpose is to cause an effect elsewhere, verifying the interaction *is*
the assertion. There is no alternative.

**The `never()` assertions and what they defend against.**
Tasks 2.3, 2.4 and 2.6 all assert `save` was never called. Each defends a
different property:

- **2.3** -- a lookup failure must not write anything
- **2.4** -- a business rule violation must not write anything
- **2.6** -- a read operation must not write anything

All three are invariants that no assertion on the return value could express.
They also all survive refactoring: they describe *what the service must not do*
rather than how it is currently written.

**On `any(Account.class)`.**
An argument matcher meaning "any value of this type." It is required here
because we are asserting that `save` was never called at all, so there is no
specific argument to name.

Mockito has a rule worth mentioning if a student hits it: within a single
`verify` or `when` call, **either all arguments are matchers or none are**.
Mixing a matcher with a literal -- `verify(db).transfer(any(), "ACC-002")` --
throws `InvalidUseOfMatchersException`. The fix is `eq("ACC-002")` to wrap the
literal in a matcher.

**Why Task 2.5 asserts on captured values rather than on `from` and `to`.**
The test could assert `from.getBalance()` directly, since those objects are
still in scope and were mutated in place. Using the captor instead proves
something slightly stronger: that the objects **handed to `save`** hold the
updated balances.

In this implementation those are the same objects, so the distinction is
academic. It stops being academic the moment the service copies, wraps or maps
an account before persisting it -- at which point asserting on the local
variable would keep passing while the wrong data went to the database. The
captor asserts on what actually crossed the boundary.

**On the order dependence in Task 2.5.**
`savedAccounts.get(0)` and `.get(1)` assume the source is saved before the
destination. That is true of the current implementation, and asserting it means
the test notices if it changes.

Whether that is desirable is a genuine judgment call, and a good discussion
point. Arguments for asserting the order: in a real system, save order can
matter for lock acquisition and deadlock avoidance, so it may be part of the
contract. Arguments against: it makes the test brittle to a change that has no
behavioural consequence.

The order-independent version, for reference:

```java
List<Account> savedAccounts = captor.getAllValues();
assertThat(savedAccounts)
        .extracting(Account::getAccountNumber)
        .containsExactlyInAnyOrder("ACC-001", "ACC-002");
```

This was kept out of the lab because method references and `extracting` add
syntax load for non-Java students. Mention it if a strong student asks.

**Why `Account` is real and only `AccountDatabase` is mocked.**
This is the most important idea in Part 2 and the one most worth drawing out in
the debrief.

`AccountDatabase` is mocked because it is an *obstacle*: there is no
implementation, a real one would be slow and stateful, and we do not want to
test it here.

`Account` is real because its behaviour is *the thing we care about*. The
balances in Task 2.5 change from $100 to $70 and from $20 to $50 because real
`Account` logic ran. If `Account` were mocked, its `deposit` and `withdraw`
would do nothing, the balances would never change, and the test could only
verify that some calls happened in some order -- it could no longer verify that
the correct amount of money moved.

The rule of thumb: **mock what gets in the way; keep real what you are trying
to verify.** Students who mock reflexively end up with tests that pass no
matter what the code does.

---

## Reflection Question Answers

### Question 1
*Part 1 needed no mocks; Part 2 did. In your own words, what property of a class determines whether testing it requires a mock?*

A class needs a mock when it **depends on a collaborator that is inconvenient,
unavailable, slow, or non-deterministic to use for real**.

`Account` depends on nothing outside itself. It holds a balance and applies
rules to it. Constructing one takes a single line and behaves identically every
time. There is nothing to substitute.

`AccountService` depends on `AccountDatabase`. That interface has no
implementation in the project, so a test literally cannot construct the service
without providing something. Even if a real implementation existed, using it
would mean starting a database, managing schema and state, and accepting that
the test now also tests the database.

Specific reasons a collaborator warrants a mock:

- It performs I/O -- database, network, file system
- It requires infrastructure that is not present in a test environment
- It behaves non-deterministically -- current time, random values, latency
- It is expensive to construct or configure
- It is hard to drive into a particular state, especially failure states
  (Task 2.3 makes an account vanish in one line)

Conversely, a collaborator does **not** need mocking when it is cheap to
construct, deterministic, and does nothing you would rather avoid. `Account` in
Part 2 is exactly that, which is why we used the real one.

A practical three-question test:

1. Can I construct this collaborator in one or two lines?
2. Will it behave identically every run?
3. Does it do anything I do not want my test doing?

If the answers are yes, yes, no -- use the real thing.

---

### Question 2
*In Task 2.1 the test asserts nothing about a return value -- it only calls `verify`. What is `verify` checking that an ordinary assertion could not reach?*

`AccountService.deposit` is declared `void`. It returns nothing, so there is no
value for `assertThat` to inspect.

Nor is there observable state on the service itself. `AccountService` holds only
its database reference; it has no balance, no counter, no history. After
`deposit` returns, the service looks exactly as it did before.

The only evidence the method did anything is **what it asked its collaborator to
do**. `verify(database).get("ACC-001")` inspects Mockito's recording of calls
made to the mock, and asserts that `get` was called exactly once with exactly
that argument.

This is the distinction between **state-based** and **interaction-based**
testing. State-based asks "what does the world look like afterwards?" and needs
something observable to look at. Interaction-based asks "what did this object
ask its collaborators to do?" and works even when nothing observable changed.

For a method whose entire purpose is to produce an effect on something else --
send an email, publish a message, write a row -- interaction verification is not
merely one option. It is the only way to test it at all.

---

### Question 3
*Tasks 1.3, 2.3 and 2.4 each assert that something did not happen. Describe a specific bug that the "did not happen" assertion would catch and the exception assertion would miss.*

**Task 2.3 example.** Suppose someone refactors `AccountService.withdraw` to
look like this:

```java
public void withdraw(String accountNumber, BigDecimal amount) {
    Account account = database.get(accountNumber);
    database.save(account);                       // saves before validating
    if (account == null) {
        throw new AccountNotFoundException("No account found with number " + accountNumber);
    }
    account.withdraw(amount);
    database.save(account);
}
```

Run Task 2.3 against this. `get("ACC-999")` returns `null`, the
`AccountNotFoundException` is thrown with the right message, and
`assertThatThrownBy` passes completely.

But `save(null)` was already called. In a real system that is at best a spurious
write and at worst a null-pointer failure or a corrupted row -- on every single
lookup miss, which is a common and expected event.

Only `verify(database, never()).save(any(Account.class))` fails. Without it,
this refactor ships.

**Task 2.4 example.** A service that swallows the exception and saves anyway:

```java
try {
    account.withdraw(amount);
} catch (InsufficientFundsException e) {
    database.save(account);   // "save what we have"
    throw e;
}
```

The exception still propagates, so the exception assertion passes. But a write
occurred on a failed operation.

**Task 1.3 example.** The subtract-before-validate implementation shown in the
Part 1 notes: the right exception is thrown, and the balance is left negative.

The general principle: **an exception assertion tells you the failure was
reported correctly. It tells you nothing about what happened before the failure
was reported.** Error paths are where side effects hide, because they are the
least-exercised code in the system. Assert on what must not have happened --
nothing persisted, nothing sent, nothing charged, no state mutated.

---

### Question 4
*Task 2.5 mocks the `AccountDatabase` but uses a real `Account`. Why not mock `Account` as well? What would the test still prove if you did, and what would it stop proving?*

If `Account` were mocked, its `deposit` and `withdraw` methods would do nothing
at all -- a mock's `void` methods are no-ops unless stubbed otherwise.

**What the test would still prove:**

- Both accounts were fetched from the database
- `save` was called exactly twice
- `withdraw` was called on one account and `deposit` on the other, with the
  expected amount, if you added `verify` calls for those

**What the test would stop proving:**

- That the source balance became $70.00
- That the destination balance became $50.00
- That the correct amount moved
- That the money arithmetic is right at all

The balances would remain at their original $100.00 and $20.00, because nothing
would ever change them. The assertions on $70.00 and $50.00 would fail.

You could rewrite the test to verify interactions only -- "`withdraw` was called
with 30.00 on the source, `deposit` was called with 30.00 on the destination."
That test would pass. But notice what it now checks: only that `AccountService`
calls the methods you expected, in the pattern you expected. It has become a
restatement of the implementation. Rename the method or reorder the calls, and
the test breaks even though behaviour is unchanged; introduce an arithmetic bug
inside `Account`, and the test stays green.

This is the classic over-mocking failure. Mocking every collaborator produces
tests that verify the code is written the way it is written, rather than that it
does what it should.

**The rule:** mock the collaborators that get in the way. Keep real the ones
whose behaviour is the point of the test. Here, `AccountDatabase` is an
obstacle; `Account` is the subject.

---

### Question 5
*`AccountService` receives its `AccountDatabase` through its constructor. Suppose instead it created one internally. What would that do to the tests in Part 2, and what does that tell you about the relationship between a class's design and its testability?*

Suppose the class were written this way:

```java
public class AccountService {
    private final AccountDatabase database = new PostgresAccountDatabase();

    public AccountService() { }
    ...
}
```

**Every test in Part 2 becomes impossible as written.**

`@InjectMocks` has nowhere to inject. The constructor takes no parameters, so
Mockito can create the service but cannot supply the mock. The `@Mock
AccountDatabase` field would still be created, and would simply be ignored --
the service would use its own internal instance instead.

The concrete consequences:

- Tests would attempt real database connections and fail, or hang
- Task 2.3's missing account could not be simulated without deleting a real row
- Task 2.4's insufficient-funds case would need a real account with a specific
  real balance
- `verify(database, never()).save(...)` becomes meaningless -- the mock is not
  the object being used
- Tests would be slow, order-dependent, and dependent on external state

The class would be effectively untestable in isolation. The only route would be
to change the production code -- which is what anyone would end up doing.

**The broader lesson.** Testability is a property of the design of the code
under test, not of the test framework. Mockito is powerful, but it cannot create
a seam that does not exist. If a class constructs its own dependencies, it welds
itself to them, and no testing tool can separate them afterwards.

Constructor injection creates that seam. It costs one parameter and returns:

- Substitutable dependencies in tests
- Explicit, readable dependencies -- the constructor signature documents exactly
  what the class needs
- `final` fields, so the dependency cannot be swapped after construction
- The impossibility of constructing the object in an incomplete state

This connects directly to the *design for testability* material from the
best-practices module. Control points and observation points do not appear by
accident; they are designed in. Constructor injection is one of the cheapest and
highest-value control points available, and its absence is one of the most
common reasons legacy code resists testing.

Frameworks like Spring automate the wiring, but the underlying principle is
plain Java and needs no framework at all -- as this lab demonstrates.

---

## Common Student Errors and How to Diagnose Them

| Symptom | Cause | Fix |
|---|---|---|
| `NullPointerException` on the first line of any Part 2 test | `@ExtendWith(MockitoExtension.class)` missing | Add the annotation to the class |
| `AccountNotFoundException` in a test that should succeed | Missing stub, or account number mismatch between stub and call | Compare the strings character by character |
| `UnnecessaryStubbingException` | A stub was declared but never called | Remove it, or fix the argument so it matches |
| `expected: 125.00 but was: 125.00` | `isEqualTo` used on a `BigDecimal` | Use `isEqualByComparingTo` |
| `InvalidUseOfMatchersException` | Matchers mixed with literals in one call | Wrap literals with `eq(...)` |
| `Wanted 1 time but was 2 times` | Verification count wrong, or the service really does call twice | Read the failure trace -- Mockito prints every actual call |
| Tests pass in IntelliJ, `mvn test` finds none | Class name does not end in `Test` | Rename it |
| `WARNING: A Java agent has been loaded dynamically` | Mockito self-attaching on Java 21 | Harmless; see the lab's troubleshooting section to silence it |

---
