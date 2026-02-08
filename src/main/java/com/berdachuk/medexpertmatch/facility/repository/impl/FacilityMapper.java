package com.berdachuk.medexpertmatch.facility.repository.impl;

import com.berdachuk.medexpertmatch.facility.domain.Facility;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Row mapper for Facility entity.
 * Maps ResultSet rows to Facility records.
 */
@Component
public class FacilityMapper implements RowMapper<Facility> {

    @Override
    public Facility mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Facility(
                rs.getString("id"),
                rs.getString("name"),
                rs.getString("facility_type"),
                rs.getString("location_city"),
                rs.getString("location_state"),
                rs.getString("location_country"),
                getBigDecimal(rs, "location_latitude"),
                getBigDecimal(rs, "location_longitude"),
                parseStringArray(rs.getArray("capabilities")),
                rs.getObject("capacity", Integer.class),
                rs.getObject("current_occupancy", Integer.class)
        );
    }

    private BigDecimal getBigDecimal(ResultSet rs, String columnName) throws SQLException {
        BigDecimal value = rs.getBigDecimal(columnName);
        return rs.wasNull() ? null : value;
    }

    private List<String> parseStringArray(Array array) throws SQLException {
        if (array == null) {
            return new ArrayList<>();
        }
        String[] values = (String[]) array.getArray();
        return values == null ? new ArrayList<>() : List.of(values);
    }
}
