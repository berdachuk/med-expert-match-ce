@req-001 @scn-002 @doctor-matcher
Feature: Doctor Matching (SCN-002)

  As a medical professional
  I want the system to match a case to the best specialists
  So that I receive a ranked list of specialists with score breakdown

  # REQ-001: Specialist Matching — specialty filtering
  Scenario: SCN-002 Matching filters by specialty
    Given a medical case requiring "Cardiology" specialty
    And a cardiologist doctor exists
    And a neurologist doctor exists
    When the system matches doctors to the case
    Then the match results include the cardiologist
    And the match results exclude the neurologist

  # REQ-001: Specialist Matching — score breakdown
  Scenario: SCN-002 Matching returns score breakdown
    Given a medical case requiring "Cardiology" specialty
    And a doctor with cardiology specialty exists
    When the system matches doctors to the case
    Then each match result has a score greater than 0
    And each match result has a rank starting from 1
