package com.berdachuk.medexpertmatch.medicalcoding.repository.impl;

import com.berdachuk.medexpertmatch.core.repository.sql.InjectSql;
import com.berdachuk.medexpertmatch.medicalcoding.domain.ICD10Code;
import com.berdachuk.medexpertmatch.medicalcoding.repository.ICD10CodeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Repository for ICD-10 code data access.
 */
@Slf4j
@Repository
public class ICD10CodeRepositoryImpl implements ICD10CodeRepository {

    private final NamedParameterJdbcTemplate namedJdbcTemplate;
    private final ICD10CodeMapper icd10CodeMapper;

    @InjectSql("/sql/medicalcoding/findById.sql")
    private String findByIdSql;

    @InjectSql("/sql/medicalcoding/findByCode.sql")
    private String findByCodeSql;

    @InjectSql("/sql/medicalcoding/findByCodes.sql")
    private String findByCodesSql;

    @InjectSql("/sql/medicalcoding/findByCategory.sql")
    private String findByCategorySql;

    @InjectSql("/sql/medicalcoding/findByParentCode.sql")
    private String findByParentCodeSql;

    @InjectSql("/sql/medicalcoding/findAll.sql")
    private String findAllSql;

    @InjectSql("/sql/medicalcoding/insert.sql")
    private String insertSql;

    @InjectSql("/sql/medicalcoding/update.sql")
    private String updateSql;

    @InjectSql("/sql/medicalcoding/deleteAll.sql")
    private String deleteAllSql;

    public ICD10CodeRepositoryImpl(
            NamedParameterJdbcTemplate namedJdbcTemplate,
            ICD10CodeMapper icd10CodeMapper) {
        this.namedJdbcTemplate = namedJdbcTemplate;
        this.icd10CodeMapper = icd10CodeMapper;
    }

    @Override
    public Optional<ICD10Code> findById(String codeId) {
        Map<String, Object> params = Map.of("id", codeId);
        List<ICD10Code> results = namedJdbcTemplate.query(findByIdSql, params, icd10CodeMapper);
        return Optional.ofNullable(DataAccessUtils.uniqueResult(results));
    }

    @Override
    public Optional<ICD10Code> findByCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }

        Map<String, Object> params = Map.of("code", code);
        List<ICD10Code> results = namedJdbcTemplate.query(findByCodeSql, params, icd10CodeMapper);
        return Optional.ofNullable(DataAccessUtils.uniqueResult(results));
    }

    @Override
    public List<ICD10Code> findByCodes(List<String> codes) {
        if (codes.isEmpty()) {
            return List.of();
        }

        Map<String, Object> params = Map.of("codes", codes.toArray(new String[0]));
        return namedJdbcTemplate.query(findByCodesSql, params, icd10CodeMapper);
    }

    @Override
    public List<ICD10Code> findByCategory(String category, int maxResults) {
        if (category == null || category.isBlank()) {
            return List.of();
        }

        Map<String, Object> params = Map.of(
                "category", category,
                "maxResults", maxResults
        );

        return namedJdbcTemplate.query(findByCategorySql, params, icd10CodeMapper);
    }

    @Override
    public List<ICD10Code> findByParentCode(String parentCode) {
        if (parentCode == null || parentCode.isBlank()) {
            return List.of();
        }

        Map<String, Object> params = Map.of("parentCode", parentCode);
        return namedJdbcTemplate.query(findByParentCodeSql, params, icd10CodeMapper);
    }

    @Override
    public List<ICD10Code> findAll() {
        return namedJdbcTemplate.query(findAllSql, Map.of(), icd10CodeMapper);
    }

    @Override
    public String insert(ICD10Code icd10Code) {
        Map<String, Object> params = new HashMap<>();
        params.put("id", icd10Code.id());
        params.put("code", icd10Code.code());
        params.put("description", icd10Code.description());
        params.put("category", icd10Code.category());
        params.put("parentCode", icd10Code.parentCode());
        params.put("relatedCodes", icd10Code.relatedCodes() != null ? icd10Code.relatedCodes().toArray(new String[0]) : new String[0]);

        return namedJdbcTemplate.queryForObject(insertSql, params, String.class);
    }

    @Override
    public String update(ICD10Code icd10Code) {
        Map<String, Object> params = new HashMap<>();
        params.put("id", icd10Code.id());
        params.put("code", icd10Code.code());
        params.put("description", icd10Code.description());
        params.put("category", icd10Code.category());
        params.put("parentCode", icd10Code.parentCode());
        params.put("relatedCodes", icd10Code.relatedCodes() != null ? icd10Code.relatedCodes().toArray(new String[0]) : new String[0]);

        int rowsUpdated = namedJdbcTemplate.update(updateSql, params);
        if (rowsUpdated == 0) {
            throw new org.springframework.dao.EmptyResultDataAccessException("ICD-10 code not found with id: " + icd10Code.id(), 1);
        }
        return icd10Code.id();
    }

    @Override
    public List<String> insertBatch(List<ICD10Code> icd10Codes) {
        if (icd10Codes.isEmpty()) {
            return List.of();
        }

        SqlParameterSource[] batchParams = icd10Codes.stream()
                .map(code -> new MapSqlParameterSource()
                        .addValue("id", code.id())
                        .addValue("code", code.code())
                        .addValue("description", code.description())
                        .addValue("category", code.category())
                        .addValue("parentCode", code.parentCode())
                        .addValue("relatedCodes", code.relatedCodes() != null ? code.relatedCodes().toArray(new String[0]) : new String[0]))
                .toArray(SqlParameterSource[]::new);

        namedJdbcTemplate.batchUpdate(insertSql, batchParams);
        return icd10Codes.stream().map(ICD10Code::id).collect(Collectors.toList());
    }

    @Override
    public List<String> updateBatch(List<ICD10Code> icd10Codes) {
        if (icd10Codes.isEmpty()) {
            return List.of();
        }

        SqlParameterSource[] batchParams = icd10Codes.stream()
                .map(code -> new MapSqlParameterSource()
                        .addValue("id", code.id())
                        .addValue("code", code.code())
                        .addValue("description", code.description())
                        .addValue("category", code.category())
                        .addValue("parentCode", code.parentCode())
                        .addValue("relatedCodes", code.relatedCodes() != null ? code.relatedCodes().toArray(new String[0]) : new String[0]))
                .toArray(SqlParameterSource[]::new);

        int[] updateCounts = namedJdbcTemplate.batchUpdate(updateSql, batchParams);

        // Check if any update failed (0 rows updated)
        for (int i = 0; i < updateCounts.length; i++) {
            if (updateCounts[i] == 0) {
                throw new org.springframework.dao.EmptyResultDataAccessException(
                        "ICD-10 code not found with id: " + icd10Codes.get(i).id(), 1);
            }
        }

        return icd10Codes.stream().map(ICD10Code::id).collect(Collectors.toList());
    }

    @Override
    public Set<String> findExistingCodes(List<String> codes) {
        if (codes.isEmpty()) {
            return Set.of();
        }

        List<ICD10Code> existingCodes = findByCodes(codes);
        return existingCodes.stream()
                .map(ICD10Code::code)
                .collect(Collectors.toSet());
    }

    @Override
    public int deleteAll() {
        return namedJdbcTemplate.update(deleteAllSql, Map.of());
    }
}
