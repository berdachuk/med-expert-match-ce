SELECT id, code, description, category, parent_code, related_codes
FROM medexpertmatch.icd10_codes
WHERE category = :category
LIMIT :maxResults
