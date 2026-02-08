package com.berdachuk.medexpertmatch.ingestion.service.impl;

import com.berdachuk.medexpertmatch.core.util.IdGenerator;
import com.berdachuk.medexpertmatch.doctor.domain.Doctor;
import com.berdachuk.medexpertmatch.doctor.repository.DoctorRepository;
import com.berdachuk.medexpertmatch.facility.repository.FacilityRepository;
import com.berdachuk.medexpertmatch.ingestion.service.DoctorGeneratorService;
import com.berdachuk.medexpertmatch.ingestion.service.SyntheticDataGenerationProgress;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service implementation for generating doctors.
 */
@Slf4j
@Service
public class DoctorGeneratorServiceImpl implements DoctorGeneratorService {

    private static final int MAX_DOCTORS_PER_BATCH = 100000;
    private static final int MIN_SPECIALTIES_PER_DOCTOR = 1;
    private static final int MAX_SPECIALTIES_PER_DOCTOR = 3;
    private static final double TELEHEALTH_PROBABILITY = 0.6;
    private static final int MIN_FACILITIES_PER_DOCTOR = 0;
    private static final int MAX_FACILITIES_PER_DOCTOR = 3;

    private final DoctorRepository doctorRepository;
    private final FacilityRepository facilityRepository;
    private final Faker faker = new Faker();
    private final Random random = new Random();
    private final Counter doctorsGeneratedCounter;

    public DoctorGeneratorServiceImpl(
            DoctorRepository doctorRepository,
            FacilityRepository facilityRepository,
            MeterRegistry meterRegistry) {
        this.doctorRepository = doctorRepository;
        this.facilityRepository = facilityRepository;
        this.doctorsGeneratedCounter = Counter.builder("synthetic.data.doctors.generated")
                .description("Total number of doctors generated")
                .register(meterRegistry);
    }

    @Override
    @Transactional
    public void generateDoctors(int count, SyntheticDataGenerationProgress progress,
                                List<String> medicalSpecialties, List<String> availabilityStatuses) {
        if (count < 0) {
            throw new IllegalArgumentException("Count must be non-negative, got: " + count);
        }
        if (count > MAX_DOCTORS_PER_BATCH) {
            throw new IllegalArgumentException(
                    String.format("Count exceeds maximum: %d (max: %d)", count, MAX_DOCTORS_PER_BATCH));
        }

        List<String> loadedMedicalSpecialties = medicalSpecialties != null && !medicalSpecialties.isEmpty()
                ? medicalSpecialties : List.of("General Medicine");
        List<String> loadedAvailabilityStatuses = availabilityStatuses != null && !availabilityStatuses.isEmpty()
                ? availabilityStatuses : List.of("AVAILABLE");

        log.info("Generating {} doctors", count);

        // Load actual facility IDs from database
        List<String> availableFacilityIds = facilityRepository.findAllIds(0);
        if (availableFacilityIds.isEmpty()) {
            log.warn("No facilities found in database. Doctors will be created without facility affiliations.");
        }

        Set<String> generatedEmails = new HashSet<>();
        List<Doctor> doctors = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            if (progress != null && progress.isCancelled()) {
                log.info("Generation cancelled during doctor generation at {}/{}", i, count);
                return;
            }

            if (progress != null && count > 0 && (i + 1) % Math.max(1, count / 10) == 0) {
                int doctorProgress = 20 + ((i + 1) * 15 / count);
                progress.updateProgress(doctorProgress, "Doctors",
                        String.format("Generating doctors: %d/%d", i + 1, count));
            }

            String doctorId = IdGenerator.generateDoctorId();
            String firstName = faker.name().firstName();
            String lastName = faker.name().lastName();
            String name = "Dr. " + firstName + " " + lastName;

            String emailSuffix = doctorId.length() >= 8 ? doctorId.substring(0, 8) : doctorId;
            String email = firstName.toLowerCase() + "." + lastName.toLowerCase() + "." + emailSuffix + "@medical.example.com";

            int emailCounter = 1;
            String originalEmail = email;
            while (generatedEmails.contains(email)) {
                email = originalEmail.replace("@medical.example.com",
                        "." + emailCounter + "@medical.example.com");
                emailCounter++;
            }
            generatedEmails.add(email);

            List<String> specialties = new ArrayList<>();
            int specialtyCount = random.nextInt(MAX_SPECIALTIES_PER_DOCTOR - MIN_SPECIALTIES_PER_DOCTOR + 1) + MIN_SPECIALTIES_PER_DOCTOR;
            Set<String> selectedSpecialties = new HashSet<>();
            while (selectedSpecialties.size() < specialtyCount) {
                selectedSpecialties.add(loadedMedicalSpecialties.get(random.nextInt(loadedMedicalSpecialties.size())));
            }
            specialties.addAll(selectedSpecialties);

            List<String> certifications = new ArrayList<>();
            if (random.nextBoolean()) {
                certifications.add("Board Certified");
            }
            if (random.nextBoolean()) {
                certifications.add("Fellowship Trained");
            }

            Set<String> facilityIdsSet = new LinkedHashSet<>();
            int facilityCount = random.nextInt(MAX_FACILITIES_PER_DOCTOR - MIN_FACILITIES_PER_DOCTOR + 1) + MIN_FACILITIES_PER_DOCTOR;

            // Only assign facilities if they exist in the database
            if (!availableFacilityIds.isEmpty() && facilityCount > 0) {
                // Limit facility count to available facilities
                int maxFacilities = Math.min(facilityCount, availableFacilityIds.size());
                List<String> shuffledFacilities = new ArrayList<>(availableFacilityIds);
                Collections.shuffle(shuffledFacilities, random);

                for (int j = 0; j < maxFacilities; j++) {
                    facilityIdsSet.add(shuffledFacilities.get(j));
                }
            }
            List<String> facilityIds = new ArrayList<>(facilityIdsSet);

            boolean telehealthEnabled = random.nextDouble() < TELEHEALTH_PROBABILITY;
            String availabilityStatus = loadedAvailabilityStatuses.get(random.nextInt(loadedAvailabilityStatuses.size()));

            Doctor doctor = new Doctor(
                    doctorId,
                    name,
                    email,
                    specialties,
                    certifications,
                    facilityIds,
                    telehealthEnabled,
                    availabilityStatus
            );

            doctors.add(doctor);
        }

        batchProcess(
                doctors,
                Doctor::id,
                (ids) -> doctorRepository.findByIds(ids).stream()
                        .collect(Collectors.toMap(Doctor::id, Function.identity())),
                doctorRepository::insertBatch,
                doctorRepository::updateBatch,
                "doctors"
        );

        doctorsGeneratedCounter.increment(count);
        log.info("Generated {} doctors", count);
    }

    private <T> void batchProcess(
            List<T> items,
            Function<T, String> getId,
            Function<List<String>, Map<String, T>> getExistingItems,
            java.util.function.Consumer<List<T>> insertBatch,
            java.util.function.Consumer<List<T>> updateBatch,
            String entityName) {

        if (items.isEmpty()) {
            log.debug("No {} to process", entityName);
            return;
        }

        List<String> ids = items.stream().map(getId).toList();
        Map<String, T> existingItems = getExistingItems.apply(ids);
        Set<String> existingIds = existingItems.keySet();

        List<T> toInsert = new ArrayList<>();
        List<T> toUpdate = new ArrayList<>();

        for (T item : items) {
            String id = getId.apply(item);
            if (existingIds.contains(id)) {
                toUpdate.add(item);
            } else {
                toInsert.add(item);
            }
        }

        if (!toInsert.isEmpty()) {
            insertBatch.accept(toInsert);
            log.debug("Inserted {} new {}", toInsert.size(), entityName);
        }

        if (!toUpdate.isEmpty()) {
            updateBatch.accept(toUpdate);
            log.debug("Updated {} existing {}", toUpdate.size(), entityName);
        }
    }
}
