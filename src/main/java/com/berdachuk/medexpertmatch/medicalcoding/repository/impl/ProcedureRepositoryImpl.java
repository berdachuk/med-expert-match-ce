package com.berdachuk.medexpertmatch.medicalcoding.repository.impl;

import com.berdachuk.medexpertmatch.core.repository.sql.InjectSql;
import com.berdachuk.medexpertmatch.medicalcoding.domain.Procedure;
import com.berdachuk.medexpertmatch.medicalcoding.repository.ProcedureRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Repository for medical procedure data access.
 */
@Slf4j
@Repository
public class ProcedureRepositoryImpl implements ProcedureRepository {

    private final NamedParameterJdbcTemplate namedJdbcTemplate;
    private final ProcedureMapper procedureMapper;

    @InjectSql("/sql/medicalcoding/procedure/findById.sql")
    private String findByIdSql;

    @InjectSql("/sql/medicalcoding/procedure/findByName.sql")
    private String findByNameSql;

    @InjectSql("/sql/medicalcoding/procedure/findByNormalizedName.sql")
    private String findByNormalizedNameSql;

    @InjectSql("/sql/medicalcoding/procedure/findByCategory.sql")
    private String findByCategorySql;

    @InjectSql("/sql/medicalcoding/procedure/findAll.sql")
    private String findAllSql;

    @InjectSql("/sql/medicalcoding/procedure/insert.sql")
    private String insertSql;

    @InjectSql("/sql/medicalcoding/procedure/update.sql")
    private String updateSql;

    @InjectSql("/sql/medicalcoding/procedure/deleteAll.sql")
    private String deleteAllSql;

    public ProcedureRepositoryImpl(
            NamedParameterJdbcTemplate namedJdbcTemplate,
            ProcedureMapper procedureMapper) {
        this.namedJdbcTemplate = namedJdbcTemplate;
        this.procedureMapper = procedureMapper;
    }

    @Override
    public Optional<Procedure> findById(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        Map<String, Object> params = Map.of("id", id);
        List<Procedure> results = namedJdbcTemplate.query(findByIdSql, params, procedureMapper);
        return Optional.ofNullable(DataAccessUtils.uniqueResult(results));
    }

    @Override
    public Optional<Procedure> findByName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        Map<String, Object> params = Map.of("name", name.toLowerCase());
        List<Procedure> results = namedJdbcTemplate.query(findByNameSql, params, procedureMapper);
        return Optional.ofNullable(DataAccessUtils.uniqueResult(results));
    }

    @Override
    public Optional<Procedure> findByNormalizedName(String normalizedName) {
        if (normalizedName == null || normalizedName.isBlank()) {
            return Optional.empty();
        }
        Map<String, Object> params = Map.of("normalizedName", normalizedName.toLowerCase());
        List<Procedure> results = namedJdbcTemplate.query(findByNormalizedNameSql, params, procedureMapper);
        return Optional.ofNullable(DataAccessUtils.uniqueResult(results));
    }

    @Override
    public List<Procedure> findByCategory(String category) {
        if (category == null || category.isBlank()) {
            return List.of();
        }
        Map<String, Object> params = Map.of("category", category);
        return namedJdbcTemplate.query(findByCategorySql, params, procedureMapper);
    }

    @Override
    public List<Procedure> findAll() {
        return namedJdbcTemplate.query(findAllSql, Map.of(), procedureMapper);
    }

    @Override
    public String insert(Procedure procedure) {
        Map<String, Object> params = new HashMap<>();
        params.put("id", procedure.id());
        params.put("name", procedure.name());
        params.put("normalizedName", procedure.normalizedName());
        params.put("description", procedure.description());
        params.put("category", procedure.category());

        namedJdbcTemplate.queryForObject(insertSql, params, String.class);
        return procedure.id();
    }

    @Override
    public String update(Procedure procedure) {
        Map<String, Object> params = new HashMap<>();
        params.put("id", procedure.id());
        params.put("name", procedure.name());
        params.put("normalizedName", procedure.normalizedName());
        params.put("description", procedure.description());
        params.put("category", procedure.category());

        int rowsUpdated = namedJdbcTemplate.update(updateSql, params);
        if (rowsUpdated == 0) {
            throw new org.springframework.dao.EmptyResultDataAccessException("Procedure not found with id: " + procedure.id(), 1);
        }
        return procedure.id();
    }

    @Override
    public int deleteAll() {
        return namedJdbcTemplate.update(deleteAllSql, Map.of());
    }
}
