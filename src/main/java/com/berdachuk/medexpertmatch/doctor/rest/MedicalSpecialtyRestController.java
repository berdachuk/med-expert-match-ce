package com.berdachuk.medexpertmatch.doctor.rest;

import com.berdachuk.medexpertmatch.doctor.domain.MedicalSpecialty;
import com.berdachuk.medexpertmatch.doctor.repository.MedicalSpecialtyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * REST controller for medical specialty API endpoints.
 * Provides query functionality for medical specialties.
 */
@Slf4j
@RestController
@RequestMapping("/api/specialties")
public class MedicalSpecialtyRestController {

    private final MedicalSpecialtyRepository medicalSpecialtyRepository;

    public MedicalSpecialtyRestController(MedicalSpecialtyRepository medicalSpecialtyRepository) {
        this.medicalSpecialtyRepository = medicalSpecialtyRepository;
    }

    /**
     * Gets a medical specialty by its name.
     *
     * @param name The medical specialty name
     * @return Medical specialty as JSON
     */
    @GetMapping("/name/{name}")
    public ResponseEntity<MedicalSpecialty> getByName(@PathVariable String name) {
        log.info("GET /api/specialties/name/{}", name);

        return medicalSpecialtyRepository.findByName(name)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Medical specialty not found: " + name));
    }

    /**
     * Gets a medical specialty by its ID.
     *
     * @param id The medical specialty ID
     * @return Medical specialty as JSON
     */
    @GetMapping("/{id}")
    public ResponseEntity<MedicalSpecialty> getById(@PathVariable String id) {
        log.info("GET /api/specialties/{}", id);

        return medicalSpecialtyRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Medical specialty not found: " + id));
    }
}
