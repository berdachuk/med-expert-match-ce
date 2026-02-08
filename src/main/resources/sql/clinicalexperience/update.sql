UPDATE medexpertmatch.clinical_experiences
SET procedures_performed = :proceduresPerformed,
    complexity_level = :complexityLevel,
    outcome = :outcome,
    complications = :complications,
    time_to_resolution = :timeToResolution,
    rating = :rating,
    updated_at = CURRENT_TIMESTAMP
WHERE id = :id
