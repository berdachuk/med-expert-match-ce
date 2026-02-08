SELECT id, name, normalized_name, description, category
FROM medexpertmatch.procedures
WHERE category = :category
ORDER BY name
