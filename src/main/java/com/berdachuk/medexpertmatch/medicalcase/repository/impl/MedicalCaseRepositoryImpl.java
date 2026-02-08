package com.berdachuk.medexpertmatch.medicalcase.repository.impl;

import com.berdachuk.medexpertmatch.core.repository.sql.InjectSql;
import com.berdachuk.medexpertmatch.core.util.IdGenerator;
import com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase;
import com.berdachuk.medexpertmatch.medicalcase.domain.UrgencyLevel;
import com.berdachuk.medexpertmatch.medicalcase.repository.MedicalCaseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Repository for medical case data access.
 */
@Slf4j
@Repository
public class MedicalCaseRepositoryImpl implements MedicalCaseRepository {

    private final NamedParameterJdbcTemplate namedJdbcTemplate;
    private final MedicalCaseMapper medicalCaseMapper;

    @InjectSql("/sql/medicalcase/findById.sql")
    private String findByIdSql;

    @InjectSql("/sql/medicalcase/findByIds.sql")
    private String findByIdsSql;

    @InjectSql("/sql/medicalcase/findByUrgencyLevel.sql")
    private String findByUrgencyLevelSql;

    @InjectSql("/sql/medicalcase/findByCaseType.sql")
    private String findByCaseTypeSql;

    @InjectSql("/sql/medicalcase/findByRequiredSpecialty.sql")
    private String findByRequiredSpecialtySql;

    @InjectSql("/sql/medicalcase/findByIcd10Code.sql")
    private String findByIcd10CodeSql;

    @InjectSql("/sql/medicalcase/insert.sql")
    private String insertSql;

    @InjectSql("/sql/medicalcase/update.sql")
    private String updateSql;

    @InjectSql("/sql/medicalcase/findAllIds.sql")
    private String findAllIdsSql;

    @InjectSql("/sql/medicalcase/deleteAll.sql")
    private String deleteAllSql;

    @InjectSql("/sql/medicalcase/findWithoutEmbeddings.sql")
    private String findWithoutEmbeddingsSql;

    @InjectSql("/sql/medicalcase/findWithoutDescriptions.sql")
    private String findWithoutDescriptionsSql;

    @InjectSql("/sql/medicalcase/updateEmbedding.sql")
    private String updateEmbeddingSql;

    @InjectSql("/sql/medicalcase/updateAbstract.sql")
    private String updateAbstractSql;

    @InjectSql("/sql/medicalcase/search.sql")
    private String searchSql;

    @InjectSql("/sql/medicalcase/findAllPaginated.sql")
    private String findAllPaginatedSql;

    @InjectSql("/sql/medicalcase/hasEmbedding.sql")
    private String hasEmbeddingSql;

    @InjectSql("/sql/medicalcase/calculateVectorSimilarity.sql")
    private String calculateVectorSimilaritySql;

    public MedicalCaseRepositoryImpl(
            NamedParameterJdbcTemplate namedJdbcTemplate,
            MedicalCaseMapper medicalCaseMapper) {
        this.namedJdbcTemplate = namedJdbcTemplate;
        this.medicalCaseMapper = medicalCaseMapper;
    }

    @Override
    public Optional<MedicalCase> findById(String caseId) {
        // Normalize case ID to lowercase for case-insensitive lookup
        // Case IDs are 24-character hex strings (CHAR(24)) and should be case-insensitive
        String normalizedCaseId = caseId != null ? caseId.toLowerCase() : null;
        Map<String, Object> params = Map.of("id", normalizedCaseId);
        List<MedicalCase> results = namedJdbcTemplate.query(findByIdSql, params, medicalCaseMapper);
        return Optional.ofNullable(DataAccessUtils.uniqueResult(results));
    }

    @Override
    public List<MedicalCase> findByIds(List<String> caseIds) {
        if (caseIds.isEmpty()) {
            return List.of();
        }

        // Normalize case IDs to lowercase for case-insensitive lookup
        // Case IDs are 24-character hex strings (CHAR(24)) and should be case-insensitive
        String[] normalizedCaseIds = caseIds.stream()
                .map(caseId -> caseId != null ? caseId.toLowerCase() : null)
                .toArray(String[]::new);

        Map<String, Object> params = Map.of("ids", normalizedCaseIds);
        return namedJdbcTemplate.query(findByIdsSql, params, medicalCaseMapper);
    }

    @Override
    public List<MedicalCase> findByUrgencyLevel(String urgencyLevel, int maxResults) {
        if (urgencyLevel == null || urgencyLevel.isBlank()) {
            return List.of();
        }

        Map<String, Object> params = Map.of(
                "urgencyLevel", urgencyLevel,
                "maxResults", maxResults
        );

        return namedJdbcTemplate.query(findByUrgencyLevelSql, params, medicalCaseMapper);
    }

    @Override
    public List<MedicalCase> findByCaseType(String caseType, int maxResults) {
        if (caseType == null || caseType.isBlank()) {
            return List.of();
        }

        Map<String, Object> params = Map.of(
                "caseType", caseType,
                "maxResults", maxResults
        );

        return namedJdbcTemplate.query(findByCaseTypeSql, params, medicalCaseMapper);
    }

    @Override
    public List<MedicalCase> findByRequiredSpecialty(String specialty, int maxResults) {
        if (specialty == null || specialty.isBlank()) {
            return List.of();
        }

        Map<String, Object> params = Map.of(
                "specialty", specialty,
                "maxResults", maxResults
        );

        return namedJdbcTemplate.query(findByRequiredSpecialtySql, params, medicalCaseMapper);
    }

    @Override
    public List<MedicalCase> findByIcd10Code(String icd10Code, int maxResults) {
        if (icd10Code == null || icd10Code.isBlank()) {
            return List.of();
        }

        Map<String, Object> params = Map.of(
                "icd10Code", icd10Code,
                "maxResults", maxResults
        );

        return namedJdbcTemplate.query(findByIcd10CodeSql, params, medicalCaseMapper);
    }

    @Override
    public String insert(MedicalCase medicalCase) {
        // Generate ID if null (for new cases)
        String caseId = medicalCase.id();
        if (caseId == null || caseId.isBlank()) {
            caseId = IdGenerator.generateId();
        } else {
            // Normalize case ID to lowercase for consistency
            // Case IDs are 24-character hex strings (CHAR(24)) and should be case-insensitive
            caseId = caseId.toLowerCase();
        }

        Map<String, Object> params = new HashMap<>();
        params.put("id", caseId);
        params.put("patientAge", medicalCase.patientAge());
        params.put("chiefComplaint", medicalCase.chiefComplaint());
        params.put("symptoms", medicalCase.symptoms());
        params.put("currentDiagnosis", medicalCase.currentDiagnosis());
        params.put("icd10Codes", medicalCase.icd10Codes() != null ? medicalCase.icd10Codes().toArray(new String[0]) : new String[0]);
        params.put("snomedCodes", medicalCase.snomedCodes() != null ? medicalCase.snomedCodes().toArray(new String[0]) : new String[0]);
        // Default to MEDIUM if urgencyLevel is null (database requires NOT NULL)
        params.put("urgencyLevel", medicalCase.urgencyLevel() != null ? medicalCase.urgencyLevel().name() : UrgencyLevel.MEDIUM.name());
        params.put("requiredSpecialty", medicalCase.requiredSpecialty());
        params.put("caseType", medicalCase.caseType().name());
        params.put("additionalNotes", medicalCase.additionalNotes());
        params.put("abstract", medicalCase.abstractText());

        return namedJdbcTemplate.queryForObject(insertSql, params, String.class);
    }

    @Override
    public String update(MedicalCase medicalCase) {
        // Normalize case ID to lowercase for case-insensitive lookup
        // Case IDs are 24-character hex strings (CHAR(24)) and should be case-insensitive
        String normalizedCaseId = medicalCase.id() != null ? medicalCase.id().toLowerCase() : null;
        Map<String, Object> params = new HashMap<>();
        params.put("id", normalizedCaseId);
        params.put("patientAge", medicalCase.patientAge());
        params.put("chiefComplaint", medicalCase.chiefComplaint());
        params.put("symptoms", medicalCase.symptoms());
        params.put("currentDiagnosis", medicalCase.currentDiagnosis());
        params.put("icd10Codes", medicalCase.icd10Codes() != null ? medicalCase.icd10Codes().toArray(new String[0]) : new String[0]);
        params.put("snomedCodes", medicalCase.snomedCodes() != null ? medicalCase.snomedCodes().toArray(new String[0]) : new String[0]);
        // Default to MEDIUM if urgencyLevel is null (database requires NOT NULL)
        params.put("urgencyLevel", medicalCase.urgencyLevel() != null ? medicalCase.urgencyLevel().name() : UrgencyLevel.MEDIUM.name());
        params.put("requiredSpecialty", medicalCase.requiredSpecialty());
        params.put("caseType", medicalCase.caseType().name());
        params.put("additionalNotes", medicalCase.additionalNotes());
        params.put("abstract", medicalCase.abstractText());

        int rowsUpdated = namedJdbcTemplate.update(updateSql, params);
        if (rowsUpdated == 0) {
            throw new org.springframework.dao.EmptyResultDataAccessException("Medical case not found with id: " + normalizedCaseId, 1);
        }
        return normalizedCaseId;
    }

    @Override
    public List<String> findAllIds(int limit) {
        Map<String, Object> params = Map.of("limit", limit > 0 ? limit : Integer.MAX_VALUE);
        return namedJdbcTemplate.query(findAllIdsSql, params, (rs, rowNum) -> rs.getString("id"));
    }

    @Override
    public List<MedicalCase> findAllPaginated(int offset, int limit) {
        if (limit <= 0) {
            return List.of();
        }
        Map<String, Object> params = Map.of(
                "offset", Math.max(0, offset),
                "limit", limit
        );
        return namedJdbcTemplate.query(findAllPaginatedSql, params, medicalCaseMapper);
    }

    @Override
    public List<String> insertBatch(List<MedicalCase> medicalCases) {
        if (medicalCases.isEmpty()) {
            return List.of();
        }

        SqlParameterSource[] batchParams = medicalCases.stream()
                .map(medicalCase -> {
                    String caseId = medicalCase.id();
                    if (caseId == null || caseId.isBlank()) {
                        caseId = IdGenerator.generateId();
                    } else {
                        caseId = caseId.toLowerCase();
                    }
                    return new MapSqlParameterSource()
                            .addValue("id", caseId)
                            .addValue("patientAge", medicalCase.patientAge())
                            .addValue("chiefComplaint", medicalCase.chiefComplaint())
                            .addValue("symptoms", medicalCase.symptoms())
                            .addValue("currentDiagnosis", medicalCase.currentDiagnosis())
                            .addValue("icd10Codes", medicalCase.icd10Codes() != null ? medicalCase.icd10Codes().toArray(new String[0]) : new String[0])
                            .addValue("snomedCodes", medicalCase.snomedCodes() != null ? medicalCase.snomedCodes().toArray(new String[0]) : new String[0])
                            .addValue("urgencyLevel", medicalCase.urgencyLevel() != null ? medicalCase.urgencyLevel().name() : UrgencyLevel.MEDIUM.name())
                            .addValue("requiredSpecialty", medicalCase.requiredSpecialty())
                            .addValue("caseType", medicalCase.caseType().name())
                            .addValue("additionalNotes", medicalCase.additionalNotes())
                            .addValue("abstract", medicalCase.abstractText());
                })
                .toArray(SqlParameterSource[]::new);

        namedJdbcTemplate.batchUpdate(insertSql, batchParams);
        return medicalCases.stream()
                .map(mc -> {
                    String caseId = mc.id();
                    return (caseId == null || caseId.isBlank()) ? IdGenerator.generateId() : caseId.toLowerCase();
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<String> updateBatch(List<MedicalCase> medicalCases) {
        if (medicalCases.isEmpty()) {
            return List.of();
        }

        SqlParameterSource[] batchParams = medicalCases.stream()
                .map(medicalCase -> {
                    String normalizedCaseId = medicalCase.id() != null ? medicalCase.id().toLowerCase() : null;
                    return new MapSqlParameterSource()
                            .addValue("id", normalizedCaseId)
                            .addValue("patientAge", medicalCase.patientAge())
                            .addValue("chiefComplaint", medicalCase.chiefComplaint())
                            .addValue("symptoms", medicalCase.symptoms())
                            .addValue("currentDiagnosis", medicalCase.currentDiagnosis())
                            .addValue("icd10Codes", medicalCase.icd10Codes() != null ? medicalCase.icd10Codes().toArray(new String[0]) : new String[0])
                            .addValue("snomedCodes", medicalCase.snomedCodes() != null ? medicalCase.snomedCodes().toArray(new String[0]) : new String[0])
                            .addValue("urgencyLevel", medicalCase.urgencyLevel() != null ? medicalCase.urgencyLevel().name() : UrgencyLevel.MEDIUM.name())
                            .addValue("requiredSpecialty", medicalCase.requiredSpecialty())
                            .addValue("caseType", medicalCase.caseType().name())
                            .addValue("additionalNotes", medicalCase.additionalNotes())
                            .addValue("abstract", medicalCase.abstractText());
                })
                .toArray(SqlParameterSource[]::new);

        int[] updateCounts = namedJdbcTemplate.batchUpdate(updateSql, batchParams);

        // Check if any update failed (0 rows updated)
        for (int i = 0; i < updateCounts.length; i++) {
            if (updateCounts[i] == 0) {
                String normalizedCaseId = medicalCases.get(i).id() != null ? medicalCases.get(i).id().toLowerCase() : null;
                throw new org.springframework.dao.EmptyResultDataAccessException(
                        "Medical case not found with id: " + normalizedCaseId, 1);
            }
        }

        return medicalCases.stream()
                .map(mc -> mc.id() != null ? mc.id().toLowerCase() : null)
                .collect(Collectors.toList());
    }

    @Override
    public int deleteAll() {
        return namedJdbcTemplate.update(deleteAllSql, Map.of());
    }

    @Override
    public List<MedicalCase> findWithoutEmbeddings() {
        return namedJdbcTemplate.query(findWithoutEmbeddingsSql, Map.of(), medicalCaseMapper);
    }

    @Override
    public List<MedicalCase> findWithoutDescriptions() {
        return namedJdbcTemplate.query(findWithoutDescriptionsSql, Map.of(), medicalCaseMapper);
    }

    @Override
    public void updateAbstract(String caseId, String abstractText) {
        String normalizedCaseId = caseId != null ? caseId.toLowerCase() : null;
        Map<String, Object> params = new HashMap<>();
        params.put("id", normalizedCaseId);
        params.put("abstract", abstractText);
        namedJdbcTemplate.update(updateAbstractSql, params);
    }

    @Override
    public List<MedicalCase> search(String query, String specialty, String urgencyLevel, String caseId, int offset,
                                    int maxResults) {
        Map<String, Object> params = new HashMap<>();

        // Normalize query - use null for empty/blank strings
        String normalizedQuery = (query != null && !query.isBlank()) ? query : null;
        params.put("query", normalizedQuery);

        // Normalize specialty - use null for empty/blank strings
        String normalizedSpecialty = (specialty != null && !specialty.isBlank()) ? specialty : null;
        params.put("specialty", normalizedSpecialty);

        // Normalize urgencyLevel - use null for empty/blank strings
        String normalizedUrgencyLevel = (urgencyLevel != null && !urgencyLevel.isBlank()) ? urgencyLevel : null;
        params.put("urgencyLevel", normalizedUrgencyLevel);

        // Normalize caseId - use null for empty/blank strings
        String normalizedCaseId = (caseId != null && !caseId.isBlank()) ? caseId.trim() : null;
        params.put("caseId", normalizedCaseId);

        params.put("offset", Math.max(0, offset));
        params.put("maxResults", maxResults);

        return namedJdbcTemplate.query(searchSql, params, medicalCaseMapper);
    }

    @Override
    public void updateEmbedding(String caseId, List<Double> embedding, int dimension) {
        // Normalize case ID to lowercase for case-insensitive lookup
        // Case IDs are 24-character hex strings (CHAR(24)) and should be case-insensitive
        String normalizedCaseId = caseId != null ? caseId.toLowerCase() : null;

        // Convert to float array
        float[] embeddingArray = new float[embedding.size()];
        for (int i = 0; i < embedding.size(); i++) {
            embeddingArray[i] = embedding.get(i).floatValue();
        }

        // Normalize to 1536 dimensions (database schema supports max 1536)
        float[] normalizedEmbedding = normalizeEmbeddingDimension(embeddingArray, 1536);
        int normalizedDimension = 1536; // Always use normalized dimension

        String vectorString = formatVector(normalizedEmbedding);

        Map<String, Object> params = new HashMap<>();
        params.put("id", normalizedCaseId);
        params.put("embedding", vectorString);
        params.put("dimension", normalizedDimension);

        namedJdbcTemplate.update(updateEmbeddingSql, params);
    }

    /**
     * Normalizes embedding to target dimension.
     */
    private float[] normalizeEmbeddingDimension(float[] embedding, int targetDimension) {
        if (embedding.length == targetDimension) {
            return embedding;
        }

        float[] normalized = new float[targetDimension];
        int copyLength = Math.min(embedding.length, targetDimension);
        System.arraycopy(embedding, 0, normalized, 0, copyLength);
        // Remaining elements are already zero (default float value)

        return normalized;
    }

    /**
     * Formats float array as PostgreSQL vector string.
     */
    private String formatVector(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(String.format("%.6f", vector[i]));
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public boolean hasEmbedding(String caseId) {
        String normalizedCaseId = caseId != null ? caseId.toLowerCase() : null;
        Map<String, Object> params = Map.of("caseId", normalizedCaseId);
        Boolean hasEmbedding = namedJdbcTemplate.queryForObject(hasEmbeddingSql, params, Boolean.class);
        return hasEmbedding != null && hasEmbedding;
    }

    @Override
    public Double calculateVectorSimilarity(String queryCaseId, List<String> doctorCaseIds) {
        String normalizedQueryCaseId = queryCaseId != null ? queryCaseId.toLowerCase() : null;

        // Normalize doctor case IDs to lowercase
        String[] normalizedDoctorCaseIds = doctorCaseIds.stream()
                .map(caseId -> caseId != null ? caseId.toLowerCase() : null)
                .toArray(String[]::new);

        Map<String, Object> params = new HashMap<>();
        params.put("queryCaseId", normalizedQueryCaseId);
        params.put("doctorCaseIds", normalizedDoctorCaseIds);

        return namedJdbcTemplate.queryForObject(calculateVectorSimilaritySql, params, Double.class);
    }
}
