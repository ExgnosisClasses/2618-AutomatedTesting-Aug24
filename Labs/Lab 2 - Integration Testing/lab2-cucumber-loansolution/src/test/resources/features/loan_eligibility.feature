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
