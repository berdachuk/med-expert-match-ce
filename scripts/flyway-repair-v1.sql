UPDATE medexpertmatch.flyway_schema_history SET checksum = 1648671259 WHERE version = '1';
SELECT version, checksum, success FROM medexpertmatch.flyway_schema_history ORDER BY installed_rank;
