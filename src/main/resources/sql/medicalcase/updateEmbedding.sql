UPDATE medexpertmatch.medical_cases
SET embedding = :embedding::vector,
    embedding_dimension = :dimension
WHERE id = :id
