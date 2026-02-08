INSERT INTO medexpertmatch.procedures (id, name, normalized_name, description, category)
VALUES (:id, :name, :normalizedName, :description, :category)
RETURNING id
