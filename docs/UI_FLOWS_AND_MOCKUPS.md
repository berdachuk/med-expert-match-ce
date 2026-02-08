# UI Flows and Form Mockups

**Last Updated:** 2026-02-03  
**Version:** 1.0  
**Status:** Design Phase

## Document Purpose

This document provides detailed UI flows and visual mockups for all MedExpertMatch user interfaces. All mockups are
created using PlantUML format, which can be rendered using PlantUML tools or integrated into documentation systems.

## UI Pages Overview

MedExpertMatch includes the following main UI pages:

1. **Home Page** (`/`) - Dashboard and navigation
2. **Find Specialist** (`/match`) - Specialist matching interface
3. **Consultation Queue** (`/queue`) - Queue prioritization and management
4. **Network Analytics** (`/analytics`) - Expertise network analysis
5. **Case Analysis** (`/analyze/{caseId}`) - AI-powered case analysis
6. **Regional Routing** (`/routing`) - Multi-facility routing
7. **Doctor Profile** (`/doctors/{doctorId}`) - Doctor details and history
8. **Synthetic Data** (`/admin/synthetic-data`) - Admin synthetic data generation (Administrator only)
9. **Graph Visualization** (`/admin/graph-visualization`) - Admin graph view (Administrator only)

## Page 1: Home Page (`/`)

### Purpose

Main dashboard providing navigation to all features and overview of system status.

### UI Flow

```plantuml
@startuml Home Page Flow
skinparam componentStyle rectangle
skinparam backgroundColor #FFFFFF

actor User
participant "Home Page" as Home
participant "Navigation Menu" as Nav
participant "Dashboard Widgets" as Widgets

User -> Home: Access /
Home -> Nav: Display navigation menu
Home -> Widgets: Display dashboard widgets

Nav --> User: Show menu items:
Nav --> User: - Find Specialist, Queue, Analytics, Case Analysis, Routing
Nav --> User: - Synthetic Data, Graph Visualization (Administrator only)
Nav --> User: - User selector (Regular User / Administrator)

Widgets --> User: Show statistics:
Widgets --> User: - Active cases
Widgets --> User: - Pending consultations
Widgets --> User: - System status

User -> Home: Click navigation item
Home -> Home: Route to selected page

@enduml
```

### Form Mockup

```plantuml
@startsalt
{+
  {* MedExpertMatch - Home | User Name ▼ }
  ==
  Navigation:
  [Home] [Find Specialist] [Queue] [Analytics] [Analysis] [Routing] [Admin]
  ==
  Statistics:
  [Active Cases: 42] [Pending: 18] [Doctors: 156] [Status: ✓ Healthy]
  ==
  Quick Actions:
  [Find Specialist] [View Queue] [Generate Test Data]
  ==
  Recent Activity:
  - Case #12345 | Matched to Dr. Smith
  - Case #12344 | Priority updated
  - Case #12343 | Consultation requested
  ==
  [Copyright © 2026 Siarhei Berdachuk] [Privacy] [Terms] [Support]
}
@endsalt
```

## Page 2: Find Specialist (`/match`)

### Purpose

Interface for finding and matching specialists to medical cases (Use Case 1: Specialist Matching).

### UI Flow

```plantuml
@startuml Find Specialist Flow
skinparam componentStyle rectangle
skinparam backgroundColor #FFFFFF

actor "Attending Physician" as Physician
participant "Find Specialist Page" as MatchPage
participant "Case Selection" as CaseSelect
participant "Case Form" as CaseForm
participant "Matching Service" as MatchService
participant "Results Display" as Results

Physician -> MatchPage: Navigate to /match
MatchPage -> CaseSelect: Display case selection

alt Existing Case Selected
  CaseSelect -> MatchPage: Load case details
else New Case Created
  MatchPage -> CaseForm: Display case form
  Physician -> CaseForm: Fill case details:
  Note right of CaseForm: - Chief complaint\n- Symptoms\n- Urgency level\n- Required specialty
  CaseForm -> MatchPage: Save case
end

MatchPage -> MatchService: Click "Find Specialists"
MatchService -> MatchService: Call agent/match API
MatchService -> Results: Return ranked specialists

Results -> Physician: Display results:
Note right of Results: - Doctor name\n- Specialty\n- Match score\n- Rationale\n- Availability

Physician -> Results: Review results
alt Select Specialist
  Results -> MatchPage: Click "Request Consultation"
  MatchPage -> MatchService: Send consultation request
  MatchService -> Physician: Show success message
end

@enduml
```

### Form Mockup

```plantuml
@startsalt
{+
  {* Find Specialist | }
  ==
  Case Selection:
  [Select Existing Case ▼] [OR] [Create New Case]
  ==
  Case Details:
  Type: ( ) Inpatient  ( ) Second Opinion  (•) Consult Request
  Chief Complaint: [________________________________]
  Symptoms: [________________________________]
            [________________________________]
  Urgency: [CRITICAL ▼]
  Specialty: [Cardiology ▼]
  Notes: [________________________________]
  ==
  [Find Specialists]
  ==
  Results:
  ┌─────────────────────────────────────────┐
  │ Dr. Jane Smith, MD | Cardiology        │
  │ Match: 95/100 ████████████████         │
  │ "Expert in acute MI cases..."          │
  │ Available: Now | Cases: 45 (98% success)│
  │ [Request Consultation] [View Profile]   │
  └─────────────────────────────────────────┘
  ┌─────────────────────────────────────────┐
  │ Dr. John Doe, MD | Cardiology          │
  │ Match: 87/100 █████████████             │
  │ "Strong experience with..."             │
  │ Available: 2 hours | Cases: 32 (94%)   │
  │ [Request Consultation] [View Profile]   │
  └─────────────────────────────────────────┘
  ┌─────────────────────────────────────────┐
  │ Dr. Sarah Johnson, MD | Cardiology      │
  │ Match: 82/100 ████████████             │
  │ "Board-certified cardiologist..."       │
  │ Available: Tomorrow | Cases: 28 (92%)  │
  │ [Request Consultation] [View Profile]   │
  └─────────────────────────────────────────┘
}
@endsalt
```

## Page 3: Consultation Queue (`/queue`)

### Purpose

Prioritize and manage consultation requests (Use Case 3: Queue Prioritization).

### UI Flow

```plantuml
@startuml Consultation Queue Flow
skinparam componentStyle rectangle
skinparam backgroundColor #FFFFFF

actor "Coordinator" as Coordinator
participant "Queue Page" as QueuePage
participant "Prioritization Service" as PriorityService
participant "Queue Table" as QueueTable
participant "Assignment Modal" as AssignModal

Coordinator -> QueuePage: Navigate to /queue
QueuePage -> PriorityService: Auto-load and prioritize
PriorityService -> QueueTable: Display prioritized queue

QueueTable -> Coordinator: Show queue with:
Note right of QueueTable: - Case ID\n- Patient (anonymized)\n- Chief Complaint\n- Urgency\n- Priority Score\n- Suggested Doctor

Coordinator -> QueueTable: Filter/Sort queue
alt Refresh Priority
  Coordinator -> PriorityService: Click "Refresh Queue"
  PriorityService -> QueueTable: Re-prioritize and update
end

alt Assign Case
  Coordinator -> QueueTable: Click "Assign Doctor"
  QueueTable -> AssignModal: Open assignment modal
  AssignModal -> Coordinator: Show doctor selection
  Coordinator -> AssignModal: Select doctor
  AssignModal -> QueueTable: Update queue
  QueueTable -> Coordinator: Show success message
end

alt Bulk Assign
  Coordinator -> QueueTable: Select multiple cases
  Coordinator -> QueueTable: Click "Bulk Assign"
  QueueTable -> AssignModal: Open bulk assignment
  AssignModal -> QueueTable: Assign all selected
end

@enduml
```

### Form Mockup

```plantuml
@startsalt
{+
  {* Consultation Queue | }
  ==
  Toolbar:
  [Refresh Queue] [Filter ▼] [Sort: Priority Score ▼] [Bulk Actions ▼]
  ==
  Queue Table:
  ┌───┬──────────┬──────────┬──────────────┬─────────┬──────┬─────────────┬──────────────┬─────────┐
  │☑ │ Case ID  │ Patient  │ Complaint    │ Urgency │Score │ Requested   │ Doctor       │ Actions │
  ├───┼──────────┼──────────┼──────────────┼─────────┼──────┼─────────────┼──────────────┼─────────┤
  │☑ │ CASE-001 │ A***     │ Acute MI     │ CRITICAL│ 95   │ 2026-01-27  │ Dr. Smith    │ [Assign]│
  │   │          │          │              │         │      │ 10:30       │              │ [View]  │
  ├───┼──────────┼──────────┼──────────────┼─────────┼──────┼─────────────┼──────────────┼─────────┤
  │☐ │ CASE-002 │ B***     │ Chest Pain   │ HIGH    │ 82   │ 2026-01-27  │ Dr. Jones    │ [Assign]│
  │   │          │          │              │         │      │ 09:15       │              │ [View]  │
  ├───┼──────────┼──────────┼──────────────┼─────────┼──────┼─────────────┼──────────────┼─────────┤
  │☐ │ CASE-003 │ C***     │ Follow-up    │ MEDIUM  │ 65   │ 2026-01-27  │ Dr. Brown    │ [Assign]│
  │   │          │          │              │         │      │ 08:00       │              │ [View]  │
  ├───┼──────────┼──────────┼──────────────┼─────────┼──────┼─────────────┼──────────────┼─────────┤
  │☐ │ CASE-004 │ D***     │ Routine      │ LOW     │ 45   │ 2026-01-26  │ Dr. Wilson   │ [Assign]│
  │   │          │          │              │         │      │ 16:30       │              │ [View]  │
  └───┴──────────┴──────────┴──────────────┴─────────┴──────┴─────────────┴──────────────┴─────────┘
  ==
  [← Previous] [1] [2] [3] [Next →] Showing 1-20 of 42 cases
}
@endsalt
```

## Page 4: Network Analytics (`/analytics`)

### Purpose

Analyze network expertise and identify top specialists (Use Case 4: Network Analytics).

### UI Flow

```plantuml
@startuml Network Analytics Flow
skinparam componentStyle rectangle
skinparam backgroundColor #FFFFFF

actor "Medical Director" as Director
participant "Analytics Page" as AnalyticsPage
participant "Query Form" as QueryForm
participant "Analytics Service" as AnalyticsService
participant "Results Visualization" as Results

Director -> AnalyticsPage: Navigate to /analytics
AnalyticsPage -> QueryForm: Display query form

Director -> QueryForm: Enter query:
Note right of QueryForm: - Condition (ICD-10 code)\n- Time period\n- Metrics to analyze

QueryForm -> AnalyticsService: Submit query
AnalyticsService -> AnalyticsService: Call agent/network-analytics API
AnalyticsService -> Results: Return expert metrics

Results -> Director: Display results:
Note right of Results: - Top experts ranked\n- Case volume\n- Outcomes\n- Complexity metrics\n- Visualizations

Director -> Results: Explore results
alt View Doctor Details
  Results -> AnalyticsPage: Click doctor name
  AnalyticsPage -> AnalyticsPage: Navigate to /doctors/{id}
end

@enduml
```

### Form Mockup

```plantuml
@startsalt
{+
  {* Network Analytics | }
  ==
  Query Form:
  ICD-10 Code: [I21.9________________]
  Time Period: [Past 2 Years ▼]
  Metrics:
  ☑ Case Volume  ☑ Success Rate  ☑ Complexity Score  ☑ Complication Rate
  ==
  [Analyze]
  ==
  Top Experts:
  ┌────────────────────────────────────────────────────────────┐
  │ Rank │ Doctor Name      │ Specialty │ Cases │ Success │    │
  ├──────┼──────────────────┼───────────┼───────┼─────────┼────┤
  │  1   │ Dr. Jane Smith  │ Cardiology│  245  │  98.5%  │[View]│
  │  2   │ Dr. John Doe    │ Cardiology│  189  │  97.2%  │[View]│
  │  3   │ Dr. Sarah J.    │ Cardiology│  156  │  96.8%  │[View]│
  └────────────────────────────────────────────────────────────┘
  ==
  Charts: [Bar Chart] [Line Chart] [Pie Chart]
  ==
  Top Facilities:
  - General Hospital - 450 cases
  - Medical Center - 320 cases
  - Regional Hospital - 280 cases
}
@endsalt
```

## Page 5: Case Analysis (`/analyze/{caseId}`)

### Purpose

AI-powered case analysis with differential diagnosis and recommendations (Use Case 5: Decision Support).

### UI Flow

```plantuml
@startuml Case Analysis Flow
skinparam componentStyle rectangle
skinparam backgroundColor #FFFFFF

actor "Specialist" as Specialist
participant "Case Analysis Page" as AnalysisPage
participant "Case Details" as CaseDetails
participant "Analysis Service" as AnalysisService
participant "Results Display" as Results

Specialist -> AnalysisPage: Navigate to /analyze/{caseId}
AnalysisPage -> CaseDetails: Load case details
CaseDetails -> Specialist: Display case information

Specialist -> AnalysisPage: Click "Analyze Case"
AnalysisPage -> AnalysisService: Call agent/analyze-case API
AnalysisService -> AnalysisService: Perform analysis:
Note right of AnalysisService: - Case analysis\n- Evidence retrieval\n- Recommendations\n- Expert suggestions

AnalysisService -> Results: Return analysis results

Results -> Specialist: Display results:
Note right of Results: - Structured summary\n- Differential diagnosis\n- Evidence points\n- Recommendations\n- Suggested experts

Specialist -> Results: Review analysis
alt Request Recommendations
  Specialist -> AnalysisPage: Click "Get Recommendations"
  AnalysisPage -> AnalysisService: Call agent/recommendations API
  AnalysisService -> Results: Return detailed recommendations
end

@enduml
```

### Form Mockup

```plantuml
@startsalt
{+
  {* Case Analysis - CASE-001 | }
  ==
  Case Information:
  Case ID: CASE-001 | Patient: A*** | Urgency: CRITICAL
  Chief Complaint: Acute chest pain
  ICD-10: I21.9, R06.02
  ==
  AI Analysis Results:
  Summary: [65-year-old male presenting with acute onset...]
  ==
  Differential Diagnosis:
  1. Acute Myocardial Infarction - 85% probability
  2. Unstable Angina - 12% probability
  3. Aortic Dissection - 3% probability
  ==
  Clinical Evidence:
  • ACC/AHA Guidelines for STEMI...
  • Recent studies on PCI outcomes...
  • High-quality evidence for...
  ==
  Recommendations:
  Diagnostic: 1. Immediate ECG  2. Cardiac enzymes  3. Chest X-ray
  Treatment: 1. Primary PCI (preferred)  2. Thrombolytic  3. Medical mgmt
  Monitoring: 1. Continuous monitoring  2. Serial ECGs  3. Follow-up imaging
  ==
  Suggested Experts:
  • Dr. Jane Smith - Cardiology - 95% match [Request Consultation]
  • Dr. John Doe - Interventional Cardiology - 92% match [Request Consultation]
  ==
  [Save Analysis] [Export PDF] [Share] [Request Second Opinion]
}
@endsalt
```

## Page 6: Regional Routing (`/routing`)

### Purpose

Route complex cases to appropriate facilities and specialists (Use Case 6: Regional Routing).

### UI Flow

```plantuml
@startuml Regional Routing Flow
skinparam componentStyle rectangle
skinparam backgroundColor #FFFFFF

actor "Regional Operator" as Operator
participant "Routing Page" as RoutingPage
participant "Case Selection" as CaseSelect
participant "Routing Service" as RoutingService
participant "Facility Results" as Results

Operator -> RoutingPage: Navigate to /routing
RoutingPage -> CaseSelect: Display case selection

Operator -> CaseSelect: Select case
CaseSelect -> RoutingPage: Load case details

Operator -> RoutingPage: Click "Route Case"
RoutingPage -> RoutingService: Call agent/route-case API
RoutingService -> RoutingService: Analyze routing:
Note right of RoutingService: - Case complexity\n- Required resources\n- Facility capabilities\n- Geographic factors

RoutingService -> Results: Return ranked facilities

Results -> Operator: Display results:
Note right of Results: - Ranked facilities\n- Lead specialists\n- Capability scores\n- Distance/availability

Operator -> Results: Review routing options
alt Select Facility
  Results -> RoutingPage: Click "Route to Facility"
  RoutingPage -> RoutingService: Confirm routing
  RoutingService -> Operator: Show success message
end

@enduml
```

### Form Mockup

```plantuml
@startsalt
{+
  {* Regional Routing | }
  ==
  Case Selection:
  [Select Case: CASE-001 ▼]
  Details: Complex cardiac case requiring PCI...
  ==
  Routing Criteria:
  Complexity: High | Resources: PCI, ICU | Geographic: Within 30 miles | Urgency: CRITICAL
  ==
  [Find Optimal Routes]
  ==
  Routing Options:
  ┌────────────────────────────────────────────────────────────┐
  │ ⭐ Facility 1 - Recommended                                │
  │ General Hospital | 15 miles away                           │
  │ Capability Score: 95/100 ████████████████                 │
  │ Lead Specialist: Dr. Jane Smith                            │
  │ Resources: PCI Lab, ICU, ECMO                              │
  │ Availability: Available Now                                │
  │ [Route to Facility]                                        │
  └────────────────────────────────────────────────────────────┘
  ┌────────────────────────────────────────────────────────────┐
  │ Facility 2                                                 │
  │ Medical Center | 25 miles away                             │
  │ Capability Score: 87/100 █████████████                    │
  │ Lead Specialist: Dr. John Doe                              │
  │ Resources: PCI Lab, ICU                                    │
  │ Availability: Available in 1 hour                           │
  │ [Route to Facility]                                        │
  └────────────────────────────────────────────────────────────┘
  ┌────────────────────────────────────────────────────────────┐
  │ Facility 3                                                 │
  │ Regional Hospital | 40 miles away                          │
  │ Capability Score: 78/100 ████████████                     │
  │ Lead Specialist: Dr. Sarah Johnson                         │
  │ Resources: PCI Lab                                          │
  │ Availability: Available in 2 hours                         │
  │ [Route to Facility]                                        │
  └────────────────────────────────────────────────────────────┘
}
@endsalt
```

## Page 7: Doctor Profile (`/doctors/{doctorId}`)

### Purpose

Display detailed doctor profile, experience, and case history.

### UI Flow

```plantuml
@startuml Doctor Profile Flow
skinparam componentStyle rectangle
skinparam backgroundColor #FFFFFF

actor User
participant "Doctor Profile Page" as ProfilePage
participant "Profile Data" as ProfileData
participant "Case History" as History
participant "Statistics" as Stats

User -> ProfilePage: Navigate to /doctors/{doctorId}
ProfilePage -> ProfileData: Load doctor data
ProfileData -> User: Display profile:
Note right of ProfileData: - Name, credentials\n- Specialties\n- Certifications\n- Facility affiliations

ProfilePage -> History: Load case history
History -> User: Display case history:
Note right of History: - Recent cases\n- Outcomes\n- Specializations

ProfilePage -> Stats: Load statistics
Stats -> User: Display statistics:
Note right of Stats: - Total cases\n- Success rate\n- Average complexity\n- Patient outcomes

User -> ProfilePage: Interact with profile
alt Request Consultation
  ProfilePage -> ProfilePage: Navigate to /match
end

@enduml
```

### Form Mockup

```plantuml
@startsalt
{+
  {* Doctor Profile - Dr. Jane Smith | }
  ==
  [Photo] Dr. Jane Smith, MD, FACC
  Specialty: Cardiology | Facility: General Hospital
  [Request Consultation]
  ==
  Credentials:
  Board Certifications:
  • American Board of Cardiology
  • Interventional Cardiology
  Education:
  • MD, Harvard Medical School
  • Residency, Johns Hopkins
  • Fellowship, Mayo Clinic
  ==
  Statistics:
  Total Cases: 245 | Success Rate: 98.5% | Complexity: High
  Patient Outcomes: Excellent | Years Experience: 15
  ==
  Recent Cases:
  ┌──────────┬──────────┬─────────┬────────────┐
  │ Case ID  │ Condition│ Outcome │ Date       │
  ├──────────┼──────────┼─────────┼────────────┤
  │ CASE-245 │ I21.9    │ Success │ 2026-01-26 │
  │ CASE-244 │ I21.9    │ Success │ 2026-01-25 │
  │ CASE-243 │ I20.9    │ Success │ 2026-01-24 │
  └──────────┴──────────┴─────────┴────────────┘
  ==
  Top Specializations:
  1. Acute Myocardial Infarction (I21.9) - 89 cases
  2. Unstable Angina (I20.0) - 45 cases
  3. Heart Failure (I50.9) - 32 cases
}
@endsalt
```

## Page 8: Synthetic Data (`/admin/synthetic-data`)

### Purpose

Admin interface for generating synthetic test data for demos. Visible only when Administrator is selected in the user
selector (`?user=admin`).

### UI Flow

```plantuml
@startuml Synthetic Data Flow
skinparam componentStyle rectangle
skinparam backgroundColor #FFFFFF

actor "Admin" as Admin
participant "Synthetic Data Page" as DataPage
participant "Data Form" as DataForm
participant "Generator Service" as GeneratorService
participant "Progress Display" as Progress

Admin -> DataPage: Navigate to /admin/synthetic-data
DataPage -> DataForm: Display generation form

Admin -> DataForm: Configure generation:
Note right of DataForm: - Data size\n- Options\n- Domain focus

DataForm -> GeneratorService: Submit generation request
GeneratorService -> Progress: Show progress

Progress -> Admin: Display progress:
Note right of Progress: - Generating doctors...\n- Creating cases...\n- Generating descriptions...\n- Building embeddings...\n- Creating graph...

GeneratorService -> Admin: Show completion summary:
Note right of GeneratorService: - Doctors created\n- Cases created\n- Descriptions generated\n- Embeddings generated\n- Graph built

@enduml
```

### Form Mockup

```plantuml
@startsalt
{+
  {* Synthetic Data (Admin) | }
  ==
  Generation Form:
  Data Size:
  ( ) Tiny - 5 doctors, 5 cases
  ( ) Small - 50 doctors, 100 cases
  (•) Medium - 500 doctors, 1000 cases
  ( ) Large - 2000 doctors, 4000 cases
  ( ) Huge - 50000 doctors, 100000 cases
  ==
  Options:
  ☑ Clear existing data
  ☑ Generate embeddings
  ☑ Build graph relationships
  ==
  Domain Focus:
  (•) General (all specialties)
  ( ) Cardiology
  ( ) Oncology
  ( ) Neurology
  ( ) Emergency Medicine
  ==
  [Generate Data] [Generate Data Only] [Generate Embeddings] [Build Graph] [Clear All Data]
  ==
  Progress:
  [████████████░░░░░░░░] 60%
  Status: Generating doctors... (300/500)
  ==
  Log:
  ✓ Created 300 doctors
  ✓ Created 150 medical cases
  ⏳ Generating descriptions...
  ⏳ Generating embeddings...
  ⏳ Building graph relationships...
  ==
  Completion Summary:
  ✓ Generated 500 doctors
  ✓ Generated 1000 medical cases
  ✓ Created 2500 clinical experiences
  ✓ Generated embeddings for all entities
  ✓ Built graph relationships
  Total time: 2m 34s
}
@endsalt
```

## Common UI Components

### Navigation Menu

```plantuml
@startsalt
{+
  Navigation Menu:
  [Home] [Find Specialist] [Queue] [Analytics] [Analysis] [Routing] [Admin]
  ==
  User Menu: [User Name ▼]
  • Profile
  • Settings
  • Logout
}
@endsalt
```

### Success Message Component

```plantuml
@startsalt
{+
  ┌────────────────────────────────────────────┐
  │ ✓ Consultation request sent successfully!   │
  │ [View Details]                    [×]       │
  └────────────────────────────────────────────┘
  Auto-dismisses after 5 seconds
  Can be manually dismissed
}
@endsalt
```

### Error Message Component

```plantuml
@startsalt
{+
  ┌────────────────────────────────────────────┐
  │ ⚠ Failed to load case data.                │
  │ Please try again.                           │
  │ [Need help?]                      [×]       │
  └────────────────────────────────────────────┘
  Dismissible
  Links to help/documentation
}
@endsalt
```

## User Flow Diagrams

### Complete User Journey: Specialist Matching

```plantuml
@startuml Specialist Matching User Journey
skinparam backgroundColor #FFFFFF

start

:Attending Physician logs in;
:Navigates to "Find Specialist" page;
:Selects existing case OR creates new case;

if (Create new case?) then (yes)
  :Fills case form:
  - Symptoms
  - Diagnosis
  - Urgency;
  :Saves case;
else (no)
  :Selects existing case;
endif

:Clicks "Find Specialists";
:System shows loading indicator;
:System displays ranked list of specialists;

:Physician reviews results:
- Match scores
- Rationales
- Availability;

if (Select specialist?) then (yes)
  :Clicks "Request Consultation";
  :System sends consultation request;
  :System shows success message;
  :Physician receives confirmation;
  stop
else (no)
  :Reviews other options;
  :May refine search criteria;
  :Returns to results;
endif

@enduml
```

### Complete User Journey: Queue Prioritization

```plantuml
@startuml Queue Prioritization User Journey
skinparam backgroundColor #FFFFFF

start

:Coordinator navigates to "Consultation Queue";
:System automatically loads and prioritizes queue;
:Coordinator reviews prioritized list;

if (Filter/Sort needed?) then (yes)
  :Applies filters or sorts;
  :Views filtered results;
endif

if (Refresh priority?) then (yes)
  :Clicks "Refresh Queue";
  :System re-prioritizes;
  :Updates display;
endif

if (Assign case?) then (yes)
  :Selects case(s);
  :Clicks "Assign Doctor";
  :Selects doctor from modal;
  :Confirms assignment;
  :System updates queue;
  :Shows success message;
  stop
else (no)
  :Continues reviewing queue;
endif

@enduml
```

## Responsive Design Considerations

### Mobile View

```plantuml
@startsalt
{+
  {* ☰ MedExpertMatch | 👤 }
  ==
  Mobile Layout:
  ┌─────────────────────────┐
  │ [Card: Active Cases: 42]│
  │ [Card: Pending: 18]     │
  │ [Card: Doctors: 156]   │
  └─────────────────────────┘
  ==
  Quick Actions:
  [Find Specialist]
  [View Queue]
  ==
  Navigation Drawer (☰):
  • Home
  • Find Specialist
  • Queue
  • Analytics
  • Analysis
  • Routing
  ==
  Mobile-first design
  Touch-optimized interactions
  Simplified navigation
}
@endsalt
```

## Accessibility Features

```plantuml
@startuml Accessibility Features
skinparam componentStyle rectangle
skinparam backgroundColor #F5F5F5

component "Accessibility Features" as Accessibility {
  [Keyboard Navigation] as Keyboard
  [Screen Reader Support] as ScreenReader
  [ARIA Labels] as ARIA
  [High Contrast Mode] as Contrast
  [Focus Indicators] as Focus
  [Alt Text for Images] as AltText
  [Color Contrast: WCAG AA] as ColorContrast
}

@enduml
```

## UI/UX Guidelines

### Color Scheme

```plantuml
@startsalt
{+
  Color Palette:
  Primary:   [Medical Blue #0066CC]
  Success:   [Green #28A745]
  Warning:   [Orange #FFA500]
  Error:     [Red #DC3545]
  Info:       [Blue #17A2B8]
  Background: [Light Gray #F5F5F5]
}
@endsalt
```

### Typography

```plantuml
@startsalt
{+
  Typography:
  Headings: Sans-serif, bold
  Body:     Sans-serif, regular
  Code:     Monospace
  Sizes:    Responsive scaling
}
@endsalt
```

### Spacing

```plantuml
@startsalt
{+
  Spacing Guidelines:
  Padding:  16px standard
  Margins:  24px between sections
  Grid:     12-column responsive grid
}
@endsalt
```

---

*Last updated: 2026-01-27*
