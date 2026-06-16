@req-005 @scn-008 @clinical-guideline
Feature: Clinical Guideline (SCN-008)

  As a medical professional
  I want the system to retrieve clinical guidelines for a condition
  So that I receive published guidelines with strength of recommendation

  # REQ-005: Decision Support — guideline retrieval
  Scenario: SCN-008 Clinical guideline search returns results for a condition
    Given a clinical condition "diabetes"
    When the system searches PubMed for evidence
    Then PubMed articles are returned with title and abstract

  # REQ-005: Decision Support — guideline results have metadata
  Scenario: SCN-008 Clinical guideline results have valid metadata
    Given a clinical condition "diabetes"
    When the system searches PubMed for evidence
    Then the PubMed articles have valid publication years
