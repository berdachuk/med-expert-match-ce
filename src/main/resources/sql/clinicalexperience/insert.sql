INSERT INTO medexpertmatch.clinical_experiences (id, doctor_id, case_id, procedures_performed, complexity_level, outcome, complications, time_to_resolution, rating)
VALUES (:id, :doctorId, :caseId, :proceduresPerformed, :complexityLevel, :outcome, :complications, :timeToResolution, :rating)
RETURNING id
