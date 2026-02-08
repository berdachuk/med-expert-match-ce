package com.berdachuk.medexpertmatch.retrieval.service.impl;

import com.berdachuk.medexpertmatch.core.util.IdGenerator;
import com.berdachuk.medexpertmatch.doctor.domain.Doctor;
import com.berdachuk.medexpertmatch.doctor.repository.DoctorRepository;
import com.berdachuk.medexpertmatch.facility.domain.Facility;
import com.berdachuk.medexpertmatch.facility.repository.FacilityRepository;
import com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase;
import com.berdachuk.medexpertmatch.medicalcase.repository.MedicalCaseRepository;
import com.berdachuk.medexpertmatch.retrieval.domain.*;
import com.berdachuk.medexpertmatch.retrieval.repository.ConsultationMatchRepository;
import com.berdachuk.medexpertmatch.retrieval.service.MatchingService;
import com.berdachuk.medexpertmatch.retrieval.service.SemanticGraphRetrievalService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Matching service implementation.
 * Orchestrates matching logic across multiple services.
 */
@Slf4j
@Service
public class MatchingServiceImpl implements MatchingService {

    private final MedicalCaseRepository medicalCaseRepository;
    private final DoctorRepository doctorRepository;
    private final FacilityRepository facilityRepository;
    private final SemanticGraphRetrievalService semanticGraphRetrievalService;
    private final ConsultationMatchRepository consultationMatchRepository;
    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    public MatchingServiceImpl(
            MedicalCaseRepository medicalCaseRepository,
            DoctorRepository doctorRepository,
            FacilityRepository facilityRepository,
            SemanticGraphRetrievalService semanticGraphRetrievalService,
            ConsultationMatchRepository consultationMatchRepository,
            NamedParameterJdbcTemplate namedJdbcTemplate) {
        this.medicalCaseRepository = medicalCaseRepository;
        this.doctorRepository = doctorRepository;
        this.facilityRepository = facilityRepository;
        this.semanticGraphRetrievalService = semanticGraphRetrievalService;
        this.consultationMatchRepository = consultationMatchRepository;
        this.namedJdbcTemplate = namedJdbcTemplate;
    }

    @Override
    @Transactional
    public List<DoctorMatch> matchDoctorsToCase(String caseId, MatchOptions options) {
        // Validate and normalize case ID
        if (caseId == null || caseId.isBlank()) {
            throw new IllegalArgumentException("Case ID cannot be null or empty");
        }

        // Normalize case ID to lowercase for case-insensitive lookup
        String normalizedCaseId = caseId.trim().toLowerCase();

        // Load the medical case
        MedicalCase medicalCase = medicalCaseRepository.findById(normalizedCaseId)
                .orElseThrow(() -> new IllegalArgumentException("Medical case not found: " + caseId + " (normalized: " + normalizedCaseId + ")"));

        // Find candidate doctors based on options
        List<Doctor> candidates = findCandidateDoctors(medicalCase, options);

        // Score each candidate using SemanticGraphRetrievalService
        List<DoctorMatch> matches = new ArrayList<>();
        int rank = 1;

        for (Doctor doctor : candidates) {
            ScoreResult scoreResult = semanticGraphRetrievalService.score(medicalCase, doctor);

            // Apply minimum score filter if specified
            if (options.minScore() != null && scoreResult.overallScore() < options.minScore()) {
                continue;
            }

            DoctorMatch match = new DoctorMatch(
                    doctor,
                    scoreResult.overallScore(),
                    rank++,
                    scoreResult.rationale()
            );

            matches.add(match);
        }

        // Sort by score descending and limit results
        List<DoctorMatch> result = matches.stream()
                .sorted(Comparator.comparing(DoctorMatch::matchScore).reversed())
                .limit(options.maxResults())
                .collect(Collectors.toList());

        // Persist consultation matches for dashboard and history
        consultationMatchRepository.deleteByCaseId(normalizedCaseId);
        if (!result.isEmpty()) {
            List<ConsultationMatch> toSave = result.stream()
                    .map(m -> new ConsultationMatch(
                            IdGenerator.generateId(),
                            normalizedCaseId,
                            m.doctor().id(),
                            m.matchScore(),
                            m.rationale(),
                            m.rank(),
                            "PENDING"))
                    .toList();
            consultationMatchRepository.insertBatch(toSave);
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FacilityMatch> matchFacilitiesForCase(String caseId, RoutingOptions options) {
        // Validate and normalize case ID
        if (caseId == null || caseId.isBlank()) {
            throw new IllegalArgumentException("Case ID cannot be null or empty");
        }

        // Normalize case ID to lowercase for case-insensitive lookup
        // Case IDs are 24-character hex strings (CHAR(24)) and should be case-insensitive
        String normalizedCaseId = caseId.trim().toLowerCase();

        // Load the medical case
        MedicalCase medicalCase = medicalCaseRepository.findById(normalizedCaseId)
                .orElseThrow(() -> new IllegalArgumentException("Medical case not found: " + caseId + " (normalized: " + normalizedCaseId + ")"));

        // Find candidate facilities based on options
        List<Facility> candidates = findCandidateFacilities(medicalCase, options);

        // Score each candidate using SemanticGraphRetrievalService
        List<FacilityMatch> matches = new ArrayList<>();
        int rank = 1;

        for (Facility facility : candidates) {
            RouteScoreResult routeResult = semanticGraphRetrievalService.semanticGraphRetrievalRouteScore(medicalCase, facility);

            // Apply minimum score filter if specified
            if (options.minScore() != null && routeResult.overallScore() < options.minScore()) {
                continue;
            }

            // Apply facility type filter if specified
            if (options.preferredFacilityTypes() != null && !options.preferredFacilityTypes().isEmpty()) {
                boolean matchesType = options.preferredFacilityTypes().stream()
                        .anyMatch(type -> type.equalsIgnoreCase(facility.facilityType()));
                if (!matchesType) {
                    continue;
                }
            }

            // Apply required capabilities filter if specified
            if (options.requiredCapabilities() != null && !options.requiredCapabilities().isEmpty()) {
                boolean hasAllCapabilities = facility.capabilities() != null &&
                        facility.capabilities().containsAll(options.requiredCapabilities());
                if (!hasAllCapabilities) {
                    continue;
                }
            }

            FacilityMatch match = new FacilityMatch(
                    facility,
                    routeResult.overallScore(),
                    rank++,
                    routeResult.rationale()
            );

            matches.add(match);
        }

        // Sort by route score descending and limit results
        return matches.stream()
                .sorted(Comparator.comparing(FacilityMatch::routeScore).reversed())
                .limit(options.maxResults())
                .collect(Collectors.toList());
    }

    /**
     * Finds candidate doctors based on medical case and match options.
     */
    private List<Doctor> findCandidateDoctors(MedicalCase medicalCase, MatchOptions options) {
        List<Doctor> candidates = new ArrayList<>();

        // If preferred specialties are specified, use them
        if (options.preferredSpecialties() != null && !options.preferredSpecialties().isEmpty()) {
            for (String specialty : options.preferredSpecialties()) {
                List<Doctor> doctors = doctorRepository.findBySpecialty(specialty, options.maxResults() * 2);
                candidates.addAll(doctors);
            }
        } else if (medicalCase.requiredSpecialty() != null) {
            // Use case's required specialty
            List<Doctor> doctors = doctorRepository.findBySpecialty(
                    medicalCase.requiredSpecialty(),
                    options.maxResults() * 2
            );
            candidates.addAll(doctors);
        } else {
            // Fallback: get all doctors (limited)
            List<String> doctorIds = doctorRepository.findAllIds(options.maxResults() * 2);
            candidates.addAll(doctorRepository.findByIds(doctorIds));
        }

        // Apply all filters in a single pass to improve performance
        return candidates.stream()
                .distinct()
                .filter(doctor -> !Boolean.TRUE.equals(options.requireTelehealth()) || doctor.telehealthEnabled())
                .filter(doctor -> options.preferredFacilityIds() == null || options.preferredFacilityIds().isEmpty() ||
                        doctor.facilityIds().stream().anyMatch(facId -> options.preferredFacilityIds().contains(facId)))
                .filter(doctor -> options.preferredSpecialties() == null || options.preferredSpecialties().isEmpty() ||
                        doctor.specialties().stream().anyMatch(spec -> options.preferredSpecialties().stream()
                                .anyMatch(pref -> spec.equalsIgnoreCase(pref))))
                .collect(Collectors.toList());
    }

    /**
     * Finds candidate facilities based on medical case and routing options.
     * Queries FacilityRepository and filters by preferred types and required capabilities.
     * Geographic filter (maxDistanceKm) is not applied; case/facility coordinates would be required.
     */
    private List<Facility> findCandidateFacilities(MedicalCase medicalCase, RoutingOptions options) {
        List<Facility> all = facilityRepository.findAll();
        int limit = Math.max(options.maxResults() * 2, 10);
        return all.stream()
                .filter(f -> options.preferredFacilityTypes() == null || options.preferredFacilityTypes().isEmpty()
                        || (f.facilityType() != null && options.preferredFacilityTypes().stream()
                        .anyMatch(type -> type.equalsIgnoreCase(f.facilityType()))))
                .filter(f -> options.requiredCapabilities() == null || options.requiredCapabilities().isEmpty()
                        || (f.capabilities() != null && f.capabilities().containsAll(options.requiredCapabilities())))
                .limit(limit)
                .collect(Collectors.toList());
    }
}
