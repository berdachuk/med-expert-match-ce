package com.berdachuk.medexpertmatch.bdd;

import com.berdachuk.medexpertmatch.integration.BaseIntegrationTest;
import io.cucumber.spring.CucumberContextConfiguration;

/**
 * Spring Boot + Cucumber context configuration.
 * Extends BaseIntegrationTest to reuse the Testcontainers PostgreSQL setup.
 */
@CucumberContextConfiguration
public class CucumberSpringConfiguration extends BaseIntegrationTest {
}
