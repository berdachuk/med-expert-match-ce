INSERT INTO medexpertmatch.medical_cases (id, patient_age, chief_complaint, symptoms, current_diagnosis, icd10_codes, snomed_codes, urgency_level, required_specialty, case_type, additional_notes, abstract)
VALUES (:id, :patientAge, :chiefComplaint, :symptoms, :currentDiagnosis, :icd10Codes, :snomedCodes, :urgencyLevel, :requiredSpecialty, :caseType, :additionalNotes, :abstract)
RETURNING id
