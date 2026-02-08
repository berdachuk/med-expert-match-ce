package com.berdachuk.medexpertmatch.ingestion.service.impl;

import com.berdachuk.medexpertmatch.core.util.IdGenerator;
import com.berdachuk.medexpertmatch.facility.domain.Facility;
import com.berdachuk.medexpertmatch.facility.repository.FacilityRepository;
import com.berdachuk.medexpertmatch.ingestion.service.FacilityGeneratorService;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Service implementation for generating facilities.
 */
@Slf4j
@Service
public class FacilityGeneratorServiceImpl implements FacilityGeneratorService {

    private static final int MAX_FACILITIES_PER_BATCH = 10000;

    private final FacilityRepository facilityRepository;
    private final Faker faker = new Faker();
    private final Random random = new Random();

    public FacilityGeneratorServiceImpl(FacilityRepository facilityRepository) {
        this.facilityRepository = facilityRepository;
    }

    @Override
    @Transactional
    public void generateFacilities(int count, List<String> facilityTypes, List<String> facilityCapabilities) {
        List<String> loadedFacilityTypes = facilityTypes != null ? facilityTypes : List.of();
        List<String> loadedCapabilities = facilityCapabilities != null ? facilityCapabilities : List.of();
        if (count < 0) {
            throw new IllegalArgumentException("Count must be non-negative, got: " + count);
        }
        if (count > MAX_FACILITIES_PER_BATCH) {
            throw new IllegalArgumentException(
                    String.format("Count exceeds maximum: %d (max: %d)", count, MAX_FACILITIES_PER_BATCH));
        }

        log.info("Generating {} facilities", count);

        for (int i = 0; i < count; i++) {
            String facilityId = IdGenerator.generateFacilityId();
            String name = faker.company().name() + " Medical Center";
            String facilityType = loadedFacilityTypes.isEmpty() ? "HOSPITAL" : loadedFacilityTypes.get(random.nextInt(loadedFacilityTypes.size()));
            String locationCity = faker.address().city();
            String locationState = faker.address().state();
            String locationCountry = "USA";

            java.math.BigDecimal locationLatitude = java.math.BigDecimal.valueOf(
                    24.0 + random.nextDouble() * 25.0
            ).setScale(8, java.math.RoundingMode.HALF_UP);

            java.math.BigDecimal locationLongitude = java.math.BigDecimal.valueOf(
                    -125.0 + random.nextDouble() * 59.0
            ).setScale(8, java.math.RoundingMode.HALF_UP);

            List<String> facilityCapabilitiesList = new ArrayList<>();
            if (!loadedCapabilities.isEmpty()) {
                int capabilityCount = Math.min(random.nextInt(4) + 2, loadedCapabilities.size());
                Set<String> selectedCapabilities = new HashSet<>();
                while (selectedCapabilities.size() < capabilityCount && selectedCapabilities.size() < loadedCapabilities.size()) {
                    selectedCapabilities.add(loadedCapabilities.get(random.nextInt(loadedCapabilities.size())));
                }
                facilityCapabilitiesList.addAll(selectedCapabilities);
            }

            int capacity = random.nextInt(451) + 50;
            int currentOccupancy = random.nextInt(capacity + 1);

            Facility facility = new Facility(
                    facilityId,
                    name,
                    facilityType,
                    locationCity,
                    locationState,
                    locationCountry,
                    locationLatitude,
                    locationLongitude,
                    facilityCapabilitiesList,
                    capacity,
                    currentOccupancy
            );

            if (facilityRepository.findById(facilityId).isPresent()) {
                facilityRepository.update(facility);
            } else {
                facilityRepository.insert(facility);
            }
        }

        log.info("Generated {} facilities", count);
    }
}
