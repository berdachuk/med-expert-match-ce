SELECT id, size, doctor_count, case_count, start_time, end_time,
       total_duration_ms, description_ms, embedding_ms,
       clinical_experience_ms, graph_build_ms, error_message
FROM medexpertmatch.synthetic_data_generation_runs
WHERE size = :size
ORDER BY start_time DESC
LIMIT :limit