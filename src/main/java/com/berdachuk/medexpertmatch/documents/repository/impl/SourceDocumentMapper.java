package com.berdachuk.medexpertmatch.documents.repository.impl;

import com.berdachuk.medexpertmatch.documents.domain.SourceDocumentEntity;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class SourceDocumentMapper implements RowMapper<SourceDocumentEntity> {

    @Override
    public SourceDocumentEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new SourceDocumentEntity(
                rs.getString("id"),
                rs.getString("external_id"),
                rs.getString("title"),
                rs.getString("category"),
                rs.getString("source_name"),
                rs.getString("source_url"),
                rs.getString("content"),
                rs.getString("content_hash"),
                rs.getString("source_format")
        );
    }
}
