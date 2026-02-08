SELECT embedding IS NOT NULL as has_embedding
FROM medexpertmatch.medical_cases
WHERE id = :caseId