# Banking Vulnerable Lab -- SAST Triage Project

A small Spring Boot application with deliberately introduced security
vulnerabilities: hardcoded credentials, weak cryptography, weak randomness,
command injection, path traversal, and SQL injection.

One finding is deliberate **false-positive bait**, so you can practise
classifying rather than just fixing.

Used in **Lab 4.1, Parts 3 to 5**.

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

The project key, project name and server URL are already set in `pom.xml`.

Results appear at `http://localhost:9000` under **Banking Vulnerable Lab**.

## Files of note

| File | What to look at |
|---|---|
| `service/AdminService.java` | how the admin password is declared; the shell command; the empty catch block |
| `util/CryptoUtil.java` | the hashing algorithm, the token generator, the cipher mode, and `TEST_FIXTURE_KEY` |
| `service/AccountRepository.java` | two query methods -- one is vulnerable, one is not. Work out which. |
| `service/DocumentService.java` | how the file path is built |

## Note

This application is intentionally insecure. Do not deploy it, do not copy code
from it into anything real, and do not expose it to a network.
