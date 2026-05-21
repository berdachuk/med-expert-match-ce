package com.berdachuk.medexpertmatch.documents;

import com.berdachuk.medexpertmatch.documents.domain.SourceDocumentEntity;

import java.util.List;
import java.util.Optional;

public interface DocumentCatalogApi {

    List<SourceDocumentEntity> listDocuments(int limit);

    Optional<SourceDocumentEntity> getDocument(String id);

    long countDocuments();

    List<String> listCategories();
}
