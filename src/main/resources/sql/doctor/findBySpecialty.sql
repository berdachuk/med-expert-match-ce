SELECT id, name, email, specialties, certifications, facility_ids, telehealth_enabled, availability_status
FROM medexpertmatch.doctors
WHERE :specialty = ANY(specialties)
LIMIT :maxResults
