# Lab 4.1 -- Code Quality and SAST Triage with SonarQube

> **Course:** Software Test Automation Survey
> **Module:** Lab 4 - Static Analysis and Code Quality
> **Estimated time:** 100-120 minutes
> **Environment:** Windows 11, IntelliJ IDEA, Maven, Java 21, SonarQube Community Build (local, no Docker)

---

## Overview

Every lab so far has run code to find problems. This one finds problems **without running anything**.

Static analysis reads source code and bytecode, applies a rule set, and reports what it finds. It catches a category of defect that tests structurally cannot: a hardcoded password is not a test failure, it is a code review failure. A resource leak may never surface in a test that runs for 40 milliseconds. A weak cipher produces perfectly correct output.

You will scan two projects.

**Project A -- `banking-tests-lab-sonar-demo`.** A broad, deliberately flawed Spring Boot application with roughly seventy findings across every SonarQube category. You will not fix anything here. The exercise is learning to *read a report*: what the categories mean, how severity works, where coverage fits, and what a Quality Gate does.

**Project B -- `banking-vulnerable-lab`.** A smaller application focused on security. Here you go deeper: triage five findings using the four-category workflow, remediate one, re-scan to prove the fix, and write an audit-quality suppression justification for a false positive.

Reading a report and triaging a report are different skills. Project A teaches the first, Project B the second.

### Learning objectives

By the end of this lab you will be able to:

- Start a locally installed SonarQube server and generate an analysis token
- Run a Maven scan and understand what gets sent to the server
- Distinguish Bugs, Vulnerabilities, Security Hotspots and Code Smells
- Read a coverage report and explain why partial coverage can mislead
- Apply the four-category triage workflow to security findings
- Remediate a true positive and verify the fix by re-scanning
- Write a suppression justification that survives an audit
- Explain what static analysis cannot find

---

## Before You Start

### What you need

- **IntelliJ IDEA** (Community Edition is fine)
- **JDK 21** on your PATH
- **Maven** -- bundled with IntelliJ
- **SonarQube Community Build**, already installed on your lab VM at `C:\sonarqube`
- A modern browser
- Both project folders from the course repository

### What you do NOT need

- **Docker.** SonarQube runs directly on the VM as a Windows service. Earlier versions of this lab used a container; the VM cannot run one because of nested virtualization limits, and it turns out not to matter. The ZIP distribution works identically.
- An internet connection during the lab, provided Maven dependencies were pre-cached on the image
- A commercial SAST licence

### A note on the tool

Most organisations run a commercial SAST tool -- Checkmarx, Snyk, Veracode, Fortify. This lab uses SonarQube Community Build because it is free and runs locally.

**The triage workflow is identical across all of them.** The vocabulary differs slightly (SonarQube says "False Positive" where Checkmarx says "Not Exploitable") but the meaning is the same, and all of them use **CWE** numbers. CWE-327 means weak cryptography in every tool that exists. The tool changes; the vocabulary does not.

### What Community Build detects, and what it does not

SonarQube Community Build catches **pattern-based** issues: hardcoded credentials, weak crypto algorithms, weak randomness, insecure cipher modes, resource leaks, and a large catalogue of maintainability rules.

What it does **not** catch is anything needing **taint analysis** -- tracing untrusted input through the program to a dangerous sink. SQL injection, command injection, path traversal and XSS all need this, and the rules are paywalled to SonarQube's commercial editions.

This matters for your expectations. Both projects contain injection vulnerabilities. Community Build will flag some of them on pattern alone and miss others entirely. That gap is itself one of the lessons: **a green scan from a free tool is not the same as a secure application.**

---

## Part 1 -- Start SonarQube and Get a Token

**Estimated time:** 10-15 minutes

SonarQube is already installed on your VM. You are starting it, not installing it. (If you need to build a VM yourself, **Appendix A** has the full installation procedure.)

### Task 1.1 -- Start the Server

SonarQube is not installed as a Windows service, so you manually have to start.

In a command window, type the command

```
StartSonar
```

Sonarqube is up an running when you see the output in the window say "SonarQube is operationsal"

Leave that window open -- closing it stops the server.

### Task 2.1 -- Login

Browse to `http://localhost:9000`.

There is a text file on your desktop named `SonarqubeCreds.txt`

This contains your login id `admin` and password `Pa$$w0rdPa$$`, as well as the token that you will use in this lab.

You should land on a dashboard with no projects yet.

> **If the page does not load:** the server is still starting. Check `C:\sonarqube\logs\sonar.log` and wait for the line reporting that SonarQube is operational.

### Task 1.3 -- Note which mode your instance is in

Look at **Administration -> Configuration -> General Settings -> Mode**.

SonarQube has two presentations of the same analysis:

- **Standard Experience** -- findings are Bugs, Vulnerabilities and Code Smells, with severities Blocker / Critical / Major / Minor / Info
- **MQR Mode** (Multi-Quality Rule, the default on new instances) -- findings map to software qualities (Reliability, Security, Maintainability) with severities Blocker / High / Medium / Low / Info

**This lab is written in Standard Experience vocabulary**, because that is what nearly every tutorial, screenshot and Stack Overflow answer uses. If your instance is in MQR Mode, the findings are the same but the labels differ. 

Your VM is configured to use the Standard Experience

Translate as follows:

| Standard Experience | MQR Mode |
|---|---|
| Bug | Reliability issue |
| Vulnerability | Security issue |
| Code Smell | Maintainability issue |
| Critical / Major / Minor | High / Medium / Low |

Note which mode you are in before continuing, so the screens match what you read here.

### Task 1.4 -- Generate an analysis token

This is already done for you, but if you need to generate a new token for some reason, you can use this instructions to create a new one.

Maven authenticates to SonarQube with a token, never a password.

1. Click your avatar, top right -> **My Account**
2. **Security** tab
3. Under *Generate Tokens*:
   - Name: `lab4-scan-token`
   - Type: **Global Analysis Token**
   - Expires in: the longest option offered
4. Click **Generate**

A string like `sqa_a1b2c3...` appears. **Copy it now and paste it somewhere you can get at.** SonarQube shows it exactly once. If you lose it, generate another.

**Why Global Analysis Token and not User Token?** A global analysis token can create and analyse any project. A project-scoped token would need one per project, and you are scanning two. A User Token carries your full permissions, which is more authority than a scan needs.

> **Never commit a token.** In this lab you will paste it on the command line. In a real pipeline it belongs in a secrets store and arrives as an environment variable. This is the same principle you applied in Lab 2.1 when the SonarQube token stayed out of `sonar-project.properties`.

---

## Part 2 -- Read a Report

**Estimated time:** 35-40 minutes
**Project:** `banking-tests-lab-sonar-demo`

### Context

This project is a Spring Boot banking application with roughly seventy planted defects. It compiles cleanly -- that matters, because SonarQube analyses bytecode as well as source, and a project that does not compile gets a much shallower scan.

You are not fixing anything in this part. You are learning to read.

### Task 2.1 -- Import and skim the project

1. In IntelliJ: **File -> Open**, select the `banking-tests-lab-sonar-demo` folder
2. Accept the Maven import and wait for indexing

While it indexes, open a few files and just look:

- `service/AuditService.java` -- the **clean reference**. No planted findings. Note the injected `Clock`, the defensive copy, the specific exceptions.
- `repository/AccountRepository.java` -- how queries are built and how resources are (not) closed
- `service/TransferService.java` -- one very long method
- `controller/ConfigController.java` -- what these endpoints expose

Do not hunt for bugs yet. Get a feel for the shape.

### Task 2.2 -- Run the scan

Open the IntelliJ terminal (**View -> Tool Windows -> Terminal**) in the project root:

**_Note: This command below may not work in PowerShell so either run it in a command prompt or use double quotes like this "-Dsonar.token=YOUR_TOKEN_HERE"_**

This lab was tested using cmd prompts
```
mvn clean verify sonar:sonar -Dsonar.token=YOUR_TOKEN_HERE
```

That is the whole command. The server URL, project key and project name are already set in `pom.xml`, so the token is the only thing you supply.

What happens, in order:

1. `clean` deletes previous build output
2. `verify` compiles the code and runs `AuditServiceTest`, with the JaCoCo agent attached
3. JaCoCo writes `target/site/jacoco/jacoco.xml`
4. `sonar:sonar` uploads **source, bytecode, and the coverage report** to your local server
5. The server queues an analysis task and processes it

**All three uploads matter.** Source alone gives shallow analysis. Bytecode enables the deeper rules. Without the coverage report, coverage shows as 0% and students conclude the scan failed.

The build ends with something like:

```
[INFO] ANALYSIS SUCCESSFUL, you can find the results at: http://localhost:9000/dashboard?id=banking-tests-lab-sonar-demo
[INFO] BUILD SUCCESS
```

**`BUILD SUCCESS` means the upload worked, not that the code is clean.** The server still needs 30-60 seconds to process. Open the dashboard link and refresh until numbers appear.

### Task 2.3 -- Understand the four categories

The dashboard splits findings four ways. The distinction is the single most useful thing to take from this part.

| Category | Question it answers | Example here |
|---|---|---|
| **Bug** | Is this code wrong? | `==` used to compare Strings |
| **Vulnerability** | Is this code exploitable? | Password hardcoded in `application.properties` |
| **Security Hotspot** | Is this code *security-sensitive*? | `new Random()` -- fine for shuffling, not for tokens |
| **Code Smell** | Will this code be painful to maintain? | A method with cognitive complexity of 40 |

**The Hotspot category is the one people misunderstand.** A Vulnerability is something SonarQube is confident is wrong. A Hotspot is something SonarQube *cannot decide about without context it does not have*.

`new Random()` is the perfect example. In a dice-rolling game it is correct. In a session-token generator it is a serious vulnerability. The same line of code, the same rule, two opposite verdicts — and only a human who knows the intent can tell them apart.

So a Hotspot is an explicit invitation: *a person needs to look at this*. Teams that treat Hotspots as noise and filter them out have disabled the part of the tool that was asking for their judgment.

### Task 2.4 -- Survey the findings

Open the project, then the **Issues** tab. Use the left sidebar to filter by **Type**.

Record what you see:

| Category | Count on your scan |
|---|---|
| Bugs | |
| Vulnerabilities | |
| Security Hotspots | |
| Code Smells | |

Expect roughly 8-12 bugs, 12-18 vulnerabilities, 12-18 hotspots and 25-35 code smells. Exact numbers vary by SonarQube version and edition; if you are within range, your scan worked.

Now filter by **Severity** and look at Blocker only. There should be a handful. These are where you would start on a real project.

### Task 2.5 -- Drill into one finding of each type

Pick one Bug, one Vulnerability, one Hotspot and one Code Smell. For each, click into it and find:

- The **rule name** and its ID (like `java:S2068`)
- The **"Why is this an issue?"** explanation
- The **"How can I fix it?"** section
- The **CWE number**, if the rule maps to one
- The exact file and line

Write one sentence per finding: what the issue is and how you would fix it.

**Suggested findings, if you want a steer:**

- **Bug:** `AuthService.authenticate` compares Strings with `==`
- **Vulnerability:** the hardcoded password in `application.properties`
- **Hotspot:** `new Random()` in `AuthService`
- **Code Smell:** cognitive complexity in `TransferService.executeTransfer`

### Task 2.6 -- The String `==` bug

Find the finding on `AuthService.authenticate`. The code:

```java
if (username == ADMIN_USERNAME) { ... }
```

This compiles. It runs. It sometimes even works.

`==` on objects compares **references**, not contents. Two `String` objects with identical characters are only `==` if they are the same object. Java interns string *literals*, so `"admin" == "admin"` is true — which is exactly why this bug is so dangerous. It works in every test that passes a literal, and fails the moment the username arrives from an HTTP request, a database, or a file, because those strings are built at runtime and are not interned.

**A unit test would very likely miss this**, because a test written by the same developer probably passes a literal too. Static analysis catches it by looking at types, not values. This is a clean example of the two techniques covering different ground.

### Task 2.7 -- Coverage, and why partial is worse than zero

Open the **Measures** tab, then **Coverage**.

Overall coverage is below 10%. Exactly one class — `AuditService` — has meaningful coverage. Everything else has none.

Two questions:

**Which class is covered, and why does that matter?** `AuditService` is also the only class with no planted defects. That is not a coincidence, and it is the argument for tests as a quality control: code that someone wrote tests for tends to be code someone thought carefully about.

**Why is partial coverage often more dangerous than zero?** A project at 0% makes no claims. Nobody looks at that number and feels safe. A project at, say, 65% invites a conclusion: "most of the code is tested." But coverage is not distributed evenly by risk. The 35% left uncovered is frequently the error handling, the edge cases, and the rarely-exercised paths — precisely where defects concentrate. The number reassures without justifying the reassurance.

Recall the lesson from Lab 1.1: **coverage measures execution, not verification.** A test with no assertions still counts. Coverage is a lower bound on what is untested, never a measure of quality.

### Task 2.8 -- The Quality Gate

Go to the project home. There is a **Quality Gate** badge at the top, and it says **Failed**.

Click through to see which conditions failed. The default "Sonar way" gate requires, on **new code**: no open issues, coverage at or above 80%, duplication below 3%, and a security hotspot review rate of 100%.

A Quality Gate is a **pass/fail verdict** rather than a pile of numbers, and that is what makes it usable in a pipeline. `mvn sonar:sonar` can be configured to fail the build when the gate fails, which is the mechanism by which static analysis actually blocks a merge instead of generating a report nobody reads.

Notice the phrase **new code**. Sonar's "Clean as You Code" model evaluates the gate against code changed since a reference point, not the whole codebase. The reasoning is practical: a team inheriting 400,000 lines of legacy code cannot fix it all, but they can require that everything they touch from today forward is clean.

The trap for this lab: on a first analysis, *all* code is new, so the gate is meaningful. On a second analysis with nothing changed, new code is empty, and the gate can go green with no conditions evaluated at all — which looks like you fixed everything. You did not. Watch for this in Part 4.

### Task 2.9 -- What SonarQube did not find

This is the most important task in Part 2.

Open `pom.xml` and find this dependency:

```xml
<dependency>
    <groupId>commons-collections</groupId>
    <artifactId>commons-collections</artifactId>
    <version>3.2.1</version>
</dependency>
```

This version has notorious deserialization vulnerabilities (CVE-2015-7501, CVE-2015-6420). It was the basis of a whole generation of Java exploit chains.

Search the SonarQube dashboard for it.

**It is not there.** No edition of SonarQube reports third-party dependency CVEs out of the box. SonarQube analyses **your code**. Known-vulnerable dependencies are a different tool's job: OWASP Dependency-Check, Snyk, Dependabot, or GitHub's dependency scanning.

This matters more than it might seem. In most real applications the overwhelming majority of shipped code came from dependencies, not from the team. A pipeline with SAST and no SCA (Software Composition Analysis) is inspecting the small part and ignoring the large part.

Now open `controller/ConfigController.java` and find the `shutdown` endpoint — an unauthenticated HTTP endpoint that shuts down the JVM.

SonarQube probably does not flag it. Every individual line is unremarkable: a mapping annotation, a method call. The danger is in the **combination** — no authentication plus a destructive system effect. SAST tools reason about patterns in code, not about whether an endpoint should exist.

**Static analysis is one layer. It is not a substitute for design review or threat modelling.**

---

## Part 3 -- Triage Five Findings

**Estimated time:** 25-30 minutes
**Project:** `banking-vulnerable-lab`

### Context

Part 2 was about breadth. Part 3 is about depth, on a smaller and more focused project.

A SAST tool produces findings, not decisions. **Triage is the human's job**, and it is the skill that separates someone who runs a scanner from someone who uses one. For each finding you apply three steps:

1. **Confirm** — read the finding and the code. Does the alleged problem actually exist?
2. **Classify** — into exactly one of four categories:
   - *True positive, fix now*
   - *True positive, accepted risk*
   - *False positive*
   - *Out of scope*
3. **Act** — record the classification in the tool, **with a comment**

Step 3 is the deliverable. A finding marked "False Positive" with no comment is worse than an untriaged finding, because it looks like someone made a decision when nobody did.

### Task 3.1 -- Scan the second project

Open `banking-vulnerable-lab` in IntelliJ (**File -> Open**; you can have both projects open at once).

Before scanning, read three files:

- `service/AdminService.java` — the password declaration, the shell command, the catch block
- `util/CryptoUtil.java` — the hashing, the token generator, the cipher mode, and the constant at the top
- `service/AccountRepository.java` — **two** query methods. One is vulnerable, one is not. Work out which before you scan.

Then scan:

```
mvn clean verify sonar:sonar -Dsonar.token=YOUR_TOKEN_HERE
```

Open the **Banking Vulnerable Lab** project in SonarQube. You should see roughly 4-8 security findings plus several code smells, spread across `AdminService` and `CryptoUtil`.

### Task 3.2 -- Finding A: hardcoded credential

Find the finding on `AdminService.java`. Rule: *Credentials should not be hard-coded* (`java:S2068`), severity Blocker.

```java
private static final String ADMIN_PASSWORD = "Admin@123!Production";

public boolean authenticateAdmin(String suppliedPassword) {
    return ADMIN_PASSWORD.equals(suppliedPassword);
}
```

**Confirm.** The production admin password is a string literal. Anyone with read access to the source knows it. So does anyone who can run `strings` on the compiled JAR — obfuscation does not help, since the value must exist at runtime to be compared.

**Classify: true positive, fix now.**

There is a second consequence worth stating: if this has ever been committed to source control, the password is in the repository history permanently. Deleting the line does not remove it. **The credential must be rotated, not just removed.** People miss this constantly.

**Act.** Expand the finding, set **Status** to *Confirmed*, and add a comment:

> Confirmed hardcoded credential. Production admin password is a string literal in source. Fix: read from environment variable or secrets manager. Note the value is already compromised via source control history — rotation is required in addition to the code change.

### Task 3.3 -- Finding B: weak hash algorithm

Find the finding on `CryptoUtil.hashPassword`. Rule: weak cryptographic algorithm (`java:S4790` / CWE-327).

```java
MessageDigest digest = MessageDigest.getInstance("MD5");
byte[] hash = digest.digest(password.getBytes());
```

**Confirm.** MD5 has been cryptographically broken since the early 2000s; collisions are generatable in seconds on a laptop.

For password hashing it is doubly wrong, and for a reason worth understanding. MD5 is **fast** — that is what it was designed for. Speed is exactly the wrong property for password hashing, because it means an attacker with a stolen hash database can try billions of candidates per second. Purpose-built password hashes (bcrypt, scrypt, Argon2) are *deliberately slow* and have a tunable cost factor.

There is no salt here either, so identical passwords produce identical hashes and precomputed rainbow tables apply directly.

**Classify: true positive, fix now.** You will fix this one in Part 4.

**Act.** Status *Confirmed*, with a comment recording the analysis.

### Task 3.4 -- Finding C: insecure cipher mode

Find the finding on `CryptoUtil.encryptSensitiveData`. Rule: insecure cipher mode (`java:S5542`).

```java
Cipher cipher = Cipher.getInstance("AES");
```

**Confirm.** This is a true positive, and the reason is a nasty piece of API design: `Cipher.getInstance("AES")` does not mean "AES with sensible defaults". It silently selects **ECB mode**. Nothing in the code says "ECB" — you have to know that omitting the mode picks the worst one.

ECB encrypts identical plaintext blocks to identical ciphertext blocks, so structure in the plaintext survives into the ciphertext. The canonical demonstration is the "ECB penguin": an image encrypted with AES/ECB still visibly shows the penguin.

The fix is to name the mode explicitly: `AES/GCM/NoPadding` for authenticated encryption, or `AES/CBC/PKCS5Padding` with a unique IV per message.

**Classify: true positive, fix now.** For a banking application, "accepted risk" would be a very hard sell.

**Act.** Status *Confirmed*, with a comment.

### Task 3.5 -- Finding D: insecure randomness

Find the finding on `CryptoUtil.generateSessionToken`. Rule: `java:S2245`. Note this arrives as a **Security Hotspot**, not a Vulnerability.

```java
public String generateSessionToken() {
    Random random = new Random();
    StringBuilder token = new StringBuilder();
    for (int i = 0; i < 32; i++) {
        token.append(Integer.toHexString(random.nextInt(16)));
    }
    return token.toString();
}
```

**Confirm.** `java.util.Random` is a linear congruential generator. Given a modest number of observed outputs, its internal state can be recovered and all future outputs predicted. It is seeded from the system clock by default, so the seed space is also guessable.

For shuffling a playlist, fine. For session tokens, it means an attacker who obtains a few tokens can predict the next one and hijack a session.

**Why is this a Hotspot rather than a Vulnerability?** Because the rule fires on `new Random()` and the tool cannot know what the value is for. Here the method name says `generateSessionToken`, so a human resolves it instantly. That resolution is what Hotspots exist to collect.

**Classify: true positive, fix now.** The fix is `java.security.SecureRandom`.

**Act.** Hotspots have their own workflow. Open the **Security Hotspots** tab, select the hotspot, review the explanation, and set the status to **Acknowledged** (or **To Review** with a comment), noting that this is a real security use and must be fixed.

### Task 3.6 -- Finding E: the false positive

Find the finding on `CryptoUtil.TEST_FIXTURE_KEY`. SonarQube may flag it as a hardcoded credential because the name contains "Key" and the value is a string literal that looks secret-ish.

```java
/**
 * Test fixture key used only by unit tests. NOT a real production secret.
 * The actual production key is loaded from the EncryptionConfigService at runtime
 * (see EncryptionConfigService.getProductionKey()). This constant exists solely
 * so unit tests have a deterministic key for round-trip encryption tests.
 *
 * SAST scanners may flag this as a hardcoded credential. That would be a false
 * positive: the value is not a credential and never reaches production code.
 */
public static final String TEST_FIXTURE_KEY = "TestFixtureKey16";
```

**Confirm.** The Javadoc documents the purpose. The value is deterministic test data, not a secret. Real key material is loaded from configuration at runtime.

**Classify: false positive.**

Be honest about *why* the tool got it wrong: it matched a name-plus-literal pattern without examining the documented purpose or the use sites. That is not a bug in SonarQube — pattern matching is what it does. Recognising the limit of the technique is the analyst's job.

**Act.** Set the status to **Resolved as False Positive** and add the justification you will write in Part 5.

> **If SonarQube did not flag this**, that is fine — do the analysis anyway and note in your write-up that the tool correctly ignored it. Whether a specific rule fires varies by version and rule pack.

### Task 3.7 -- The one that is not vulnerable

Open `service/AccountRepository.java` and compare the two methods.

```java
// findByAccountNumber
String query = "SELECT ... FROM accounts WHERE account_number = '" + accountNumber + "'";
return jdbcTemplate.query(query, this::mapRow);

// findByStatus
String query = "SELECT ... FROM accounts WHERE status = ?";
return jdbcTemplate.query(query, this::mapRow, status);
```

The first concatenates user input into SQL — textbook injection. Supplying `' OR '1'='1` returns every account.

The second uses a **parameterized query**. The `?` is a placeholder; the driver sends the SQL and the parameter separately, so the value can never be parsed as SQL. It is not vulnerable no matter what `status` contains.

Two things to take away.

**SonarQube Community may not flag either.** Full SQL injection detection needs taint analysis, which lives in the paid editions. If you see nothing here, the vulnerability is still real — your tool just cannot see it.

**Tools sometimes flag the safe one.** Some scanners cannot trace parameterization through the JdbcTemplate API and report `findByStatus` anyway. If that happens, it is a false positive, and recognising it requires understanding *why* parameterized queries are safe. You cannot triage what you do not understand.

### Verify Part 3

In the Issues view, filter by **Status**. You should have several findings marked *Confirmed* and possibly one *Resolved as False Positive* — each with a comment.

That is the audit trail. In a regulated environment, an auditor reads exactly these comments to decide whether your dismissals were sound.

---

## Part 4 -- Remediate and Re-scan

**Estimated time:** 15-20 minutes

### Context

A fix is not done when you have written it. It is done when the scan confirms it.

You will fix Finding B — the MD5 password hash — using Spring Security's `BCryptPasswordEncoder`.

### Task 4.1 -- Add the dependency

Open `banking-vulnerable-lab/pom.xml` and add to `<dependencies>`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

Reload the Maven project when IntelliJ offers, or right-click `pom.xml` -> **Maven** -> **Reload Project**.

### Task 4.2 -- Apply the fix

Open `util/CryptoUtil.java`.

**Before:**

```java
public String hashPassword(String password) {
    try {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        byte[] hash = digest.digest(password.getBytes());

        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
    } catch (NoSuchAlgorithmException e) {
        throw new RuntimeException("Hash algorithm not available", e);
    }
}
```

**After:**

```java
private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder =
        new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();

public String hashPassword(String password) {
    return passwordEncoder.encode(password);
}
```

You can remove the now-unused `MessageDigest` and `NoSuchAlgorithmException` imports.

Notice what the fix eliminates beyond the algorithm: bcrypt generates and embeds a **per-password salt** automatically, and applies a tunable **cost factor**. The replacement is four lines and removes three separate problems.

> Production code would inject the `PasswordEncoder` as a Spring `@Bean` rather than instantiating it inline. Inline is fine for the lab; the security property is identical.

### Task 4.3 -- Re-scan

```
mvn clean verify sonar:sonar -Dsonar.token=YOUR_TOKEN_HERE
```

Wait for the build, then 30-60 seconds for the server to process.

### Task 4.4 -- Verify the fix

Refresh the Issues view.

- The MD5 finding on `hashPassword` is **gone** (or shows as *Closed* if you filter by status)
- The other findings are unchanged — you did not touch them
- The total count dropped by one

**If the finding is still open**, your fix did not address what the rule was matching. The usual cause is leaving a `MessageDigest.getInstance("MD5")` call elsewhere in the file. Read the finding's line number again.

### Task 4.5 -- The Quality Gate trap

Look at the Quality Gate badge now.

Depending on how new code is configured, it may have gone **green** — despite dozens of open findings.

This is the "new code" behaviour from Task 2.8. The gate evaluates conditions against code changed since the reference point. If your only change was the `hashPassword` fix, then "new code" is a handful of clean lines, every condition passes trivially, and the gate reports success over an application full of hardcoded credentials.

**A green Quality Gate does not mean a clean project.** It means the code you touched most recently met the bar. That is a genuinely useful thing to measure — it is what makes Clean as You Code workable on legacy systems — but reading it as "the application is fine" is a serious misreading, and one that gets made in real organisations.

Check the **Overall Code** tab alongside **New Code** to see the difference.

### What this proves

You have now run the complete remediation cycle:

1. The scan found a finding
2. You confirmed it was real and classified it
3. You changed the code
4. A new scan confirmed the finding is gone

This is exactly what a CI pipeline automates. Every commit triggers a scan, new findings block the merge, remediated findings drop off. The workflow is identical in Checkmarx, Snyk and everything else in the category.

---

## Part 5 -- Write a Suppression Justification

**Estimated time:** 10 minutes

### Context

When you dismiss a finding, the comment you leave *is* the deliverable. Future readers — a security engineer, an auditor, you in eight months — will read it to decide whether your judgment was sound. A good justification survives review. A bad one becomes an audit finding of its own.

"Not a real issue" is not a justification. Neither is "false positive" on its own.

### Task 5.1 -- The three parts

A defensible justification contains:

1. **The tool's allegation.** What was flagged, and under which rule?
2. **The actual behaviour of the code.** Why is the allegation wrong, or why is the risk acceptable?
3. **Why the tool got it wrong**, or what mitigates the risk. This is the diagnostic insight, and it is the part that distinguishes analysis from assertion.

Part 3 is what a reviewer looks for. Anyone can assert a finding is wrong; explaining *why the tool reached its conclusion* proves you understood both the code and the tool.

### Task 5.2 -- Worked example

For the `TEST_FIXTURE_KEY` false positive:

> **False positive.** The tool flagged `CryptoUtil.TEST_FIXTURE_KEY` under the hardcoded-credentials rule (S2068) because the constant name contains "Key" and its value is a string literal resembling a secret.
>
> The constant is a deterministic test fixture, documented as such in its Javadoc, used to give unit tests a stable key for round-trip encryption tests. Production key material is loaded at runtime from `EncryptionConfigService.getProductionKey()`; this value never reaches a production code path.
>
> The rule matches on a name-plus-string-literal pattern and does not examine the constant's documented purpose, its use sites, or the runtime configuration that supplies the real key. No remediation required. The Javadoc serves as the durable audit trail; if the constant is ever used outside test code this classification must be revisited.

Note the final clause. It states the condition under which the dismissal stops being valid. That is what makes it durable rather than a snapshot of one person's opinion.

### Task 5.3 -- Write your own

Pick a finding you classified as *false positive* or *accepted risk* in Part 3 and write a justification covering all three parts.

If SonarQube did not flag `TEST_FIXTURE_KEY`, write one for any finding you would defend, framed as if it had been flagged.

### Task 5.4 -- Record it

In SonarQube, find your issue, add the justification as a comment, and set the status:

- **Resolved as False Positive** for false positives
- **Confirmed**, with the justification as the comment, for accepted risks

Save your justification in `lab4-notes.md` as well, so you have it outside the tool.

---

## Troubleshooting

**`http://localhost:9000` does not load**
The service is not running or is still starting. Check `services.msc`, then `C:\sonarqube\logs\sonar.log`.

**`Not authorized. Please check the properties sonar.login and sonar.password`**
The token is wrong, expired, or was pasted with a trailing space. Generate a fresh one.

**`sonar.login is deprecated`**
Use `-Dsonar.token=` instead. Same value, current parameter name.

**`BUILD SUCCESS` but the dashboard is empty**
The server is still processing. Wait 60 seconds and refresh. If it stays empty, check **Administration -> Background Tasks** for a failed task.

**Coverage shows 0%**
JaCoCo did not run or its report was not found. Confirm `target/site/jacoco/jacoco.xml` exists. Running `mvn sonar:sonar` without `verify` first is the usual cause — the report has to exist before the scan uploads it.

**Far fewer findings than expected**
The project did not compile, so only source-level rules ran. `mvn clean verify` must succeed before `sonar:sonar`. Look for `Java Main Files AST scan` in the scanner output.

**`OutOfMemoryError` during analysis**
Give the scanner more heap: `set SONAR_SCANNER_OPTS=-Xmx1024m` before the Maven command.

**Analysis fails with a Java version error**
SonarQube itself needs Java 21 or 25, and the projects target Java 21. Check `java -version` and, for the server, that `SONAR_JAVA_PATH` points at a supported JDK.

**The dashboard labels do not match this lab**
Your instance is in MQR Mode. See the translation table in Task 1.3.

---

## Reflection Questions

Answer in `lab4-notes.md`.

1. Task 2.6 showed a String compared with `==`. Explain why a unit test written by the same developer would probably not catch it, and why static analysis does. What does that tell you about how the two techniques should be combined?

2. A Security Hotspot and a Vulnerability are both security findings. What distinguishes them, and what does a team lose by filtering Hotspots out of their dashboard?

3. Task 2.7 argued that partial coverage can be more dangerous than zero coverage. Do you agree? Make the strongest case you can for the opposite position.

4. After your fix in Part 4 the Quality Gate may have gone green while dozens of findings remained open. Explain the mechanism. Then argue both sides: why is Clean as You Code a sensible default, and how could it mislead a manager reading the dashboard?

5. Two things in this lab were invisible to SonarQube: the `commons-collections` CVE and the unauthenticated `shutdown` endpoint. They are invisible for *different reasons*. Explain each, and name the kind of control that would catch each one.

---

## What You Have Built

You have run a static analysis tool end to end over two real codebases, and practised both of the skills it demands.

**Reading a report.** Four categories answering four different questions. Severity as a starting point, not a ranking. Coverage as a lower bound on what is untested. A Quality Gate as a pass/fail verdict a pipeline can act on.

**Triaging a report.** Confirm, classify, act — with the comment as the deliverable. Four categories, of which "false positive" is the one that requires the most understanding to use honestly.

**Verifying a fix.** The remediation cycle is not complete until a re-scan confirms it.

### Key takeaways

1. **SAST tools are noisy by design.** They report what *might* be a problem. Deciding is the human's job, and that judgment is the skill worth having.

2. **Triage produces an audit trail.** A dismissal without a justification is worse than no triage, because it looks like a decision was made.

3. **CWE is the portable vocabulary.** CWE-327 means weak crypto in every tool. Products change; the vocabulary does not.

4. **Re-scan after every fix.** A change that *should* fix something but does not is more dangerous than no change, because the team believes the issue is closed.

5. **Static analysis has a shape, and it has edges.** It finds patterns in your code. It does not find vulnerable dependencies, and it does not find dangerous designs assembled from individually innocuous lines. A pipeline needs SAST *and* SCA *and* design review — none of them substitutes for another.

6. **Tests and static analysis catch different things.** The `==` bug slips past tests and is trivial for a linter. A wrong interest calculation passes every linter and fails the first unit test. Labs 1 through 3 built one half; this lab built the other.

---

## What's Next

This is the last lab in the course. The wrap-up session covers building all four techniques into a CI pipeline: unit tests in the commit stage, integration and E2E in the acceptance stage, and static analysis as a quality gate on every merge.

---

## Appendix A -- Installing SonarQube on Windows (No Docker)

Your VM already has this. Included so you can rebuild it, or set one up at work.

### 1. Install a supported JDK

SonarQube Community Build runs on **Java 21 or Java 25**. Newer Java versions are not supported and the server will refuse to start. Install Temurin 21 if you do not have it.

### 2. Download

Get the Community Build ZIP from `https://www.sonarsource.com/products/sonarqube/downloads/`. The site steers toward the commercial editions; Community Build is further down the page. Binaries live under `/Distribution/` — anything under `/CommercialDistribution/` is the wrong one.

### 3. Unzip

Extract to **`C:\sonarqube`**.

The path must not contain spaces, must not begin with a digit, and must not sit under `Program Files`. Elasticsearch, which SonarQube embeds, is fussy about all three.

### 4. Point it at the right Java

Set a **system** environment variable:

```
SONAR_JAVA_PATH = C:\Program Files\Eclipse Adoptium\jdk-21\bin\java.exe
```

This matters if the machine's default `JAVA_HOME` is a version SonarQube does not support. It must be a *system* variable, not a user one, or the Windows service will not see it.

### 5. Exclude the folder from Windows Defender

Add `C:\sonarqube` as an exclusion in Windows Security.

Sonar warns that antivirus scanning of the analysis machine causes unpredictable behaviour, and real-time scanning of the Elasticsearch index directory makes startup crawl. This is not optional on a Windows VM.

### 6. Quiet it down for classroom use

In `C:\sonarqube\conf\sonar.properties`:

```properties
sonar.telemetry.enable=false
sonar.updatecenter.activate=false
```

Leave the database settings alone — the bundled H2 database is intended for evaluation and is right for a single-user lab VM.

### 7. First start

```
C:\sonarqube\bin\windows-x86-64\StartSonar.bat
```

Run as a normal user, **not** elevated. First startup takes several minutes while Elasticsearch builds its index. Do this during image build, never in front of a class.

Browse to `http://localhost:9000`, log in `admin`/`admin`, change the password.

### 8. Install as a service

```
C:\sonarqube\bin\windows-x86-64\SonarService.bat install
C:\sonarqube\bin\windows-x86-64\SonarService.bat start
```

Set it to **Automatic (Delayed Start)** in `services.msc` so it does not compete with IntelliJ during boot.

Stop it with `SonarService.bat stop` rather than killing the process — that performs a graceful shutdown and lets in-progress tasks finish. Killing the wrapper can orphan the Elasticsearch process, which then holds a lock on its index directory and prevents the next start.

### 9. Prepare for the class

- Generate the analysis token students will use, or document how to make one
- Decide MQR vs Standard Experience and set it (see Task 1.3)
- Run both lab scans once, then delete the projects — this warms the analyzer cache and proves the setup
- Pre-populate `~/.m2` by running `mvn clean verify` in both projects
- Verify the service survives a reboot
- Snapshot

### Sizing

SonarQube wants 4 GB RAM and 2 cores minimum, plus 10% free disk. Elasticsearch marks its indices read-only at a 95% disk-usage watermark, which produces confusing failures. With IntelliJ and a browser alongside, specify the VM at **12-16 GB RAM, 4 vCPU, 80 GB SSD-backed**.
