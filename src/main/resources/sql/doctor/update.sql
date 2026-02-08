UPDATE medexpertmatch.doctors
SET name = :name,
    email = :email,
    specialties = :specialties,
    certifications = :certifications,
    facility_ids = :facilityIds,
    telehealth_enabled = :telehealthEnabled,
    availability_status = :availabilityStatus,
    updated_at = CURRENT_TIMESTAMP
WHERE id = :id
