package com.berdachuk.medexpertmatch.medicalcoding.repository.impl;

import com.berdachuk.medexpertmatch.medicalcoding.domain.Procedure;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Row mapper for Procedure entity.
 * Maps ResultSet rows to Procedure records.
 */
@Component
public class ProcedureMapper implements RowMapper<Procedure> {

    @Override
    public Procedure mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Procedure(
                rs.getString("id"),
                rs.getString("name"),
                rs.getString("normalized_name"),
                rs.getString("description"),
                rs.getString("category")
        );
    }
}
