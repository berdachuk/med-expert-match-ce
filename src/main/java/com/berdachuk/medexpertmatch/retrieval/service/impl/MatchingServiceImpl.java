package com.berdachuk.medexpertmatch.retrieval.service.impl;

import com.berdachuk.medexpertmatch.core.util.GeoDistance;
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
import com.berdachuk.medexpertmatch.retrieval.service.RerankingService;
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
    private final RerankingService rerankingService;

    public MatchingServiceImpl(
            MedicalCaseRepository medicalCaseRepository,
            DoctorRepository doctorRepository,
            FacilityRepository facilityRepository,
            SemanticGraphRetrievalService semanticGraphRetrievalService,
            ConsultationMatchRepository consultationMatchRepository,
            NamedParameterJdbcTemplate namedJdbcTemplate,
            RerankingService rerankingService) {
        this.medicalCaseRepository = medicalCaseRepository;
        this.doctorRepository = doctorRepository;
        this.facilityRepository = facilityRepository;
        this.semanticGraphRetrievalService = semanticGraphRetrievalService;
        this.consultationMatchRepository = consultationMatchRepository;
        this.namedJdbcTemplate = namedJdbcTemplate;
        this.rerankingService = rerankingService;
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

        // Defensive validation: if the case has no real medical data on its own fields,
        // try to extend the request context from related sources (abstract, similar cases
        // in the DB) before refusing the match. Without this, the graph returns default
        // 0.30 scores for every doctor and the vector search matches against whatever
        // abstract text exists, producing random, low-confidence results.
        if (hasInsufficientMedicalData(medicalCase)) {
            log.warn("Case {} has no real medical data (chiefComplaint/icd10Codes/currentDiagnosis are all blank or invalid); attempting to extend context from related cases",
                    normalizedCaseId);
            String extendedContext = buildExtendedContext(medicalCase);
            if (extendedContext != null) {
                // Return an empty match list with the extended context surfaced in the
                // response metadata. The caller (LLM harness) can either re-run the match
                // with the extended context injected, or present the context to the user
                // so they can supply the missing data.
                log.info("Extended context for case {}: '{}'", normalizedCaseId, extendedContext);
                throw new IllegalStateException(
                        "Insufficient medical data on case " + caseId
                                + " for matching, but related context was found: '" + extendedContext
                                + "'. Please re-run the match with the extended context "
                                + "(e.g. pass the borrowed chief complaint in userFocus or update the case).");
            }
            log.warn("No extended context available for case {}; refusing match", normalizedCaseId);
            throw new IllegalStateException(
                    "Insufficient medical data for case " + caseId
                            + ": chiefComplaint, currentDiagnosis, and icd10Codes are all blank or invalid, "
                            + "and no related cases were found to extend the context. "
                            + "Please provide a real chief complaint, diagnosis, or ICD-10 code to enable matching.");
        }

        // Find candidate doctors based on options
        List<Doctor> candidates = findCandidateDoctors(medicalCase, options);

        // Score each candidate using SemanticGraphRetrievalService
        List<DoctorMatch> unsortedMatches = new ArrayList<>();

        List<String> excludedDoctorIds = options.excludedDoctorIds() != null
                ? options.excludedDoctorIds()
                : List.of();

        for (Doctor doctor : candidates) {
            if (excludedDoctorIds.contains(doctor.id())) {
                continue;
            }
            ScoreResult scoreResult = semanticGraphRetrievalService.score(medicalCase, doctor);

            // Apply minimum score filter if specified
            if (options.minScore() != null && scoreResult.overallScore() < options.minScore()) {
                continue;
            }

            // Create match with temporary rank 0; will assign correct rank after sorting
            DoctorMatch match = new DoctorMatch(
                    doctor,
                    scoreResult.overallScore(),
                    0, // Temporary rank; will be assigned after sorting
                    scoreResult.rationale()
            );

            unsortedMatches.add(match);
        }

        // Sort by score descending, assign correct ranks, and limit results
        List<DoctorMatch> sortedMatches = unsortedMatches.stream()
                .sorted(Comparator.comparing(DoctorMatch::matchScore).reversed())
                .collect(Collectors.toList());

        List<DoctorMatch> reranked = rerankingService.rerank(normalizedCaseId, sortedMatches, options.maxResults());
        List<DoctorMatch> limited = reranked.stream().limit(options.maxResults()).toList();

        boolean appendMatches = !excludedDoctorIds.isEmpty();
        int startRank = appendMatches
                ? consultationMatchRepository.findMaxRankByCaseId(normalizedCaseId)
                : 0;

        List<DoctorMatch> result = new ArrayList<>();
        int rank = startRank + 1;
        for (DoctorMatch m : limited) {
            result.add(new DoctorMatch(
                    m.doctor(),
                    m.matchScore(),
                    rank++,
                    m.rationale()
            ));
        }

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
            if (appendMatches) {
                consultationMatchRepository.insertBatch(toSave);
            } else {
                consultationMatchRepository.deleteByCaseId(normalizedCaseId);
                consultationMatchRepository.insertBatch(toSave);
            }
        } else if (!appendMatches) {
            consultationMatchRepository.deleteByCaseId(normalizedCaseId);
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
        List<FacilityMatch> unsortedMatches = new ArrayList<>();

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

            // Create match with temporary rank 0; will assign correct rank after sorting
            FacilityMatch match = new FacilityMatch(
                    facility,
                    routeResult.overallScore(),
                    0, // Temporary rank; will be assigned after sorting
                    routeResult.rationale()
            );

            unsortedMatches.add(match);
        }

        // Sort by route score descending, assign correct ranks, and limit results
        List<FacilityMatch> sortedMatches = unsortedMatches.stream()
                .sorted(Comparator.comparing(FacilityMatch::routeScore).reversed())
                .limit(options.maxResults())
                .collect(Collectors.toList());

        // Assign correct ranks after sorting (1 = best match)
        List<FacilityMatch> result = new ArrayList<>();
        int rank = 1;
        for (FacilityMatch m : sortedMatches) {
            result.add(new FacilityMatch(
                    m.facility(),
                    m.routeScore(),
                    rank++,
                    m.rationale()
            ));
        }

        return result;
    }

    /**
     * Finds candidate doctors based on medical case and match options.
     */
    private List<Doctor> findCandidateDoctors(MedicalCase medicalCase, MatchOptions options) {
        List<Doctor> candidates = new ArrayList<>();
        List<String> excludedDoctorIds = options.excludedDoctorIds() != null
                ? options.excludedDoctorIds()
                : List.of();
        boolean broadenSearch = Boolean.TRUE.equals(options.broadenCandidatePool())
                || !excludedDoctorIds.isEmpty();

        if (broadenSearch) {
            int poolSize = Math.max(options.maxResults() * 10, 50);
            List<String> doctorIds = doctorRepository.findAllIds(poolSize);
            candidates.addAll(doctorRepository.findByIds(doctorIds));
        } else if (options.preferredSpecialties() != null && !options.preferredSpecialties().isEmpty()) {
            for (String specialty : options.preferredSpecialties()) {
                List<Doctor> doctors = doctorRepository.findBySpecialty(specialty, options.maxResults() * 2);
                candidates.addAll(doctors);
            }
        } else if (medicalCase.requiredSpecialty() != null) {
            List<Doctor> doctors = doctorRepository.findBySpecialty(
                    medicalCase.requiredSpecialty(),
                    options.maxResults() * 2
            );
            candidates.addAll(doctors);
            if (candidates.isEmpty()) {
                log.warn("No doctors for required specialty '{}'; falling back to full pool for case {}",
                        medicalCase.requiredSpecialty(), medicalCase.id());
                int poolSize = Math.max(options.maxResults() * 10, 50);
                candidates.addAll(doctorRepository.findByIds(doctorRepository.findAllIds(poolSize)));
            }
        } else {
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
     */
    private List<Facility> findCandidateFacilities(MedicalCase medicalCase, RoutingOptions options) {
        validateGeographicFilteringSupport(medicalCase, options);

        List<Facility> all = facilityRepository.findAll();
        int limit = Math.max(options.maxResults() * 2, 10);
        return all.stream()
                .filter(f -> options.preferredFacilityTypes() == null || options.preferredFacilityTypes().isEmpty()
                        || (f.facilityType() != null && options.preferredFacilityTypes().stream()
                        .anyMatch(type -> type.equalsIgnoreCase(f.facilityType()))))
                .filter(f -> options.requiredCapabilities() == null || options.requiredCapabilities().isEmpty()
                        || (f.capabilities() != null && f.capabilities().containsAll(options.requiredCapabilities())))
                .filter(f -> options.maxDistanceKm() == null
                        || isWithinMaxDistance(medicalCase, f, options.maxDistanceKm()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    private void validateGeographicFilteringSupport(MedicalCase medicalCase, RoutingOptions options) {
        if (options.maxDistanceKm() == null) {
            return;
        }

        if (medicalCase.locationLatitude() == null || medicalCase.locationLongitude() == null) {
            throw new IllegalArgumentException("maxDistanceKm requires medical case coordinates for case: " + medicalCase.id());
        }
    }

    private boolean isWithinMaxDistance(MedicalCase medicalCase, Facility facility, double maxDistanceKm) {
        Double distanceKm = GeoDistance.calculateDistanceKm(
                medicalCase.locationLatitude(),
                medicalCase.locationLongitude(),
                facility.locationLatitude(),
                facility.locationLongitude()
        );
        return distanceKm != null && distanceKm <= maxDistanceKm;
    }

    /**
     * Returns true when a case has no real medical data to match against.
     * A case is considered insufficient when ALL of the following are blank or
     * non-medical (e.g. a MongoDB ObjectId stored by mistake):
     * <ul>
     *   <li>chiefComplaint is null/blank or matches a 24-char hex ObjectId</li>
     *   <li>currentDiagnosis is null/blank or matches a 24-char hex ObjectId</li>
     *   <li>icd10Codes is null/empty or every code is blank/looks like an ObjectId</li>
     * </ul>
     * In that situation the graph returns default 0.30 scores for every doctor and
     * the vector search matches against whatever abstract text exists, producing
     * random, low-confidence results.
     */
    private boolean hasInsufficientMedicalData(MedicalCase medicalCase) {
        boolean chiefOk = isRealMedicalTerm(medicalCase.chiefComplaint());
        boolean diagnosisOk = isRealMedicalTerm(medicalCase.currentDiagnosis());
        boolean icdOk = medicalCase.icd10Codes() != null
                && !medicalCase.icd10Codes().isEmpty()
                && medicalCase.icd10Codes().stream().anyMatch(this::isRealMedicalTerm);
        return !chiefOk && !diagnosisOk && !icdOk;
    }

    private boolean isRealMedicalTerm(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String trimmed = value.trim();
        // Reject 24-char hex strings (MongoDB ObjectId shape) stored as text.
        if (trimmed.length() == 24 && trimmed.matches("[0-9a-fA-F]{24}")) {
            return false;
        }
        // Reject anything that contains no letters (a medical term must have letters).
        return trimmed.chars().anyMatch(Character::isLetter);
    }

    /**
     * Tries to build an extended medical context for a case whose own fields are blank.
     * Strategy:
     *  1. Mine the case's {@code symptoms}, {@code additionalNotes}, and {@code abstractText}
     *     for any real medical term. Free text can still carry useful signal even when
     *     structured fields are blank — but we reject obvious LLM "thought" blocks and
     *     metadata echoes that just restate that the data is missing.
     *  2. If that fails, look at other cases with the same {@code caseType} and borrow
     *     the chief complaint of the first one that has real medical data.
     *  3. Returns {@code null} if no usable context can be found.
     */
    String buildExtendedContext(MedicalCase medicalCase) {
        String fromCaseText = firstMedicalTerm(medicalCase.symptoms(), medicalCase.additionalNotes(),
                medicalCase.abstractText());
        if (fromCaseText != null) {
            return fromCaseText;
        }
        if (medicalCase.caseType() == null) {
            return null;
        }
        try {
            List<MedicalCase> sameType = medicalCaseRepository.findByCaseType(medicalCase.caseType().name(), 5);
            for (MedicalCase other : sameType) {
                if (other.id().equals(medicalCase.id())) {
                    continue;
                }
                String otherChief = isRealMedicalTerm(other.chiefComplaint()) ? other.chiefComplaint() : null;
                String otherDiag = isRealMedicalTerm(other.currentDiagnosis()) ? other.currentDiagnosis() : null;
                String otherIcd = (other.icd10Codes() != null)
                        ? other.icd10Codes().stream().filter(this::isRealMedicalTerm).findFirst().orElse(null)
                        : null;
                String borrowed = firstNonBlank(otherChief, otherDiag, otherIcd);
                if (borrowed != null) {
                    log.info("Borrowed medical context from sibling case {} ('{}') for case {}",
                            other.id(), borrowed, medicalCase.id());
                    return borrowed;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to query sibling cases for context extension on case {}: {}",
                    medicalCase.id(), e.getMessage());
        }
        return null;
    }

    /**
     * Returns the first non-blank / non-ObjectId / non-template-echo term found across
     * the given free-text fields. Returns {@code null} if no real medical term is found.
     */
    private String firstMedicalTerm(String... fields) {
        for (String f : fields) {
            String cleaned = stripTemplateEcho(f);
            if (isRealMedicalTerm(cleaned)) {
                return cleaned.trim();
            }
        }
        return null;
    }

    /**
     * Strips common LLM "thought" / metadata-echo boilerplate that some legacy
     * abstract fields contain (e.g. "&lt;unused94&gt;thought\nThe user wants a clinical
     * case summary..."). Returns the original string when no boilerplate is detected.
     */
    private static String stripTemplateEcho(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.startsWith("<unused") || trimmed.toLowerCase().startsWith("thought")
                || trimmed.toLowerCase().startsWith("the user wants")) {
            return null;
        }
        return trimmed;
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }
}
