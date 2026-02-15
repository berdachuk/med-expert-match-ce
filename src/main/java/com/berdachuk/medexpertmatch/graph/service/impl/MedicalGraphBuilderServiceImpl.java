package com.berdachuk.medexpertmatch.graph.service.impl;

import com.berdachuk.medexpertmatch.clinicalexperience.repository.ClinicalExperienceRepository;
import com.berdachuk.medexpertmatch.doctor.repository.DoctorRepository;
import com.berdachuk.medexpertmatch.doctor.repository.MedicalSpecialtyRepository;
import com.berdachuk.medexpertmatch.facility.repository.FacilityRepository;
import com.berdachuk.medexpertmatch.graph.exception.GraphOperationException;
import com.berdachuk.medexpertmatch.graph.service.GraphService;
import com.berdachuk.medexpertmatch.graph.service.MedicalGraphBuilderService;
import com.berdachuk.medexpertmatch.medicalcase.repository.MedicalCaseRepository;
import com.berdachuk.medexpertmatch.medicalcoding.repository.ICD10CodeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service implementation for building medical graph relationships from database data.
 * Populates Apache AGE graph with doctors, medical cases, ICD-10 codes, specialties, and facilities.
 */
@Slf4j
@Service
public class MedicalGraphBuilderServiceImpl implements MedicalGraphBuilderService {
    private static final String GRAPH_NAME = "medexpertmatch_graph";
    private final NamedParameterJdbcTemplate namedJdbcTemplate;
    private final GraphService graphService;
    private final DoctorRepository doctorRepository;
    private final MedicalCaseRepository medicalCaseRepository;
    private final ClinicalExperienceRepository clinicalExperienceRepository;
    private final ICD10CodeRepository icd10CodeRepository;
    private final MedicalSpecialtyRepository medicalSpecialtyRepository;
    private final FacilityRepository facilityRepository;

    public MedicalGraphBuilderServiceImpl(
            NamedParameterJdbcTemplate namedJdbcTemplate,
            GraphService graphService,
            DoctorRepository doctorRepository,
            MedicalCaseRepository medicalCaseRepository,
            ClinicalExperienceRepository clinicalExperienceRepository,
            ICD10CodeRepository icd10CodeRepository,
            MedicalSpecialtyRepository medicalSpecialtyRepository,
            FacilityRepository facilityRepository) {
        this.namedJdbcTemplate = namedJdbcTemplate;
        this.graphService = graphService;
        this.doctorRepository = doctorRepository;
        this.medicalCaseRepository = medicalCaseRepository;
        this.clinicalExperienceRepository = clinicalExperienceRepository;
        this.icd10CodeRepository = icd10CodeRepository;
        this.medicalSpecialtyRepository = medicalSpecialtyRepository;
        this.facilityRepository = facilityRepository;
    }

    @Override
    public void buildGraph() {
        long startTime = System.currentTimeMillis();
        log.info("Starting medical graph build process...");

        // Use graphService.createGraphIfNotExists() to ensure consistent transaction handling
        // This method uses REQUIRES_NEW propagation, which is necessary for graph operations
        log.info("Ensuring graph structure exists...");
        graphService.createGraphIfNotExists();
        log.info("Graph structure ready");

        // Build graph vertices and edges
        log.info("Building graph vertices and edges...");
        long verticesStartTime = System.currentTimeMillis();

        log.info("  Creating doctor vertices...");
        long doctorStartTime = System.currentTimeMillis();
        createDoctorVertices();
        long doctorEndTime = System.currentTimeMillis();
        log.info("  Doctor vertices created in {}ms", doctorEndTime - doctorStartTime);

        log.info("  Creating medical case vertices...");
        long caseStartTime = System.currentTimeMillis();
        createMedicalCaseVertices();
        long caseEndTime = System.currentTimeMillis();
        log.info("  Medical case vertices created in {}ms", caseEndTime - caseStartTime);

        log.info("  Creating ICD-10 code vertices...");
        long icd10StartTime = System.currentTimeMillis();
        createIcd10CodeVertices();
        long icd10EndTime = System.currentTimeMillis();
        log.info("  ICD-10 code vertices created in {}ms", icd10EndTime - icd10StartTime);

        log.info("  Creating medical specialty vertices...");
        long specialtyStartTime = System.currentTimeMillis();
        createMedicalSpecialtyVertices();
        long specialtyEndTime = System.currentTimeMillis();
        log.info("  Medical specialty vertices created in {}ms", specialtyEndTime - specialtyStartTime);

        log.info("  Creating facility vertices...");
        long facilityStartTime = System.currentTimeMillis();
        createFacilityVertices();
        long facilityEndTime = System.currentTimeMillis();
        log.info("  Facility vertices created in {}ms", facilityEndTime - facilityStartTime);

        long verticesEndTime = System.currentTimeMillis();
        log.info("Graph vertices creation completed in {}ms", verticesEndTime - verticesStartTime);

        // Create graph indexes for better query performance
        log.info("Creating graph indexes...");
        createGraphIndexes();
        log.info("Graph indexes creation completed");

        // Create relationships
        log.info("Building graph relationships...");
        long relationshipsStartTime = System.currentTimeMillis();

        log.info("  Creating treated relationships...");
        createTreatedRelationships();
        log.info("  Treated relationships created");

        log.info("  Creating specializes-in relationships...");
        createSpecializesInRelationships();
        log.info("  Specializes-in relationships created");

        log.info("  Creating has-condition relationships...");
        createHasConditionRelationships();
        log.info("  Has-condition relationships created");

        log.info("  Creating treats-condition relationships...");
        createTreatsConditionRelationships();
        log.info("  Treats-condition relationships created");

        log.info("  Creating requires-specialty relationships...");
        createRequiresSpecialtyRelationships();
        log.info("  Requires-specialty relationships created");

        log.info("  Creating affiliated-with relationships...");
        createAffiliatedWithRelationships();
        log.info("  Affiliated-with relationships created");

        long relationshipsEndTime = System.currentTimeMillis();
        log.info("Graph relationships creation completed in {}ms", relationshipsEndTime - relationshipsStartTime);

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        log.info("Medical graph build process completed successfully in {}ms", totalTime);
        log.info("Graph build summary:");
        log.info("  - Total execution time: {}ms", totalTime);
        log.info("  - Vertices creation time: {}ms", verticesEndTime - verticesStartTime);
        log.info("  - Relationships creation time: {}ms", relationshipsEndTime - relationshipsStartTime);
    }


    /**
     * Creates graph indexes for frequently queried properties.
     */
    private void createGraphIndexes() {
        try {
            String graphName = GRAPH_NAME;

            // Check if graph tables exist before creating indexes
            String checkTableSql = String.format("""
                    SELECT EXISTS (
                        SELECT FROM information_schema.tables 
                        WHERE table_schema = 'ag_catalog' 
                        AND table_name = 'ag_%s_Doctor'
                    )
                    """, graphName);

            Boolean tableExists = namedJdbcTemplate.getJdbcTemplate().queryForObject(checkTableSql, Boolean.class);
            if (tableExists == null || !tableExists) {
                log.debug("Graph tables do not exist yet, skipping index creation");
                return;
            }

            // Create GIN indexes on properties JSONB column for efficient property lookups
            try {
                String doctorIndexSql = String.format("""
                        CREATE INDEX IF NOT EXISTS idx_%s_doctor_props_id 
                        ON ag_catalog.ag_%s_Doctor USING gin ((properties jsonb_path_ops))
                        """, graphName, graphName);
                namedJdbcTemplate.getJdbcTemplate().execute(doctorIndexSql);
            } catch (Exception e) {
                log.debug("Could not create Doctor index: {}", e.getMessage());
            }

            try {
                String caseIndexSql = String.format("""
                        CREATE INDEX IF NOT EXISTS idx_%s_medicalcase_props_id 
                        ON ag_catalog.ag_%s_MedicalCase USING gin ((properties jsonb_path_ops))
                        """, graphName, graphName);
                namedJdbcTemplate.getJdbcTemplate().execute(caseIndexSql);
            } catch (Exception e) {
                log.debug("Could not create MedicalCase index: {}", e.getMessage());
            }

            try {
                String icd10IndexSql = String.format("""
                        CREATE INDEX IF NOT EXISTS idx_%s_icd10code_props_code 
                        ON ag_catalog.ag_%s_ICD10Code USING gin ((properties jsonb_path_ops))
                        """, graphName, graphName);
                namedJdbcTemplate.getJdbcTemplate().execute(icd10IndexSql);
            } catch (Exception e) {
                log.debug("Could not create ICD10Code index: {}", e.getMessage());
            }

            try {
                String specialtyIndexSql = String.format("""
                        CREATE INDEX IF NOT EXISTS idx_%s_medicalspecialty_props_id 
                        ON ag_catalog.ag_%s_MedicalSpecialty USING gin ((properties jsonb_path_ops))
                        """, graphName, graphName);
                namedJdbcTemplate.getJdbcTemplate().execute(specialtyIndexSql);
            } catch (Exception e) {
                log.debug("Could not create MedicalSpecialty index: {}", e.getMessage());
            }

            try {
                String facilityIndexSql = String.format("""
                        CREATE INDEX IF NOT EXISTS idx_%s_facility_props_id 
                        ON ag_catalog.ag_%s_Facility USING gin ((properties jsonb_path_ops))
                        """, graphName, graphName);
                namedJdbcTemplate.getJdbcTemplate().execute(facilityIndexSql);
            } catch (Exception e) {
                log.debug("Could not create Facility index: {}", e.getMessage());
            }

            log.debug("Graph indexes created successfully");
        } catch (Exception e) {
            // Index creation might fail if tables don't exist yet or indexes already exist
            // Log but continue - indexes are optional for functionality
            log.debug("Could not create graph indexes: {}", e.getMessage());
        }
    }

    /**
     * Creates Doctor vertices from doctors table.
     */
    private void createDoctorVertices() {
        List<String> doctorIds = doctorRepository.findAllIds(0);
        int[] created = {0};
        int[] failed = {0};
        for (String doctorId : doctorIds) {
            doctorRepository.findById(doctorId).ifPresent(doctor -> {
                try {
                    createDoctorVertex(doctor.id(), doctor.name(), doctor.email());
                    created[0]++;
                } catch (Exception e) {
                    failed[0]++;
                    log.warn("Failed to create doctor vertex for doctor {}: {}", doctor.id(), e.getMessage());
                    // Continue with next doctor
                }
            });
            if ((created[0] + failed[0]) % 100 == 0) {
                log.debug("  Processed {}/{} doctor vertices ({} created, {} failed)", created[0] + failed[0], doctorIds.size(), created[0], failed[0]);
            }
        }
        log.info("  Created {} doctor vertices ({} failed)", created[0], failed[0]);
    }

    /**
     * Creates MedicalCase vertices from medical_cases table.
     */
    private void createMedicalCaseVertices() {
        List<String> caseIds = medicalCaseRepository.findAllIds(0);
        int[] created = {0};
        int[] failed = {0};
        for (String caseId : caseIds) {
            medicalCaseRepository.findById(caseId).ifPresent(medicalCase -> {
                try {
                    createMedicalCaseVertex(
                            medicalCase.id(),
                            medicalCase.chiefComplaint() != null ? medicalCase.chiefComplaint() : "",
                            medicalCase.urgencyLevel() != null ? medicalCase.urgencyLevel().name() : "MEDIUM"
                    );
                    created[0]++;
                } catch (Exception e) {
                    failed[0]++;
                    log.warn("Failed to create medical case vertex for case {}: {}", medicalCase.id(), e.getMessage());
                    // Continue with next case
                }
            });
            if ((created[0] + failed[0]) % 100 == 0) {
                log.debug("  Processed {}/{} medical case vertices ({} created, {} failed)", created[0] + failed[0], caseIds.size(), created[0], failed[0]);
            }
        }
        log.info("  Created {} medical case vertices ({} failed)", created[0], failed[0]);
    }

    /**
     * Creates ICD10Code vertices from icd10_codes table.
     */
    private void createIcd10CodeVertices() {
        List<com.berdachuk.medexpertmatch.medicalcoding.domain.ICD10Code> codes = icd10CodeRepository.findAll();
        int created = 0;
        for (var code : codes) {
            createIcd10CodeVertex(code.code(), code.description() != null ? code.description() : "");
            created++;
            if (created % 100 == 0) {
                log.debug("  Processed {}/{} ICD-10 code vertices", created, codes.size());
            }
        }
        log.info("  Created {} ICD-10 code vertices", created);
    }

    /**
     * Creates MedicalSpecialty vertices from medical_specialties table.
     */
    private void createMedicalSpecialtyVertices() {
        List<com.berdachuk.medexpertmatch.doctor.domain.MedicalSpecialty> specialties = medicalSpecialtyRepository.findAll();
        int created = 0;
        for (var specialty : specialties) {
            createMedicalSpecialtyVertex(specialty.id(), specialty.name());
            created++;
        }
        log.info("  Created {} medical specialty vertices", created);
    }

    /**
     * Creates Facility vertices from facilities table.
     */
    private void createFacilityVertices() {
        List<com.berdachuk.medexpertmatch.facility.domain.Facility> facilities = facilityRepository.findAll();
        int created = 0;
        int failed = 0;
        for (var facility : facilities) {
            try {
                createFacilityVertex(
                        facility.id(),
                        facility.name(),
                        facility.facilityType() != null ? facility.facilityType() : ""
                );
                created++;
            } catch (Exception e) {
                failed++;
                log.warn("Failed to create facility vertex for facility {}: {}", facility.id(), e.getMessage());
                // Continue with next facility
            }
        }
        log.info("  Created {} facility vertices ({} failed)", created, failed);
    }

    /**
     * Creates TREATED relationships from clinical experiences.
     */
    private void createTreatedRelationships() {
        long startTime = System.currentTimeMillis();
        String sql = """
                SELECT doctor_id, case_id
                FROM medexpertmatch.clinical_experiences
                WHERE doctor_id IS NOT NULL AND case_id IS NOT NULL
                """;

        List<TreatedRelationship> relationships = namedJdbcTemplate.query(sql, (rs, rowNum) -> {
            String doctorId = rs.getString("doctor_id");
            String caseId = rs.getString("case_id");
            if (doctorId != null && caseId != null) {
                return new TreatedRelationship(doctorId, caseId);
            }
            return null;
        }).stream().filter(r -> r != null).distinct().toList();

        // Process in chunks of 1000
        int batchSize = 1000;
        int totalRelationships = relationships.size();
        int processed = 0;

        for (int i = 0; i < relationships.size(); i += batchSize) {
            int end = Math.min(i + batchSize, relationships.size());
            List<TreatedRelationship> batch = relationships.subList(i, end);
            createTreatedRelationshipsBatch(batch);
            processed += batch.size();
            if (processed % 5000 == 0 || processed == totalRelationships) {
                log.debug("  Processed {}/{} treated relationships", processed, totalRelationships);
            }
        }

        long endTime = System.currentTimeMillis();
        log.info("  Created {} treated relationships in {}ms", totalRelationships, endTime - startTime);
    }

    /**
     * Creates SPECIALIZES_IN relationships from doctor specialties.
     */
    private void createSpecializesInRelationships() {
        long startTime = System.currentTimeMillis();
        List<String> doctorIds = doctorRepository.findAllIds(0);
        List<SpecializesInRelationship> relationships = new ArrayList<>();

        for (String doctorId : doctorIds) {
            doctorRepository.findById(doctorId).ifPresent(doctor -> {
                if (doctor.specialties() != null) {
                    for (String specialty : doctor.specialties()) {
                        if (specialty != null && !specialty.isEmpty()) {
                            relationships.add(new SpecializesInRelationship(doctorId, specialty));
                        }
                    }
                }
            });
        }

        // Deduplicate
        Set<SpecializesInRelationship> relationshipSet = new LinkedHashSet<>(relationships);
        List<SpecializesInRelationship> uniqueRelationships = new ArrayList<>(relationshipSet);

        // Process in chunks of 1000
        int batchSize = 1000;
        int totalRelationships = uniqueRelationships.size();
        int processed = 0;

        for (int i = 0; i < uniqueRelationships.size(); i += batchSize) {
            int end = Math.min(i + batchSize, uniqueRelationships.size());
            List<SpecializesInRelationship> batch = uniqueRelationships.subList(i, end);
            createSpecializesInRelationshipsBatch(batch);
            processed += batch.size();
            if (processed % 5000 == 0 || processed == totalRelationships) {
                log.debug("  Processed {}/{} specializes-in relationships", processed, totalRelationships);
            }
        }

        long endTime = System.currentTimeMillis();
        log.info("  Created {} specializes-in relationships in {}ms", totalRelationships, endTime - startTime);
    }

    /**
     * Creates HAS_CONDITION relationships from medical case ICD-10 codes.
     */
    private void createHasConditionRelationships() {
        long startTime = System.currentTimeMillis();
        List<String> caseIds = medicalCaseRepository.findAllIds(0);
        List<HasConditionRelationship> relationships = new ArrayList<>();

        for (String caseId : caseIds) {
            medicalCaseRepository.findById(caseId).ifPresent(medicalCase -> {
                if (medicalCase.icd10Codes() != null) {
                    for (String icd10Code : medicalCase.icd10Codes()) {
                        if (icd10Code != null && !icd10Code.isEmpty()) {
                            relationships.add(new HasConditionRelationship(caseId, icd10Code));
                        }
                    }
                }
            });
        }

        // Deduplicate
        Set<HasConditionRelationship> relationshipSet = new LinkedHashSet<>(relationships);
        List<HasConditionRelationship> uniqueRelationships = new ArrayList<>(relationshipSet);

        // Process in chunks of 1000
        int batchSize = 1000;
        int totalRelationships = uniqueRelationships.size();
        int processed = 0;

        for (int i = 0; i < uniqueRelationships.size(); i += batchSize) {
            int end = Math.min(i + batchSize, uniqueRelationships.size());
            List<HasConditionRelationship> batch = uniqueRelationships.subList(i, end);
            createHasConditionRelationshipsBatch(batch);
            processed += batch.size();
            if (processed % 5000 == 0 || processed == totalRelationships) {
                log.debug("  Processed {}/{} has-condition relationships", processed, totalRelationships);
            }
        }

        long endTime = System.currentTimeMillis();
        log.info("  Created {} has-condition relationships in {}ms", totalRelationships, endTime - startTime);
    }

    /**
     * Creates TREATS_CONDITION relationships from clinical experiences and medical cases.
     */
    private void createTreatsConditionRelationships() {
        long startTime = System.currentTimeMillis();

        // Get all experiences via SQL
        String sql = """
                SELECT DISTINCT ce.doctor_id, mc.icd10_codes
                FROM medexpertmatch.clinical_experiences ce
                JOIN medexpertmatch.medical_cases mc ON ce.case_id = mc.id
                WHERE ce.doctor_id IS NOT NULL AND mc.icd10_codes IS NOT NULL
                """;

        List<TreatsConditionRelationship> relationships = new ArrayList<>();
        namedJdbcTemplate.query(sql, (rs, rowNum) -> {
            String doctorId = rs.getString("doctor_id");
            String[] icd10Codes = (String[]) rs.getArray("icd10_codes").getArray();
            if (doctorId != null && icd10Codes != null) {
                for (String icd10Code : icd10Codes) {
                    if (icd10Code != null && !icd10Code.isEmpty()) {
                        relationships.add(new TreatsConditionRelationship(doctorId, icd10Code));
                    }
                }
            }
            return null;
        });

        // Deduplicate
        Set<TreatsConditionRelationship> relationshipSet = new LinkedHashSet<>(relationships);
        List<TreatsConditionRelationship> uniqueRelationships = new ArrayList<>(relationshipSet);

        // Process in chunks of 1000
        int batchSize = 1000;
        int totalRelationships = uniqueRelationships.size();
        int processed = 0;

        for (int i = 0; i < uniqueRelationships.size(); i += batchSize) {
            int end = Math.min(i + batchSize, uniqueRelationships.size());
            List<TreatsConditionRelationship> batch = uniqueRelationships.subList(i, end);
            createTreatsConditionRelationshipsBatch(batch);
            processed += batch.size();
            if (processed % 5000 == 0 || processed == totalRelationships) {
                log.debug("  Processed {}/{} treats-condition relationships", processed, totalRelationships);
            }
        }

        long endTime = System.currentTimeMillis();
        log.info("  Created {} treats-condition relationships in {}ms", totalRelationships, endTime - startTime);
    }

    /**
     * Creates REQUIRES_SPECIALTY relationships from medical case required specialty.
     */
    private void createRequiresSpecialtyRelationships() {
        long startTime = System.currentTimeMillis();
        List<String> caseIds = medicalCaseRepository.findAllIds(0);
        List<RequiresSpecialtyRelationship> relationships = new ArrayList<>();

        for (String caseId : caseIds) {
            medicalCaseRepository.findById(caseId).ifPresent(medicalCase -> {
                if (medicalCase.requiredSpecialty() != null && !medicalCase.requiredSpecialty().isEmpty()) {
                    relationships.add(new RequiresSpecialtyRelationship(caseId, medicalCase.requiredSpecialty()));
                }
            });
        }

        // Process in chunks of 1000
        int batchSize = 1000;
        int totalRelationships = relationships.size();
        int processed = 0;

        for (int i = 0; i < relationships.size(); i += batchSize) {
            int end = Math.min(i + batchSize, relationships.size());
            List<RequiresSpecialtyRelationship> batch = relationships.subList(i, end);
            createRequiresSpecialtyRelationshipsBatch(batch);
            processed += batch.size();
            if (processed % 5000 == 0 || processed == totalRelationships) {
                log.debug("  Processed {}/{} requires-specialty relationships", processed, totalRelationships);
            }
        }

        long endTime = System.currentTimeMillis();
        log.info("  Created {} requires-specialty relationships in {}ms", totalRelationships, endTime - startTime);
    }

    /**
     * Creates AFFILIATED_WITH relationships from doctor facility IDs.
     */
    private void createAffiliatedWithRelationships() {
        long startTime = System.currentTimeMillis();
        List<String> doctorIds = doctorRepository.findAllIds(0);
        List<AffiliatedWithRelationship> relationships = new ArrayList<>();

        for (String doctorId : doctorIds) {
            doctorRepository.findById(doctorId).ifPresent(doctor -> {
                if (doctor.facilityIds() != null) {
                    for (String facilityId : doctor.facilityIds()) {
                        if (facilityId != null && !facilityId.isEmpty()) {
                            relationships.add(new AffiliatedWithRelationship(doctorId, facilityId));
                        }
                    }
                }
            });
        }

        // Deduplicate
        Set<AffiliatedWithRelationship> relationshipSet = new LinkedHashSet<>(relationships);
        List<AffiliatedWithRelationship> uniqueRelationships = new ArrayList<>(relationshipSet);

        // Process in chunks of 1000
        int batchSize = 1000;
        int totalRelationships = uniqueRelationships.size();
        int processed = 0;

        for (int i = 0; i < uniqueRelationships.size(); i += batchSize) {
            int end = Math.min(i + batchSize, uniqueRelationships.size());
            List<AffiliatedWithRelationship> batch = uniqueRelationships.subList(i, end);
            createAffiliatedWithRelationshipsBatch(batch);
            processed += batch.size();
            if (processed % 5000 == 0 || processed == totalRelationships) {
                log.debug("  Processed {}/{} affiliated-with relationships", processed, totalRelationships);
            }
        }

        long endTime = System.currentTimeMillis();
        log.info("  Created {} affiliated-with relationships in {}ms", totalRelationships, endTime - startTime);
    }

    // Individual vertex creation methods

    @Override
    public void createDoctorVertex(String doctorId, String name, String email) {
        // Apache AGE compatible: Use MERGE with all properties in the MERGE clause
        // This avoids SET clause which may not work with embedded parameters
        String cypher = "MERGE (d:Doctor {id: $doctorId, name: $name, email: $email})";

        Map<String, Object> params = new HashMap<>();
        params.put("doctorId", doctorId);
        params.put("name", name != null ? name : "");
        params.put("email", email != null ? email : "");

        graphService.executeCypher(cypher, params);
    }

    @Override
    public void createMedicalCaseVertex(String caseId, String chiefComplaint, String urgencyLevel) {
        // Apache AGE compatible: Use MERGE with all properties in the MERGE clause
        String cypher = "MERGE (c:MedicalCase {id: $caseId, chiefComplaint: $chiefComplaint, urgencyLevel: $urgencyLevel})";

        Map<String, Object> params = new HashMap<>();
        params.put("caseId", caseId);
        params.put("chiefComplaint", chiefComplaint != null ? chiefComplaint : "");
        params.put("urgencyLevel", urgencyLevel != null ? urgencyLevel : "MEDIUM");

        graphService.executeCypher(cypher, params);
    }

    @Override
    public void createIcd10CodeVertex(String code, String description) {
        // Apache AGE compatible: Use MERGE with all properties in the MERGE clause
        String cypher = "MERGE (i:ICD10Code {code: $code, description: $description})";

        Map<String, Object> params = new HashMap<>();
        params.put("code", code);
        params.put("description", description != null ? description : "");

        graphService.executeCypher(cypher, params);
    }

    @Override
    public void createMedicalSpecialtyVertex(String specialtyId, String name) {
        // Apache AGE compatible: Use MERGE with all properties in the MERGE clause
        String cypher = "MERGE (s:MedicalSpecialty {id: $specialtyId, name: $name})";

        Map<String, Object> params = new HashMap<>();
        params.put("specialtyId", specialtyId);
        params.put("name", name);

        graphService.executeCypher(cypher, params);
    }

    @Override
    public void createFacilityVertex(String facilityId, String name, String facilityType) {
        // Apache AGE compatible: Use MERGE with all properties in the MERGE clause
        String cypher = "MERGE (f:Facility {id: $facilityId, name: $name, facilityType: $facilityType})";

        Map<String, Object> params = new HashMap<>();
        params.put("facilityId", facilityId);
        params.put("name", name != null ? name : "");
        params.put("facilityType", facilityType != null ? facilityType : "");

        graphService.executeCypher(cypher, params);
    }

    // Individual relationship creation methods

    @Override
    public void createTreatedRelationship(String doctorId, String caseId) {
        String cypher = """
                MATCH (d:Doctor {id: $doctorId})
                MATCH (c:MedicalCase {id: $caseId})
                MERGE (d)-[:TREATED]->(c)
                """;

        Map<String, Object> params = new HashMap<>();
        params.put("doctorId", doctorId);
        params.put("caseId", caseId);

        graphService.executeCypher(cypher, params);
    }

    @Override
    public void createConsultedOnRelationship(String doctorId, String caseId) {
        String cypher = """
                MATCH (d:Doctor {id: $doctorId})
                MATCH (c:MedicalCase {id: $caseId})
                MERGE (d)-[:CONSULTED_ON]->(c)
                """;

        Map<String, Object> params = new HashMap<>();
        params.put("doctorId", doctorId);
        params.put("caseId", caseId);

        graphService.executeCypher(cypher, params);
    }

    @Override
    public void createSpecializesInRelationship(String doctorId, String specialtyName) {
        // Look up specialty to get full entity data (id and name)
        Optional<com.berdachuk.medexpertmatch.doctor.domain.MedicalSpecialty> specialty =
                medicalSpecialtyRepository.findByName(specialtyName);

        if (specialty.isEmpty()) {
            log.warn("MedicalSpecialty not found by name: {}, creating vertex with name only", specialtyName);
            // Fallback: create with name only if specialty not found in database
            String cypher = """
                    MATCH (d:Doctor {id: $doctorId})
                    MERGE (s:MedicalSpecialty {name: $specialtyName})
                    MERGE (d)-[:SPECIALIZES_IN]->(s)
                    """;
            Map<String, Object> params = new HashMap<>();
            params.put("doctorId", doctorId);
            params.put("specialtyName", specialtyName);
            graphService.executeCypher(cypher, params);
        } else {
            // Use full entity data: id and name
            String cypher = """
                    MATCH (d:Doctor {id: $doctorId})
                    MERGE (s:MedicalSpecialty {id: $specialtyId, name: $specialtyName})
                    MERGE (d)-[:SPECIALIZES_IN]->(s)
                    """;
            Map<String, Object> params = new HashMap<>();
            params.put("doctorId", doctorId);
            params.put("specialtyId", specialty.get().id());
            params.put("specialtyName", specialtyName);
            graphService.executeCypher(cypher, params);
        }
    }

    @Override
    public void createTreatsConditionRelationship(String doctorId, String icd10Code) {
        // Look up ICD-10 code to get full entity data (code and description)
        Optional<com.berdachuk.medexpertmatch.medicalcoding.domain.ICD10Code> code =
                icd10CodeRepository.findByCode(icd10Code);

        if (code.isEmpty()) {
            log.warn("ICD10Code not found by code: {}, creating vertex with code only", icd10Code);
            // Fallback: create with code only if code not found in database
            String cypher = """
                    MATCH (d:Doctor {id: $doctorId})
                    MERGE (i:ICD10Code {code: $icd10Code})
                    MERGE (d)-[:TREATS_CONDITION]->(i)
                    """;
            Map<String, Object> params = new HashMap<>();
            params.put("doctorId", doctorId);
            params.put("icd10Code", icd10Code);
            graphService.executeCypher(cypher, params);
        } else {
            // Use full entity data: code and description
            String cypher = """
                    MATCH (d:Doctor {id: $doctorId})
                    MERGE (i:ICD10Code {code: $icd10Code, description: $description})
                    MERGE (d)-[:TREATS_CONDITION]->(i)
                    """;
            Map<String, Object> params = new HashMap<>();
            params.put("doctorId", doctorId);
            params.put("icd10Code", icd10Code);
            params.put("description", code.get().description() != null ? code.get().description() : "");
            graphService.executeCypher(cypher, params);
        }
    }

    @Override
    public void createHasConditionRelationship(String caseId, String icd10Code) {
        // Look up ICD-10 code to get full entity data (code and description)
        Optional<com.berdachuk.medexpertmatch.medicalcoding.domain.ICD10Code> code =
                icd10CodeRepository.findByCode(icd10Code);

        if (code.isEmpty()) {
            log.warn("ICD10Code not found by code: {}, creating vertex with code only", icd10Code);
            // Fallback: create with code only if code not found in database
            String cypher = """
                    MATCH (c:MedicalCase {id: $caseId})
                    MERGE (i:ICD10Code {code: $icd10Code})
                    MERGE (c)-[:HAS_CONDITION]->(i)
                    """;
            Map<String, Object> params = new HashMap<>();
            params.put("caseId", caseId);
            params.put("icd10Code", icd10Code);
            graphService.executeCypher(cypher, params);
        } else {
            // Use full entity data: code and description
            String cypher = """
                    MATCH (c:MedicalCase {id: $caseId})
                    MERGE (i:ICD10Code {code: $icd10Code, description: $description})
                    MERGE (c)-[:HAS_CONDITION]->(i)
                    """;
            Map<String, Object> params = new HashMap<>();
            params.put("caseId", caseId);
            params.put("icd10Code", icd10Code);
            params.put("description", code.get().description() != null ? code.get().description() : "");
            graphService.executeCypher(cypher, params);
        }
    }

    @Override
    public void createRequiresSpecialtyRelationship(String caseId, String specialtyName) {
        // Look up specialty to get full entity data (id and name)
        Optional<com.berdachuk.medexpertmatch.doctor.domain.MedicalSpecialty> specialty =
                medicalSpecialtyRepository.findByName(specialtyName);

        if (specialty.isEmpty()) {
            log.warn("MedicalSpecialty not found by name: {}, creating vertex with name only", specialtyName);
            // Fallback: create with name only if specialty not found in database
            String cypher = """
                    MATCH (c:MedicalCase {id: $caseId})
                    MERGE (s:MedicalSpecialty {name: $specialtyName})
                    MERGE (c)-[:REQUIRES_SPECIALTY]->(s)
                    """;
            Map<String, Object> params = new HashMap<>();
            params.put("caseId", caseId);
            params.put("specialtyName", specialtyName);
            graphService.executeCypher(cypher, params);
        } else {
            // Use full entity data: id and name
            String cypher = """
                    MATCH (c:MedicalCase {id: $caseId})
                    MERGE (s:MedicalSpecialty {id: $specialtyId, name: $specialtyName})
                    MERGE (c)-[:REQUIRES_SPECIALTY]->(s)
                    """;
            Map<String, Object> params = new HashMap<>();
            params.put("caseId", caseId);
            params.put("specialtyId", specialty.get().id());
            params.put("specialtyName", specialtyName);
            graphService.executeCypher(cypher, params);
        }
    }

    @Override
    public void createAffiliatedWithRelationship(String doctorId, String facilityId) {
        String cypher = """
                MATCH (d:Doctor {id: $doctorId})
                MATCH (f:Facility {id: $facilityId})
                MERGE (d)-[:AFFILIATED_WITH]->(f)
                """;

        Map<String, Object> params = new HashMap<>();
        params.put("doctorId", doctorId);
        params.put("facilityId", facilityId);

        graphService.executeCypher(cypher, params);
    }

    // Batch relationship creation methods

    @Override
    public void createTreatedRelationshipsBatch(List<TreatedRelationship> relationships) {
        if (relationships == null || relationships.isEmpty()) {
            return;
        }

        StringBuilder relationshipsList = new StringBuilder("[");
        boolean first = true;
        for (TreatedRelationship rel : relationships) {
            if (!first) {
                relationshipsList.append(", ");
            }
            first = false;
            String doctorId = escapeCypherString(rel.doctorId());
            String caseId = escapeCypherString(rel.caseId());
            relationshipsList.append("{doctorId: '").append(doctorId)
                    .append("', caseId: '").append(caseId).append("'}");
        }
        relationshipsList.append("]");

        String cypher = String.format("""
                UNWIND %s AS rel
                MATCH (d:Doctor {id: rel.doctorId})
                MATCH (c:MedicalCase {id: rel.caseId})
                MERGE (d)-[:TREATED]->(c)
                """, relationshipsList);

        graphService.executeCypher(cypher, new HashMap<>());
    }

    @Override
    public void createSpecializesInRelationshipsBatch(List<SpecializesInRelationship> relationships) {
        if (relationships == null || relationships.isEmpty()) {
            return;
        }

        // Pre-load all specialties into a map for efficient lookup
        Map<String, com.berdachuk.medexpertmatch.doctor.domain.MedicalSpecialty> specialtyMap =
                medicalSpecialtyRepository.findAll().stream()
                        .collect(java.util.stream.Collectors.toMap(
                                com.berdachuk.medexpertmatch.doctor.domain.MedicalSpecialty::name,
                                specialty -> specialty,
                                (existing, replacement) -> existing // Keep first if duplicates
                        ));

        // Split into two batches: with id (most common) and without id (fallback)
        List<SpecializesInRelationship> withId = new ArrayList<>();
        List<SpecializesInRelationship> withoutId = new ArrayList<>();

        for (SpecializesInRelationship rel : relationships) {
            if (specialtyMap.containsKey(rel.specialtyName())) {
                withId.add(rel);
            } else {
                withoutId.add(rel);
            }
        }

        // Process batch with id (use full entity data: id and name)
        if (!withId.isEmpty()) {
            StringBuilder withIdList = new StringBuilder("[");
            boolean first = true;
            for (SpecializesInRelationship rel : withId) {
                if (!first) {
                    withIdList.append(", ");
                }
                first = false;
                String doctorId = escapeCypherString(rel.doctorId());
                String specialtyName = escapeCypherString(rel.specialtyName());
                String specialtyId = escapeCypherString(specialtyMap.get(specialtyName).id());
                withIdList.append("{doctorId: '").append(doctorId)
                        .append("', specialtyId: '").append(specialtyId)
                        .append("', specialtyName: '").append(specialtyName).append("'}");
            }
            withIdList.append("]");

            String cypherWithId = String.format("""
                    UNWIND %s AS rel
                    MATCH (d:Doctor {id: rel.doctorId})
                    MERGE (s:MedicalSpecialty {id: rel.specialtyId, name: rel.specialtyName})
                    MERGE (d)-[:SPECIALIZES_IN]->(s)
                    """, withIdList);
            graphService.executeCypher(cypherWithId, new HashMap<>());
        }

        // Process batch without id (fallback: use name only)
        if (!withoutId.isEmpty()) {
            StringBuilder withoutIdList = new StringBuilder("[");
            boolean first = true;
            for (SpecializesInRelationship rel : withoutId) {
                if (!first) {
                    withoutIdList.append(", ");
                }
                first = false;
                String doctorId = escapeCypherString(rel.doctorId());
                String specialtyName = escapeCypherString(rel.specialtyName());
                withoutIdList.append("{doctorId: '").append(doctorId)
                        .append("', specialtyName: '").append(specialtyName).append("'}");
            }
            withoutIdList.append("]");

            String cypherWithoutId = String.format("""
                    UNWIND %s AS rel
                    MATCH (d:Doctor {id: rel.doctorId})
                    MERGE (s:MedicalSpecialty {name: rel.specialtyName})
                    MERGE (d)-[:SPECIALIZES_IN]->(s)
                    """, withoutIdList);
            graphService.executeCypher(cypherWithoutId, new HashMap<>());
        }
    }

    @Override
    public void createHasConditionRelationshipsBatch(List<HasConditionRelationship> relationships) {
        if (relationships == null || relationships.isEmpty()) {
            return;
        }

        // Pre-load all ICD-10 codes into a map for efficient lookup
        Map<String, com.berdachuk.medexpertmatch.medicalcoding.domain.ICD10Code> codeMap =
                icd10CodeRepository.findAll().stream()
                        .collect(java.util.stream.Collectors.toMap(
                                com.berdachuk.medexpertmatch.medicalcoding.domain.ICD10Code::code,
                                code -> code,
                                (existing, replacement) -> existing // Keep first if duplicates
                        ));

        // Split into two batches: with description (most common) and without description (fallback)
        List<HasConditionRelationship> withDescription = new ArrayList<>();
        List<HasConditionRelationship> withoutDescription = new ArrayList<>();

        for (HasConditionRelationship rel : relationships) {
            if (codeMap.containsKey(rel.icd10Code())) {
                withDescription.add(rel);
            } else {
                withoutDescription.add(rel);
            }
        }

        // Process batch with description (use full entity data: code and description)
        if (!withDescription.isEmpty()) {
            StringBuilder withDescList = new StringBuilder("[");
            boolean first = true;
            for (HasConditionRelationship rel : withDescription) {
                if (!first) {
                    withDescList.append(", ");
                }
                first = false;
                String caseId = escapeCypherString(rel.caseId());
                String icd10Code = escapeCypherString(rel.icd10Code());
                String description = escapeCypherString(
                        codeMap.get(icd10Code).description() != null ?
                                codeMap.get(icd10Code).description() : "");
                withDescList.append("{caseId: '").append(caseId)
                        .append("', icd10Code: '").append(icd10Code)
                        .append("', description: '").append(description).append("'}");
            }
            withDescList.append("]");

            String cypherWithDesc = String.format("""
                    UNWIND %s AS rel
                    MATCH (c:MedicalCase {id: rel.caseId})
                    MERGE (i:ICD10Code {code: rel.icd10Code, description: rel.description})
                    MERGE (c)-[:HAS_CONDITION]->(i)
                    """, withDescList);
            graphService.executeCypher(cypherWithDesc, new HashMap<>());
        }

        // Process batch without description (fallback: use code only)
        if (!withoutDescription.isEmpty()) {
            StringBuilder withoutDescList = new StringBuilder("[");
            boolean first = true;
            for (HasConditionRelationship rel : withoutDescription) {
                if (!first) {
                    withoutDescList.append(", ");
                }
                first = false;
                String caseId = escapeCypherString(rel.caseId());
                String icd10Code = escapeCypherString(rel.icd10Code());
                withoutDescList.append("{caseId: '").append(caseId)
                        .append("', icd10Code: '").append(icd10Code).append("'}");
            }
            withoutDescList.append("]");

            String cypherWithoutDesc = String.format("""
                    UNWIND %s AS rel
                    MATCH (c:MedicalCase {id: rel.caseId})
                    MERGE (i:ICD10Code {code: rel.icd10Code})
                    MERGE (c)-[:HAS_CONDITION]->(i)
                    """, withoutDescList);
            graphService.executeCypher(cypherWithoutDesc, new HashMap<>());
        }
    }

    @Override
    public void createTreatsConditionRelationshipsBatch(List<TreatsConditionRelationship> relationships) {
        if (relationships == null || relationships.isEmpty()) {
            return;
        }

        // Pre-load all ICD-10 codes into a map for efficient lookup
        Map<String, com.berdachuk.medexpertmatch.medicalcoding.domain.ICD10Code> codeMap =
                icd10CodeRepository.findAll().stream()
                        .collect(java.util.stream.Collectors.toMap(
                                com.berdachuk.medexpertmatch.medicalcoding.domain.ICD10Code::code,
                                code -> code,
                                (existing, replacement) -> existing // Keep first if duplicates
                        ));

        // Split into two batches: with description (most common) and without description (fallback)
        List<TreatsConditionRelationship> withDescription = new ArrayList<>();
        List<TreatsConditionRelationship> withoutDescription = new ArrayList<>();

        for (TreatsConditionRelationship rel : relationships) {
            if (codeMap.containsKey(rel.icd10Code())) {
                withDescription.add(rel);
            } else {
                withoutDescription.add(rel);
            }
        }

        // Process batch with description (use full entity data: code and description)
        if (!withDescription.isEmpty()) {
            StringBuilder withDescList = new StringBuilder("[");
            boolean first = true;
            for (TreatsConditionRelationship rel : withDescription) {
                if (!first) {
                    withDescList.append(", ");
                }
                first = false;
                String doctorId = escapeCypherString(rel.doctorId());
                String icd10Code = escapeCypherString(rel.icd10Code());
                String description = escapeCypherString(
                        codeMap.get(icd10Code).description() != null ?
                                codeMap.get(icd10Code).description() : "");
                withDescList.append("{doctorId: '").append(doctorId)
                        .append("', icd10Code: '").append(icd10Code)
                        .append("', description: '").append(description).append("'}");
            }
            withDescList.append("]");

            String cypherWithDesc = String.format("""
                    UNWIND %s AS rel
                    MATCH (d:Doctor {id: rel.doctorId})
                    MERGE (i:ICD10Code {code: rel.icd10Code, description: rel.description})
                    MERGE (d)-[:TREATS_CONDITION]->(i)
                    """, withDescList);
            graphService.executeCypher(cypherWithDesc, new HashMap<>());
        }

        // Process batch without description (fallback: use code only)
        if (!withoutDescription.isEmpty()) {
            StringBuilder withoutDescList = new StringBuilder("[");
            boolean first = true;
            for (TreatsConditionRelationship rel : withoutDescription) {
                if (!first) {
                    withoutDescList.append(", ");
                }
                first = false;
                String doctorId = escapeCypherString(rel.doctorId());
                String icd10Code = escapeCypherString(rel.icd10Code());
                withoutDescList.append("{doctorId: '").append(doctorId)
                        .append("', icd10Code: '").append(icd10Code).append("'}");
            }
            withoutDescList.append("]");

            String cypherWithoutDesc = String.format("""
                    UNWIND %s AS rel
                    MATCH (d:Doctor {id: rel.doctorId})
                    MERGE (i:ICD10Code {code: rel.icd10Code})
                    MERGE (d)-[:TREATS_CONDITION]->(i)
                    """, withoutDescList);
            graphService.executeCypher(cypherWithoutDesc, new HashMap<>());
        }
    }

    @Override
    public void createRequiresSpecialtyRelationshipsBatch(List<RequiresSpecialtyRelationship> relationships) {
        if (relationships == null || relationships.isEmpty()) {
            return;
        }

        // Pre-load all specialties into a map for efficient lookup
        Map<String, com.berdachuk.medexpertmatch.doctor.domain.MedicalSpecialty> specialtyMap =
                medicalSpecialtyRepository.findAll().stream()
                        .collect(java.util.stream.Collectors.toMap(
                                com.berdachuk.medexpertmatch.doctor.domain.MedicalSpecialty::name,
                                specialty -> specialty,
                                (existing, replacement) -> existing // Keep first if duplicates
                        ));

        // Split into two batches: with id (most common) and without id (fallback)
        List<RequiresSpecialtyRelationship> withId = new ArrayList<>();
        List<RequiresSpecialtyRelationship> withoutId = new ArrayList<>();

        for (RequiresSpecialtyRelationship rel : relationships) {
            if (specialtyMap.containsKey(rel.specialtyName())) {
                withId.add(rel);
            } else {
                withoutId.add(rel);
            }
        }

        // Process batch with id (use full entity data: id and name)
        if (!withId.isEmpty()) {
            StringBuilder withIdList = new StringBuilder("[");
            boolean first = true;
            for (RequiresSpecialtyRelationship rel : withId) {
                if (!first) {
                    withIdList.append(", ");
                }
                first = false;
                String caseId = escapeCypherString(rel.caseId());
                String specialtyName = escapeCypherString(rel.specialtyName());
                String specialtyId = escapeCypherString(specialtyMap.get(specialtyName).id());
                withIdList.append("{caseId: '").append(caseId)
                        .append("', specialtyId: '").append(specialtyId)
                        .append("', specialtyName: '").append(specialtyName).append("'}");
            }
            withIdList.append("]");

            String cypherWithId = String.format("""
                    UNWIND %s AS rel
                    MATCH (c:MedicalCase {id: rel.caseId})
                    MERGE (s:MedicalSpecialty {id: rel.specialtyId, name: rel.specialtyName})
                    MERGE (c)-[:REQUIRES_SPECIALTY]->(s)
                    """, withIdList);
            graphService.executeCypher(cypherWithId, new HashMap<>());
        }

        // Process batch without id (fallback: use name only)
        if (!withoutId.isEmpty()) {
            StringBuilder withoutIdList = new StringBuilder("[");
            boolean first = true;
            for (RequiresSpecialtyRelationship rel : withoutId) {
                if (!first) {
                    withoutIdList.append(", ");
                }
                first = false;
                String caseId = escapeCypherString(rel.caseId());
                String specialtyName = escapeCypherString(rel.specialtyName());
                withoutIdList.append("{caseId: '").append(caseId)
                        .append("', specialtyName: '").append(specialtyName).append("'}");
            }
            withoutIdList.append("]");

            String cypherWithoutId = String.format("""
                    UNWIND %s AS rel
                    MATCH (c:MedicalCase {id: rel.caseId})
                    MERGE (s:MedicalSpecialty {name: rel.specialtyName})
                    MERGE (c)-[:REQUIRES_SPECIALTY]->(s)
                    """, withoutIdList);
            graphService.executeCypher(cypherWithoutId, new HashMap<>());
        }
    }

    @Override
    public void createAffiliatedWithRelationshipsBatch(List<AffiliatedWithRelationship> relationships) {
        if (relationships == null || relationships.isEmpty()) {
            return;
        }

        StringBuilder relationshipsList = new StringBuilder("[");
        boolean first = true;
        for (AffiliatedWithRelationship rel : relationships) {
            if (!first) {
                relationshipsList.append(", ");
            }
            first = false;
            String doctorId = escapeCypherString(rel.doctorId());
            String facilityId = escapeCypherString(rel.facilityId());
            relationshipsList.append("{doctorId: '").append(doctorId)
                    .append("', facilityId: '").append(facilityId).append("'}");
        }
        relationshipsList.append("]");

        String cypher = String.format("""
                UNWIND %s AS rel
                MATCH (d:Doctor {id: rel.doctorId})
                MATCH (f:Facility {id: rel.facilityId})
                MERGE (d)-[:AFFILIATED_WITH]->(f)
                """, relationshipsList);

        graphService.executeCypher(cypher, new HashMap<>());
    }

    /**
     * Escapes special characters in a string for use in Cypher queries.
     */
    private String escapeCypherString(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void clearGraph() {
        log.info("clearGraph() called");
        boolean graphExists = graphService.graphExists();
        log.info("Graph exists check result: {}", graphExists);

        if (!graphExists) {
            log.info("Graph does not exist, nothing to clear");
            return;
        }

        log.info("Clearing all vertices and edges from graph: {}", GRAPH_NAME);
        try {
            // Delete all edges first (to avoid constraint violations)
            log.info("Executing DELETE query for edges...");
            String deleteEdgesQuery = "MATCH ()-[e]->() DELETE e";
            List<Map<String, Object>> edgeResult = graphService.executeCypher(deleteEdgesQuery, new HashMap<>());
            log.info("Deleted all edges from graph, result size: {}", edgeResult != null ? edgeResult.size() : 0);

            // Delete all vertices
            log.info("Executing DELETE query for vertices...");
            String deleteVerticesQuery = "MATCH (v) DELETE v";
            List<Map<String, Object>> vertexResult = graphService.executeCypher(deleteVerticesQuery, new HashMap<>());
            log.info("Deleted all vertices from graph, result size: {}", vertexResult != null ? vertexResult.size() : 0);

            log.info("Graph cleared successfully");
        } catch (Exception e) {
            log.error("Failed to clear graph: {}", e.getMessage(), e);
            throw new GraphOperationException("Failed to clear graph: " + e.getMessage(), e);
        }
    }
}
