#!/usr/bin/env bash
docker exec medexpertmatch-postgres psql -U medexpertmatch -d medexpertmatch -c "SELECT id, specialties, telehealth_enabled FROM medexpertmatch.doctors;"
docker exec medexpertmatch-postgres psql -U medexpertmatch -d medexpertmatch -c "SELECT id, required_specialty, urgency_level FROM medexpertmatch.medical_cases WHERE id='6a1c79a862d83900018ecef3';"
docker exec medexpertmatch-postgres psql -U medexpertmatch -d medexpertmatch -c "SELECT doctor_id, match_score FROM medexpertmatch.consultation_matches WHERE case_id='6a1c79a862d83900018ecef3' ORDER BY match_score DESC;"
