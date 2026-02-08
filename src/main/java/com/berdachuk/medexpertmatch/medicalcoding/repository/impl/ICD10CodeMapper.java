package com.berdachuk.medexpertmatch.medicalcoding.repository.impl;

import com.berdachuk.medexpertmatch.medicalcoding.domain.ICD10Code;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Row mapper for ICD10Code entity.
 * Maps ResultSet rows to ICD10Code records.
 */
@Component
public class ICD10CodeMapper implements RowMapper<ICD10Code> {

    @Override
    public ICD10Code mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new ICD10Code(
                rs.getString("id"),
                rs.getString("code"),
                rs.getString("description"),
                rs.getString("category"),
                rs.getString("parent_code"),
                parseStringArray(rs.getArray("related_codes"))
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
