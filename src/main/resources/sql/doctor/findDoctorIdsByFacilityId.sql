SELECT id
FROM medexpertmatch.doctors
WHERE :facilityId = ANY(facility_ids)
LIMIT :limit
