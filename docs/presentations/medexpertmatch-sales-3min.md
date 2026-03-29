---
revealjs:
  presentation: true
  height: 800
---

## MedExpertMatch

<div class="reveal-slide-row">

<div class="reveal-slide-text-col">

**MedExpertMatch**

*Right specialist. Right time. AI-powered.*

</div>

<div class="reveal-slide-image-col">

<img class="reveal-slide-image" width="768" height="1024" src="../images/slide-intro-why.png" alt="MedExpertMatch title visual" />

</div>

</div>

Note: ~10 s. Hook: AI-powered expert matching in minutes, not days.

---

## The problem

<div class="reveal-slide-row">

<div class="reveal-slide-text-col">

- Patients wait **days or weeks** for specialist consultation
- Matching relies on **“who do I know?”** or who is next in line
- Second opinions go to the **wrong sub-specialist**
- **Urgent** consults sit behind routine ones (FIFO)
- **“Who is good at what”** is anecdotal—no data

</div>

<div class="reveal-slide-image-col">

<img class="reveal-slide-image" width="768" height="1024" src="../images/problem1-consultation-delays.png" alt="Consultation delay and routing chaos" />

</div>

</div>

Note: ~18 s. Emphasize wait time, ad hoc matching, FIFO vs urgency.

---

## The cost

<div class="reveal-slide-row">

<div class="reveal-slide-text-col">

- Longer **length of stay**, more anxiety, worse outcomes
- **Wrong routing**, overload on the wrong people
- **Opaque referrals**—hard to justify or improve

</div>

<div class="reveal-slide-image-col">

<img class="reveal-slide-image" width="768" height="1024" src="../images/slide-metrics-honesty.png" alt="Cost and outcomes" />

</div>

</div>

Note: ~12 s. Connect problem to operational and clinical cost.

---

## MedExpertMatch

<div class="reveal-slide-row">

<div class="reveal-slide-text-col">

- **MedGemma (HAI-DEF)** for case analysis and medical reasoning
- Match cases to specialists in **minutes**
- **Data-driven** expertise: who really handles what
- **One AI copilot**: analysis, evidence, recommendations, experts
- **Runs fully locally**—no data to external AI; HIPAA-aware

</div>

<div class="reveal-slide-image-col">

<img class="reveal-slide-image" width="768" height="1024" src="../images/slide-llm-tools.png" alt="AI copilot and local stack" />

</div>

</div>

Note: ~12 s. Pivot to solution: MedGemma, minutes, one copilot, local option.

---

## How matching works

<div class="reveal-slide-row">

<div class="reveal-slide-text-col">

- **Hybrid GraphRAG**: vector + graph + history
- **40%** semantic similarity (PgVector)
- **30%** graph relationships (Apache AGE)
- **30%** historical performance
- Ranked list with **rationales**

</div>

<div class="reveal-slide-image-col">

<img class="reveal-slide-image" width="768" height="1024" src="../images/slide-hybrid-graphrag.png" alt="Hybrid GraphRAG channels" />

</div>

</div>

Note: ~18 s. Say “40-30-30” clearly; demo will show live scores.

---

## What you get

<div class="reveal-slide-row">

<div class="reveal-slide-text-col">

- **Find Specialist** — consults and second opinions in minutes
- **Consultation queue** — prioritized by urgency
- **Network analytics** — experts by ICD-10, volume, outcomes
- **AI copilot** — analysis, guidelines, PubMed, recommendations
- **Regional routing** — facility and lead by complexity and capacity
- **FHIR ingestion** & **APIs** — EMR and portals
- **Synthetic data** — demo / evaluation datasets
- **Admin: graph view** — doctors, cases, facilities

</div>

<div class="reveal-slide-image-col">

<img class="reveal-slide-image" width="768" height="1024" src="../images/slide-six-scenarios.png" alt="Product capabilities" />

</div>

</div>

Note: ~24 s. Keep pace; next slide is live demo (~45 s).

---

## Live demo

<div class="reveal-slide-row">

<div class="reveal-slide-text-col">

**Switch to the application (~45 s)**

1. **Find Specialist** — match one case; show ranked list + rationales  
2. **Consultation queue** — urgency order  
3. **Chat** — analyze case / recommendations  
4. **Admin → graph** — doctors, cases, facilities  

If the app is unavailable: skip and offer a follow-up demo.

</div>

<div class="reveal-slide-image-col">

<img class="reveal-slide-image" width="768" height="1024" src="../images/slide-demo-match.png" alt="Application demo" />

</div>

</div>

Note: Full step table and prep in PRESENTATION_SALES_3MIN.md. Return to slides after demo.

---

## Why MedExpertMatch

<div class="reveal-slide-row">

<div class="reveal-slide-text-col">

- **Minutes, not days** — one pipeline, three signals, explainable
- **Real expertise** — graph + analytics, not job titles alone
- **One copilot** — not five disconnected tools
- **Urgent first** — queue by need, not arrival time
- **Fully local & secure** — your infrastructure; no data to external AI; no PHI in logs

</div>

<div class="reveal-slide-image-col">

<img class="reveal-slide-image" width="768" height="1024" src="../images/slide-conclusion.png" alt="Why MedExpertMatch" />

</div>

</div>

Note: ~18 s. Reinforce after demo.

---

## Next step

<div class="reveal-slide-row">

<div class="reveal-slide-text-col">

**MedExpertMatch** — right specialist, right time.

*Demo • Full documentation • MedGemma Impact Challenge 2026*

</div>

<div class="reveal-slide-image-col">

<img class="reveal-slide-image" width="768" height="1024" src="../images/slide-thank-you.png" alt="Thank you and next steps" />

</div>

</div>

Note: ~15 s. CTA: docs, longer demo, thank you.

=====

## Below the deck (references)

Full **speaker script**, image/video prompts, timing tables, and demo preparation: [Sales presentation (3 min)](../PRESENTATION_SALES_3MIN.md).

- [Demo Guide](../DEMO_GUIDE.md)
- [Use Cases](../USE_CASES.md)
- [Full 45-minute deck](medexpertmatch-full-presentation.md)
