INSERT INTO medexpertmatch.synthetic_data_generation_runs
    (id, size, doctor_count, case_count, start_time, end_time, total_duration_ms,
     description_ms, embedding_ms, clinical_experience_ms, graph_build_ms, error_message)
VALUES
    (:id, :size, :doctorCount, :caseCount, :startTime, :endTime, :totalDurationMs,
     :descriptionMs, :embeddingMs, :clinicalExperienceMs, :graphBuildMs, :errorMessage)