Feature: Loan eligibility

  As a lending officer
  I want applications assessed against our eligibility rules
  So that every applicant receives a consistent, explainable decision

  An applicant qualifies for a $5,000 loan only when all three of these
  are true. If more than one fails, the first failure listed wins.

    1. Annual income is at least $50,000
    2. Credit score is at least 750
    3. Continuous employment is at least 180 days

  # ---------------------------------------------------------------
  # TODO 1.1 -- Scenario: an applicant who meets every requirement
  #
  #   income 50000, credit score 750, 180 days employed
  #   expected decision: "Approved for $5,000"
  # ---------------------------------------------------------------


  # ---------------------------------------------------------------
  # TODO 2.1 -- Scenario: income below the threshold
  #
  #   income 49999, credit score 750, 180 days employed
  #   expected decision: "Rejected: Income too low"
  # ---------------------------------------------------------------


  # ---------------------------------------------------------------
  # TODO 2.2 -- Scenario: credit score below the threshold
  #
  #   income 50000, credit score 749, 180 days employed
  #   expected decision: "Rejected: Credit score too low"
  # ---------------------------------------------------------------


  # ---------------------------------------------------------------
  # TODO 2.3 -- Scenario: employed for too short a time
  #
  #   income 50000, credit score 750, 179 days employed
  #   expected decision: "Rejected: Not employed long enough"
  # ---------------------------------------------------------------


  # ---------------------------------------------------------------
  # TODO 3.1 -- Scenario Outline covering all four cases,
  #             plus TODO 3.2 -- a second Examples block for
  #             values comfortably inside the thresholds
  # ---------------------------------------------------------------
