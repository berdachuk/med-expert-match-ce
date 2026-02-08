SELECT id
FROM medexpertmatch.medical_cases
ORDER BY created_at DESC
LIMIT :limit
