package com.berdachuk.medexpertmatch;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * REQ-016: integration coverage for registered requirement.
 */
public class ModulithVerificationIT {

    @Test
    void verifyApplicationModuleStructure() {
        ApplicationModules.of(MedExpertMatchApplication.class).verify();
    }
}
