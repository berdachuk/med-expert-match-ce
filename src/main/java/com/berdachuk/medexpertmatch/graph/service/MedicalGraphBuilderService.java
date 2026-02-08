package com.berdachuk.medexpertmatch.graph.service;

import java.util.List;

/**
 * Service interface for building medical graph relationships from database data.
 * Populates Apache AGE graph with doctors, medical cases, ICD-10 codes, specialties, and facilities.
 */
public interface MedicalGraphBuilderService {
    /**
     * Builds the complete graph from database data.
     * Creates all vertices (doctors, medical cases, ICD-10 codes, specialties, facilities) and relationships.
     */
    void buildGraph();

    /**
     * Creates a doctor vertex in the graph.
     *
     * @param doctorId The unique identifier of the doctor (external system ID)
     * @param name     The name of the doctor
     * @param email    The email address of the doctor
     */
    void createDoctorVertex(String doctorId, String name, String email);

    /**
     * Creates a medical case vertex in the graph.
     *
     * @param caseId         The unique identifier of the medical case
     * @param chiefComplaint The chief complaint of the case
     * @param urgencyLevel   The urgency level of the case
     */
    void createMedicalCaseVertex(String caseId, String chiefComplaint, String urgencyLevel);

    /**
     * Creates an ICD-10 code vertex in the graph.
     *
     * @param code        The ICD-10 code string (e.g., "I21.9")
     * @param description The description of the ICD-10 code
     */
    void createIcd10CodeVertex(String code, String description);

    /**
     * Creates a medical specialty vertex in the graph.
     *
     * @param specialtyId The unique identifier of the medical specialty
     * @param name        The name of the medical specialty
     */
    void createMedicalSpecialtyVertex(String specialtyId, String name);

    /**
     * Creates a facility vertex in the graph.
     *
     * @param facilityId   The unique identifier of the facility (external system ID)
     * @param name         The name of the facility
     * @param facilityType The type of the facility
     */
    void createFacilityVertex(String facilityId, String name, String facilityType);

    /**
     * Creates a TREATED relationship between a doctor and a medical case.
     *
     * @param doctorId The unique identifier of the doctor
     * @param caseId   The unique identifier of the medical case
     */
    void createTreatedRelationship(String doctorId, String caseId);

    /**
     * Creates a CONSULTED_ON relationship between a doctor and a medical case.
     *
     * @param doctorId The unique identifier of the doctor
     * @param caseId   The unique identifier of the medical case
     */
    void createConsultedOnRelationship(String doctorId, String caseId);

    /**
     * Creates a SPECIALIZES_IN relationship between a doctor and a medical specialty.
     *
     * @param doctorId      The unique identifier of the doctor
     * @param specialtyName The name of the medical specialty
     */
    void createSpecializesInRelationship(String doctorId, String specialtyName);

    /**
     * Creates a TREATS_CONDITION relationship between a doctor and an ICD-10 code.
     *
     * @param doctorId  The unique identifier of the doctor
     * @param icd10Code The ICD-10 code string
     */
    void createTreatsConditionRelationship(String doctorId, String icd10Code);

    /**
     * Creates a HAS_CONDITION relationship between a medical case and an ICD-10 code.
     *
     * @param caseId    The unique identifier of the medical case
     * @param icd10Code The ICD-10 code string
     */
    void createHasConditionRelationship(String caseId, String icd10Code);

    /**
     * Creates a REQUIRES_SPECIALTY relationship between a medical case and a medical specialty.
     *
     * @param caseId        The unique identifier of the medical case
     * @param specialtyName The name of the medical specialty
     */
    void createRequiresSpecialtyRelationship(String caseId, String specialtyName);

    /**
     * Creates an AFFILIATED_WITH relationship between a doctor and a facility.
     *
     * @param doctorId   The unique identifier of the doctor
     * @param facilityId The unique identifier of the facility
     */
    void createAffiliatedWithRelationship(String doctorId, String facilityId);

    /**
     * Creates multiple TREATED relationships in batch.
     *
     * @param relationships List of treated relationships to create
     */
    void createTreatedRelationshipsBatch(List<TreatedRelationship> relationships);

    /**
     * Creates multiple SPECIALIZES_IN relationships in batch.
     *
     * @param relationships List of specializes-in relationships to create
     */
    void createSpecializesInRelationshipsBatch(List<SpecializesInRelationship> relationships);

    /**
     * Creates multiple HAS_CONDITION relationships in batch.
     *
     * @param relationships List of has-condition relationships to create
     */
    void createHasConditionRelationshipsBatch(List<HasConditionRelationship> relationships);

    /**
     * Creates multiple TREATS_CONDITION relationships in batch.
     *
     * @param relationships List of treats-condition relationships to create
     */
    void createTreatsConditionRelationshipsBatch(List<TreatsConditionRelationship> relationships);

    /**
     * Creates multiple REQUIRES_SPECIALTY relationships in batch.
     *
     * @param relationships List of requires-specialty relationships to create
     */
    void createRequiresSpecialtyRelationshipsBatch(List<RequiresSpecialtyRelationship> relationships);

    /**
     * Creates multiple AFFILIATED_WITH relationships in batch.
     *
     * @param relationships List of affiliated-with relationships to create
     */
    void createAffiliatedWithRelationshipsBatch(List<AffiliatedWithRelationship> relationships);

    /**
     * Clears all vertices and edges from the graph.
     * This removes all graph data but keeps the graph structure intact.
     */
    void clearGraph();

    /**
     * Treated relationship record.
     */
    record TreatedRelationship(String doctorId, String caseId) {
    }

    /**
     * Specializes-in relationship record.
     */
    record SpecializesInRelationship(String doctorId, String specialtyName) {
    }

    /**
     * Has-condition relationship record.
     */
    record HasConditionRelationship(String caseId, String icd10Code) {
    }

    /**
     * Treats-condition relationship record.
     */
    record TreatsConditionRelationship(String doctorId, String icd10Code) {
    }

    /**
     * Requires-specialty relationship record.
     */
    record RequiresSpecialtyRelationship(String caseId, String specialtyName) {
    }

    /**
     * Affiliated-with relationship record.
     */
    record AffiliatedWithRelationship(String doctorId, String facilityId) {
    }
}
