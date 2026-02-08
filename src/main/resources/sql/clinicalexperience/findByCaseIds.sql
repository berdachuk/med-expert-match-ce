SELECT id, doctor_id, case_id, procedures_performed, complexity_level, outcome, complications, time_to_resolution, rating
FROM medexpertmatch.clinical_experiences
WHERE case_id = ANY(:caseIds)
ORDER BY case_id, created_at DESC
