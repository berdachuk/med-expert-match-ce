# MedExpertMatch Demo Guide

This guide provides instructions for preparing and running the MedExpertMatch demo.

## Prerequisites

1. **Database**: PostgreSQL 17 with PgVector and Apache AGE 1.6.0
2. **Java**: JDK 21
3. **Maven**: 3.9+
4. **Docker**: For test containers and demo database

## Demo Preparation

### Step 1: Build Test Container

```bash
./scripts/build-test-container.sh
```

### Step 2: Start Demo Database

```bash
docker compose -f docker-compose.dev.yml up -d postgres-demo
```

### Step 3: Start Application

```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=demo
```

### Step 4: Generate Demo Dataset

Generate medium-sized dataset (500 doctors, 1000 cases):

```bash
# Via REST API
curl -X POST "http://localhost:8080/api/v1/test-data/generate?size=medium&clear=true"

# Or via UI
# Navigate to http://localhost:8080/admin/test-data
# Select "Medium" size and check "Clear existing data first"
# Click "Generate Test Data"
```

### Step 5: Verify Data Generation

```bash
# Check doctors count
curl "http://localhost:8080/api/v1/doctors" | jq '. | length'

# Check cases count
curl "http://localhost:8080/api/v1/medical-cases" | jq '. | length'
```

## Demo Scenarios

### Use Case 1: Specialist Matching

**Scenario**: Match doctors to a complex inpatient case

1. Navigate to `/match`
2. Select a medical case ID from the dropdown
3. Click "Match Doctors"
4. Review matched doctors with scores and rationale

**Expected Result**: Doctors matched based on:

- Specialty alignment
- Clinical experience with similar cases
- Historical performance
- Vector similarity (when embeddings are available)

### Use Case 2: Second Opinion (Telehealth)

**Scenario**: Find telehealth-enabled doctors for second opinion

1. Navigate to `/match`
2. Select a case that requires second opinion
3. Click "Match Doctors"
4. Filter results to show only telehealth-enabled doctors

**Expected Result**: Doctors with:

- Telehealth capability enabled
- Relevant specialty
- High match scores

### Use Case 3: Queue Prioritization

**Scenario**: Prioritize consultation queue based on urgency and complexity

1. Navigate to `/queue`
2. Click "Prioritize Queue"
3. Review prioritized cases

**Expected Result**: Cases ordered by:

- Urgency level (CRITICAL > HIGH > MEDIUM > LOW)
- Complexity score
- Time in queue
- Resource availability

### Use Case 4: Network Analytics

**Scenario**: Analyze network expertise for ICD-10 code I21.9 (Acute MI)

1. Navigate to `/analytics`
2. Click "Generate Analytics"
3. Review network analysis

**Expected Result**: Analytics showing:

- Doctors with most experience treating I21.9
- Case volume by specialty
- Success rates
- Geographic distribution

### Use Case 5: Decision Support

**Scenario**: Get AI-powered case analysis and recommendations

1. Navigate to `/analyze/{caseId}` (replace with actual case ID)
2. Review case analysis
3. Review evidence-based recommendations
4. Review matched doctors

**Expected Result**:

- Case analysis with ICD-10 codes
- Evidence-based recommendations
- Matched specialists
- Clinical reasoning

### Use Case 6: Regional Routing

**Scenario**: Route case to appropriate facility

1. Navigate to `/routing`
2. Select a case ID
3. Click "Route Case"
4. Review facility routing recommendations

**Expected Result**: Facilities ranked by:

- Geographic proximity
- Capacity availability
- Specialty capabilities
- Historical performance

## UI Pages

### Home Page (`/`)

- Dashboard with statistics
- Quick actions
- Recent activity

### Find Specialist (`/match`)

- Case selection
- Doctor matching
- Match results with scores

### Consultation Queue (`/queue`)

- Queue prioritization
- Prioritized case list
- Urgency indicators

### Network Analytics (`/analytics`)

- Network analysis generation
- Analytics results
- Expertise visualization

### Case Analysis (`/analyze/{caseId}`)

- Case details
- AI analysis
- Recommendations
- Matched doctors

### Regional Routing (`/routing`)

- Case selection
- Facility routing
- Route recommendations

### Doctor Profile (`/doctors/{doctorId}`)

- Doctor information
- Specialties
- Clinical experience
- Performance metrics

### Synthetic Data (`/admin/synthetic-data`)

- Administrator only (select Administrator in the user selector)
- Data size selection
- Generate test data
- Clear data
- Generation status

### Graph Visualization (`/admin/graph-visualization`)

- Administrator only (select Administrator in the user selector)
- Graph view of doctors, cases, and relationships

## Testing

### Run All Integration Tests

```bash
mvn verify
```

### Run Specific Test Suite

```bash
mvn test -Dtest=MedicalAgentControllerIT
mvn test -Dtest=TestDataGeneratorIntegrationIT
```

### Run UI Tests

```bash
mvn test -Dtest=HomeControllerIT
```

## Troubleshooting

### Database Connection Issues

```bash
# Check database is running
docker ps | grep postgres

# Check database logs
docker logs medexpertmatch-postgres-demo
```

### Test Container Issues

```bash
# Rebuild test container
./scripts/build-test-container.sh

# Clean Maven cache
mvn clean
```

### Application Startup Issues

```bash
# Check application logs
tail -f logs/medexpertmatch.log

# Verify configuration
cat src/main/resources/application-demo.yml
```

## Performance Considerations

- **Small Dataset**: 50 doctors, 100 cases - Fast queries (< 1s)
- **Medium Dataset**: 500 doctors, 1000 cases - Moderate queries (1-3s)
- **Large Dataset**: 2000 doctors, 5000 cases - Slower queries (3-10s)
- **Huge Dataset**: 5000 doctors, 10000 cases - Slow queries (10-30s)

For demo purposes, **medium** dataset is recommended for good balance between realism and performance.

## Next Steps

1. Generate embeddings for all entities (when EmbeddingService is implemented)
2. Build graph relationships in Apache AGE (when GraphService is fully configured)
3. Add more sophisticated UI components
4. Implement caching for better performance
5. Add monitoring and metrics
