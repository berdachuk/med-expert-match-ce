# Web Module

Thymeleaf server-side rendering for the web UI. The presentation layer for all use cases.

## Purpose

- Thymeleaf controllers for all UI pages (Home, Doctor, Queue, Case Analysis, Match, Routing, Analytics, etc.)
- `LogStreamController` — SSE-based log streaming to browser
- `SyntheticDataWebController` — UI for synthetic data generation
- `GraphVisualizationWebController` — graph visualization in browser

## Module Dependencies

`@ApplicationModule(allowedDependencies = {"core", "llm", "medicalcase", "doctor", "clinicalexperience", "graph", "ingestion", "retrieval"})`

## Conventions

- Use `@Controller` (not `@RestController`) — return template name strings
- Use `Model` parameter to pass data to templates
- Templates in `src/main/resources/templates/`, fragments in `templates/fragments/`
- Static resources (CSS/JS/images) in `src/main/resources/static/`
- Thymeleaf templates auto-reload via Spring Boot DevTools in `local` profile

## Constraints

- Never expose raw API responses in UI — always transform to view-friendly models
- Never log or display PHI in web pages
- Keep controller logic thin — delegate to service modules for business logic
- Do NOT add domain logic in controllers

## Related Skills

- `code-style` — naming and formatting conventions
- `testing` — controller integration test patterns
