UPDATE medexpertmatch.procedures
SET name = :name,
    normalized_name = :normalizedName,
    description = :description,
    category = :category
WHERE id = :id
