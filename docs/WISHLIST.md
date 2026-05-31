# Product Wishlist

Nice-to-have features and UX improvements not yet scheduled in an active milestone. Items here may graduate into `.agents/plans/M{NN}-*.md` when prioritized.

**Last updated:** 2026-05-31

---

## W-01: Real-time agent activity panel (Cursor-style) — ✅ Delivered (M17)

**Priority:** High (chat UX)  
**Status:** ✅ Delivered in **M17**  
**Related:** `static/js/chat.js`, `static/css/chat.css`, `templates/chat.html`

---

## W-02: Markdown rendering in AI chat responses — ✅ Delivered (M17 + M18)

**Priority:** High (chat UX)  
**Status:** ✅ Delivered in **M17** (client streaming) + **M18** (SSR allowlist)  
**Related:** `web/service/ChatMarkdownRenderer.java`, `static/js/chat.js`, `templates/chat.html`

---

## W-03: Playwright chat browser verification — ✅ Delivered (M19 + M20)

**Priority:** Medium (CI confidence)  
**Status:** MockMvc smoke in M18; Playwright profile in **M19**; full navigation in **M20** (`ChatPlaywrightSmokeTest`)  
**Related:** `.agents/plans/archive/M20-chat-governance-and-a2a-contracts.md`, `ChatPlaywrightSmokeTest`

---

## W-04: Chat transcript export — ✅ Delivered (M19)

**Priority:** Medium (compliance / portability)  
**Status:** ✅ `GET /api/v1/chats/{id}/export` with PHI redaction  
**Related:** `ChatExportService.java`

---

## W-05: *(placeholder for future items)*

Add new wishlist entries as `W-{NN}` with the same structure.
