package com.berdachuk.medexpertmatch;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Spring Modulith verification test.
 * <p>
 * This test verifies that the application module structure adheres to Spring Modulith constraints.
 * <p>
 * NOTE: This test is intentionally disabled because:
 * 1. The core module contains shared infrastructure intentionally used across all modules
 * 2. Orchestration services (MedicalAgentServiceImpl, MedicalAgentTools) legitimately depend on
 * multiple domain modules to coordinate complex workflows
 * 3. The project architecture uses cross-module dependencies as an intentional design choice
 * for medical expert matching workflows
 * <p>
 * Module dependencies are declared explicitly in package-info.java files for documentation.
 * Cross-module dependencies serve legitimate business coordination needs.
 * <p>
 * To run manually: Remove @Disabled annotation
 */
public class ModulithVerificationIT {

    @Test
    void verifyApplicationModuleStructure() {
        ApplicationModules.of(MedExpertMatchApplication.class).verify();
    }
}
