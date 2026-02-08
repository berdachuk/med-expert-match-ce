package com.berdachuk.medexpertmatch.medicalcase.repository.impl;

import com.berdachuk.medexpertmatch.medicalcase.domain.CaseType;
import com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase;
import com.berdachuk.medexpertmatch.medicalcase.domain.UrgencyLevel;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Row mapper for MedicalCase entity.
 * Maps ResultSet rows to MedicalCase records.
 */
@Component
public class MedicalCaseMapper implements RowMapper<MedicalCase> {

    @Override
    public MedicalCase mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new MedicalCase(
                rs.getString("id"),
                rs.getObject("patient_age", Integer.class),
                rs.getString("chief_complaint"),
                rs.getString("symptoms"),
                rs.getString("current_diagnosis"),
                parseStringArray(rs.getArray("icd10_codes")),
                parseStringArray(rs.getArray("snomed_codes")),
                UrgencyLevel.valueOf(rs.getString("urgency_level")),
                rs.getString("required_specialty"),
                CaseType.valueOf(rs.getString("case_type")),
                rs.getString("additional_notes"),
                rs.getString("abstract")  // Maps to abstractText field
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
