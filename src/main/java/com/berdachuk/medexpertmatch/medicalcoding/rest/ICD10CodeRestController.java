package com.berdachuk.medexpertmatch.medicalcoding.rest;

import com.berdachuk.medexpertmatch.medicalcoding.domain.ICD10Code;
import com.berdachuk.medexpertmatch.medicalcoding.repository.ICD10CodeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * REST controller for ICD-10 code API endpoints.
 * Provides query functionality for ICD-10 codes.
 */
@Slf4j
@RestController
@RequestMapping("/api/icd10")
public class ICD10CodeRestController {

    private final ICD10CodeRepository icd10CodeRepository;

    public ICD10CodeRestController(ICD10CodeRepository icd10CodeRepository) {
        this.icd10CodeRepository = icd10CodeRepository;
    }

    /**
     * Gets an ICD-10 code by its code string (e.g., "I21.9").
     *
     * @param code The ICD-10 code string
     * @return ICD-10 code as JSON
     */
    @GetMapping("/code/{code}")
    public ResponseEntity<ICD10Code> getByCode(@PathVariable String code) {
        log.info("GET /api/icd10/code/{}", code);

        return icd10CodeRepository.findByCode(code)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ICD-10 code not found: " + code));
    }

    /**
     * Gets an ICD-10 code by its ID.
     *
     * @param id The ICD-10 code ID
     * @return ICD-10 code as JSON
     */
    @GetMapping("/{id}")
    public ResponseEntity<ICD10Code> getById(@PathVariable String id) {
        log.info("GET /api/icd10/{}", id);

        return icd10CodeRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ICD-10 code not found: " + id));
    }
}
