UPDATE medexpertmatch.medical_specialties
SET name = :name,
    normalized_name = :normalizedName,
    description = :description,
    icd10_code_ranges = :icd10CodeRanges,
    related_specialties = :relatedSpecialties
WHERE id = :id
RETURNING id
