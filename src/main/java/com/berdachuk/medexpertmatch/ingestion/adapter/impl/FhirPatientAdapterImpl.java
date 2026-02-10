package com.berdachuk.medexpertmatch.ingestion.adapter.impl;

import com.berdachuk.medexpertmatch.ingestion.adapter.FhirPatientAdapter;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r5.model.Patient;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.Date;

/**
 * Implementation of FhirPatientAdapter.
 * Extracts anonymized patient data from FHIR Patient resources.
 */
@Slf4j
@Component
public class FhirPatientAdapterImpl implements FhirPatientAdapter {

    @Override
    public Integer extractAge(Patient patient) {
        if (patient == null) {
            return null;
        }

        // Extract birth date
        if (!patient.hasBirthDate()) {
            return null;
        }

        Date birthDate = patient.getBirthDate();
        if (birthDate == null) {
            return null;
        }

        // Calculate age from birth date
        LocalDate birthLocalDate = birthDate.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
        LocalDate now = LocalDate.now();
        Period period = Period.between(birthLocalDate, now);
        int years = period.getYears();

        // Reject invalid ages (negative = future birth date; > 120 = likely data error)
        if (years < 0 || years > 120) {
            log.warn("Invalid patient age from birth date: {} (birth: {}, now: {}). Returning null.", years, birthLocalDate, now);
            return null;
        }
        return years;
    }

    @Override
    public boolean isAnonymized(Patient patient) {
        if (patient == null) {
            return false;
        }

        // Check for PHI indicators
        // Patient should not have identifiers, names, addresses, or contact information
        boolean hasIdentifiers = patient.hasIdentifier() && !patient.getIdentifier().isEmpty();
        boolean hasNames = patient.hasName() && !patient.getName().isEmpty();
        boolean hasAddresses = patient.hasAddress() && !patient.getAddress().isEmpty();
        boolean hasTelecom = patient.hasTelecom() && !patient.getTelecom().isEmpty();

        // If any PHI is present, consider it not anonymized
        if (hasIdentifiers || hasNames || hasAddresses || hasTelecom) {
            log.warn("Patient resource contains PHI - identifiers: {}, names: {}, addresses: {}, telecom: {}",
                    hasIdentifiers, hasNames, hasAddresses, hasTelecom);
            return false;
        }

        return true;
    }
}
