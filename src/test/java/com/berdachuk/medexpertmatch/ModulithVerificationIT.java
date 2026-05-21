package com.berdachuk.medexpertmatch;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

public class ModulithVerificationIT {

    @Test
    void verifyApplicationModuleStructure() {
        ApplicationModules.of(MedExpertMatchApplication.class).verify();
    }
}
