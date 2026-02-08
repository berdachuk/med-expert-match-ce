package com.berdachuk.medexpertmatch.doctor.repository.impl;

import com.berdachuk.medexpertmatch.core.exception.RetrievalException;
import com.berdachuk.medexpertmatch.core.repository.sql.InjectSql;
import com.berdachuk.medexpertmatch.doctor.domain.Doctor;
import com.berdachuk.medexpertmatch.doctor.repository.DoctorRepository;
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
 * Repository for doctor data access.
 */
@Slf4j
@Repository
public class DoctorRepositoryImpl implements DoctorRepository {

    private final NamedParameterJdbcTemplate namedJdbcTemplate;
    private final DoctorMapper doctorMapper;

    @InjectSql("/sql/doctor/findById.sql")
    private String findByIdSql;

    @InjectSql("/sql/doctor/findByEmail.sql")
    private String findByEmailSql;

    @InjectSql("/sql/doctor/findByIds.sql")
    private String findByIdsSql;

    @InjectSql("/sql/doctor/findDoctorIdsByName.sql")
    private String findDoctorIdsByNameSql;

    @InjectSql("/sql/doctor/findDoctorIdsByNameSimilarity.sql")
    private String findDoctorIdsByNameSimilaritySql;

    @InjectSql("/sql/doctor/findDoctorIdsByFacilityId.sql")
    private String findDoctorIdsByFacilityIdSql;

    @InjectSql("/sql/doctor/findBySpecialty.sql")
    private String findBySpecialtySql;

    @InjectSql("/sql/doctor/findTelehealthEnabled.sql")
    private String findTelehealthEnabledSql;

    @InjectSql("/sql/doctor/insert.sql")
    private String insertSql;

    @InjectSql("/sql/doctor/update.sql")
    private String updateSql;

    @InjectSql("/sql/doctor/findAllIds.sql")
    private String findAllIdsSql;

    @InjectSql("/sql/doctor/deleteAll.sql")
    private String deleteAllSql;

    public DoctorRepositoryImpl(
            NamedParameterJdbcTemplate namedJdbcTemplate,
            DoctorMapper doctorMapper) {
        this.namedJdbcTemplate = namedJdbcTemplate;
        this.doctorMapper = doctorMapper;
    }

    @Override
    public Optional<Doctor> findById(String doctorId) {
        Map<String, Object> params = Map.of("id", doctorId);
        List<Doctor> results = namedJdbcTemplate.query(findByIdSql, params, doctorMapper);
        return Optional.ofNullable(DataAccessUtils.uniqueResult(results));
    }

    @Override
    public Optional<Doctor> findByEmail(String email) {
        Map<String, Object> params = Map.of("email", email);
        List<Doctor> results = namedJdbcTemplate.query(findByEmailSql, params, doctorMapper);
        return Optional.ofNullable(DataAccessUtils.uniqueResult(results));
    }

    @Override
    public List<Doctor> findByIds(List<String> doctorIds) {
        if (doctorIds.isEmpty()) {
            return List.of();
        }

        Map<String, Object> params = Map.of("ids", doctorIds.toArray(new String[0]));
        return namedJdbcTemplate.query(findByIdsSql, params, doctorMapper);
    }

    @Override
    public List<String> findDoctorIdsByName(String name, int maxResults) {
        if (name == null || name.isBlank()) {
            return List.of();
        }

        String namePattern = "%" + name.trim() + "%";

        Map<String, Object> params = Map.of(
                "namePattern", namePattern,
                "maxResults", maxResults
        );

        return namedJdbcTemplate.query(findDoctorIdsByNameSql, params, (rs, rowNum) -> rs.getString("id"));
    }

    @Override
    public List<String> findDoctorIdsByNameSimilarity(String name, double similarityThreshold, int maxResults) {
        if (name == null || name.isBlank()) {
            return List.of();
        }

        Map<String, Object> params = Map.of(
                "name", name.trim(),
                "similarityThreshold", similarityThreshold,
                "maxResults", maxResults
        );

        try {
            return namedJdbcTemplate.query(findDoctorIdsByNameSimilaritySql, params, (rs, rowNum) -> rs.getString("id"));
        } catch (org.springframework.jdbc.BadSqlGrammarException |
                 org.springframework.jdbc.UncategorizedSQLException e) {
            throw new RetrievalException("pg_trgm extension is required for doctor name similarity search", e);
        }
    }

    @Override
    public List<String> findDoctorIdsByFacilityId(String facilityId, int limit) {
        if (facilityId == null || facilityId.isBlank()) {
            return List.of();
        }
        Map<String, Object> params = Map.of(
                "facilityId", facilityId,
                "limit", limit > 0 ? limit : 500
        );
        return namedJdbcTemplate.query(findDoctorIdsByFacilityIdSql, params, (rs, rowNum) -> rs.getString("id"));
    }

    @Override
    public List<Doctor> findBySpecialty(String specialty, int maxResults) {
        if (specialty == null || specialty.isBlank()) {
            return List.of();
        }

        Map<String, Object> params = Map.of(
                "specialty", specialty,
                "maxResults", maxResults
        );

        return namedJdbcTemplate.query(findBySpecialtySql, params, doctorMapper);
    }

    @Override
    public List<Doctor> findTelehealthEnabled(int maxResults) {
        Map<String, Object> params = Map.of("maxResults", maxResults);
        return namedJdbcTemplate.query(findTelehealthEnabledSql, params, doctorMapper);
    }

    @Override
    public String insert(Doctor doctor) {
        Map<String, Object> params = new HashMap<>();
        params.put("id", doctor.id());
        params.put("name", doctor.name());
        params.put("email", doctor.email());
        params.put("specialties", doctor.specialties() != null ? doctor.specialties().toArray(new String[0]) : new String[0]);
        params.put("certifications", doctor.certifications() != null ? doctor.certifications().toArray(new String[0]) : new String[0]);
        params.put("facilityIds", doctor.facilityIds() != null ? doctor.facilityIds().toArray(new String[0]) : new String[0]);
        params.put("telehealthEnabled", doctor.telehealthEnabled());
        params.put("availabilityStatus", doctor.availabilityStatus());

        return namedJdbcTemplate.queryForObject(insertSql, params, String.class);
    }

    @Override
    public String update(Doctor doctor) {
        Map<String, Object> params = new HashMap<>();
        params.put("id", doctor.id());
        params.put("name", doctor.name());
        params.put("email", doctor.email());
        params.put("specialties", doctor.specialties() != null ? doctor.specialties().toArray(new String[0]) : new String[0]);
        params.put("certifications", doctor.certifications() != null ? doctor.certifications().toArray(new String[0]) : new String[0]);
        params.put("facilityIds", doctor.facilityIds() != null ? doctor.facilityIds().toArray(new String[0]) : new String[0]);
        params.put("telehealthEnabled", doctor.telehealthEnabled());
        params.put("availabilityStatus", doctor.availabilityStatus());

        int rowsUpdated = namedJdbcTemplate.update(updateSql, params);
        if (rowsUpdated == 0) {
            throw new org.springframework.dao.EmptyResultDataAccessException("Doctor not found with id: " + doctor.id(), 1);
        }
        return doctor.id();
    }

    @Override
    public List<String> findAllIds(int limit) {
        Map<String, Object> params = Map.of("limit", limit > 0 ? limit : Integer.MAX_VALUE);
        return namedJdbcTemplate.query(findAllIdsSql, params, (rs, rowNum) -> rs.getString("id"));
    }

    @Override
    public List<String> insertBatch(List<Doctor> doctors) {
        if (doctors.isEmpty()) {
            return List.of();
        }

        SqlParameterSource[] batchParams = doctors.stream()
                .map(doctor -> new MapSqlParameterSource()
                        .addValue("id", doctor.id())
                        .addValue("name", doctor.name())
                        .addValue("email", doctor.email())
                        .addValue("specialties", doctor.specialties() != null ? doctor.specialties().toArray(new String[0]) : new String[0])
                        .addValue("certifications", doctor.certifications() != null ? doctor.certifications().toArray(new String[0]) : new String[0])
                        .addValue("facilityIds", doctor.facilityIds() != null ? doctor.facilityIds().toArray(new String[0]) : new String[0])
                        .addValue("telehealthEnabled", doctor.telehealthEnabled())
                        .addValue("availabilityStatus", doctor.availabilityStatus()))
                .toArray(SqlParameterSource[]::new);

        namedJdbcTemplate.batchUpdate(insertSql, batchParams);
        return doctors.stream().map(Doctor::id).collect(Collectors.toList());
    }

    @Override
    public List<String> updateBatch(List<Doctor> doctors) {
        if (doctors.isEmpty()) {
            return List.of();
        }

        SqlParameterSource[] batchParams = doctors.stream()
                .map(doctor -> new MapSqlParameterSource()
                        .addValue("id", doctor.id())
                        .addValue("name", doctor.name())
                        .addValue("email", doctor.email())
                        .addValue("specialties", doctor.specialties() != null ? doctor.specialties().toArray(new String[0]) : new String[0])
                        .addValue("certifications", doctor.certifications() != null ? doctor.certifications().toArray(new String[0]) : new String[0])
                        .addValue("facilityIds", doctor.facilityIds() != null ? doctor.facilityIds().toArray(new String[0]) : new String[0])
                        .addValue("telehealthEnabled", doctor.telehealthEnabled())
                        .addValue("availabilityStatus", doctor.availabilityStatus()))
                .toArray(SqlParameterSource[]::new);

        int[] updateCounts = namedJdbcTemplate.batchUpdate(updateSql, batchParams);

        // Check if any update failed (0 rows updated)
        for (int i = 0; i < updateCounts.length; i++) {
            if (updateCounts[i] == 0) {
                throw new org.springframework.dao.EmptyResultDataAccessException(
                        "Doctor not found with id: " + doctors.get(i).id(), 1);
            }
        }

        return doctors.stream().map(Doctor::id).collect(Collectors.toList());
    }

    @Override
    public int deleteAll() {
        return namedJdbcTemplate.update(deleteAllSql, Map.of());
    }
}
