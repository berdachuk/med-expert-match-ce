package com.berdachuk.medexpertmatch.doctor.repository.impl;

import com.berdachuk.medexpertmatch.core.repository.sql.InjectSql;
import com.berdachuk.medexpertmatch.doctor.domain.MedicalSpecialty;
import com.berdachuk.medexpertmatch.doctor.repository.MedicalSpecialtyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Repository for medical specialty data access.
 */
@Slf4j
@Repository
public class MedicalSpecialtyRepositoryImpl implements MedicalSpecialtyRepository {

    private final NamedParameterJdbcTemplate namedJdbcTemplate;
    private final MedicalSpecialtyMapper medicalSpecialtyMapper;

    @InjectSql("/sql/doctor/medicalspecialty/findById.sql")
    private String findByIdSql;

    @InjectSql("/sql/doctor/medicalspecialty/findByName.sql")
    private String findByNameSql;

    @InjectSql("/sql/doctor/medicalspecialty/findAll.sql")
    private String findAllSql;

    @InjectSql("/sql/doctor/medicalspecialty/insert.sql")
    private String insertSql;

    @InjectSql("/sql/doctor/medicalspecialty/update.sql")
    private String updateSql;

    @InjectSql("/sql/doctor/medicalspecialty/deleteAll.sql")
    private String deleteAllSql;

    public MedicalSpecialtyRepositoryImpl(
            NamedParameterJdbcTemplate namedJdbcTemplate,
            MedicalSpecialtyMapper medicalSpecialtyMapper) {
        this.namedJdbcTemplate = namedJdbcTemplate;
        this.medicalSpecialtyMapper = medicalSpecialtyMapper;
    }

    @Override
    public Optional<MedicalSpecialty> findById(String specialtyId) {
        Map<String, Object> params = Map.of("id", specialtyId);
        List<MedicalSpecialty> results = namedJdbcTemplate.query(findByIdSql, params, medicalSpecialtyMapper);
        return Optional.ofNullable(DataAccessUtils.uniqueResult(results));
    }

    @Override
    public Optional<MedicalSpecialty> findByName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }

        Map<String, Object> params = Map.of("name", name);
        List<MedicalSpecialty> results = namedJdbcTemplate.query(findByNameSql, params, medicalSpecialtyMapper);
        return Optional.ofNullable(DataAccessUtils.uniqueResult(results));
    }

    @Override
    public List<MedicalSpecialty> findAll() {
        return namedJdbcTemplate.query(findAllSql, medicalSpecialtyMapper);
    }

    @Override
    public String insert(MedicalSpecialty specialty) {
        Map<String, Object> params = new HashMap<>();
        params.put("id", specialty.id());
        params.put("name", specialty.name());
        params.put("normalizedName", specialty.normalizedName());
        params.put("description", specialty.description());
        params.put("icd10CodeRanges", specialty.icd10CodeRanges() != null ? specialty.icd10CodeRanges().toArray(new String[0]) : new String[0]);
        params.put("relatedSpecialties", specialty.relatedSpecialtyIds() != null ? specialty.relatedSpecialtyIds().toArray(new String[0]) : new String[0]);

        return namedJdbcTemplate.queryForObject(insertSql, params, String.class);
    }

    @Override
    public String update(MedicalSpecialty specialty) {
        Map<String, Object> params = new HashMap<>();
        params.put("id", specialty.id());
        params.put("name", specialty.name());
        params.put("normalizedName", specialty.normalizedName());
        params.put("description", specialty.description());
        params.put("icd10CodeRanges", specialty.icd10CodeRanges() != null ? specialty.icd10CodeRanges().toArray(new String[0]) : new String[0]);
        params.put("relatedSpecialties", specialty.relatedSpecialtyIds() != null ? specialty.relatedSpecialtyIds().toArray(new String[0]) : new String[0]);

        return namedJdbcTemplate.queryForObject(updateSql, params, String.class);
    }

    @Override
    public int deleteAll() {
        return namedJdbcTemplate.update(deleteAllSql, Map.of());
    }
}
