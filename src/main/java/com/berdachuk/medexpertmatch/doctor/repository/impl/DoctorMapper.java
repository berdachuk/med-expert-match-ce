package com.berdachuk.medexpertmatch.doctor.repository.impl;

import com.berdachuk.medexpertmatch.doctor.domain.Doctor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Row mapper for Doctor entity.
 * Maps ResultSet rows to Doctor records.
 * <p>
 * This mapper is reusable across all DoctorRepository methods,
 * providing consistent mapping logic and easier maintenance.
 */
@Component
public class DoctorMapper implements RowMapper<Doctor> {

    @Override
    public Doctor mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Doctor(
                rs.getString("id"),
                rs.getString("name"),
                rs.getString("email"),
                parseStringArray(rs.getArray("specialties")),
                parseStringArray(rs.getArray("certifications")),
                parseStringArray(rs.getArray("facility_ids")),
                rs.getBoolean("telehealth_enabled"),
                rs.getString("availability_status")
        );
    }

    private List<String> parseStringArray(Array array) throws SQLException {
        if (array == null) {
            return new ArrayList<>();
        }
        String[] values = (String[]) array.getArray();
        return values == null ? new ArrayList<>() : List.of(values);
    }
}
