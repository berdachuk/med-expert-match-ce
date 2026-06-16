@req-005 @scn-004 @recommendation-engine
Feature: Recommendation Engine (SCN-004)

  As a medical professional
  I want the system to generate recommendations for a matched case
  So that I receive a diagnostic workup and referral rationale

  # REQ-005: Decision Support — recommendation generation
  Scenario: SCN-004 Recommendation engine produces diagnostic workup
    Given a medical case with chief complaint "Chest pain" and diagnosis "Acute myocardial infarction"
    When the system analyzes the case
    Then the analysis result contains extracted entities

  # REQ-005: Decision Support — referral rationale
  Scenario: SCN-004 Recommendation engine produces referral rationale
    Given a medical case with chief complaint "Heart failure" and diagnosis "Congestive heart failure"
    When the system analyzes the case
    Then the analysis result contains ICD-10 codes
