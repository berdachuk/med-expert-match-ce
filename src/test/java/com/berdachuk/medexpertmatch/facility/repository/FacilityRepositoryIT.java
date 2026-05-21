package com.berdachuk.medexpertmatch.facility.repository;

import com.berdachuk.medexpertmatch.core.util.IdGenerator;
import com.berdachuk.medexpertmatch.facility.domain.Facility;
import com.berdachuk.medexpertmatch.integration.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FacilityRepositoryIT extends BaseIntegrationTest {

    @Autowired
    private FacilityRepository facilityRepository;

    @Autowired
    private NamedParameterJdbcTemplate namedJdbcTemplate;

    @BeforeEach
    void setUp() {
        namedJdbcTemplate.getJdbcTemplate().execute("DELETE FROM medexpertmatch.clinical_experiences");
        namedJdbcTemplate.getJdbcTemplate().execute("DELETE FROM medexpertmatch.consultation_matches");
        namedJdbcTemplate.getJdbcTemplate().execute("DELETE FROM medexpertmatch.facilities");
    }

    @Test
    void shouldInsertAndFindById() {
        String id = IdGenerator.generateFacilityId();
        Facility facility = createFacility(id, "General Hospital", "ACADEMIC");
        facilityRepository.insert(facility);

        Facility found = facilityRepository.findById(id).orElseThrow();
        assertEquals("General Hospital", found.name());
        assertEquals("ACADEMIC", found.facilityType());
    }

    @Test
    void shouldReturnEmptyForNonExistentId() {
        assertTrue(facilityRepository.findById("nonexistent-id").isEmpty());
    }

    @Test
    void shouldFindAll() {
        facilityRepository.insert(createFacility("Hosp A"));
        facilityRepository.insert(createFacility("Hosp B"));

        List<Facility> all = facilityRepository.findAll();
        assertTrue(all.size() >= 2);
    }

    @Test
    void shouldFindAllIds() {
        facilityRepository.insert(createFacility("Hosp X"));
        facilityRepository.insert(createFacility("Hosp Y"));

        List<String> ids = facilityRepository.findAllIds(10);
        assertEquals(2, ids.size());
    }

    @Test
    void shouldUpdateFacility() {
        String id = IdGenerator.generateFacilityId();
        Facility facility = createFacility(id, "Old Name", "COMMUNITY");
        facilityRepository.insert(facility);

        Facility updated = new Facility(id, "New Name", "SPECIALTY_CENTER",
                "New City", "NY", "US",
                new BigDecimal("41.0"), new BigDecimal("-74.0"),
                List.of("PCI"), 200, 50);
        String returnedId = facilityRepository.update(updated);
        assertEquals(id, returnedId);

        Facility found = facilityRepository.findById(id).orElseThrow();
        assertEquals("New Name", found.name());
        assertEquals("SPECIALTY_CENTER", found.facilityType());
        assertEquals(200, found.capacity());
    }

    @Test
    void shouldUpdateNonExistentThrowsException() {
        Facility updated = createFacility("nonexistent-id", "Name");
        assertThrows(Exception.class, () -> facilityRepository.update(updated));
    }

    @Test
    void shouldInsertDuplicateIdThrowsException() {
        String id = IdGenerator.generateFacilityId();
        facilityRepository.insert(createFacility(id, "First"));
        assertThrows(Exception.class, () -> facilityRepository.insert(createFacility(id, "Second")));
    }

    @Test
    void shouldDeleteAll() {
        facilityRepository.insert(createFacility("Hosp 1"));
        facilityRepository.insert(createFacility("Hosp 2"));
        int deleted = facilityRepository.deleteAll();
        assertTrue(deleted >= 2);
        assertTrue(facilityRepository.findAllIds(10).isEmpty());
    }

    @Test
    void shouldHandleNullCapabilities() {
        String id = IdGenerator.generateFacilityId();
        Facility facility = new Facility(id, "No Capabilities", "clinic",
                "City", "State", "Country",
                null, null, null, null, null);
        facilityRepository.insert(facility);

        Facility found = facilityRepository.findById(id).orElseThrow();
        assertNotNull(found);
    }

    @Test
    void shouldFindAllIdsWithMaxResults() {
        for (int i = 0; i < 5; i++) {
            facilityRepository.insert(createFacility("Hosp " + i));
        }

        List<String> ids = facilityRepository.findAllIds(2);
        assertEquals(2, ids.size());
    }

    @Test
    void shouldInsertWithFullFields() {
        String id = IdGenerator.generateFacilityId();
        Facility facility = new Facility(id, "Complete Hospital", "ACADEMIC",
                "Boston", "MA", "USA",
                new BigDecimal("42.3601"), new BigDecimal("-71.0589"),
                List.of("PCI", "ECMO", "ICU"),
                500, 200);
        facilityRepository.insert(facility);

        Facility found = facilityRepository.findById(id).orElseThrow();
        assertEquals("Complete Hospital", found.name());
        assertEquals("Boston", found.locationCity());
        assertEquals(new BigDecimal("42.3601").stripTrailingZeros(), found.locationLatitude().stripTrailingZeros());
        assertEquals(List.of("PCI", "ECMO", "ICU"), found.capabilities());
        assertEquals(500, found.capacity());
        assertEquals(200, found.currentOccupancy());
    }

    private Facility createFacility(String name) {
        return createFacility(IdGenerator.generateFacilityId(), name, "ACADEMIC");
    }

    private Facility createFacility(String id, String name) {
        return createFacility(id, name, "ACADEMIC");
    }

    private Facility createFacility(String id, String name, String facilityType) {
        return new Facility(id, name, facilityType,
                "City", "State", "Country",
                new BigDecimal("40.0"), new BigDecimal("-74.0"),
                List.of(), 100, 0);
    }
}
