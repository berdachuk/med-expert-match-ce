UPDATE medexpertmatch.synthetic_data_generation_runs
SET end_time = :endTime,
    total_duration_ms = :totalDurationMs,
    description_ms = :descriptionMs,
    embedding_ms = :embeddingMs,
    clinical_experience_ms = :clinicalExperienceMs,
    graph_build_ms = :graphBuildMs,
    error_message = :errorMessage
WHERE id = :id