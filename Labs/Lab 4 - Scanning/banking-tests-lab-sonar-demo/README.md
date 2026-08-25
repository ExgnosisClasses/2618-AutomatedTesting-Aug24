# Banking Tests Lab -- SonarQube Demo Project

A deliberately flawed Spring Boot project for practising how to **read** a
SonarQube report.

**This project is not meant to run.** It is meant to be scanned. The code
compiles cleanly so SonarQube can perform deep analysis, but several classes
will throw exceptions or behave incorrectly if you actually start the
application. The exercise is to scan it, then read the dashboard.

Used in **Lab 4.1, Part 2**.

## Prerequisites

- JDK 21 on your PATH
- Maven
- SonarQube running locally at `http://localhost:9000`
  (installed on the lab VM as a Windows service -- no Docker)
- A SonarQube analysis token

## How to scan

From this folder:

```
mvn clean verify sonar:sonar -Dsonar.token=YOUR_TOKEN_HERE
```

`clean verify` compiles the code, runs the single test we ship
(`AuditServiceTest`), and writes a JaCoCo coverage report to
`target/site/jacoco/`. `sonar:sonar` then sends the source, the bytecode and
the coverage report to your local SonarQube server.

The project key, project name and server URL are already set in `pom.xml`, so
the token is the only thing you supply.

When the scan finishes, open `http://localhost:9000` and find the
**Banking Tests Lab (Sonar Demo)** project.

## What you should see

A lot.

| Category | Approximate count |
|---|---|
| Vulnerabilities | 12-18 |
| Security Hotspots | 12-18 |
| Bugs | 8-12 |
| Code Smells | 25-35 |
| Coverage | below 10% |

Your job in Part 2 is not to fix anything. It is to navigate the dashboard,
drill into findings, and learn how SonarQube categorises and explains them.

## Files of note

- `src/main/java/com/example/banking/service/AuditService.java` is the
  **clean reference**. Everything else has planted issues. Compare the others
  against this one.
- `src/test/java/com/example/banking/service/AuditServiceTest.java` is the
  only test in the project. It exists so the coverage metric is *partial*
  rather than zero -- which turns out to be the more interesting case.

## A note on editions

SonarQube Community Build reports pattern-based findings. Developer Edition and
above add taint analysis, which catches data-flow vulnerabilities (SQL
injection, path traversal, XSS) that Community cannot follow end to end. Your
instructor's answer key flags which findings depend on which edition.

No edition of SonarQube reports third-party dependency CVEs out of the box.
This project deliberately depends on `commons-collections 3.2.1`, which has
well-known deserialization CVEs, so you can see that gap for yourself.
