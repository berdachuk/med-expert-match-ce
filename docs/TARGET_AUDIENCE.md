# Target Audience

This document describes the primary users and audiences of MedExpertMatch.

## Primary User Roles

### 1. Attending Physician / Case Manager (Hospital)

**Context:** Physician or case manager in a hospital who needs a specialist for a complex inpatient case.

**Goals:**

- Find the best specialist in the network quickly
- Get a timely, high-quality consultation
- Reduce delay to consultation and length of stay

**Main use:** Specialist matching for complex inpatient cases (Find Specialist). May use EMR integration or direct text
input.

**Use case:** [Specialist Matching for Complex Inpatient Cases](USE_CASES.md)

---

### 2. Referring Physician or Patient (Second Opinion / Telehealth)

**Context:** A doctor referring a patient for a second opinion, or a patient requesting a second opinion via a portal.

**Goals:**

- Get a second opinion from the most appropriate specialist
- Confirm or adjust the treatment plan
- Prefer telehealth when relevant

**Main use:** Online second opinion flow. Upload medical records (FHIR Bundle); system matches to the right
sub-specialist by diagnosis and complexity.

**Use case:** [Online Second Opinion / Telehealth](USE_CASES.md)

---

### 3. Consultation Coordinator / Department Head

**Context:** Person who manages the consultation queue and assigns consults to specialists.

**Goals:**

- Prioritize consult requests by clinical urgency
- Ensure the sickest patients are seen first
- Avoid urgent consults being buried in a FIFO queue

**Main use:** Consultation queue prioritization. Opens the consult queue; system returns consults sorted by priority (
CRITICAL / HIGH / MEDIUM / LOW) with optional suggested doctors.

**Use case:** [Prioritizing the Consultation Queue](USE_CASES.md)

---

### 4. Chief Medical Officer / Quality and Analytics Team

**Context:** Medical director or analytics team responsible for routing policy, capability planning, and quality.

**Goals:**

- See which specialists and facilities actually handle complex cases in specific domains
- Plan routing and capability development
- Support mentorship and learning programs with data

**Main use:** Network analytics. Queries such as "top experts for ICD-10 I21.9 in the past 2 years"; gets ranked doctors
and facilities with volume, complexity, outcomes.

**Use case:** [Network Analytics](USE_CASES.md)

---

### 5. Specialist / Consulting Physician

**Context:** Specialist or consulting physician working on a case and needing decision support and colleagues to discuss
with.

**Goals:**

- Get structured case analysis, differential diagnosis, and evidence-based recommendations
- Find colleagues or multidisciplinary team members to discuss the case
- Improve diagnostic and treatment decisions with human-in-the-loop AI

**Main use:** AI analysis and expert suggestions (analyze case, recommendations, evidence retrieval, colleague
matching).

**Use case:** [Human-in-the-Loop Decision Support](USE_CASES.md)

---

### 6. Regional Health Authority / Multi-Hospital Network

**Context:** Regional operator or central system that routes complex cases across hospitals and centers.

**Goals:**

- Route complex cases to the most capable centers and specialists
- Improve patient outcomes and use resources effectively
- Make referrals and transfers transparent and measurable

**Main use:** Cross-organization / regional routing. Submits case; system returns ranked facilities with suggested lead
specialists and explanations (case complexity, outcomes, capacity, geography).

**Use case:** [Cross-Organization / Regional Routing](USE_CASES.md)

---

## Secondary Roles (Demo / Development)

### Regular User

**Context:** Default role in the demo UI (simulated security).

**Access:** Main application flows (Find Specialist, chats, consultation queue, etc.). No access to admin-only pages.

### Administrator

**Context:** Admin role in the demo UI (simulated security, e.g. `?user=admin`).

**Access:** All regular features plus:

- Synthetic Data generation (`/admin/synthetic-data`)
- Graph Visualization (`/admin/graph-visualization`)

*Note: The application uses simulated roles for demo and development; production would use real authentication and
authorization.*

---

## Summary Table

| Audience                           | Primary need                             | Main feature(s)                                           |
|------------------------------------|------------------------------------------|-----------------------------------------------------------|
| Attending physician / Case manager | Fast specialist for complex case         | Find Specialist                                           |
| Referring physician / Patient      | Second opinion from right sub-specialist | Find Specialist (second opinion)                          |
| Consultation coordinator           | Urgency-based queue                      | Consultation Queue                                        |
| CMO / Analytics team               | Data on real expertise                   | Network Analytics                                         |
| Specialist / Consulting physician  | Analysis, evidence, colleagues           | Case analysis, Evidence, Recommendations, Expert matching |
| Regional health authority          | Optimal facility and specialist routing  | Regional Routing                                          |
| Administrator (demo)               | Synthetic data, graph inspection         | Synthetic Data, Graph Visualization                       |

---

*Last updated: 2026-02-08*
