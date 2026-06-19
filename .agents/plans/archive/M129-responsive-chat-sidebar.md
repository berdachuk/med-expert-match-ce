# M129: Responsive Chat Sidebar — Hide on Small/Medium, Hamburger Menu

**Status:** Active (planned 2026-06-19)
**Created:** 2026-06-19

## Problem Statement

The AI Chat page has a left sidebar with chat history that occupies `col-md-3 col-lg-2` (25% on medium, ~16.7% on large). On small and medium devices (phones, tablets), this sidebar takes up significant screen real estate, leaving little room for the actual chat messages and input area. Users on mobile devices cannot see the chat content properly.

## Goal

1. Hide the chat sidebar on screens smaller than `lg` (992px) by default
2. Show a hamburger menu button (☰) in the top-left of the chat area when the sidebar is hidden
3. Clicking the hamburger toggles the sidebar as an overlay/off-canvas panel
4. Clicking outside the sidebar or selecting a chat closes it

## Non-Goals

| Don't | Why |
|-------|------|
| Touch the sidebar content or behavior | Only visibility/toggle changes |
| Add new dependencies | Bootstrap 5 has offcanvas built-in |
| Change the desktop layout | `col-md-3 col-lg-2` stays as-is on lg+ |
| Add i18n keys | Use existing Bootstrap aria labels |

## Changes

### Part 1 — Template (`chat.html`)

| Area | File | Change |
|------|------|--------|
| Sidebar column | `chat.html` | Add `d-none d-lg-block` class to the sidebar column (`col-md-3 col-lg-2`) |
| Hamburger button | `chat.html` | Add a `d-lg-none` button with `☰` icon above the message panel, toggles sidebar visibility |
| Offcanvas | `chat.html` | Wrap sidebar in Bootstrap offcanvas component for slide-in on small screens |

### Part 2 — CSS (`chat.css`)

| Area | File | Change |
|------|------|--------|
| Offcanvas styling | `chat.css` | Ensure offcanvas sidebar has proper z-index, width, and transition |
| Hamburger button | `chat.css` | Style the toggle button (position, size, hover) |

### Part 3 — JavaScript (`chat.js`)

| Area | File | Change |
|------|------|--------|
| Toggle logic | `chat.js` | Wire hamburger click to show/hide offcanvas sidebar |
| Auto-close | `chat.js` | Close sidebar on chat selection (click on a chat item) |
| Click-outside | `chat.js` | Close sidebar when clicking outside the offcanvas panel |

## Phases

| Phase | Task | Status |
|-------|------|--------|
| 1 | Add offcanvas wrapper to sidebar in `chat.html` + hamburger button | Pending |
| 2 | Add CSS for offcanvas sidebar and hamburger button | Pending |
| 3 | Add JS toggle logic: hamburger click, auto-close on chat select, click-outside close | Pending |
| 4 | Manual smoke test: resize browser to <992px, verify sidebar hidden, hamburger visible, toggle works | Pending |

## Acceptance Criteria

- [ ] On screens <992px, the chat sidebar is hidden by default
- [ ] A hamburger menu button (☰) is visible in the top-left of the chat area on small screens
- [ ] Clicking the hamburger shows the sidebar as an overlay panel
- [ ] Clicking a chat in the sidebar closes it
- [ ] Clicking outside the sidebar closes it
- [ ] On screens ≥992px, the sidebar is always visible (no change from current behavior)
- [ ] No regressions on desktop layout

## References

- `src/main/resources/templates/chat.html` — main chat template
- `src/main/resources/templates/fragments/chat-sidebar.html` — sidebar fragment
- `src/main/resources/static/css/chat.css` — chat-specific styles
- `src/main/resources/static/js/chat.js` — chat JavaScript
- Bootstrap 5 Offcanvas: https://getbootstrap.com/docs/5.3/components/offcanvas/
