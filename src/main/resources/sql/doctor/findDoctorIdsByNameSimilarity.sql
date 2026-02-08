SELECT id, similarity(name, :name) as sim
FROM medexpertmatch.doctors
WHERE similarity(name, :name) >= :similarityThreshold
ORDER BY sim DESC
LIMIT :maxResults
