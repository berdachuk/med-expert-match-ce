# Network Analytics

## Purpose

Network Analytics (Use Case 4) lets CMOs and analytics teams discover top experts and analyze expertise networks. The
system queries the expertise graph (Apache AGE), runs aggregate metrics, and returns a readable report. The UI shows
progress, allows cancel, and renders results as Markdown.

## Benefits

- **Data-driven expertise mapping** – See who actually treats which conditions and how experts connect to cases and
  specialties.
- **Transparent long-running jobs** – Users are told the job may take one to several minutes and can cancel at any time.
- **Readable results** – Output is rendered as Markdown (headings, lists, code) for quick scanning.
- **Single action** – One click to generate; no form to fill.

## How It Works

### User flow

1. User opens **Network Analytics** (`/analytics`).
2. User clicks **Generate Analytics**.
3. The page shows a spinner and **Cancel**; the request runs in the background.
4. On completion, **Analytics Results** shows the report (Markdown). On cancel, a "Request cancelled" message appears.

### Sequence diagram

```mermaid
sequenceDiagram
    participant User
    participant UI as Analytics Page
    participant API as POST /api/v1/agent/network-analytics
    participant Agent as MedicalAgentService
    participant Graph as Apache AGE / Tools

    User->>UI: Click Generate Analytics
    UI->>UI: Show progress, hide button
    UI->>API: POST {} (fetch + AbortController)
    API->>Agent: networkAnalytics(request)
    Agent->>Graph: network-analyzer skill (graph queries, metrics)
    Graph-->>Agent: Results
    Agent-->>API: AgentResponse(response, metadata)
    API-->>UI: JSON
    UI->>UI: Render Markdown, show result card
    UI->>User: Analytics Results

    Note over User,UI: User may click Cancel anytime
    User->>UI: Click Cancel
    UI->>UI: Abort fetch
    UI->>User: "Request cancelled"
```

### Component flow

```mermaid
graph LR
    subgraph UI["/analytics"]
        A[Generate button]
        B[Progress + Cancel]
        C[Results card]
    end
    subgraph Backend
        D[AnalyticsController]
        E[MedicalAgentController]
        F[MedicalAgentServiceImpl]
        G[network-analyzer skill]
    end
    A -->|click| B
    B -->|fetch| E
    E --> F
    F --> G
    G -->|response| F
    F --> E
    E -->|JSON| C
```

### API

| Item     | Value                                                  |
|----------|--------------------------------------------------------|
| Endpoint | `POST /api/v1/agent/network-analytics`                 |
| Request  | Optional JSON body (e.g. `{}`)                         |
| Response | `AgentResponse`: `response` (string), `metadata` (map) |

## Related

- [Use Cases](USE_CASES.md) – Use Case 4: Network Analytics
- [Medical Agent Tools](MEDICAL_AGENT_TOOLS.md) – network-analyzer tools
- [Architecture](ARCHITECTURE.md) – API and pages
