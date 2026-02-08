SELECT id
FROM medexpertmatch.facilities
ORDER BY created_at
LIMIT CASE WHEN :limit > 0 THEN :limit ELSE 2147483647 END
