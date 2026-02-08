SELECT id, doctor_id, case_id, procedures_performed, complexity_level, outcome, complications, time_to_resolution, rating
FROM medexpertmatch.clinical_experiences
WHERE doctor_id = ANY(:doctorIds)
ORDER BY doctor_id, created_at DESC
