SELECT id
FROM medexpertmatch.doctors
WHERE name ILIKE :namePattern
LIMIT :maxResults
