UPDATE medexpertmatch.medical_cases
SET abstract = :abstract,
    updated_at = CURRENT_TIMESTAMP
WHERE id = :id
