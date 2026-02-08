SELECT AVG(1 - (mc1.embedding <=> mc2.embedding::vector)) as avg_similarity
FROM medexpertmatch.medical_cases mc1
JOIN medexpertmatch.medical_cases mc2 ON mc2.id = :queryCaseId
WHERE mc1.id = ANY(:doctorCaseIds)
AND mc1.embedding IS NOT NULL
AND mc2.embedding IS NOT NULL