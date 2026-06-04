-- MedExpertMatch Database Schema
-- Version 1.0.0 (MVP - Consolidated)
-- Consolidated schema with external system ID support
-- External system IDs: supports UUID strings, 19-digit numeric strings, or other formats (VARCHAR(74))
-- Examples: "550e8400-e29b-41d4-a716-446655440000" (UUID), "8760000000000420950" (19-digit numeric)
-- Internal IDs (medical cases, clinical experiences) use MongoDB-compatible 24-character hex strings (CHAR(24))
-- Includes: core tables, graph schema, AI session/chat tables, functions, and triggers

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
    location_latitude DECIMAL(10, 8),
    location_longitude DECIMAL(11, 8),
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

-- ============================================
-- AI Session Tables (Spring AI Session JDBC)
-- ============================================

CREATE TABLE IF NOT EXISTS medexpertmatch.ai_session (
    id            VARCHAR(255)  NOT NULL PRIMARY KEY,
    user_id       VARCHAR(255)  NOT NULL,
    created_at    TIMESTAMP     NOT NULL,
    expires_at    TIMESTAMP,
    metadata      TEXT,
    event_version BIGINT        NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_ai_session_user_id
    ON medexpertmatch.ai_session (user_id);

CREATE INDEX IF NOT EXISTS idx_ai_session_expires_at
    ON medexpertmatch.ai_session (expires_at);

CREATE TABLE IF NOT EXISTS medexpertmatch.ai_session_event (
    id              VARCHAR(255)  NOT NULL PRIMARY KEY,
    session_id      VARCHAR(255)  NOT NULL,
    "timestamp"     TIMESTAMP     NOT NULL,
    message_type    VARCHAR(20)   NOT NULL,
    message_content TEXT,
    message_data    TEXT,
    synthetic       BOOLEAN       NOT NULL DEFAULT FALSE,
    branch          VARCHAR(500),
    metadata        TEXT,
    CONSTRAINT fk_ai_session_event_session
        FOREIGN KEY (session_id) REFERENCES medexpertmatch.ai_session (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_ai_session_event_session_ts
    ON medexpertmatch.ai_session_event (session_id, "timestamp");

-- ============================================
-- API Session Tokens and Audit Log
-- ============================================

CREATE TABLE IF NOT EXISTS medexpertmatch.api_session_token (
    id CHAR(24) PRIMARY KEY CHECK (id ~ '^[0-9a-fA-F]{24}$'),
    api_key VARCHAR(64) NOT NULL UNIQUE,
    description VARCHAR(255),
    rate_limit_tier VARCHAR(20) NOT NULL DEFAULT 'default' CHECK (rate_limit_tier IN ('default', 'high', 'unlimited')),
    expires_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_used_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS medexpertmatch.audit_log (
    id CHAR(24) PRIMARY KEY CHECK (id ~ '^[0-9a-fA-F]{24}$'),
    action VARCHAR(50) NOT NULL,
    resource_type VARCHAR(50) NOT NULL,
    resource_id VARCHAR(255),
    actor VARCHAR(255),
    details JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_audit_log_action ON medexpertmatch.audit_log (action);
CREATE INDEX IF NOT EXISTS idx_audit_log_resource_type ON medexpertmatch.audit_log (resource_type);
CREATE INDEX IF NOT EXISTS idx_audit_log_created_at ON medexpertmatch.audit_log (created_at);

-- ============================================
-- AI Chat Sessions and Message History (M13)
-- ============================================

CREATE TABLE IF NOT EXISTS medexpertmatch.chat (
    id CHAR(24) PRIMARY KEY CHECK (id ~ '^[0-9a-fA-F]{24}$'),
    user_id VARCHAR(255) NOT NULL,
    name VARCHAR(255),
    agent_id VARCHAR(50) NOT NULL DEFAULT 'auto',
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_activity_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    message_count INT DEFAULT 0,
    metadata JSONB
);

CREATE UNIQUE INDEX IF NOT EXISTS chat_user_default_unique
    ON medexpertmatch.chat (user_id, is_default) WHERE is_default = TRUE;
CREATE INDEX IF NOT EXISTS chat_user_id_idx ON medexpertmatch.chat (user_id);
CREATE INDEX IF NOT EXISTS chat_last_activity_at_idx ON medexpertmatch.chat (last_activity_at DESC);

CREATE TABLE IF NOT EXISTS medexpertmatch.chat_message (
    id CHAR(24) PRIMARY KEY CHECK (id ~ '^[0-9a-fA-F]{24}$'),
    chat_id CHAR(24) NOT NULL REFERENCES medexpertmatch.chat (id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL CHECK (role IN ('user', 'assistant', 'system')),
    content TEXT NOT NULL,
    sequence_number INT NOT NULL,
    tokens_used INT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    metadata JSONB,
    deleted_at TIMESTAMP,
    CONSTRAINT chat_message_chat_sequence_unique UNIQUE (chat_id, sequence_number)
);

CREATE INDEX IF NOT EXISTS chat_message_chat_id_idx ON medexpertmatch.chat_message (chat_id);
CREATE INDEX IF NOT EXISTS chat_message_chat_id_sequence_idx ON medexpertmatch.chat_message (chat_id, sequence_number);

-- ============================================
-- Evaluation Tables
-- ============================================

CREATE TABLE IF NOT EXISTS medexpertmatch.evaluation_dataset (
    id CHAR(24) PRIMARY KEY CHECK (id ~ '^[0-9a-fA-F]{24}$'),
    name VARCHAR(255) NOT NULL UNIQUE,
    version VARCHAR(50),
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS medexpertmatch.evaluation_case (
    id CHAR(24) PRIMARY KEY CHECK (id ~ '^[0-9a-fA-F]{24}$'),
    dataset_id CHAR(24) NOT NULL REFERENCES medexpertmatch.evaluation_dataset(id) ON DELETE CASCADE,
    question TEXT NOT NULL,
    ground_truth_answer TEXT NOT NULL,
    meta_json JSONB
);

CREATE TABLE IF NOT EXISTS medexpertmatch.evaluation_run (
    id CHAR(24) PRIMARY KEY CHECK (id ~ '^[0-9a-fA-F]{24}$'),
    dataset_id CHAR(24) NOT NULL REFERENCES medexpertmatch.evaluation_dataset(id),
    normalized_accuracy DOUBLE PRECISION,
    mean_semantic_similarity DOUBLE PRECISION,
    semantic_accuracy_at_threshold DOUBLE PRECISION,
    config JSONB
);

CREATE TABLE IF NOT EXISTS medexpertmatch.evaluation_result (
    id CHAR(24) PRIMARY KEY CHECK (id ~ '^[0-9a-fA-F]{24}$'),
    run_id CHAR(24) NOT NULL REFERENCES medexpertmatch.evaluation_run(id) ON DELETE CASCADE,
    case_id CHAR(24) NOT NULL REFERENCES medexpertmatch.evaluation_case(id),
    predicted_answer TEXT,
    exact_match BOOLEAN NOT NULL DEFAULT FALSE,
    normalized_match BOOLEAN NOT NULL DEFAULT FALSE,
    semantic_similarity DOUBLE PRECISION,
    semantic_pass BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_evaluation_case_dataset_id ON medexpertmatch.evaluation_case (dataset_id);
CREATE INDEX IF NOT EXISTS idx_evaluation_result_run_id ON medexpertmatch.evaluation_result (run_id);
CREATE INDEX IF NOT EXISTS idx_evaluation_run_dataset_id ON medexpertmatch.evaluation_run (dataset_id);

-- ============================================
-- LLM Harness Plan Artefacts (M30)
-- ============================================

CREATE TABLE IF NOT EXISTS medexpertmatch.llm_agent_plan_artefact (
    session_id VARCHAR(128) PRIMARY KEY,
    workflow_type VARCHAR(32) NOT NULL,
    case_id VARCHAR(24),
    steps_json JSONB NOT NULL,
    acceptance_criteria_json JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_llm_agent_plan_case_id ON medexpertmatch.llm_agent_plan_artefact (case_id);

-- ============================================
-- LLM Harness Workflow Runs (M31)
-- ============================================

CREATE TABLE IF NOT EXISTS medexpertmatch.llm_harness_workflow_run (
    run_id VARCHAR(36) PRIMARY KEY,
    session_id VARCHAR(128) NOT NULL,
    case_id VARCHAR(24),
    workflow_type VARCHAR(32) NOT NULL,
    state VARCHAR(32) NOT NULL,
    resume_token VARCHAR(64) NOT NULL,
    payload_json JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_llm_harness_workflow_run_session ON medexpertmatch.llm_harness_workflow_run (session_id);

-- ============================================
-- LLM Harness Chain Events (M33)
-- ============================================

CREATE TABLE IF NOT EXISTS medexpertmatch.llm_harness_chain_event (
    id VARCHAR(36) PRIMARY KEY,
    chain_root_session_id VARCHAR(128) NOT NULL,
    session_id VARCHAR(128) NOT NULL,
    case_id VARCHAR(24),
    step VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_llm_harness_chain_root ON medexpertmatch.llm_harness_chain_event (chain_root_session_id);

-- ============================================
-- Chat Goal Context (M36 — cross-device conversation continuity)
-- ============================================

CREATE TABLE IF NOT EXISTS medexpertmatch.chat_goal_context (
    session_id    VARCHAR(255) PRIMARY KEY,
    goal_type     VARCHAR(64) NOT NULL,
    case_id       VARCHAR(32),
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- ============================================
-- Document Ingestion Tables
-- ============================================

CREATE TABLE IF NOT EXISTS medexpertmatch.source_document (
    id CHAR(24) PRIMARY KEY CHECK (id ~ '^[0-9a-fA-F]{24}$'),
    external_id VARCHAR(255),
    title VARCHAR(500),
    category VARCHAR(100),
    source_name VARCHAR(255),
    source_url VARCHAR(1000),
    content TEXT NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    source_format VARCHAR(20),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_source_document_content_hash UNIQUE (content_hash)
);

CREATE INDEX IF NOT EXISTS idx_source_document_category ON medexpertmatch.source_document (category);
CREATE INDEX IF NOT EXISTS idx_source_document_created_at ON medexpertmatch.source_document (created_at);

CREATE TABLE IF NOT EXISTS medexpertmatch.document_chunk (
    id CHAR(24) PRIMARY KEY CHECK (id ~ '^[0-9a-fA-F]{24}$'),
    document_id CHAR(24) NOT NULL REFERENCES medexpertmatch.source_document(id) ON DELETE CASCADE,
    chunk_index INT NOT NULL,
    chunk_text TEXT NOT NULL,
    embedding vector(768),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_document_chunk_document_id ON medexpertmatch.document_chunk (document_id);
CREATE INDEX IF NOT EXISTS idx_document_chunk_embedding ON medexpertmatch.document_chunk
    USING hnsw (embedding vector_cosine_ops) WITH (m = 16, ef_construction = 64);

CREATE TABLE IF NOT EXISTS medexpertmatch.ingestion_job (
    id CHAR(24) PRIMARY KEY CHECK (id ~ '^[0-9a-fA-F]{24}$'),
    status VARCHAR(20) NOT NULL DEFAULT 'RUNNING' CHECK (status IN ('RUNNING', 'COMPLETED', 'FAILED')),
    documents_loaded INT DEFAULT 0,
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);
