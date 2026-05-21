package com.berdachuk.medexpertmatch.documents;

import com.berdachuk.medexpertmatch.documents.domain.DocumentSearchResult;

import java.util.List;

public interface DocumentSearchApi {

    List<DocumentSearchResult> searchChunks(String query, int topK);
}
