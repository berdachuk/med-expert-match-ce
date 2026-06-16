@req-005 @scn-005 @clinical-advisor
Feature: Clinical Advisor (SCN-005)

  As a medical professional
  I want the system to provide clinical advice for a case
  So that I receive a differential diagnosis and risk assessment

  # REQ-005: Decision Support — clinical experience data
  Scenario: SCN-005 Clinical advisor retrieves historical experience
    Given a medical case with chief complaint "Chest pain" and diagnosis "Acute myocardial infarction"
    And a doctor with clinical experience for the case exists
    When the system retrieves clinical experience for the doctor
    Then the clinical experience contains outcome and rating data

  # REQ-005: Decision Support — risk assessment data
  Scenario: SCN-005 Clinical advisor retrieves risk assessment data
    Given a medical case with chief complaint "Chest pain" and diagnosis "Acute myocardial infarction"
    And a doctor with clinical experience for the case exists
    When the system retrieves clinical experience for the doctor
    Then the clinical experience contains procedure data
