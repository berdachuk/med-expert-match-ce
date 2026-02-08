package com.berdachuk.medexpertmatch.clinicalexperience.repository.impl;

import com.berdachuk.medexpertmatch.clinicalexperience.domain.ClinicalExperience;
import com.berdachuk.medexpertmatch.clinicalexperience.repository.ClinicalExperienceRepository;
import com.berdachuk.medexpertmatch.core.repository.sql.InjectSql;
import com.berdachuk.medexpertmatch.core.util.IdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Repository for clinical experience data access.
 */
@Slf4j
@Repository
public class ClinicalExperienceRepositoryImpl implements ClinicalExperienceRepository {

    private final NamedParameterJdbcTemplate namedJdbcTemplate;
    private final ClinicalExperienceMapper clinicalExperienceMapper;

    @InjectSql("/sql/clinicalexperience/findById.sql")
    private String findByIdSql;

    @InjectSql("/sql/clinicalexperience/findByDoctorId.sql")
    private String findByDoctorIdSql;

    @InjectSql("/sql/clinicalexperience/findByCaseId.sql")
    private String findByCaseIdSql;

    @InjectSql("/sql/clinicalexperience/findByDoctorIds.sql")
    private String findByDoctorIdsSql;

    @InjectSql("/sql/clinicalexperience/findByCaseIds.sql")
    private String findByCaseIdsSql;

    @InjectSql("/sql/clinicalexperience/findByIds.sql")
    private String findByIdsSql;

    @InjectSql("/sql/clinicalexperience/insert.sql")
    private String insertSql;

    @InjectSql("/sql/clinicalexperience/update.sql")
    private String updateSql;

    @InjectSql("/sql/clinicalexperience/deleteAll.sql")
    private String deleteAllSql;

    public ClinicalExperienceRepositoryImpl(
            NamedParameterJdbcTemplate namedJdbcTemplate,
            ClinicalExperienceMapper clinicalExperienceMapper) {
        this.namedJdbcTemplate = namedJdbcTemplate;
        this.clinicalExperienceMapper = clinicalExperienceMapper;
    }

    @Override
    public Optional<ClinicalExperience> findById(String experienceId) {
        Map<String, Object> params = Map.of("id", experienceId);
        List<ClinicalExperience> results = namedJdbcTemplate.query(findByIdSql, params, clinicalExperienceMapper);
        return Optional.ofNullable(DataAccessUtils.uniqueResult(results));
    }

    @Override
    public List<ClinicalExperience> findByDoctorId(String doctorId) {
        if (doctorId == null || doctorId.isBlank()) {
            return List.of();
        }

        Map<String, Object> params = Map.of("doctorId", doctorId);
        return namedJdbcTemplate.query(findByDoctorIdSql, params, clinicalExperienceMapper);
    }

    @Override
    public List<ClinicalExperience> findByCaseId(String caseId) {
        if (caseId == null || caseId.isBlank()) {
            return List.of();
        }

        Map<String, Object> params = Map.of("caseId", caseId);
        return namedJdbcTemplate.query(findByCaseIdSql, params, clinicalExperienceMapper);
    }

    @Override
    public Map<String, List<ClinicalExperience>> findByDoctorIds(List<String> doctorIds) {
        if (doctorIds.isEmpty()) {
            return Map.of();
        }

        Map<String, Object> params = Map.of("doctorIds", doctorIds.toArray(new String[0]));
        List<ClinicalExperience> experiences = namedJdbcTemplate.query(findByDoctorIdsSql, params, clinicalExperienceMapper);

        // Group by doctor ID
        return experiences.stream()
                .collect(Collectors.groupingBy(ClinicalExperience::doctorId));
    }

    @Override
    public Map<String, List<ClinicalExperience>> findByCaseIds(List<String> caseIds) {
        if (caseIds.isEmpty()) {
            return Map.of();
        }

        Map<String, Object> params = Map.of("caseIds", caseIds.toArray(new String[0]));
        List<ClinicalExperience> experiences = namedJdbcTemplate.query(findByCaseIdsSql, params, clinicalExperienceMapper);

        // Group by case ID
        return experiences.stream()
                .collect(Collectors.groupingBy(ClinicalExperience::caseId));
    }

    @Override
    public String insert(ClinicalExperience clinicalExperience) {
        // Generate ID if null (for new experiences)
        String experienceId = clinicalExperience.id();
        if (experienceId == null || experienceId.isBlank()) {
            experienceId = IdGenerator.generateId();
        }

        // Normalize case ID to lowercase (case IDs are 24-character hex strings, case-insensitive)
        String normalizedCaseId = clinicalExperience.caseId() != null ? clinicalExperience.caseId().toLowerCase() : null;

        Map<String, Object> params = new HashMap<>();
        params.put("id", experienceId);
        params.put("doctorId", clinicalExperience.doctorId());
        params.put("caseId", normalizedCaseId);
        params.put("proceduresPerformed", clinicalExperience.proceduresPerformed() != null ? clinicalExperience.proceduresPerformed().toArray(new String[0]) : new String[0]);
        params.put("complexityLevel", clinicalExperience.complexityLevel());
        params.put("outcome", clinicalExperience.outcome());
        params.put("complications", clinicalExperience.complications() != null ? clinicalExperience.complications().toArray(new String[0]) : new String[0]);
        params.put("timeToResolution", clinicalExperience.timeToResolution());
        params.put("rating", clinicalExperience.rating());

        return namedJdbcTemplate.queryForObject(insertSql, params, String.class);
    }

    @Override
    public String update(ClinicalExperience clinicalExperience) {
        Map<String, Object> params = new HashMap<>();
        params.put("id", clinicalExperience.id());
        params.put("doctorId", clinicalExperience.doctorId());
        params.put("caseId", clinicalExperience.caseId());
        params.put("proceduresPerformed", clinicalExperience.proceduresPerformed() != null ? clinicalExperience.proceduresPerformed().toArray(new String[0]) : new String[0]);
        params.put("complexityLevel", clinicalExperience.complexityLevel());
        params.put("outcome", clinicalExperience.outcome());
        params.put("complications", clinicalExperience.complications() != null ? clinicalExperience.complications().toArray(new String[0]) : new String[0]);
        params.put("timeToResolution", clinicalExperience.timeToResolution());
        params.put("rating", clinicalExperience.rating());

        int rowsUpdated = namedJdbcTemplate.update(updateSql, params);
        if (rowsUpdated == 0) {
            throw new org.springframework.dao.EmptyResultDataAccessException("Clinical experience not found with id: " + clinicalExperience.id(), 1);
        }
        return clinicalExperience.id();
    }

    @Override
    public List<String> insertBatch(List<ClinicalExperience> clinicalExperiences) {
        if (clinicalExperiences.isEmpty()) {
            return List.of();
        }

        SqlParameterSource[] batchParams = clinicalExperiences.stream()
                .map(experience -> {
                    String experienceId = experience.id();
                    if (experienceId == null || experienceId.isBlank()) {
                        experienceId = IdGenerator.generateId();
                    }
                    return new MapSqlParameterSource()
                            .addValue("id", experienceId)
                            .addValue("doctorId", experience.doctorId())
                            .addValue("caseId", experience.caseId())
                            .addValue("proceduresPerformed", experience.proceduresPerformed() != null ? experience.proceduresPerformed().toArray(new String[0]) : new String[0])
                            .addValue("complexityLevel", experience.complexityLevel())
                            .addValue("outcome", experience.outcome())
                            .addValue("complications", experience.complications() != null ? experience.complications().toArray(new String[0]) : new String[0])
                            .addValue("timeToResolution", experience.timeToResolution())
                            .addValue("rating", experience.rating());
                })
                .toArray(SqlParameterSource[]::new);

        namedJdbcTemplate.batchUpdate(insertSql, batchParams);
        return clinicalExperiences.stream()
                .map(exp -> {
                    String id = exp.id();
                    return (id == null || id.isBlank()) ? IdGenerator.generateId() : id;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<String> updateBatch(List<ClinicalExperience> clinicalExperiences) {
        if (clinicalExperiences.isEmpty()) {
            return List.of();
        }

        SqlParameterSource[] batchParams = clinicalExperiences.stream()
                .map(experience -> new MapSqlParameterSource()
                        .addValue("id", experience.id())
                        .addValue("doctorId", experience.doctorId())
                        .addValue("caseId", experience.caseId())
                        .addValue("proceduresPerformed", experience.proceduresPerformed() != null ? experience.proceduresPerformed().toArray(new String[0]) : new String[0])
                        .addValue("complexityLevel", experience.complexityLevel())
                        .addValue("outcome", experience.outcome())
                        .addValue("complications", experience.complications() != null ? experience.complications().toArray(new String[0]) : new String[0])
                        .addValue("timeToResolution", experience.timeToResolution())
                        .addValue("rating", experience.rating()))
                .toArray(SqlParameterSource[]::new);

        int[] updateCounts = namedJdbcTemplate.batchUpdate(updateSql, batchParams);

        // Check if any update failed (0 rows updated)
        for (int i = 0; i < updateCounts.length; i++) {
            if (updateCounts[i] == 0) {
                throw new org.springframework.dao.EmptyResultDataAccessException(
                        "Clinical experience not found with id: " + clinicalExperiences.get(i).id(), 1);
            }
        }

        return clinicalExperiences.stream().map(ClinicalExperience::id).collect(Collectors.toList());
    }

    @Override
    public Set<String> findExistingIds(List<String> experienceIds) {
        if (experienceIds.isEmpty()) {
            return Set.of();
        }

        Map<String, Object> params = Map.of("ids", experienceIds.toArray(new String[0]));
        return namedJdbcTemplate.query(findByIdsSql, params, (rs, rowNum) -> rs.getString("id"))
                .stream()
                .collect(Collectors.toSet());
    }

    @Override
    public int deleteAll() {
        return namedJdbcTemplate.update(deleteAllSql, Map.of());
    }
}
