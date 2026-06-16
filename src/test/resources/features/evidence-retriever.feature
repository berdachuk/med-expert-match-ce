@req-005 @scn-003 @evidence-retriever
Feature: Evidence Retriever (SCN-003)

  As a medical professional
  I want the system to retrieve clinical evidence for a condition
  So that I receive PubMed articles and local document results with citations

  # REQ-005: Decision Support — evidence retrieval
  Scenario: SCN-003 PubMed search returns articles for a condition
    Given a clinical condition "diabetes"
    When the system searches PubMed for evidence
    Then PubMed articles are returned with title and abstract

  # REQ-005: Decision Support — empty query returns no results
  Scenario: SCN-003 Empty query returns no results
    Given a clinical condition ""
    When the system searches PubMed for evidence
    Then no PubMed articles are returned
