# M125: Main Menu Restructure — AI Chat as Primary Entry Point

**Status:** Planned  
**Date:** 2026-06-19  
**IDs:** REQ-125, SCN-125

---

## 1. Goal

Replace the current dashboard home page (`/` → `index.html`) with the AI Chat page as the default landing page. The dashboard (stat cards + quick actions) is removed as a landing page; AI Chat becomes the primary user entry point at the root URL.

---

## 2. Motivation

- AI Chat is the most frequently used feature and the natural starting point for user workflows (case analysis, specialist matching, routing, etc.)
- The dashboard stats are low-value noise for most sessions
- Eliminates one click (Home → Chat) from the common path

---

## 3. Changes

### 3.1 Controllers

#### 3.1.1 `HomeController.java`

**Action:** Remove `HomeController` entirely. Its only endpoint `GET /` is no longer needed.

#### 3.1.2 `ChatWebController.java`

**Action:** Remap from `@RequestMapping("/chat")` to handle `GET /` (root) as the primary mapping. Keep `GET /chat` as an alias for backward compatibility.

- Change class-level mapping from `@RequestMapping("/chat")` to no class-level mapping
- Add `@GetMapping("/")` and keep `@GetMapping("/chat")` as alias
- Both delegate to the same `chatPage(...)` method
- The `currentPage` attribute changes from `"chat"` to something consistent — either keep `"chat"` or use `"index"` — pick `"chat"` since it reflects the rendered content

### 3.2 Thymeleaf Templates

#### 3.2.1 `index.html`

**Action:** Remove or archive. This template is no longer rendered by any controller.

#### 3.2.2 `chat.html`

**Action:** No structural changes — this template now renders at `/` and `/chat`. The `currentPage` attribute passed by `ChatWebController` should be `"chat"`.

### 3.3 Navigation Bar (`fragments/header.html`)

The nav currently uses `currentPage == 'index'` as a gate:
- Only shows sub-page links (Find Specialist, Case Analysis, Queue, Analytics, Routing, AI Chat) when `currentPage == 'index'`
- Shows a back arrow when on any non-index page

With chat as root, the nav logic changes:

| Current Behavior | New Behavior |
|---|---|
| Home link (`/`) always visible | Home link removed; replaced by **AI Chat** link pointing to `/` |
| Sub-page links only when `currentPage == 'index'` | Sub-page links always visible in the nav (no gating) |
| AI Chat link only when `currentPage == 'index'` | AI Chat link removed (Home replaces it at root) |
| Back arrow when `currentPage != 'index'` | Back arrow when `currentPage != 'chat'` (always goes to `/`) |

**Actions:**
- Replace the "Home" link block (lines 52-55) with an "AI Chat" link pointing to `/` with active style when `currentPage == 'chat'`
- Remove the gating `th:if="${currentPage == 'index'}"` on all sub-page links (Find Specialist, Case Analysis, Queue, Analytics, Routing) — they should always be visible as `th:a` elements
- Keep active/highlighted style logic based on each sub-page's `currentPage`
- The back arrow condition changes from `currentPage != 'index'` to `currentPage != 'chat'`
- Remove the dedicated "AI Chat" nav item (lines 82-86) since the renamed "Home" link now serves this role

### 3.4 i18n (`messages.properties`, `messages_ru.properties`)

| Key | Current | New |
|---|---|---|
| `nav.home` | `Home` / `Главная` | `AI Chat` / `ИИ-чат` |
| `nav.chat` | `AI Chat` / `ИИ-чат` | Remove (no longer a separate nav item) |

### 3.5 Tests

#### 3.5.1 `HomeControllerIT.java`

**Action:** Remove this test file since `HomeController` is deleted.

#### 3.5.2 `ChatWebController*IT.java`

**Action:** Update or add integration test verifying `GET /` returns the `chat` view with expected model attributes (`currentPage`, `chats`, `currentChat`, `messages`, `currentUserId`). Extend existing test if one exists, or create `ChatWebControllerRootIT.java`.

#### 3.5.3 Navigation tests (if any)

Check if any `header.html` fragment tests exist and update assertions for the new nav link structure.

---

## 4. Risk Assessment

| Risk | Impact | Mitigation |
|---|---|---|
| Hardcoded `/chat` links in templates | Links break | Audit all templates for `/chat` references; keep `/chat` as alias so they still work |
| External bookmarks to `/` show chat instead of dashboard | Low (dashboard was not a bookmark target) | Acceptable — this is the intended behavior |
| Admin users losing quick access to admin dashboard | Low | Admin dashboard link remains in nav (admin-only); back arrow for admin users preserved |

---

## 5. Files Changed

| File | Change |
|---|---|
| `.../web/controller/HomeController.java` | Delete entire file |
| `.../web/controller/ChatWebController.java` | Add `GET /` mapping, keep `GET /chat` alias |
| `resources/templates/index.html` | Delete entire file |
| `resources/templates/fragments/header.html` | Rewrite nav: Home→AI Chat at root, sub-page links always visible, remove gating |
| `resources/i18n/messages.properties` | `nav.home=AI Chat`, remove `nav.chat` |
| `resources/i18n/messages_ru.properties` | `nav.home=ИИ-чат`, remove `nav.chat` |
| `.../web/controller/HomeControllerIT.java` | Delete entire file |
| `.../web/controller/ChatWebControllerIT.java` (or new file) | Add test for `GET /` → `chat` view |

---

## 6. Acceptance Criteria

1. `GET /` renders the AI Chat page (chat template with sidebar, messages, composer)
2. `GET /chat` continues to work and renders the same page
3. Navigation bar shows "AI Chat" as the first/active link at root
4. Sub-page links (Find Specialist, Case Analysis, Queue, Analytics, Routing) are always visible in the nav
5. Back arrow appears on sub-pages pointing to `/`
6. Admin navigation still functions (back arrow + admin links)
7. `mvn verify` passes (all tests green)
8. No dangling `/chat` redirects or broken links