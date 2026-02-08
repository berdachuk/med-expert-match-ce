package com.berdachuk.medexpertmatch.facility.repository.impl;

import com.berdachuk.medexpertmatch.core.repository.sql.InjectSql;
import com.berdachuk.medexpertmatch.facility.domain.Facility;
import com.berdachuk.medexpertmatch.facility.repository.FacilityRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Repository for facility data access.
 */
@Slf4j
@Repository
public class FacilityRepositoryImpl implements FacilityRepository {

    private final NamedParameterJdbcTemplate namedJdbcTemplate;
    private final FacilityMapper facilityMapper;

    @InjectSql("/sql/facility/findById.sql")
    private String findByIdSql;

    @InjectSql("/sql/facility/findAll.sql")
    private String findAllSql;

    @InjectSql("/sql/facility/findAllIds.sql")
    private String findAllIdsSql;

    @InjectSql("/sql/facility/insert.sql")
    private String insertSql;

    @InjectSql("/sql/facility/update.sql")
    private String updateSql;

    @InjectSql("/sql/facility/deleteAll.sql")
    private String deleteAllSql;

    public FacilityRepositoryImpl(
            NamedParameterJdbcTemplate namedJdbcTemplate,
            FacilityMapper facilityMapper) {
        this.namedJdbcTemplate = namedJdbcTemplate;
        this.facilityMapper = facilityMapper;
    }

    @Override
    public Optional<Facility> findById(String facilityId) {
        Map<String, Object> params = Map.of("id", facilityId);
        List<Facility> results = namedJdbcTemplate.query(findByIdSql, params, facilityMapper);
        return Optional.ofNullable(DataAccessUtils.uniqueResult(results));
    }

    @Override
    public List<Facility> findAll() {
        return namedJdbcTemplate.query(findAllSql, facilityMapper);
    }

    @Override
    public List<String> findAllIds(int limit) {
        Map<String, Object> params = Map.of("limit", limit);
        return namedJdbcTemplate.query(findAllIdsSql, params, (rs, rowNum) -> rs.getString("id"));
    }

    @Override
    public String insert(Facility facility) {
        Map<String, Object> params = new HashMap<>();
        params.put("id", facility.id());
        params.put("name", facility.name());
        params.put("facilityType", facility.facilityType());
        params.put("locationCity", facility.locationCity());
        params.put("locationState", facility.locationState());
        params.put("locationCountry", facility.locationCountry());
        params.put("locationLatitude", facility.locationLatitude());
        params.put("locationLongitude", facility.locationLongitude());
        params.put("capabilities", facility.capabilities() != null ? facility.capabilities().toArray(new String[0]) : new String[0]);
        params.put("capacity", facility.capacity());
        params.put("currentOccupancy", facility.currentOccupancy());

        return namedJdbcTemplate.queryForObject(insertSql, params, String.class);
    }

    @Override
    public String update(Facility facility) {
        Map<String, Object> params = new HashMap<>();
        params.put("id", facility.id());
        params.put("name", facility.name());
        params.put("facilityType", facility.facilityType());
        params.put("locationCity", facility.locationCity());
        params.put("locationState", facility.locationState());
        params.put("locationCountry", facility.locationCountry());
        params.put("locationLatitude", facility.locationLatitude());
        params.put("locationLongitude", facility.locationLongitude());
        params.put("capabilities", facility.capabilities() != null ? facility.capabilities().toArray(new String[0]) : new String[0]);
        params.put("capacity", facility.capacity());
        params.put("currentOccupancy", facility.currentOccupancy());

        return namedJdbcTemplate.queryForObject(updateSql, params, String.class);
    }

    @Override
    public int deleteAll() {
        return namedJdbcTemplate.update(deleteAllSql, Map.of());
    }
}
