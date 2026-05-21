package com.berdachuk.medexpertmatch.chunking.repository.impl;

import com.berdachuk.medexpertmatch.chunking.domain.DocumentChunk;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class ChunkMapper implements RowMapper<DocumentChunk> {

    @Override
    public DocumentChunk mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new DocumentChunk(
                rs.getString("id"),
                rs.getString("document_id"),
                rs.getInt("chunk_index"),
                rs.getString("chunk_text"),
                null
        );
    }
}
