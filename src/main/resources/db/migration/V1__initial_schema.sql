-- MedExpertMatch Database Schema
-- Version 1.0.0 (MVP - Consolidated)
-- Consolidated schema with external system ID support
-- External system IDs: supports UUID strings, 19-digit numeric strings, or other formats (VARCHAR(74))
-- Examples: "550e8400-e29b-41d4-a716-446655440000" (UUID), "8760000000000420950" (19-digit numeric)
-- Internal IDs (medical cases, clinical experiences) use MongoDB-compatible 24-character hex strings (CHAR(24))
-- Includes: core tables, graph schema, functions, and triggers

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS vector;

-- Enable pg_trgm extension for fuzzy text matching (similarity search)
-- Used for doctor name similarity search to handle typos and variations
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Try to enable AGE extension (optional - may not be available in all environments)
DO $$
BEGIN
    CREATE EXTENSION IF NOT EXISTS age;
EXCEPTION
    WHEN OTHERS THEN
        -- AGE extension not available, continue without it
        NULL;
END $$;

-- Set default schema
SET search_path = medexpertmatch, public;

-- Try to load and configure Apache AGE (if available)
DO $$
BEGIN
    LOAD 'age';
    SET search_path = ag_catalog, "$user", public, medexpertmatch;
EXCEPTION
    WHEN OTHERS THEN
        -- AGE not available, continue with medexpertmatch schema
        SET search_path = medexpertmatch, public;
END $$;

-- ============================================
-- Core Tables
-- ============================================

-- Doctor table (external system IDs - VARCHAR(74))
-- External system IDs: supports UUID strings, 19-digit numeric strings, or other formats
-- Examples: "550e8400-e29b-41d4-a716-446655440000" (UUID), "8760000000000420950" (19-digit numeric)
CREATE TABLE medexpertmatch.doctors (
    id VARCHAR(74) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE,
    specialties TEXT[], -- Array of medical specialties
    certifications TEXT[], -- Array of board certifications
    facility_ids TEXT[], -- Array of facility IDs (external system IDs)
    telehealth_enabled BOOLEAN DEFAULT FALSE,
    availability_status VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    metadata JSONB
);

CREATE INDEX doctors_email_idx ON medexpertmatch.doctors(email);
CREATE INDEX doctors_specialties_idx ON medexpertmatch.doctors USING GIN(specialties);
CREATE INDEX doctors_telehealth_enabled_idx ON medexpertmatch.doctors(telehealth_enabled);
CREATE INDEX doctors_availability_status_idx ON medexpertmatch.doctors(availability_status);

-- Medical Case table (internal IDs - CHAR(24))
-- Internal MongoDB-compatible IDs for medical cases
CREATE TABLE medexpertmatch.medical_cases (
    id CHAR(24) PRIMARY KEY,
    patient_age INT, -- Anonymized patient age
    chief_complaint TEXT,
    symptoms TEXT,
    current_diagnosis VARCHAR(255),
    icd10_codes TEXT[], -- Array of ICD-10 codes
    snomed_codes TEXT[], -- Array of SNOMED codes
    urgency_level VARCHAR(20) NOT NULL CHECK (urgency_level IN ('CRITICAL', 'HIGH', 'MEDIUM', 'LOW')),
    required_specialty VARCHAR(100),
    case_type VARCHAR(50) NOT NULL CHECK (case_type IN ('INPATIENT', 'SECOND_OPINION', 'CONSULT_REQUEST')),
    additional_notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Comprehensive medical case abstract (LLM-enhanced or simple concatenation)
    abstract TEXT,
    -- Vector embedding for semantic search (1536 dimensions for MedGemma)
    embedding vector(1536),
    -- Track the actual embedding dimension used
    embedding_dimension INT,
    metadata JSONB
);

CREATE INDEX medical_cases_urgency_level_idx ON medexpertmatch.medical_cases(urgency_level);
CREATE INDEX medical_cases_case_type_idx ON medexpertmatch.medical_cases(case_type);
CREATE INDEX medical_cases_required_specialty_idx ON medexpertmatch.medical_cases(required_specialty);
CREATE INDEX medical_cases_icd10_codes_idx ON medexpertmatch.medical_cases USING GIN(icd10_codes);
CREATE INDEX medical_cases_snomed_codes_idx ON medexpertmatch.medical_cases USING GIN(snomed_codes);
-- HNSW index for vector similarity search (1536 dimensions)
CREATE INDEX medical_cases_embedding_idx ON medexpertmatch.medical_cases 
    USING hnsw (embedding vector_cosine_ops) WITH (m = 16, ef_construction = 64);
CREATE INDEX medical_cases_embedding_dimension_idx ON medexpertmatch.medical_cases(embedding_dimension);
CREATE INDEX medical_cases_created_at_idx ON medexpertmatch.medical_cases(created_at DESC);
-- Full-text search index for abstract (for potential future use)
CREATE INDEX medical_cases_abstract_idx ON medexpertmatch.medical_cases 
    USING GIN(to_tsvector('english', abstract));

-- Clinical Experience table (internal IDs - CHAR(24))
-- Links doctors to cases with outcomes and metrics
-- doctor_id: External system ID (VARCHAR(74)) - supports UUID, 19-digit numeric, or other formats
-- case_id: Internal ID (CHAR(24))
CREATE TABLE medexpertmatch.clinical_experiences (
    id CHAR(24) PRIMARY KEY,
    doctor_id VARCHAR(74) NOT NULL REFERENCES medexpertmatch.doctors(id) ON DELETE CASCADE,
    case_id CHAR(24) NOT NULL REFERENCES medexpertmatch.medical_cases(id) ON DELETE CASCADE,
    procedures_performed TEXT[],
    complexity_level VARCHAR(20) CHECK (complexity_level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    outcome VARCHAR(50), -- 'SUCCESS', 'IMPROVED', 'STABLE', 'COMPLICATED', etc.
    complications TEXT[],
    time_to_resolution INT, -- Days
    rating INT CHECK (rating >= 1 AND rating <= 5),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    metadata JSONB
);

CREATE INDEX clinical_experiences_doctor_id_idx ON medexpertmatch.clinical_experiences(doctor_id);
CREATE INDEX clinical_experiences_case_id_idx ON medexpertmatch.clinical_experiences(case_id);
CREATE INDEX clinical_experiences_outcome_idx ON medexpertmatch.clinical_experiences(outcome);
CREATE INDEX clinical_experiences_complexity_level_idx ON medexpertmatch.clinical_experiences(complexity_level);
CREATE INDEX clinical_experiences_doctor_case_idx ON medexpertmatch.clinical_experiences(doctor_id, case_id);

-- ICD-10 Code table (internal IDs - CHAR(24))
-- ICD-10 code hierarchy and descriptions
CREATE TABLE medexpertmatch.icd10_codes (
    id CHAR(24) PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE, -- e.g., "I21.9"
    description TEXT NOT NULL,
    category VARCHAR(100), -- e.g., "Diseases of the circulatory system"
    parent_code VARCHAR(20), -- Parent code in hierarchy
    related_codes TEXT[], -- Array of related ICD-10 codes
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX icd10_codes_code_idx ON medexpertmatch.icd10_codes(code);
CREATE INDEX icd10_codes_category_idx ON medexpertmatch.icd10_codes(category);
CREATE INDEX icd10_codes_parent_code_idx ON medexpertmatch.icd10_codes(parent_code);

-- Medical Specialty table (internal IDs - CHAR(24))
-- Medical specialty definitions and ICD-10 code ranges
CREATE TABLE medexpertmatch.medical_specialties (
    id CHAR(24) PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    normalized_name VARCHAR(255) NOT NULL, -- Normalized for matching
    description TEXT,
    icd10_code_ranges TEXT[], -- Array of ICD-10 code ranges (e.g., ["I00-I99", "E00-E90"])
    related_specialties TEXT[], -- Array of related specialty IDs
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX medical_specialties_name_idx ON medexpertmatch.medical_specialties(name);
CREATE INDEX medical_specialties_normalized_name_idx ON medexpertmatch.medical_specialties(normalized_name);

-- Medical Procedure table (internal IDs - CHAR(24))
-- Medical procedures, tests, and interventions
CREATE TABLE medexpertmatch.procedures (
    id CHAR(24) PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    normalized_name VARCHAR(255) NOT NULL, -- Normalized for matching
    description TEXT,
    category VARCHAR(100), -- e.g., "Diagnostic", "Therapeutic", "Surgical"
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX procedures_name_idx ON medexpertmatch.procedures(name);
CREATE INDEX procedures_normalized_name_idx ON medexpertmatch.procedures(normalized_name);
CREATE INDEX procedures_category_idx ON medexpertmatch.procedures(category);

-- Facility table (external system IDs - VARCHAR(74))
-- Healthcare facilities and organizations
-- External system IDs: supports UUID strings, 19-digit numeric strings, or other formats
CREATE TABLE medexpertmatch.facilities (
    id VARCHAR(74) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    facility_type VARCHAR(100), -- 'ACADEMIC', 'COMMUNITY', 'SPECIALTY_CENTER', etc.
    location_city VARCHAR(100),
    location_state VARCHAR(100),
    location_country VARCHAR(100),
    location_latitude DECIMAL(10, 8),
    location_longitude DECIMAL(11, 8),
    capabilities TEXT[], -- Array of capabilities (e.g., 'PCI', 'ECMO', 'ICU', 'SURGERY')
    capacity INT, -- Total capacity
    current_occupancy INT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    metadata JSONB
);

CREATE INDEX facilities_name_idx ON medexpertmatch.facilities(name);
CREATE INDEX facilities_facility_type_idx ON medexpertmatch.facilities(facility_type);
CREATE INDEX facilities_location_city_idx ON medexpertmatch.facilities(location_city);
CREATE INDEX facilities_capabilities_idx ON medexpertmatch.facilities USING GIN(capabilities);

-- Consultation Match table (internal IDs - CHAR(24))
-- Stores consultation matching results
-- doctor_id: External system ID (VARCHAR(74)) - supports UUID, 19-digit numeric, or other formats
CREATE TABLE medexpertmatch.consultation_matches (
    id CHAR(24) PRIMARY KEY,
    case_id CHAR(24) NOT NULL REFERENCES medexpertmatch.medical_cases(id) ON DELETE CASCADE,
    doctor_id VARCHAR(74) NOT NULL REFERENCES medexpertmatch.doctors(id) ON DELETE CASCADE,
    match_score DECIMAL(5, 2) NOT NULL CHECK (match_score >= 0 AND match_score <= 100),
    match_rationale TEXT,
    rank INT NOT NULL, -- Rank in the match results (1 = best match)
    requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_at TIMESTAMP,
    status VARCHAR(50) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'COMPLETED')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    metadata JSONB
);

CREATE INDEX consultation_matches_case_id_idx ON medexpertmatch.consultation_matches(case_id);
CREATE INDEX consultation_matches_doctor_id_idx ON medexpertmatch.consultation_matches(doctor_id);
CREATE INDEX consultation_matches_status_idx ON medexpertmatch.consultation_matches(status);
CREATE INDEX consultation_matches_case_rank_idx ON medexpertmatch.consultation_matches(case_id, rank);
CREATE INDEX consultation_matches_match_score_idx ON medexpertmatch.consultation_matches(match_score DESC);

-- ============================================
-- Graph Schema (Apache AGE)
-- ============================================

-- Create graph for medical relationships (if AGE is available)
DO $$
BEGIN
    SELECT create_graph('medexpertmatch_graph');
EXCEPTION
    WHEN OTHERS THEN
        -- AGE not available or graph already exists, continue
        NULL;
END $$;

-- Graph structure (created via application code using Cypher queries):
-- - Doctor vertices (from doctors table)
-- - MedicalCase vertices (from medical_cases table)
-- - ICD10Code vertices (from icd10_codes table)
-- - MedicalSpecialty vertices (from medical_specialties table)
-- - Facility vertices (from facilities table)
-- - Relationships:
--   - (Doctor)-[:TREATED]->(MedicalCase)
--   - (Doctor)-[:SPECIALIZES_IN]->(MedicalSpecialty)
--   - (Doctor)-[:TREATS_CONDITION]->(ICD10Code)
--   - (MedicalCase)-[:HAS_CONDITION]->(ICD10Code)
--   - (MedicalCase)-[:REQUIRES_SPECIALTY]->(MedicalSpecialty)
--   - (MedicalCase)-[:AT_FACILITY]->(Facility)
--   - (Doctor)-[:AFFILIATED_WITH]->(Facility)
--   - (Doctor)-[:CONSULTED_ON]->(MedicalCase)

-- ============================================
-- Functions
-- ============================================

-- Function to generate MongoDB-compatible ID (for internal IDs)
CREATE OR REPLACE FUNCTION medexpertmatch.generate_objectid() 
RETURNS CHAR(24) AS $$
DECLARE
    chars TEXT := '0123456789abcdef';
    result CHAR(24) := '';
    i INT;
BEGIN
    FOR i IN 1..24 LOOP
        result := result || substr(chars, floor(random() * 16)::int + 1, 1);
    END LOOP;
    RETURN result;
END;
$$ LANGUAGE plpgsql;

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION medexpertmatch.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Triggers for updated_at
CREATE TRIGGER update_doctors_updated_at BEFORE UPDATE ON medexpertmatch.doctors
    FOR EACH ROW EXECUTE FUNCTION medexpertmatch.update_updated_at_column();

CREATE TRIGGER update_medical_cases_updated_at BEFORE UPDATE ON medexpertmatch.medical_cases
    FOR EACH ROW EXECUTE FUNCTION medexpertmatch.update_updated_at_column();

CREATE TRIGGER update_clinical_experiences_updated_at BEFORE UPDATE ON medexpertmatch.clinical_experiences
    FOR EACH ROW EXECUTE FUNCTION medexpertmatch.update_updated_at_column();

CREATE TRIGGER update_facilities_updated_at BEFORE UPDATE ON medexpertmatch.facilities
    FOR EACH ROW EXECUTE FUNCTION medexpertmatch.update_updated_at_column();

CREATE TRIGGER update_consultation_matches_updated_at BEFORE UPDATE ON medexpertmatch.consultation_matches
    FOR EACH ROW EXECUTE FUNCTION medexpertmatch.update_updated_at_column();
