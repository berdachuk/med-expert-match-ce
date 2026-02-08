INSERT INTO medexpertmatch.medical_specialties (id, name, normalized_name, description, icd10_code_ranges, related_specialties)
VALUES (:id, :name, :normalizedName, :description, :icd10CodeRanges, :relatedSpecialties)
RETURNING id
