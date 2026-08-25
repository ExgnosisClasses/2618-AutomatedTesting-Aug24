# Lab 4.1 -- Answer Key and Instructor Notes

> **For instructors.** This document lists every planted violation in both lab
> projects, the expected SonarQube rule IDs, model triage answers, and answers
> to all five reflection questions. It replaces the two separate answer keys
> used when this material was split across two labs.

---

## How to Use This File

Lab 4.1 scans two projects, and the answer key is organised the same way:

| Part | Project | What the answer key gives you |
|---|---|---|
| 1 | -- | Setup verification |
| 2 | `banking-tests-lab-sonar-demo` | Complete table of planted findings by file |
| 3 | `banking-vulnerable-lab` | Model triage for each of the five findings |
| 4 | `banking-vulnerable-lab` | Expected before/after scan deltas |
| 5 | -- | Justification rubric |

Then reflection answers and delivery notes.

### Notes on rule IDs and versions

SonarQube rule IDs evolve. The IDs here are standard Java rule IDs as of
SonarQube 10.x and later with SonarJava. Your dashboard may display
`java:S2076` or `squid:S2076` depending on scanner version -- **the numeric
portion is stable**.

Some findings depend on the edition:

- **Community Build** (free, what we run on the lab VM) reports everything below
  except where noted "Developer+"
- **Developer Edition and above** add taint analysis: deep data-flow tracing for
  injection vulnerabilities
- **No edition** reports third-party dependency CVEs. Those need a separate SCA
  scan (OWASP Dependency-Check, Snyk, Dependabot)

If a planted violation does not appear in a student's report, the likely causes
in order are:

1. The project did not compile, so only source-level rules ran. `mvn clean
   verify` must succeed **before** `sonar:sonar`.
2. The rule belongs to a higher edition.
3. The scanner version predates the rule.

A correct Community Build scan of Project A reports **30+ findings** minimum.

---

## Part 1 -- Setup Verification

Before the session, confirm on the image:

- [ ] SonarQube service is **Running** and set to Automatic (Delayed Start)
- [ ] `http://localhost:9000` loads and the admin password is the one on the handout
- [ ] Instance mode (MQR vs Standard Experience) is **decided and set** -- the lab
      is written in Standard Experience vocabulary
- [ ] An analysis token exists, or students know how to generate one
- [ ] `~/.m2` is pre-populated: run `mvn clean verify` in both projects
- [ ] Both scans have been run once, then the projects deleted from SonarQube
      (warms the analyzer cache, proves the setup, leaves students a clean start)
- [ ] `C:\sonarqube` is excluded from Windows Defender

**The single most common day-of failure** is students running `mvn sonar:sonar`
without `verify` first. Coverage reports as 0%, bytecode rules do not fire, and
the finding count is a fraction of expected. Put the full command on a slide.

---

## Part 2 -- Project A: `banking-tests-lab-sonar-demo`

The broad report-reading exercise. Students fix nothing here.

### pom.xml

| Finding | Rule | Severity | Category | Detected by |
|---|---|---|---|---|
| `commons-collections 3.2.1` has known deserialization CVEs (CVE-2015-7501, CVE-2015-6420) | n/a | n/a | n/a | NOT SonarQube. Surfaces in dependency scanners (OWASP Dependency-Check, Snyk, Dependabot). Mention this distinction to students - SonarQube checks code, not dependency CVEs. |

---

### src/main/resources/application.properties

| Line | Finding | Rule | Severity | Category |
|---|---|---|---|---|
| `spring.datasource.password=admin123` | Hardcoded credential | S2068 | Blocker | Vulnerability |
| `banking.external.api.key=sk_live_...` | Hardcoded API key | S6437 | Blocker | Vulnerability |
| `banking.signing.secret=hardcoded-signing-secret-do-not-do-this` | Hardcoded secret | S6437 | Blocker | Vulnerability |
| `spring.h2.console.settings.web-allow-others=true` | Exposes H2 console to network | S6437 / S5604 | Critical | Security Hotspot |
| `debug=true` | Debug mode in production-style config | S4507 | Critical | Security Hotspot |
| `management.endpoints.web.exposure.include=*` | All actuator endpoints exposed | S6437 | Critical | Security Hotspot |
| `management.endpoint.env.show-values=always` | Exposes environment values | S6437 | Critical | Security Hotspot |

---

### src/main/java/com/example/banking/repository/AccountRepository.java

| Line area | Finding | Rule | Severity | Category |
|---|---|---|---|---|
| Class | Hardcoded JDBC username/password in source | S2068 | Blocker | Vulnerability |
| `findById` query | SQL injection via string concatenation in `executeQuery` | S2077 | Critical | Vulnerability (Developer+ for full taint analysis; basic pattern caught by Community) |
| `findById` body | Resource leak: `Connection`, `Statement`, `ResultSet` not closed (no try-with-resources) | S2095 | Major | Bug |
| `findById` catch block | `e.printStackTrace()` instead of logger | S1148 | Major | Code Smell |
| `findByCustomer` query | SQL injection via string concatenation | S2077 | Critical | Vulnerability |
| `findByCustomer` body | Resource leak: `Connection`, `Statement`, `ResultSet` not closed | S2095 | Major | Bug |
| `findByCustomer` catch | Swallowed exception (catch with no handling, no rethrow, no log) | S108 / S1166 | Critical | Code Smell |
| `updateBalance` body | Resource leak: `Connection` and `PreparedStatement` not closed | S2095 | Major | Bug |
| `updateBalance` catch | `e.printStackTrace()` | S1148 | Major | Code Smell |
| `exists` query | SQL injection via string concatenation | S2077 | Critical | Vulnerability |
| `exists` catch | Swallowed exception | S1166 | Critical | Code Smell |

**Discussion teaching point:** the `exists` method has a `finally` block that closes `Connection` but still leaks `Statement` and `ResultSet`. A partial-cleanup pattern is sometimes worse than no cleanup because it can mask the real problem. Try-with-resources is the right answer.

---

### src/main/java/com/example/banking/service/AuthService.java

| Line area | Finding | Rule | Severity | Category |
|---|---|---|---|---|
| `ADMIN_PASSWORD = "admin123"` constant | Hardcoded credential | S2068 | Blocker | Vulnerability |
| `SALT = "static-salt-1234"` constant | Hardcoded cryptographic salt (predictable per-application) | S2068 / S6437 | Critical | Security Hotspot |
| `authenticate`: `username == ADMIN_USERNAME` | String compared with `==` instead of `.equals()` | S4973 | Critical | Bug |
| `authenticate`: `user.getPasswordHash() == hashed` | String compared with `==` | S4973 | Critical | Bug |
| `hashPassword` body | Weak hashing algorithm MD5 | S4790 | Critical | Security Hotspot (often reported as Vulnerability with deeper analysis) |
| `tokenGenerator = new Random()` | Insecure pseudorandom for security-sensitive value | S2245 | Critical | Security Hotspot |
| `generateSessionToken` | Uses `Random` (already flagged) - produces predictable tokens | S2245 | Critical | Security Hotspot |
| `generatePasswordResetToken` | Uses `Random` for security-sensitive token | S2245 | Critical | Security Hotspot |
| `hashPassword` catch | `throw new RuntimeException(e)` wraps without context | S00112 | Major | Code Smell |
| `registerUser` catch | Catch of generic `Exception` | S2221 | Major | Code Smell |
| `registerUser` catch | `System.out.println` for error reporting | S106 | Major | Code Smell |

---

### src/main/java/com/example/banking/service/TransferService.java

| Line area | Finding | Rule | Severity | Category |
|---|---|---|---|---|
| `executeTransfer` method | Cognitive complexity exceeds threshold (deeply nested `if` chain) | S3776 | Critical | Code Smell |
| `executeTransfer` | Multiple magic numbers (1000000, 10000, 5000) | S109 | Minor | Code Smell |
| `executeTransfer`: `userRole == "ADMIN"` | String compared with `==` | S4973 | Critical | Bug |
| `calculateFee` `switch` | `case "CHECKING"` falls through to `case "SAVINGS"` (no `break`) | S128 | Critical | Bug |
| `calculateFee` body | Magic numbers (`0.01`, `0.005`, `0.015`, `0.02`, `5`, `2.50`) | S109 | Minor | Code Smell |
| `formatTransferReceipt` and `formatRejectionReceipt` | Duplicated code blocks | S4144 / S1192 | Major | Code Smell |
| `formatTransferReceipt` and `formatRejectionReceipt` | String literals `"----------------------------------------"`, `"TRANSFER RECEIPT"` duplicated | S1192 | Critical | Code Smell |
| `isWeekend` method | Always returns the same value (`return false`) followed by unreachable comment-only code | S3923 | Major | Bug |
| `isWeekend` method | Unused private method | S1144 | Major | Code Smell |

---

### src/main/java/com/example/banking/service/FileExportService.java

| Line area | Finding | Rule | Severity | Category |
|---|---|---|---|---|
| `exportToFile` body | Path traversal: user-controlled `filename` concatenated into file path without sanitization | S2083 | Blocker | Vulnerability (Developer+ for full taint analysis) |
| `exportToFile` body | Resource leak: `FileWriter` / `BufferedWriter` not closed in try-with-resources | S2095 | Major | Bug |
| `readExportFile` body | Path traversal in `new File(EXPORT_ROOT + filename)` | S2083 | Blocker | Vulnerability |
| `readExportFile` body | Resource leak: `FileInputStream` not closed | S2095 | Major | Bug |
| `readExportFile` body | `fis.read(data)` return value ignored | S2674 | Major | Bug |
| `archiveExport` | OS command injection via `Runtime.exec(String)` with user-controlled filename | S2076 | Blocker | Vulnerability |
| `archiveExport` | `Runtime.exec(String)` uses string-form command (parsing pitfalls) | S4036 | Critical | Security Hotspot |
| `cleanupExports` | OS command injection through `sh -c` with concatenated user input | S2076 | Blocker | Vulnerability |
| `cleanupExports` | `Runtime.exec(String)` form | S4036 | Critical | Security Hotspot |
| `parseImportXml` | XXE: `DocumentBuilderFactory` used without disabling external entities | S2755 | Critical | Vulnerability |
| `writeReport` | Path traversal: user-controlled `customerId` in file path | S2083 | Blocker | Vulnerability |
| `writeReport` | Resource leak: `FileWriter` not closed in try-with-resources | S2095 | Major | Bug |

---

### src/main/java/com/example/banking/util/CryptoUtil.java

| Line area | Finding | Rule | Severity | Category |
|---|---|---|---|---|
| `DES_KEY` field | Hardcoded encryption key | S6418 / S2068 | Blocker | Vulnerability |
| `AES_KEY` field | Hardcoded encryption key | S6418 / S2068 | Blocker | Vulnerability |
| `encryptDes` | Use of DES algorithm (broken) | S5547 | Critical | Vulnerability |
| `encryptDes` | Use of ECB cipher mode (deterministic, leaks structure) | S5542 | Critical | Vulnerability |
| `encryptAesEcb` | Use of ECB cipher mode | S5542 | Critical | Vulnerability |
| `encryptAesEcb` | No initialization vector (no IV for AES) | S5542 / S3329 | Critical | Vulnerability |

---

### src/main/java/com/example/banking/util/LegacyUserUtil.java

| Line area | Finding | Rule | Severity | Category |
|---|---|---|---|---|
| `public static Map<String, String> CACHE` | Public static mutable field | S1444 | Major | Code Smell |
| `public static List<String> RECENT_USERS` | Public static mutable field | S1444 | Major | Code Smell |
| `private static int unusedCounter` | Unused private static field | S1068 | Major | Code Smell |
| `createdAt` of type `java.util.Date` | Use of legacy `Date` API instead of `java.time` | S6829 (Java 8+ time API preferred) | Minor | Code Smell |
| Override of `hashCode` without `equals` | Equals/hashCode contract violation | S1206 | Critical | Bug |
| `formatLegacyId` method | Unused private method | S1144 | Major | Code Smell |
| `incrementUnused` method | Unused private method | S1144 | Major | Code Smell |

**Discussion teaching point:** S1206 is one of the most important "subtle" findings in Java. A class with `hashCode` but no `equals` (or vice versa) breaks hash-based collections in ways that are very hard to debug at runtime. Always implement both or neither.

---

### src/main/java/com/example/banking/filter/LoggingFilter.java

| Line area | Finding | Rule | Severity | Category |
|---|---|---|---|---|
| Throughout | `System.out.println` instead of logger | S106 | Major | Code Smell (multiple occurrences) |
| `Authorization` header logging | Logging sensitive data (bearer tokens) | S5145 / S2629 | Critical | Security Hotspot |
| `Cookie` header logging | Logging sensitive data (session cookies) | S5145 | Critical | Security Hotspot |
| Loop over all headers | Logs entire request headers (may include sensitive values) | S5145 | Critical | Security Hotspot |
| Catch of generic `Exception` | Catch of generic exception | S2221 | Major | Code Smell |

---

### src/main/java/com/example/banking/controller/UserController.java

| Line area | Finding | Rule | Severity | Category |
|---|---|---|---|---|
| `DEFAULT_API_KEY` constant | Hardcoded API key | S2068 / S6437 | Blocker | Vulnerability |
| `log.info("Login attempt for user: " + username)` | Log injection (user-controlled input in log message via concatenation) | S5145 | Critical | Vulnerability |
| `log.info("Looking up accounts for customer " + customerId)` | Log injection via concatenation | S5145 | Critical | Vulnerability |
| `login` catch | Catch of generic `Exception` | S2221 | Major | Code Smell |
| `register` catch | Catch of generic `Exception` | S2221 | Major | Code Smell |
| `getApiKey` endpoint | Exposes API key via unauthenticated endpoint | S6437 (and any custom rule for endpoint exposure) | Blocker | Vulnerability |
| `echo` endpoint | XSS: user input reflected into HTML without escaping | S5131 | Critical | Vulnerability (Developer+ for full taint analysis) |

---

### src/main/java/com/example/banking/controller/ConfigController.java

| Line area | Finding | Rule | Severity | Category |
|---|---|---|---|---|
| `@CrossOrigin(origins = "*", allowCredentials = "true")` | Permissive CORS combined with credentials - a serious combination | S5122 | Blocker | Vulnerability |
| `systemInfo` | Exposes system properties and environment via unauthenticated endpoint | S6437 / S5547 | Critical | Vulnerability |
| `activeThreads` | Exposes internal thread information | S6437 | Major | Security Hotspot |
| `shutdown` | Unauthenticated remote shutdown endpoint | n/a (no specific rule) - flagged via combination of S5852 or custom rules; pattern is unsafe regardless | Critical | Security Hotspot (instructor: this is a great discussion point - SonarQube may not flag this directly, which teaches the lesson that SAST is not a complete substitute for code review) |
| `shutdown` `Thread.sleep(1000)` | Magic number / arbitrary sleep | S2925 | Critical | Security Hotspot |
| `shutdown` catch block | Swallowed `InterruptedException` without re-interrupting thread | S2142 | Critical | Bug |
| `exec` endpoint | OS command injection via user-controlled command | S2076 | Blocker | Vulnerability |
| `exec` `Runtime.getRuntime().exec(cmd)` | `Runtime.exec(String)` form | S4036 | Critical | Security Hotspot |

---

### src/main/java/com/example/banking/service/AuditService.java

**This class is the clean reference.** No findings expected. If SonarQube reports anything here, either the scanner has been customized with non-default rules, or there is a finding that escaped the deliberate-clean review (please file a bug report against this answer key).

---

### src/main/java/com/example/banking/model/Account.java, User.java, TransferRequest.java

Mostly clean DTOs. SonarQube may report some minor findings on `User.java`:

- `User` has setters and is mutable - some teams have rules against mutable DTOs but this is not a default SonarJava rule
- No `equals`/`hashCode` on the `User` class - not flagged by default but worth noting if any rule mandates it

---

### Coverage findings

The only test is `AuditServiceTest`, covering `AuditService` thoroughly. Every other class has 0% coverage.

| Metric | Expected value |
|---|---|
| Overall line coverage | Below 10% (just `AuditService` lines) |
| Classes with coverage | 1 (AuditService) |
| Classes without coverage | All others |

SonarQube's default Quality Gate fails any project with new code coverage below 80%, so the entire project will be flagged as failing the quality gate. This is intentional.

---

### Summary by category (expected counts in Community Edition)

| Category | Approximate count |
|---|---|
| Vulnerabilities | 12-18 |
| Security Hotspots | 12-18 |
| Bugs | 8-12 |
| Code Smells | 25-35 |
| Coverage | 1 finding (low coverage) |
| Duplications | 1-2 (TransferService receipt methods) |

Exact counts vary by scanner version. If your students see far fewer findings, double-check:

1. The project actually compiled (`mvn clean verify` succeeded before `sonar:sonar` ran)
2. Bytecode was uploaded (look for `INFO: Java Main Files AST scan` in the scanner log)
3. JaCoCo coverage was produced and uploaded (`target/site/jacoco/jacoco.xml` exists)

---
---

## Part 3 -- Project B: `banking-vulnerable-lab`

The focused triage exercise. Far fewer findings, each one worked in depth.

### Complete planted-violation table

#### `src/main/java/com/example/banking/service/AdminService.java`

| Line area | Finding | Rule | Severity | Category |
|---|---|---|---|---|
| `ADMIN_PASSWORD` constant | Hardcoded credential (CWE-798) | S2068 | Blocker | Vulnerability |
| `generateHealthReport` | OS command injection via `Runtime.exec` with concatenated input (CWE-78) | S2076 | Blocker | Vulnerability (Developer+ for full taint analysis) |
| `generateHealthReport` | `Runtime.exec(String)` string form -- argument parsing pitfalls | S4036 | Critical | Security Hotspot |
| `generateHealthReport` catch | Empty catch block, exception silently swallowed | S108 / S1166 | Major | Code Smell |

#### `src/main/java/com/example/banking/util/CryptoUtil.java`

| Line area | Finding | Rule | Severity | Category |
|---|---|---|---|---|
| `hashPassword` | Weak hash algorithm MD5 (CWE-327) | S4790 | Critical | Security Hotspot, often reported as Vulnerability |
| `hashPassword` | No salt -- rainbow tables apply directly | (covered by S4790 discussion) | -- | -- |
| `generateSessionToken` | `java.util.Random` for a security value (CWE-338) | S2245 | Critical | Security Hotspot |
| `encryptSensitiveData` | `Cipher.getInstance("AES")` implicitly selects ECB (CWE-327) | S5542 | Critical | Vulnerability |
| `TEST_FIXTURE_KEY` | Hardcoded credential -- **deliberate false-positive bait** | S2068 / S6418 | Blocker | Vulnerability (may or may not fire) |
| `hashPassword` catch | `throw new RuntimeException(...)` -- generic exception | S112 | Major | Code Smell |

#### `src/main/java/com/example/banking/service/AccountRepository.java`

| Line area | Finding | Rule | Severity | Category |
|---|---|---|---|---|
| `findByAccountNumber` | SQL injection via string concatenation (CWE-89) | S2077 | Critical | Vulnerability (Developer+ for full taint analysis; Community may miss) |
| `findByStatus` | **Not vulnerable.** Parameterized query. Some scanners flag it anyway -- a false positive | -- | -- | -- |

#### `src/main/java/com/example/banking/service/DocumentService.java`

| Line area | Finding | Rule | Severity | Category |
|---|---|---|---|---|
| `readCustomerDocument` | Path traversal, unsanitized filename concatenated into a path (CWE-22) | S2083 | Blocker | Vulnerability (Developer+ for full taint analysis) |

#### `src/main/resources/application.yml`

Clean. The datasource password is empty, which is correct for in-memory H2 and
should not fire S2068. If a student reports one here, check they have not
edited the file.

---

### Model triage answers

Students should reach these conclusions. Wording will differ; the
**classification** and the **reasoning** are what you are grading.

#### Finding A -- Hardcoded credential, `AdminService.ADMIN_PASSWORD`

**Classification: true positive, fix now.**

Required reasoning:

- The value is readable by anyone with source access, and by anyone who can run
  `strings` on the JAR. Obfuscation does not help -- the value must exist at
  runtime to be compared against.
- Fix: environment variable or secrets manager.

**The point most students miss**, and the one worth drawing out in the debrief:
if this was ever committed, the password is in git history permanently.
Deleting the line does not remove it. **The credential must be rotated.** A
student whose comment says only "remove the hardcoded password" has given an
incomplete answer.

#### Finding B -- Weak hash, `CryptoUtil.hashPassword`

**Classification: true positive, fix now.**

Required reasoning:

- MD5 is collision-broken and has been since the early 2000s.
- **Speed is the deeper problem.** MD5 is fast by design, which is exactly wrong
  for password hashing -- an attacker with a stolen database tries billions of
  candidates per second. Purpose-built password hashes are deliberately slow with
  a tunable cost factor.
- No salt, so identical passwords hash identically and rainbow tables apply.
- Fix: bcrypt, scrypt or Argon2. `BCryptPasswordEncoder` in Part 4.

A student who says only "MD5 is broken, use SHA-256" has the wrong answer.
SHA-256 is also fast, and also unsalted. This is a good one to probe on.

#### Finding C -- Insecure cipher mode, `CryptoUtil.encryptSensitiveData`

**Classification: true positive, fix now.**

Required reasoning:

- `Cipher.getInstance("AES")` does not mean "AES with sensible defaults". It
  silently selects **ECB**. Nothing in the code says ECB -- you must know that
  omitting the mode picks the worst one. This is an API design trap.
- ECB maps identical plaintext blocks to identical ciphertext blocks, so
  plaintext structure survives. The "ECB penguin" is the canonical demo.
- Fix: `AES/GCM/NoPadding`, or `AES/CBC/PKCS5Padding` with a unique IV per message.

"Accepted risk" is defensible only with a strong argument that the data is
non-sensitive. For a banking application, push back.

#### Finding D -- Insecure randomness, `CryptoUtil.generateSessionToken`

**Classification: true positive, fix now.** Arrives as a **Security Hotspot**.

Required reasoning:

- `java.util.Random` is a linear congruential generator. Observing a modest
  number of outputs allows recovery of internal state and prediction of all
  future outputs. Default seeding is clock-based, so the seed space is guessable
  too.
- Consequence: an attacker with a few tokens predicts the next one and hijacks a
  session.
- Fix: `java.security.SecureRandom`.

**Ask why this is a Hotspot rather than a Vulnerability.** The rule fires on
`new Random()`; the tool cannot know the purpose. Here the method name
`generateSessionToken` resolves it in one second of human attention. That
resolution is precisely what the Hotspot category exists to collect.

#### Finding E -- `CryptoUtil.TEST_FIXTURE_KEY`

**Classification: false positive.**

Required reasoning:

- The Javadoc documents the purpose: a deterministic fixture for round-trip
  encryption tests.
- Production key material comes from `EncryptionConfigService` at runtime.
- **Why the tool was wrong:** it matched a name-plus-string-literal pattern
  without examining the documented purpose, the use sites, or the runtime
  configuration. That is not a defect in SonarQube -- pattern matching is what
  it does. Recognising the technique's limit is the analyst's job.

If the rule does not fire on your version, students still do the analysis and
note that the tool correctly ignored it. The exercise is the reasoning.

#### Task 3.7 -- The repository comparison

`findByAccountNumber` concatenates; `findByStatus` parameterizes. Students should
identify the first as vulnerable **before scanning**.

Two teaching points:

1. **Community Build may flag neither.** Full SQL injection detection needs taint
   analysis. If nothing appears, the vulnerability is still real -- the tool
   cannot see it. This is the most concrete demonstration in the lab of a tool's
   edge.
2. **Some scanners flag the safe one.** Not every tool traces parameterization
   through the JdbcTemplate API. Recognising that as a false positive requires
   understanding *why* parameterized queries are safe: the driver sends SQL and
   parameters over separate channels, so a value can never be parsed as SQL.

---

## Part 4 -- Expected Remediation Deltas

After the `BCryptPasswordEncoder` fix and a re-scan:

| Observation | Expected |
|---|---|
| MD5 finding on `hashPassword` | Gone, or status **Closed** |
| Total finding count | Down by one (occasionally two, if the generic-exception smell in the old catch block also disappears) |
| All other findings | Unchanged |
| Build | `BUILD SUCCESS` |

**If the finding is still open**, the usual cause is a leftover
`MessageDigest.getInstance("MD5")` elsewhere in the file. Have the student
re-read the finding's line number rather than re-reading their fix.

**A new finding may appear.** Adding `spring-boot-starter-security` pulls in a
default security configuration. Depending on version this can generate its own
findings or warnings. If a student's count goes *up*, that is worth discussing
rather than debugging away: adding a dependency changes your attack surface, and
that is a real effect, not a lab artifact.

### The Quality Gate trap (Task 4.5)

This is deliberate and is the sharpest teaching moment in Part 4.

After the fix, the gate may report **Passed** while dozens of findings remain
open. Mechanism: the default gate evaluates conditions against **new code**
only. The student changed a handful of clean lines, so every condition passes
trivially.

Make sure the class understands both halves:

- **Why Clean as You Code is sensible.** A team inheriting 400,000 lines cannot
  fix it all. Requiring that everything touched from today forward is clean is
  achievable and improves the codebase monotonically.
- **How it misleads.** A manager reading "Quality Gate: Passed" reasonably
  concludes the application is fine. It is not. The dashboard is answering a
  narrower question than the one being asked of it.

Have students compare the **New Code** and **Overall Code** tabs side by side.

---

## Part 5 -- Justification Rubric

Grade the three required parts:

| Part | Present? | What to look for |
|---|---|---|
| 1. The tool's allegation | | Names the rule and what it flagged |
| 2. Actual code behaviour | | Explains why the allegation is wrong, or why the risk is accepted |
| 3. **Why the tool was wrong** | | The diagnostic insight -- what the tool matched on and what it could not see |

**Part 3 is the discriminator.** Anyone can assert a finding is wrong. Explaining
*why the tool reached its conclusion* proves the student understood both the code
and the tool. A justification missing part 3 is a "Good" answer, not a "Better"
one.

**Bonus credit** for stating the condition under which the dismissal expires --
for example, "if this constant is ever referenced outside test code, this
classification must be revisited." That converts a snapshot opinion into a
durable control.

**Automatic fail:** "Not a real issue." "False positive." "Reviewed, OK." A
dismissal with no reasoning is worse than no triage, because it looks like a
decision was made.

---

## Reflection Question Answers

### Question 1 -- String `==` and the two techniques

*Why would a unit test written by the same developer probably miss it, and why does static analysis catch it?*

`==` on objects compares references. Java **interns string literals**, so
`"admin" == "admin"` is `true`. A developer writing a test for their own code
almost certainly passes a literal -- and the test passes.

The bug appears only when the string is built at runtime: parsed from an HTTP
request, read from a database, deserialized from JSON. Those strings are not
interned, so `==` is `false` even when the contents match. The defect is
invisible to the test and fatal in production.

Static analysis catches it because it reasons about **types, not values**. It
sees two `String` references compared with `==` and flags the pattern, without
caring where the strings came from.

**What this says about combining them:** the two techniques fail in opposite
directions. Tests verify behaviour for the inputs you thought of. Static
analysis verifies properties for all inputs, but only properties expressible as
patterns. A wrong interest calculation passes every linter and fails the first
unit test. A String `==` passes every test and is trivial for a linter. Neither
is a subset of the other, so a pipeline needs both.

### Question 2 -- Hotspots versus Vulnerabilities

A **Vulnerability** is a finding SonarQube is confident about: the code is
exploitable regardless of context.

A **Security Hotspot** is a finding the tool **cannot decide without context it
does not have**. `new Random()` is correct in a dice game and a serious
vulnerability in a session-token generator -- same line, same rule, opposite
verdicts. Only a human who knows the intent can classify it.

A Hotspot is therefore an explicit request for human judgment, with a workflow
attached: review, then mark Safe, Acknowledged, or Fixed.

**What a team loses by filtering them out:** exactly the findings that require
the most expertise and cannot be automated. The tool has said "I have found
something security-relevant and I need you to look" -- and been ignored. Worse,
the Hotspot count and review percentage are what a Quality Gate uses to enforce
that review, so filtering them also disables the enforcement.

Also worth noting: `new Random()` in this project is genuinely a vulnerability.
Had the team dismissed Hotspots as noise, a predictable-session-token defect
ships.

### Question 3 -- Is partial coverage more dangerous than zero?

**The lab's case:** 0% makes no claims and reassures nobody. 65% invites the
conclusion "most of the code is tested" -- but coverage is not distributed by
risk. The untested remainder is disproportionately error handling, edge cases and
rarely-exercised paths, which is where defects concentrate. The number provides
comfort it has not earned.

**The strongest opposing case** -- and students should be able to build it:

1. **Coverage is not a claim about quality, it is a claim about execution.**
   Blaming the metric for a misreading is blaming the thermometer for the fever.
   The fix is educating the reader, not discarding the measurement.
2. **65% is strictly more information than 0%.** You can see *which* 35% is
   uncovered and target it. At 0% you have no map.
3. **Partial coverage means a test harness exists.** Somebody wired up the build,
   the fixtures, the CI integration. Getting from 0% to 20% is far harder than
   from 65% to 80%, because the first step is infrastructure.
4. **In this project the correlation runs the right way.** `AuditService` is both
   the only covered class and the only clean one. Coverage was a genuine signal
   of care.
5. **The "dangerous" framing has no action attached.** Nobody sensibly argues for
   deleting tests to get to zero. If the number cannot be acted on, calling it
   dangerous is rhetoric.

**A good answer lands somewhere in between:** partial coverage is more dangerous
than zero *only when it is read as a quality score by someone who will not look
further*. That is a reporting problem, not a measurement problem. The useful
response is to pair coverage with mutation testing (Lab 1.1) so the number
reflects verification and not merely execution.

### Question 4 -- The Quality Gate mechanism, and both sides

**Mechanism.** The default gate evaluates conditions against **new code** --
lines changed since a reference point -- not the whole codebase. After the
`hashPassword` fix, new code is a handful of clean lines. Every condition passes
trivially. The gate reports Passed over an application containing hardcoded
credentials, command injection and path traversal.

**Why Clean as You Code is a sensible default:**

- A gate against overall code fails on day one for any legacy project and stays
  failed. A permanently red gate is ignored, and an ignored gate enforces nothing.
- Judging code by what the team changed is fair and actionable.
- It improves the codebase monotonically without requiring a rewrite.
- It creates the right incentive: touch a bad file, clean it up.

**How it misleads:**

- "Quality Gate: Passed" reads as "this application is fine". It means "the code
  most recently changed met the bar".
- A manager or auditor reading only the badge draws a materially wrong conclusion.
- Rarely-modified code is never re-evaluated. The worst code in a system is often
  the code nobody touches, which is exactly what this model exempts.
- It can be gamed: keep changes tiny and every gate passes forever.

**The resolution** is not to abandon the model but to report both numbers. New
Code answers "are we getting better?" Overall Code answers "where are we now?"
Publishing only the first is the failure.

### Question 5 -- Two invisible problems, two different reasons

**The `commons-collections 3.2.1` CVE.**

Invisible because it is **not in the analysed artifact**. SonarQube analyses the
source and bytecode you wrote. The vulnerability lives in a third-party library
declared in `pom.xml`. Nothing in the project's own code is wrong.

Control that catches it: **Software Composition Analysis (SCA)** -- OWASP
Dependency-Check, Snyk, Dependabot, GitHub dependency scanning. These compare a
dependency manifest against CVE databases.

Worth stressing: in most real applications the large majority of shipped code
came from dependencies. A pipeline with SAST and no SCA inspects the small part
and ignores the large part.

**The unauthenticated `/shutdown` endpoint.**

Invisible for the opposite reason: it **is** in the analysed code, but no
individual line is wrong. A mapping annotation is fine. Calling `System.exit` is
fine. The danger is the **combination** -- a destructive system effect reachable
without authentication. SAST reasons about patterns within code, not about
whether a capability should be exposed.

Controls that catch it: **design review**, **threat modelling**, and **DAST** or
penetration testing, which probe the running application and would find an
unauthenticated destructive endpoint immediately.

**The generalisation students should reach:** the two gaps are *scope* and
*abstraction level*. SAST has a bounded scope (your code) and operates at the
level of code patterns. Vulnerabilities outside that scope or above that level of
abstraction are structurally invisible to it -- not because the tool is bad, but
because that is what the technique is. Defence in depth is a response to the
shape of each technique's blind spot, not a slogan.

---

## Instructor Notes

### Timing

| Part | Budget | Notes |
|---|---|---|
| 1 -- Start SonarQube, token | 10-15 min | Fast if the image is prepared |
| 2 -- Read a report | 35-40 min | The longest part; mostly navigation and reading |
| 3 -- Triage five findings | 25-30 min | The core skill |
| 4 -- Remediate and re-scan | 15-20 min | Two scans, ~2 min each |
| 5 -- Justification | 10 min | Can be homework if time is short |

Total 100-120 minutes with discussion.

### The five moments most worth pausing on

1. **Task 2.3, the Hotspot explanation.** Students consistently treat Hotspots as
   low-priority Vulnerabilities. Get the distinction right here and Part 3
   Finding D lands on its own.

2. **Task 2.6, the String `==` bug.** Ask: "would your unit test have caught
   this?" Most say yes. Then explain literal interning. This is the clearest
   demonstration in the course that tests and static analysis are complementary
   rather than redundant.

3. **Task 2.9, what SonarQube did not find.** Show the `commons-collections`
   dependency, then search the dashboard for it and find nothing. The visual of
   searching and coming up empty is worth more than the explanation.

4. **Task 3.7, the two repository methods.** Have students predict which is
   vulnerable before scanning, then reveal that Community Build may flag neither.
   The gap between "I can see this is exploitable" and "the tool reports nothing"
   is the most valuable five minutes in the lab.

5. **Task 4.5, the green Quality Gate.** Let someone notice it went green before
   you explain why. The moment of "wait, that can't be right" does the teaching.

### If you are short on time

Cut **Part 5** and set it as written homework -- it needs no tooling.

Then compress **Part 2** by assigning Task 2.5 as a group activity: four tables,
one finding type each, five minutes, report back.

**Do not cut Part 3.** Triage is the transferable skill; everything else is
navigation. And do not cut Task 2.9 -- a lab that teaches students to trust a
green SAST scan is worse than no lab.

### If you are ahead

- Run OWASP Dependency-Check on Project A and show the `commons-collections` CVEs
  appearing where SonarQube reported nothing. This makes Task 2.9 concrete.
- Install **SonarQube for IDE** (SonarLint) in IntelliJ, bind it to the local
  server, and show findings appearing inline as you type -- the shift-left
  argument in one demonstration.
- Point `sonar.sources` at the test directory as well and discuss why test code
  deserves the same standards.
- Show the Quality Gate failing a build: add
  `-Dsonar.qualitygate.wait=true` to the Maven command and watch the build break.

### Cross-lab connections worth making explicit

- **Lab 1.1** -- coverage measures execution, not verification. Task 2.7 is the
  same argument from the tooling side. Mutation testing is the fix for both.
- **Lab 2.1** -- the SonarQube token stayed out of `sonar-project.properties`.
  Same principle as never committing the token here.
- **Lab 3.1** -- an E2E suite was fully green over an application with no business
  logic. Here a Quality Gate goes green over an application full of
  vulnerabilities. Different tools, same lesson: **understand what a green result
  is actually asserting.**
