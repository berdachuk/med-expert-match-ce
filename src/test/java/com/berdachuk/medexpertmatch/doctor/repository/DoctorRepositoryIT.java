package com.berdachuk.medexpertmatch.doctor.repository;

import com.berdachuk.medexpertmatch.core.util.IdGenerator;
import com.berdachuk.medexpertmatch.integration.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for DoctorRepository.
 * Uses Testcontainers PostgreSQL database.
 */
class DoctorRepositoryIT extends BaseIntegrationTest {

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private NamedParameterJdbcTemplate namedJdbcTemplate;

    @BeforeEach
    void setUp() {
        // Clear existing data to ensure test independence
        namedJdbcTemplate.getJdbcTemplate().execute("DELETE FROM medexpertmatch.clinical_experiences");
        namedJdbcTemplate.getJdbcTemplate().execute("DELETE FROM medexpertmatch.consultation_matches");
        namedJdbcTemplate.getJdbcTemplate().execute("DELETE FROM medexpertmatch.doctors");

        // Ensure pg_trgm extension is enabled for similarity search tests
        try {
            namedJdbcTemplate.getJdbcTemplate().execute("CREATE EXTENSION IF NOT EXISTS pg_trgm");
        } catch (Exception e) {
            // Extension might already exist or not be available - that's okay
        }
    }

    /**
     * Checks if pg_trgm extension is available in the database.
     */
    private boolean isPgTrgmAvailable() {
        try {
            namedJdbcTemplate.getJdbcTemplate().execute("SELECT similarity('test', 'test')");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Test
    void testFindById() {
        // Insert test doctor
        String testId = IdGenerator.generateDoctorId();
        String insertSql = """
                INSERT INTO medexpertmatch.doctors (id, name, email, specialties, certifications, facility_ids, telehealth_enabled, availability_status)
                VALUES (:id, :name, :email, :specialties, :certifications, :facilityIds, :telehealthEnabled, :availabilityStatus)
                """;
        Map<String, Object> params = new HashMap<>();
        params.put("id", testId);
        params.put("name", "Dr. John Smith");
        params.put("email", "john.smith@hospital.com");
        params.put("specialties", new String[]{"Cardiology", "Internal Medicine"});
        params.put("certifications", new String[]{"ABIM", "ABIM-Cardiology"});
        params.put("facilityIds", new String[]{"8009377469709733890"});
        params.put("telehealthEnabled", true);
        params.put("availabilityStatus", "AVAILABLE");
        namedJdbcTemplate.update(insertSql, params);

        // Test findById
        var result = doctorRepository.findById(testId);
        assertTrue(result.isPresent());
        assertEquals("Dr. John Smith", result.get().name());
        assertEquals("john.smith@hospital.com", result.get().email());
        assertEquals(2, result.get().specialties().size());
        assertTrue(result.get().specialties().contains("Cardiology"));
        assertTrue(result.get().telehealthEnabled());
    }

    @Test
    void testFindByIds() {
        // Insert test doctors
        String id1 = IdGenerator.generateDoctorId();
        String id2 = IdGenerator.generateDoctorId();

        String insertSql = """
                INSERT INTO medexpertmatch.doctors (id, name, email, specialties, telehealth_enabled, availability_status)
                VALUES (:id, :name, :email, :specialties, :telehealthEnabled, :availabilityStatus)
                """;

        Map<String, Object> params1 = new HashMap<>();
        params1.put("id", id1);
        params1.put("name", "Dr. Alice Johnson");
        params1.put("email", "alice@hospital.com");
        params1.put("specialties", new String[]{"Neurology"});
        params1.put("telehealthEnabled", false);
        params1.put("availabilityStatus", "AVAILABLE");
        namedJdbcTemplate.update(insertSql, params1);

        Map<String, Object> params2 = new HashMap<>();
        params2.put("id", id2);
        params2.put("name", "Dr. Bob Williams");
        params2.put("email", "bob@hospital.com");
        params2.put("specialties", new String[]{"Oncology"});
        params2.put("telehealthEnabled", true);
        params2.put("availabilityStatus", "BUSY");
        namedJdbcTemplate.update(insertSql, params2);

        // Test findByIds
        List<com.berdachuk.medexpertmatch.doctor.domain.Doctor> doctors = doctorRepository.findByIds(List.of(id1, id2));

        assertNotNull(doctors);
        assertEquals(2, doctors.size());
        assertTrue(doctors.stream().anyMatch(d -> d.id().equals(id1)));
        assertTrue(doctors.stream().anyMatch(d -> d.id().equals(id2)));
    }

    @Test
    void testFindByIdNotFound() {
        var result = doctorRepository.findById(IdGenerator.generateDoctorId());
        assertTrue(result.isEmpty());
    }

    @Test
    void testFindByIdsEmpty() {
        List<com.berdachuk.medexpertmatch.doctor.domain.Doctor> doctors = doctorRepository.findByIds(List.of());
        assertNotNull(doctors);
        assertTrue(doctors.isEmpty());
    }

    @Test
    void testFindByEmail() {
        // Insert test doctor
        String testId = IdGenerator.generateDoctorId();
        String insertSql = """
                INSERT INTO medexpertmatch.doctors (id, name, email, specialties, telehealth_enabled, availability_status)
                VALUES (:id, :name, :email, :specialties, :telehealthEnabled, :availabilityStatus)
                """;
        Map<String, Object> params = new HashMap<>();
        params.put("id", testId);
        params.put("name", "Dr. Jane Doe");
        params.put("email", "jane.doe@hospital.com");
        params.put("specialties", new String[]{"Pediatrics"});
        params.put("telehealthEnabled", true);
        params.put("availabilityStatus", "AVAILABLE");
        namedJdbcTemplate.update(insertSql, params);

        // Test findByEmail
        var result = doctorRepository.findByEmail("jane.doe@hospital.com");
        assertTrue(result.isPresent());
        assertEquals(testId, result.get().id());
        assertEquals("Dr. Jane Doe", result.get().name());
    }

    @Test
    void testFindBySpecialty() {
        // Insert test doctors
        String id1 = IdGenerator.generateDoctorId();
        String id2 = IdGenerator.generateDoctorId();

        String insertSql = """
                INSERT INTO medexpertmatch.doctors (id, name, email, specialties, telehealth_enabled, availability_status)
                VALUES (:id, :name, :email, :specialties, :telehealthEnabled, :availabilityStatus)
                """;

        Map<String, Object> params1 = new HashMap<>();
        params1.put("id", id1);
        params1.put("name", "Dr. Cardiologist 1");
        params1.put("email", "cardio1@hospital.com");
        params1.put("specialties", new String[]{"Cardiology"});
        params1.put("telehealthEnabled", false);
        params1.put("availabilityStatus", "AVAILABLE");
        namedJdbcTemplate.update(insertSql, params1);

        Map<String, Object> params2 = new HashMap<>();
        params2.put("id", id2);
        params2.put("name", "Dr. Cardiologist 2");
        params2.put("email", "cardio2@hospital.com");
        params2.put("specialties", new String[]{"Cardiology", "Internal Medicine"});
        params2.put("telehealthEnabled", true);
        params2.put("availabilityStatus", "AVAILABLE");
        namedJdbcTemplate.update(insertSql, params2);

        // Test findBySpecialty
        List<com.berdachuk.medexpertmatch.doctor.domain.Doctor> doctors = doctorRepository.findBySpecialty("Cardiology", 10);
        assertNotNull(doctors);
        assertEquals(2, doctors.size());
        assertTrue(doctors.stream().allMatch(d -> d.specialties().contains("Cardiology")));
    }

    @Test
    void testFindTelehealthEnabled() {
        // Insert test doctors
        String id1 = IdGenerator.generateDoctorId();
        String id2 = IdGenerator.generateDoctorId();

        String insertSql = """
                INSERT INTO medexpertmatch.doctors (id, name, email, specialties, telehealth_enabled, availability_status)
                VALUES (:id, :name, :email, :specialties, :telehealthEnabled, :availabilityStatus)
                """;

        Map<String, Object> params1 = new HashMap<>();
        params1.put("id", id1);
        params1.put("name", "Dr. Telehealth 1");
        params1.put("email", "tele1@hospital.com");
        params1.put("specialties", new String[]{"General Practice"});
        params1.put("telehealthEnabled", true);
        params1.put("availabilityStatus", "AVAILABLE");
        namedJdbcTemplate.update(insertSql, params1);

        Map<String, Object> params2 = new HashMap<>();
        params2.put("id", id2);
        params2.put("name", "Dr. No Telehealth");
        params2.put("email", "notele@hospital.com");
        params2.put("specialties", new String[]{"Surgery"});
        params2.put("telehealthEnabled", false);
        params2.put("availabilityStatus", "AVAILABLE");
        namedJdbcTemplate.update(insertSql, params2);

        // Test findTelehealthEnabled
        List<com.berdachuk.medexpertmatch.doctor.domain.Doctor> doctors = doctorRepository.findTelehealthEnabled(10);
        assertNotNull(doctors);
        assertEquals(1, doctors.size());
        assertEquals(id1, doctors.get(0).id());
        assertTrue(doctors.get(0).telehealthEnabled());
    }

    @Test
    void testInsert() {
        // Create doctor
        com.berdachuk.medexpertmatch.doctor.domain.Doctor doctor = new com.berdachuk.medexpertmatch.doctor.domain.Doctor(
                IdGenerator.generateDoctorId(),
                "Dr. New Doctor",
                "new@hospital.com",
                List.of("Emergency Medicine"),
                List.of("ABEM"),
                List.of("8009377469709733890"),
                true,
                "AVAILABLE"
        );

        String id = doctorRepository.insert(doctor);
        assertEquals(doctor.id(), id);

        // Verify doctor was created
        var result = doctorRepository.findById(doctor.id());
        assertTrue(result.isPresent());
        assertEquals("Dr. New Doctor", result.get().name());

        // Verify insert throws exception on duplicate ID
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> {
            doctorRepository.insert(doctor);
        });
    }

    @Test
    void testUpdate() {
        // Create doctor first
        com.berdachuk.medexpertmatch.doctor.domain.Doctor doctor = new com.berdachuk.medexpertmatch.doctor.domain.Doctor(
                IdGenerator.generateDoctorId(),
                "Dr. New Doctor",
                "new@hospital.com",
                List.of("Emergency Medicine"),
                List.of("ABEM"),
                List.of("8009377469709733890"),
                true,
                "AVAILABLE"
        );
        doctorRepository.insert(doctor);

        // Update doctor
        com.berdachuk.medexpertmatch.doctor.domain.Doctor updatedDoctor = new com.berdachuk.medexpertmatch.doctor.domain.Doctor(
                doctor.id(),
                "Dr. Updated Doctor",
                "updated@hospital.com",
                List.of("Emergency Medicine", "Critical Care"),
                List.of("ABEM", "ABIM"),
                doctor.facilityIds(),
                false,
                "BUSY"
        );

        String updatedId = doctorRepository.update(updatedDoctor);
        assertEquals(doctor.id(), updatedId);

        // Verify doctor was updated
        var updatedResult = doctorRepository.findById(doctor.id());
        assertTrue(updatedResult.isPresent());
        assertEquals("Dr. Updated Doctor", updatedResult.get().name());
        assertEquals(2, updatedResult.get().specialties().size());
        assertFalse(updatedResult.get().telehealthEnabled());

        // Verify update throws exception on non-existent ID
        com.berdachuk.medexpertmatch.doctor.domain.Doctor nonExistentDoctor = new com.berdachuk.medexpertmatch.doctor.domain.Doctor(
                IdGenerator.generateDoctorId(),
                "Dr. Non-existent",
                "nonexistent@hospital.com",
                List.of("General Practice"),
                List.of(),
                List.of(),
                false,
                "AVAILABLE"
        );
        assertThrows(org.springframework.dao.EmptyResultDataAccessException.class, () -> {
            doctorRepository.update(nonExistentDoctor);
        });
    }

    @Test
    void testFindAllIds() {
        // Insert test doctors
        String id1 = IdGenerator.generateDoctorId();
        String id2 = IdGenerator.generateDoctorId();

        String insertSql = """
                INSERT INTO medexpertmatch.doctors (id, name, email, specialties, telehealth_enabled, availability_status)
                VALUES (:id, :name, :email, :specialties, :telehealthEnabled, :availabilityStatus)
                """;

        Map<String, Object> params1 = new HashMap<>();
        params1.put("id", id1);
        params1.put("name", "Dr. Test 1");
        params1.put("email", "test1@hospital.com");
        params1.put("specialties", new String[]{"General Practice"});
        params1.put("telehealthEnabled", false);
        params1.put("availabilityStatus", "AVAILABLE");
        namedJdbcTemplate.update(insertSql, params1);

        Map<String, Object> params2 = new HashMap<>();
        params2.put("id", id2);
        params2.put("name", "Dr. Test 2");
        params2.put("email", "test2@hospital.com");
        params2.put("specialties", new String[]{"General Practice"});
        params2.put("telehealthEnabled", false);
        params2.put("availabilityStatus", "AVAILABLE");
        namedJdbcTemplate.update(insertSql, params2);

        // Test findAllIds
        List<String> ids = doctorRepository.findAllIds(10);
        assertNotNull(ids);
        assertTrue(ids.size() >= 2);
        assertTrue(ids.contains(id1));
        assertTrue(ids.contains(id2));
    }

    @Test
    void testDeleteAll() {
        // Insert test doctor
        String testId = IdGenerator.generateDoctorId();
        String insertSql = """
                INSERT INTO medexpertmatch.doctors (id, name, email, specialties, telehealth_enabled, availability_status)
                VALUES (:id, :name, :email, :specialties, :telehealthEnabled, :availabilityStatus)
                """;
        Map<String, Object> params = new HashMap<>();
        params.put("id", testId);
        params.put("name", "Dr. To Delete");
        params.put("email", "delete@hospital.com");
        params.put("specialties", new String[]{"General Practice"});
        params.put("telehealthEnabled", false);
        params.put("availabilityStatus", "AVAILABLE");
        namedJdbcTemplate.update(insertSql, params);

        // Verify doctor exists
        assertTrue(doctorRepository.findById(testId).isPresent());

        // Delete all
        int deleted = doctorRepository.deleteAll();
        assertTrue(deleted >= 1);

        // Verify doctor was deleted
        assertTrue(doctorRepository.findById(testId).isEmpty());
    }
}
