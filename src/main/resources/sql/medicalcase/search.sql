SELECT id, patient_age, chief_complaint, symptoms, current_diagnosis,
       icd10_codes, snomed_codes, urgency_level, required_specialty,
       case_type, additional_notes, abstract
FROM medexpertmatch.medical_cases
WHERE 1=1
  AND (COALESCE(:query, '') = '' OR
       chief_complaint ILIKE '%' || :query || '%' OR
       symptoms ILIKE '%' || :query || '%' OR
       additional_notes ILIKE '%' || :query || '%')
  AND (COALESCE(:specialty, '') = '' OR required_specialty = :specialty)
  AND (COALESCE(:urgencyLevel, '') = '' OR urgency_level::text = :urgencyLevel)
  AND (COALESCE(TRIM(:caseId), '') = '' OR LOWER(id) = LOWER(TRIM(:caseId)))
ORDER BY created_at DESC
LIMIT :maxResults OFFSET :offset
