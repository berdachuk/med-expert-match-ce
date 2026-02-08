UPDATE medexpertmatch.icd10_codes
SET description = :description,
    category = :category,
    parent_code = :parentCode,
    related_codes = :relatedCodes
WHERE id = :id
