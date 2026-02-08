SELECT id, name, normalized_name, description, icd10_code_ranges, related_specialties
FROM medexpertmatch.medical_specialties
WHERE name = :name
