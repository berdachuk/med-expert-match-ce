@req-005 @scn-009 @triage
Feature: Triage (SCN-009)

  As a medical professional
  I want the system to triage a new case
  So that an urgency tier (CRITICAL/HIGH/MEDIUM/LOW) is assigned

  # REQ-005: Decision Support — triage critical case
  Scenario: SCN-009 Triage assigns urgency for critical case
    Given a medical case with urgency level "CRITICAL"
    When the system analyzes the case
    Then the urgency classification is not null

  # REQ-005: Decision Support — triage low urgency case
  Scenario: SCN-009 Triage assigns urgency for low urgency case
    Given a medical case with urgency level "LOW"
    When the system analyzes the case
    Then the urgency classification is not null
