SELECT COALESCE(MAX(rank), 0)
FROM medexpertmatch.consultation_matches
WHERE case_id = :caseId
