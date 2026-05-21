package com.berdachuk.medexpertmatch.documents.service.impl;

import com.berdachuk.medexpertmatch.documents.DocumentCatalogApi;
import com.berdachuk.medexpertmatch.documents.domain.SourceDocumentEntity;
import com.berdachuk.medexpertmatch.documents.repository.SourceDocumentRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@ConditionalOnProperty(name = "medexpertmatch.documents.enabled", havingValue = "true")
public class DocumentCatalogServiceImpl implements DocumentCatalogApi {

    private final SourceDocumentRepository repository;

    public DocumentCatalogServiceImpl(SourceDocumentRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SourceDocumentEntity> listDocuments(int limit) {
        return repository.findAll(limit);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SourceDocumentEntity> getDocument(String id) {
        return repository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public long countDocuments() {
        return repository.findAllIds(0).size();
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> listCategories() {
        return repository.findCategories();
    }
}
