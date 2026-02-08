package com.berdachuk.medexpertmatch.doctor.rest;

import com.berdachuk.medexpertmatch.doctor.domain.Doctor;
import com.berdachuk.medexpertmatch.doctor.repository.DoctorRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * REST controller for doctor API endpoints.
 * Provides query functionality for doctors.
 */
@Slf4j
@RestController
@RequestMapping("/api/doctors")
public class DoctorRestController {

    private final DoctorRepository doctorRepository;

    public DoctorRestController(DoctorRepository doctorRepository) {
        this.doctorRepository = doctorRepository;
    }

    /**
     * Gets a doctor by their ID.
     *
     * @param doctorId The doctor ID
     * @return Doctor as JSON
     */
    @GetMapping("/{doctorId}")
    public ResponseEntity<Doctor> getDoctorById(@PathVariable String doctorId) {
        log.info("GET /api/doctors/{}", doctorId);

        return doctorRepository.findById(doctorId)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor not found: " + doctorId));
    }
}
