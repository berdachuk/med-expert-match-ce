# M40: Inline Agent Activity Panel (Collapsible, Inside Chat Area)

## Problem

The chat page currently has **3 columns**: left sidebar (chat list), center (messages + input), and a **dedicated right panel** (`col-lg-3`) showing live agent activity. This layout wastes screen space — the right panel is empty most of the time.

## Goal

Remove the dedicated right panel. Move agent activity into an **inline, collapsible panel** inside the center chat area, above the message input. The panel collapses to a compact summary line when inactive, and expands to show live agent steps (tool calls, reasoning, plan updates) during streaming.

## UI Behavior (after change)

- **Idle**: A thin collapsed summary bar (e.g. `▸ 3 agent(s) · 12 steps · 8s — click to expand`) sits above the input.
- **During streaming**: Panel can be expanded to show live steps inline. User can collapse it manually.
- **Post-streaming**: Auto-collapses to summary. Click toggles expand/collapse.
- **No right panel**: Center area expands to full width (or col-lg-10 alongside sidebar).

## Files to Change

| File | Change |
|------|--------|
| `src/main/resources/templates/chat.html` | Remove right col-lg-3 (lines 57-62), widen center to col-lg-10. Add inline expandable `agentActivityInlinePanel` inside message area. |
| `src/main/resources/static/js/chat.js` | Replace `renderActivityPanel` (right-panel) with inline-panel rendering. Modify `collapseActivityPanel`/`expandActivityPanel` to toggle inline panel instead of side panel. Keep `agentActivitySummary` as collapsed bar. |
| `src/main/resources/static/css/chat.css` | Update styles for inline agent panel (remove right-panel scroll, add inline expand/collapse transitions). |

## Implementation Steps

1. **HTML layout** (`chat.html`):
   - Remove `<div class="col-lg-3 d-none d-lg-flex ...">` (right panel)
   - Change center column from `col-lg-7` to `col-lg-10`
   - Replace existing `agentActivityPanel` div (currently hidden in right column) with a new `agentActivityInlinePanel` div placed in center column, below messages and above the progress panels. This panel will be `d-none` initially and shown on demand.
   - The `agentActivitySummary` bar stays where it is (above input), but now acts as the sole toggle for expand/collapse of inline panel.

2. **JavaScript** (`chat.js`):
   - `resetActivityPanel()`: instead of referencing right panel, set up inline panel (`agentActivityInlinePanel`)
   - `renderActivityPanel()`: render entries into `agentActivityInlinePanel` instead of `#agentActivityPanel`
   - `collapseActivityPanel()`: hide `agentActivityInlinePanel`, update summary text, keep `agentActivitySummary` visible
   - `expandActivityPanel()`: show `agentActivityInlinePanel`, hide summary, re-render
   - Remove references to old `#agentActivityPanel` (right-side element ID)
   - `activityPanels()` helper: update to return inline panel + summary

3. **CSS** (`chat.css`):
   - Remove: `#agentActivityPanel { max-height: ... }` (no right panel)
   - Add: `#agentActivityInlinePanel` — max-height with overflow-y, border-left accent styling matching current activity entry colors
   - Add: smooth expand/collapse transition (max-height animation or class toggle)
   - Keep: `.agent-activity-entry.*` color styles (unchanged)

## Acceptance Criteria (curl verification)

After change, when sending a message:
- Right panel must NOT appear in DOM
- `agentActivitySummary` bar appears inside chat area after streaming completes
- Clicking summary toggles inline `agentActivityInlinePanel` visibility
- During streaming, inline panel shows live agent steps

## Verification

```bash
# Start stack and check /chat page renders without right panel column
curl -s http://localhost:8094/chat | grep -c 'agentActivityPanel'  # should be 1 (inline panel only)
```
