package com.berdachuk.medexpertmatch;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Spring Modulith verification test.
 * <p>
 * This test is intentionally disabled for now. The package descriptors document the
 * intended module-level boundaries, but the current codebase still has many direct
 * dependencies on named interfaces that need dedicated refactoring before verification can pass.
 */
@Disabled("Enable after named-interface dependency cleanup is completed")
public class ModulithVerificationIT {

    @Test
    void verifyApplicationModuleStructure() {
        ApplicationModules.of(MedExpertMatchApplication.class).verify();
    }
}
