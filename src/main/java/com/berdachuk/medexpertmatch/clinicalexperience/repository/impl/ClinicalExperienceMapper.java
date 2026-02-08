package com.berdachuk.medexpertmatch.clinicalexperience.repository.impl;

import com.berdachuk.medexpertmatch.clinicalexperience.domain.ClinicalExperience;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Row mapper for ClinicalExperience entity.
 * Maps ResultSet rows to ClinicalExperience records.
 */
@Component
public class ClinicalExperienceMapper implements RowMapper<ClinicalExperience> {

    @Override
    public ClinicalExperience mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new ClinicalExperience(
                rs.getString("id"),
                rs.getString("doctor_id"),
                rs.getString("case_id"),
                parseStringArray(rs.getArray("procedures_performed")),
                rs.getString("complexity_level"),
                rs.getString("outcome"),
                parseStringArray(rs.getArray("complications")),
                rs.getObject("time_to_resolution", Integer.class),
                rs.getObject("rating", Integer.class)
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
