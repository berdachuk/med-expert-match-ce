package com.berdachuk.medexpertmatch.medicalcase.service;

import com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase;

public interface MedicalCaseDescriptionService {
    String generateDescription(MedicalCase medicalCase);
    String getOrGenerateDescription(MedicalCase medicalCase);
}