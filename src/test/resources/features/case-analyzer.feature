@req-005 @scn-001 @case-analyzer
Feature: Case Analysis (SCN-001)

  As a medical professional
  I want the system to analyze a medical case narrative
  So that I receive extracted entities, ICD-10 codes, and an urgency classification

  # REQ-005: Decision Support — case analysis with entity extraction and ICD-10 coding
  Scenario: SCN-001 Base case analysis returns entities and urgency tier
    Given a medical case with chief complaint "Chest pain" and diagnosis "Acute myocardial infarction"
    When the system analyzes the case
    Then the analysis result contains extracted entities
    And the analysis result contains ICD-10 codes
    And the analysis result contains an urgency classification

  # REQ-005: Decision Support — triage classification
  Scenario: SCN-009 Triage classifies urgency by case severity
    Given a medical case with urgency level "CRITICAL"
    When the system analyzes the case
    Then the urgency classification is not null
