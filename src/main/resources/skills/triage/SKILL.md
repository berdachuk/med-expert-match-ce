---
name: triage
description: Assess urgency and severity of incoming medical cases to prioritize and route them safely
---

# Triage

This skill guides you on how to assess the urgency and severity of an incoming medical case so it can
be prioritized and routed to the right level of care without delay.

## Overview

The Triage skill helps you:

1. **Assess urgency** - Determine how quickly a case must be seen (CRITICAL, HIGH, MEDIUM, LOW)
2. **Identify red flags** - Detect features that signal a time-critical or life-threatening condition
3. **Recommend disposition** - Suggest an appropriate care level and routing target
4. **Escalate uncertainty** - Default to the safer (more urgent) tier when signals are ambiguous

## When to Use This Skill

Use this skill when:

- A new case arrives and its priority must be established
- You need to rank a queue of cases by clinical urgency
- You need to detect red-flag symptoms that require immediate escalation
- You need to recommend a care setting (emergency, urgent, routine)
- You need to decide which case a limited specialist pool should see first

## Available Tools

This skill composes with the agent's case-analysis tools. Use the `case-analyzer` skill to extract
symptoms and findings, then apply the triage workflow below to assign an urgency tier. Where a
routing decision is needed, hand off to the `routing-planner` skill.

## Triage Tiers

- **CRITICAL**: Immediate, life- or limb-threatening; see now
- **HIGH**: Potentially serious; see promptly
- **MEDIUM**: Stable but needs timely assessment
- **LOW**: Routine; can be scheduled

## Workflow

1. **Gather presentation** - Collect chief complaint, symptoms, and relevant findings
2. **Screen for red flags** - Check for time-critical features (e.g. airway, breathing, circulation, neuro deficits)
3. **Assign urgency tier** - Map the presentation to CRITICAL, HIGH, MEDIUM, or LOW
4. **Recommend disposition** - Suggest the care level and routing target
5. **Escalate on doubt** - When signals are ambiguous, choose the more urgent tier and flag for review

## Output Format

When triaging a case, include:

- **Urgency Level**: CRITICAL, HIGH, MEDIUM, or LOW
- **Red Flags**: Any time-critical features identified
- **Rationale**: Why this urgency tier was assigned
- **Recommended Disposition**: Suggested care level / routing target
- **Review Note**: Whether the assessment is uncertain and needs human confirmation

## Medical Disclaimer

**IMPORTANT**: All triage assessments are for research and educational purposes only. This system is
not certified for clinical use and must not replace a qualified clinician's triage decision. Urgency
tiers and red-flag detection are advisory suggestions that require human-in-the-loop validation; when
in doubt, escalate to a medical professional.
