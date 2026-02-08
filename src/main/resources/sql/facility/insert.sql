INSERT INTO medexpertmatch.facilities (id, name, facility_type, location_city, location_state, location_country,
                                       location_latitude, location_longitude, capabilities, capacity, current_occupancy)
VALUES (:id, :name, :facilityType, :locationCity, :locationState, :locationCountry,
        :locationLatitude, :locationLongitude, :capabilities, :capacity, :currentOccupancy)
RETURNING id
