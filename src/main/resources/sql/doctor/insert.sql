INSERT INTO medexpertmatch.doctors (id, name, email, specialties, certifications, facility_ids, telehealth_enabled, availability_status)
VALUES (:id, :name, :email, :specialties, :certifications, :facilityIds, :telehealthEnabled, :availabilityStatus)
RETURNING id
