package com.berdachuk.medexpertmatch.doctor.repository.impl;

import com.berdachuk.medexpertmatch.doctor.domain.MedicalSpecialty;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Row mapper for MedicalSpecialty entity.
 * Maps ResultSet rows to MedicalSpecialty records.
 */
@Component
public class MedicalSpecialtyMapper implements RowMapper<MedicalSpecialty> {

    @Override
    public MedicalSpecialty mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new MedicalSpecialty(
                rs.getString("id"),
                rs.getString("name"),
                rs.getString("normalized_name"),
                rs.getString("description"),
                parseStringArray(rs.getArray("icd10_code_ranges")),
                parseStringArray(rs.getArray("related_specialties"))
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
