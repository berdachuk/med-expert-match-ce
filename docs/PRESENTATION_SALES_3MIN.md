# MedExpertMatch: Sales Presentation (max 3 min)

This document is a ready-to-deliver sales presentation with slide content, speaker script, and a **built-in application
demo** in the scenario. **Total duration: no more than 3 min** (slides ~1 min 35 s + demo ~45 s + slides 7–8 ~30 s).

---

## Slide text and timing (what to show and when)

Use this as a checklist: what text appears on each slide and for how long. All times are approximate.

| #        | When | Duration  | Text on slide                                                                                                                                                                                                                                                                                                                        |
|----------|------|-----------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **1**    | 0:00 | 10 s      | **MedExpertMatch** (title, large) / *Right specialist. Right time. AI-powered.* (tagline)                                                                                                                                                                                                                                            |
| **2**    | 0:10 | 18 s      | **The problem** (title) / 5 bullets: Patients wait days or weeks…; Matching relies on "who do I know?"…; Second opinions go to wrong sub-specialist; Urgent consults behind routine (FIFO); "Who is good at what" is anecdotal—no data                                                                                               |
| **3**    | 0:28 | 12 s      | **The cost** (title) / 3 bullets: Longer length of stay, more anxiety, worse outcomes; Wrong routing, overload on wrong people; Opaque referrals, hard to justify or improve                                                                                                                                                         |
| **4**    | 0:40 | 12 s      | **MedExpertMatch** (title) / 5 bullets: **MedGemma (HAI-DEF)** for case analysis and medical reasoning; Match cases in **minutes**; **Data-driven** expertise; **One AI copilot**; **Runs fully locally** — no data to external AI, HIPAA-aware                                                                                      |
| **5**    | 0:52 | 18 s      | **How matching works** (title) / 5 bullets: **Hybrid GraphRAG**: vector + graph + history; 40% semantic similarity (PgVector); 30% graph relationships (Apache AGE); 30% historical performance; Ranked list with rationales                                                                                                         |
| **6**    | 1:10 | 24 s      | **What you get** (title) / 9 bullets: **Find Specialist** — …; **Consultation queue** — …; **Network analytics** — …; **AI copilot** — …; **Regional routing** — …; **FHIR ingestion** — …; **FHIR & APIs** — …; **Synthetic data** — generate test dataset (doctors, cases); **Admin: graph view** — administrator views data graph |
| **Demo** | 1:34 | **~45 s** | *[Switch to application]* Find Specialist → Consultation queue → Chat → Admin graph view. See **Demo (in scenario)** below.                                                                                                                                                                                                          |
| **7**    | 2:19 | 18 s      | **Why MedExpertMatch** (title) / 5 bullets: **Minutes, not days**; **Real expertise**; **One copilot**; **Urgent first**; **Fully local & secure**                                                                                                                                                                                   |
| **8**    | 2:37 | 15 s      | **Next step** (title) / **MedExpertMatch** — right specialist, right time. / *Demo • Full documentation • MedGemma Impact Challenge 2026*                                                                                                                                                                                            |                                                                                                                                                                                                         |

**When to show what (animation suggestion):**

- **Slides 1, 4, 8:** Title and subtitle visible from the start (no build).
- **Slides 2, 3, 5, 6, 7:** Show title immediately; animate bullets one by one (on click or every 4–5 s) so the speaker
  can stay in sync. Alternatively show all text at once if you prefer no builds.

**Total:** 8 slides + **Demo** = **no more than 3 min** (slides 1–6 ~1 min 34 s, demo ~45 s, slides 7–8 ~33 s). Keep to
these timings; if you run over, shorten the demo or speak faster on slides 2 and 6.

**Application demo (in scenario):** The demo is **part of the script** and takes place **after Slide 6** (~45 s). Switch
to the app and follow **Demo (in scenario)** below. Then return to Slide 7.

---

## Slide 1: Title (≈10 s)

**Slide:**  
**MedExpertMatch**  
*Right specialist. Right time. AI-powered.*

**Script:**  
Create slide in 16:9 aspect ratio:
MedExpertMatch is an AI-powered medical expert recommendation system. It matches every case to the right specialist at
the right time—in minutes, not days.

**Visual:** Clean title slide. Logo or wordmark "MedExpertMatch" centred; tagline below. Subtle medical/tech motif (e.g.
abstract network or connection nodes) without clutter. Professional, modern, healthcare-tech feel.

**Image prompt (Midjourney):**  
`Minimalist presentation title slide, "MedExpertMatch" bold typography, subtitle "Right specialist. Right time. AI-powered.", soft gradient background blue to white, subtle abstract network nodes or connecting lines, professional healthcare technology, clean corporate style, 16:9 --v 6 --style raw`

**Image prompt (DALL-E / ChatGPT):**  
"Minimalist slide design: large title 'MedExpertMatch', tagline 'Right specialist. Right time. AI-powered.' below. Soft
blue-to-white gradient. Faint abstract network or connection nodes in background. Professional healthcare technology
style, 16:9."

**Video prompt (Runway / Pika / Sora):**  
"Abstract network of glowing nodes and thin connecting lines on a soft blue-to-white gradient background. Nodes pulse
gently and connections light up one by one. Slow zoom in. Clean, corporate, healthcare tech. 5 seconds, 16:9."

---

## Slide 2: The Problem (≈18 s)

**Slide:**  
**The problem**

- Patients wait days or weeks for specialist consultation
- Matching relies on "who do I know?" or who's next in line
- Second opinions go to the wrong sub-specialist
- Urgent consults sit behind routine ones in a FIFO queue
- "Who is good at what" is anecdotal—no data

**Script:**  
"In hospitals and networks today, patients wait days or weeks for a specialist. Matching is ad hoc: who do I know, or
who's next in the queue. Second opinions often land with a generic specialty instead of the right sub-specialist. Urgent
consults wait behind routine ones. And nobody has real data on who actually handles which complex cases—it's all word of
mouth."

**Visual:** Metaphor of delay and chaos. E.g. long corridor with waiting people; or a tangled flowchart (patient to ???
to specialist) with question marks and clocks. Slightly muted, slightly stressful palette (grey, orange accents).
Bullets on the right or below.

**Image prompt (Midjourney):**  
`Healthcare waiting room concept, long hospital corridor with silhouettes of waiting patients, clock on wall showing delay, subtle question marks and tangled arrows, muted grey and soft orange, infographic style, professional, 16:9 --v 6 --style raw`

**Image prompt (DALL-E / ChatGPT):**  
Create slide in 16:9 aspect ratio:
"Concept illustration for 'the problem': hospital corridor with waiting patients, wall clock, tangled arrows and
question marks suggesting unclear routing. Muted greys and soft orange. Professional infographic style"

**Video prompt (Runway / Pika / Sora):**  
"Slow camera move along a long hospital corridor. Silhouettes of people waiting in chairs. A large wall clock ticks.
Muted grey and soft orange lighting. Slight blur or haze. Feels like delay and uncertainty. 5–6 seconds, 16:9."

---

## Slide 3: The Cost (≈12 s)

**Slide:**  
**The cost**

- Longer length of stay, more anxiety, worse outcomes
- Wrong routing, overload on the wrong people
- Opaque referrals, hard to justify or improve

**Script:**  
"The cost is real: longer stays, more anxiety, and worse outcomes. Wrong routing overloads some teams and underuses
others. Referrals and transfers stay opaque—hard to defend and hard to improve."

**Visual:** Simple cause-effect or "cost" metaphor. E.g. downward trend line (outcomes), scales tipping wrong way, or
split diagram: overloaded team vs underused team. Red/grey accents. Keep text readable; image supports, does not
dominate.

**Image prompt (Midjourney):**  
`Abstract business cost concept, downward trend line graph, unbalanced scales, split image overloaded vs idle team, red and grey tones, clean infographic, professional presentation slide, 16:9 --v 6 --style raw`

**Image prompt (DALL-E / ChatGPT):**  
Create slide in 16:9 aspect ratio:
Abstract 'cost' illustration: downward trend, unbalanced scales, or overloaded vs underused teams. Red and grey. Clean
infographic for presentation slide

**Video prompt (Runway / Pika / Sora):**  
"Animated infographic: a red line graph trending downward. In the same frame, a balance scale tilting to one side. Red
and grey palette. Smooth motion. Clean, professional. 4–5 seconds, 16:9."

---

## Slide 4: Our Solution (≈12 s)

**Slide:**  
**MedExpertMatch**

- **MedGemma (HAI-DEF)** for case analysis and medical reasoning
- Match cases to specialists in **minutes**
- **Data-driven** expertise: who really handles what
- **One AI copilot**: analysis, evidence, recommendations, experts
- **Runs fully locally**: no data sent to external AI; secure, HIPAA-aware

**Script:**  
"MedExpertMatch fixes this. We use MedGemma—Google's HAI-DEF model—for case analysis and medical reasoning. You get
specialist matching in minutes, not days. Real expertise becomes visible—who actually handles which cases, by diagnosis
and complexity. And we give specialists one AI copilot: case analysis, evidence, recommendations, and matched colleagues
in a single flow. The application can run fully locally: your data never leaves your environment and is not sent to
external AI—secure and HIPAA-aware."

**Visual:** Positive turn. E.g. same corridor now with clear path and "minutes" clock; or one central hub (AI copilot)
connecting case, evidence, recommendations, experts. Shield or lock for privacy. Green/blue accents.

**Image prompt (Midjourney):**  
`Healthcare solution concept, clear path from patient to specialist, clock showing minutes, central AI hub connecting case evidence recommendations experts, small shield icon for privacy, blue and green tones, modern tech, 16:9 --v 6 --style raw`

**Image prompt (DALL-E / ChatGPT):**  
"Solution concept: clear path from patient to specialist, clock showing 'minutes', central hub connecting case,
evidence, recommendations, experts. Small privacy shield. Blue and green. Modern healthcare tech, 16:9."

**Video prompt (Runway / Pika / Sora):**  
"A clear path of light or line animates from a simple 'patient' icon to a central glowing hub, then to a 'specialist'
icon. The hub pulses; small nodes for case, evidence, recommendations connect to it. A small shield symbol appears. Blue
and green. 5–6 seconds, 16:9."

---

## Slide 5: How It Works (≈18 s)

**Slide:**  
**How matching works**

- **Hybrid GraphRAG**: vector + graph + history
- 40% semantic similarity (PgVector)
- 30% graph relationships (Apache AGE)
- 30% historical performance
- Ranked list with rationales

**Script:**  
"Matching is powered by what we call Hybrid GraphRAG. We combine three signals: semantic similarity from embeddings—what
the case is about; graph relationships—which doctors and facilities are connected to similar cases; and historical
performance—past outcomes and experience. Forty, thirty, thirty. You get a ranked list of specialists with clear
rationales, not a black box."

**Visual:** Diagram: one input "Medical case" flowing into three branches (or three pillars) labelled 40% / 30% / 30%,
then merging into "Ranked specialists + rationales". Optional: simple graph nodes and vectors. Blue/indigo, diagram
style.

**Image prompt (Midjourney):**  
`Infographic diagram, one input "Medical case" splits into three branches 40 percent 30 percent 30 percent, then merges to "Ranked specialists", simple graph nodes and vector arrows, blue indigo palette, clean tech diagram, 16:9 --v 6 --style raw`

**Image prompt (DALL-E / ChatGPT):**  
"Create a clean, professional tech infographic diagram in 16:9 aspect ratio:
Diagram: one box 'Medical case' splits into three branches (40%, 30%, 30%) then merges into 'Ranked specialists +
rationales'. Simple graph nodes and arrows. Blue/indigo. Clean tech infographic,

- **Hybrid GraphRAG**: vector + graph + history
- 40% semantic similarity (PgVector)
- 30% graph relationships (Apache AGE)
- 30% historical performance
- Ranked list with rationales"

**Note:** The full application demo is in the scenario after Slide 6 (see **Demo (in scenario)**). On this slide you
only describe how matching works; the live 40-30-30 result is shown during the demo.

**Video prompt (Runway / Pika / Sora):**  
"Animated diagram: one central box labelled 'Medical case' splits into three flowing branches that merge again into '
Ranked specialists'. Data or particles flow along the three paths. Blue and indigo. Clean tech infographic style. 5–6
seconds, 16:9."

---

## Slide 6: What You Get (≈24 s)

**Slide:**  
**What you get**

- **Find Specialist** — inpatient consults and second opinions in minutes
- **Consultation queue** — prioritized by urgency (CRITICAL first)
- **Network analytics** — top experts by ICD-10, volume, outcomes
- **AI copilot** — analysis, guidelines, PubMed, recommendations, colleague matching
- **Regional routing** — best facility and lead specialist by complexity and capacity
- **FHIR ingestion** — ingest data from external systems via FHIR Bundles (EMR, portals)
- **FHIR & APIs** — plug into your EMR and portals
- **Synthetic data** — generate test dataset (doctors, cases) for demo or evaluation
- **Admin: graph view** — administrator can view the data graph (doctors, cases, facilities)

**Script:**  
"You get nine things. Find Specialist: inpatient consults and second opinions in minutes, with the right sub-specialist.
A consultation queue ordered by clinical urgency—sickest first. Network analytics that show top experts by condition
code, volume, and outcomes. An AI copilot for specialists: analysis, guidelines, PubMed, recommendations, and suggested
colleagues. Regional routing: best facility and lead specialist by complexity and capacity. You can ingest data from
external systems via FHIR Bundles—so EMRs and portals push cases in. It all plugs in via FHIR and REST APIs. You can
also generate a synthetic test dataset—doctors and cases—for demo or evaluation. And the administrator can open a graph
view to see the data graph: doctors, cases, facilities, and their relationships. I'll show the app now."

**Visual:** Six icons or six short cards in a grid (2x3 or 3x2): magnifying glass / list (Find Specialist), queue with
priority badge (Consultation queue), bar chart / network (Analytics), chat + document (AI copilot), map + building (
Regional routing), plug / API (FHIR). Consistent icon style, one accent colour.

**Image prompt (Midjourney):**  
`Six feature icons in grid, healthcare app, find specialist magnifying glass, priority queue list, analytics chart network, AI chat document, map building routing, API plug, consistent line icon style, one blue accent, white background, 16:9 --v 6 --style raw`

**Image prompt (DALL-E / ChatGPT):**  
"Create a clean, professional tech infographic diagram in 16:9 aspect ratio:
Grid of six simple icons: Find Specialist (magnifying glass), Consultation queue (list with priority), Network
analytics (chart), AI copilot (chat + doc), Regional routing (map + building), FHIR/API (plug). Line icon style, one
blue accent, white background"

**Video prompt (Runway / Pika / Sora):**  
"Six simple icons appear one after another on a white grid: magnifying glass, priority list, chart, chat bubble with
document, map with building, plug. Each icon has a subtle blue glow or highlight. Clean, minimal. 6–8 seconds total, 16:
9."

---

## Demo (in scenario) — after Slide 6 (~45 s)

The demo is part of the presentation flow. After Slide 6, switch to the running application and follow these steps. *
*Keep to ~45 s total**; use short script lines.

| Step | When | Action                                                                                           | Script (short)                                     |
|------|------|--------------------------------------------------------------------------------------------------|----------------------------------------------------|
| 1    | 0:00 | Switch to app. Open Find Specialist, pick one case, run match. Show ranked list with rationales. | "Find Specialist—ranked list, 40-30-30 scoring."   |
| 2    | 0:12 | Open Consultation queue. Show list by urgency.                                                   | "Queue by urgency—sickest first."                  |
| 3    | 0:20 | Open Chat. One request: "Analyze case [id]" or "Recommendations". Show answer briefly.           | "AI copilot: analysis and colleagues."             |
| 4    | 0:32 | Switch to Administrator. Open Admin → Graph visualization. Show graph.                           | "Admin sees the graph—doctors, cases, facilities." |
| 5    | 0:45 | Switch back to slides. Go to Slide 7.                                                            | "Back to the slides."                              |

**Before the presentation:** Start the application (`mvn spring-boot:run` or run JAR). Ensure demo data exists (or
generate synthetic data). Have one medical case ID at hand. If the UI uses a role selector, know how to switch to
Administrator for the graph view (e.g. `?user=admin` or header dropdown).

**If the app is not available:** Skip the demo; go from Slide 6 straight to Slide 7 and say: "We'd be happy to show the
application in a separate demo."

---

## Slide 7: Why MedExpertMatch (≈18 s)

**Slide:**  
**Why MedExpertMatch**

- **Minutes, not days** — one pipeline, three signals, explainable
- **Real expertise** — graph + analytics, not job titles
- **One copilot** — no juggling five tools
- **Urgent first** — queue by need, not arrival time
- **Fully local & secure** — runs on your infrastructure; no data to external AI; no PHI in logs

**Script:**  
"Why us? We deliver matching in minutes with one pipeline—vector, graph, and history—and explainable rationales. We make
real expertise visible from data, not job titles. Specialists get one copilot instead of five separate tools. The queue
is ordered by clinical need, not first-come-first-served. And the application can run fully locally on your
infrastructure—data is never sent to external AI, and we don't log PHI."

**Visual:** Five short differentiators as icons with one-line labels: clock (minutes), graph/chart (real expertise),
single app/copilot vs many windows (one copilot), priority flag (urgent first), shield (privacy). Same style as Slide 6
for consistency.

**Image prompt (Midjourney):**  
`Five differentiator icons, clock minutes, graph analytics expertise, one app vs many windows copilot, priority flag urgent first, shield privacy, line icon style blue accent, professional slide, 16:9 --v 6 --style raw`

**Image prompt (DALL-E / ChatGPT):**  
Create a clean, professional tech infographic diagram in 16:9 aspect ratio:
"Five icons with labels: clock (minutes not days), graph (real expertise), single app (one copilot), priority flag (
urgent first), shield (privacy). Line style, blue accent"

**Video prompt (Runway / Pika / Sora):**  
"Five icons appear in sequence with a soft blue accent: clock, graph chart, single window vs many windows, priority
flag, shield. Simple line style. Each icon highlights briefly. Professional, minimal. 5–6 seconds, 16:9."

---

## Slide 8: Close & CTA (≈15 s)

**Slide:**  
**Next step**  
**MedExpertMatch** — right specialist, right time.  
*Demo • Full documentation • MedGemma Impact Challenge 2026*

**Script:**  
"MedExpertMatch: right specialist, right time. Full documentation is available—setup, API, use cases. We'd be glad to
show you a demo or walk through the docs. Thank you."

**Visual:** Same branding as Slide 1. Contact/CTA block: "Demo", "Full documentation", "MedGemma Impact Challenge 2026".
Optional: QR code or short URL for docs/demo. Clean, minimal.

**Image prompt (Midjourney):**  
`Presentation closing slide, MedExpertMatch logo, tagline right specialist right time, three buttons or lines Demo Docs MedGemma Impact Challenge 2026, minimal clean design, blue white, 16:9 --v 6 --style raw`

**Image prompt (DALL-E / ChatGPT):**  
Create Closing slide in 16:9 aspect ratio:
MedExpertMatch, tagline 'Right specialist, right time.' MedGemma Impact Challenge 2026.
Minimal, blue and white

**Video prompt (Runway / Pika / Sora):**  
"Minimal closing slide. Soft blue-to-white gradient. Text 'MedExpertMatch' and tagline 'Right specialist, right time.'
fade in. Three subtle elements or buttons for Demo, Docs, Challenge appear. Calm, professional. 4–5 seconds, 16:9."

**Note:** The application demo is already part of the scenario after Slide 6 (see **Demo (in scenario)**). After Slide
8, you can offer a longer demo or hand out docs.

---

## Timing Summary

| Step      | Content              | Time                                                        |
|-----------|----------------------|-------------------------------------------------------------|
| 1         | Title                | 10 s                                                        |
| 2         | Problem              | 18 s                                                        |
| 3         | Cost                 | 12 s                                                        |
| 4         | Solution             | 12 s                                                        |
| 5         | How it works         | 18 s                                                        |
| 6         | What you get         | 24 s                                                        |
| **Demo**  | **Application demo** | **~45 s**                                                   |
| 7         | Why us               | 18 s                                                        |
| 8         | Close & CTA          | 15 s                                                        |
| **Total** |                      | **≤3 min** (slides 1–6 ~1:34, demo ~0:45, slides 7–8 ~0:33) |

Keep total under 3 min. If over: shorten demo (e.g. skip graph view) or speak faster on slides 2 and 6.

---

## One-Page Cheat Sheet (for speaker)

1. **Hook:** Right specialist, right time—in minutes, not days.
2. **Problem:** Waiting days; "who do I know?"; wrong sub-specialist; FIFO queue; no data on expertise.
3. **Cost:** Longer stay, worse outcomes, wrong routing, opaque referrals.
4. **Solution:** MedGemma (HAI-DEF) for case analysis; match in minutes; data-driven expertise; one AI copilot; runs
   fully locally.
5. **How:** Hybrid GraphRAG = 40% vector + 30% graph + 30% history → ranked list with rationales.
6. **What you get:** Find Specialist, urgency queue, network analytics, AI copilot, regional routing, FHIR ingestion,
   FHIR/APIs, synthetic test data generator, Admin graph view.
7. **Demo (in scenario):** After slide 6 — switch to app: Find Specialist → queue → Chat → Admin graph view (~45 s).
   Then Slide 7.
8. **Why us:** Minutes not days, real expertise, one copilot, urgent first, fully local—no data to external AI.
9. **CTA:** Demo and full documentation; thank you.

---

## Demo Script: Step-by-Step

**Main scenario:** The presentation includes a **built-in demo after Slide 6**. Follow the steps in **Demo (in scenario)
** above (Find Specialist → Consultation queue → Chat → Admin graph view). The table there has timing and script lines.

**Preparation (before the presentation):**

- Start the application (`mvn spring-boot:run` or run JAR). Ensure demo profile has data (or generate synthetic data).
- Open the app in the browser (e.g. `http://localhost:8080`).
- Have one medical case ID ready. Know how to switch to Administrator for the graph view (e.g. `?user=admin` or header
  dropdown).

**Alternative flows (if you change the order or skip the in-scenario demo):**

- **Option A — after Slide 5 only:** Find Specialist → show ranked list and rationales ("this is the 40-30-30 result").
- **Option B — after Slide 6 (extended):** Same as Demo (in scenario): Find Specialist, Consultation queue, Chat, then *
  *Admin graph view** (switch to Administrator, open Graph visualization).
- **Option C — at the end:** After Slide 8, run Find Specialist once + one Chat question + optionally show graph view;
  then offer docs or a longer demo.

**Fallback (app not available):** Skip the demo; go from Slide 6 to Slide 7 and say you can show the app in a separate
session. Or use a short pre-recorded clip (Find Specialist + Chat + graph view) or static screenshots.

---

## Image Prompts Quick Reference

| Slide | Midjourney (short)                                                          | DALL-E (short) |
|-------|-----------------------------------------------------------------------------|----------------|
| 1     | Minimalist title "MedExpertMatch", tagline, blue gradient, abstract network | Same + 16:9    |
| 2     | Hospital corridor, waiting patients, clock, question marks, grey/orange     | Same           |
| 3     | Downward trend, unbalanced scales, overloaded vs idle, red/grey             | Same           |
| 4     | Clear path patient to specialist, clock minutes, AI hub, shield, blue/green | Same           |
| 5     | Diagram: Medical case → 40% / 30% / 30% → Ranked specialists, blue/indigo   | Same           |
| 6     | Six icons grid: find, queue, analytics, copilot, routing, API, blue accent  | Same           |
| 7     | Five icons: clock, graph, one app, priority flag, shield, blue              | Same           |
| 8     | Closing slide, logo, Demo Docs Challenge, minimal blue/white                | Same           |

All prompts assume **16:9** aspect ratio for slides. Add `--v 6 --style raw` for Midjourney if needed.

---

## Video Prompts Quick Reference

Use these as alternatives to static images. Recommended length per clip: **4–8 seconds**. Tools: Runway Gen-3, Pika,
Luma Dream Machine, Kling, or Sora (when available).

| Slide | Video concept (short)                                                            |
|-------|----------------------------------------------------------------------------------|
| 1     | Abstract network nodes pulse and connect; slow zoom; blue gradient.              |
| 2     | Slow dolly along hospital corridor; waiting silhouettes; clock; grey/orange.     |
| 3     | Animated downward trend line + tilting scale; red/grey; infographic.             |
| 4     | Path of light from patient to hub to specialist; hub pulses; shield; blue/green. |
| 5     | Diagram: one box splits into three flows, then merges; data flow; blue/indigo.   |
| 6     | Six icons appear in sequence on grid; blue accent; minimal.                      |
| 7     | Five differentiator icons appear one by one; blue highlight; line style.         |
| 8     | Title and tagline fade in; three CTA elements appear; blue/white; calm.          |

**Tips for video:** Export 16:9; loop or hold last frame if the clip is shorter than the slide duration. For tools that
support image-to-video, generate the still first with Midjourney/DALL-E, then use it as the first frame with motion (
e.g. "slow zoom in" or "subtle parallax").

---

*Last updated: 2026-02-08*
