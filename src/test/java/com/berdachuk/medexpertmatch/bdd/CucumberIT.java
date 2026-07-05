package com.berdachuk.medexpertmatch.bdd;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;

/**
 * Cucumber JUnit Platform Suite runner (SCN-001..SCN-009).
 * Discovers .feature files from src/test/resources/features/.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.berdachuk.medexpertmatch.bdd,com.berdachuk.medexpertmatch.bdd.stepdefs")
public class CucumberIT {
}
