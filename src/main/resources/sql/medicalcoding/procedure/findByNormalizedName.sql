SELECT id, name, normalized_name, description, category
FROM medexpertmatch.procedures
WHERE LOWER(normalized_name) = :normalizedName
