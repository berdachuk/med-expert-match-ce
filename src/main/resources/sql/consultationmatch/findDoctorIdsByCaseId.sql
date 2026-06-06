SELECT doctor_id
FROM medexpertmatch.consultation_matches
WHERE case_id = :caseId
ORDER BY rank
