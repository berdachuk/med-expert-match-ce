INSERT INTO medexpertmatch.icd10_codes (id, code, description, category, parent_code, related_codes)
VALUES (:id, :code, :description, :category, :parentCode, :relatedCodes)
RETURNING id
