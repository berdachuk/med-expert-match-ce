SELECT id, doctor_id, case_id, procedures_performed, complexity_level, outcome, complications, time_to_resolution, rating
FROM medexpertmatch.clinical_experiences
WHERE case_id = :caseId
ORDER BY created_at DESC
