SELECT id, name, normalized_name, description, category
FROM medexpertmatch.procedures
WHERE LOWER(name) = :name
