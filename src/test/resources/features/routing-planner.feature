@req-006 @scn-007 @routing-planner
Feature: Routing Planner (SCN-007)

  As a medical professional
  I want the system to route a case to the best facility
  So that I receive a facility recommendation with score breakdown

  # REQ-006: Regional Routing — facility routing
  Scenario: SCN-007 Routing returns facility match with score
    Given a medical case for routing requiring "Cardiology" specialty
    And a facility with "ICU" and "CARDIOLOGY" capabilities exists
    When the system routes the case to facilities
    Then the routing results include the facility
    And the routing result has a route score greater than 0

  # REQ-006: Regional Routing — geographic scoring
  Scenario: SCN-007 Geographic proximity affects routing score
    Given a medical case located in "Baltimore, MD"
    And a nearby facility in "Baltimore, MD" exists
    And a distant facility in "Los Angeles, CA" exists
    When the system routes the case to facilities
    Then the nearby facility has a higher geographic score than the distant facility
