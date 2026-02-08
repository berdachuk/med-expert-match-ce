SELECT id, name, facility_type, location_city, location_state, location_country,
       location_latitude, location_longitude, capabilities, capacity, current_occupancy
FROM medexpertmatch.facilities
ORDER BY name
