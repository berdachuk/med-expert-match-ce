UPDATE medexpertmatch.facilities
SET name = :name,
    facility_type = :facilityType,
    location_city = :locationCity,
    location_state = :locationState,
    location_country = :locationCountry,
    location_latitude = :locationLatitude,
    location_longitude = :locationLongitude,
    capabilities = :capabilities,
    capacity = :capacity,
    current_occupancy = :currentOccupancy
WHERE id = :id
RETURNING id
