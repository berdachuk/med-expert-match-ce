# Code Style

## Description
Project-wide Java, SQL, and template coding conventions. Covers naming, formatting, imports, error handling, logging, Lombok usage, and documentation rules.

## When to use
- Writing or modifying any Java code, SQL query files, or Thymeleaf templates
- Before committing — verify code matches conventions
- Code review preparation
- Answering: "How should I format/name this?"

## Instructions

### Java Formatting

- 4 spaces indentation, no tabs
- Max 120 characters per line
- Opening brace on same line (K&R style)
- Use imports, never fully-qualified class names
- Static imports only for well-known constants/utilities

### Naming

| Element | Convention | Example |
|---------|-----------|---------|
| Classes | PascalCase | `DoctorService` |
| Methods | camelCase | `findById()` |
| Variables | camelCase | `doctorId` |
| Constants | UPPER_SNAKE_CASE | `MAX_RETRY_COUNT` |
| Packages | lowercase.dots | `com.berdachuk.medexpertmatch.doctor` |
| DB columns | snake_case | `doctor_id` |
| JSON fields | camelCase | `doctorId` |

### Interface-Based Design (mandatory)

- Every service and repository must have an interface + separate implementation class
- Interface: `{Name}.java` in root of `service/` or `repository/`
- Implementation: `{Name}Impl.java` in `service/impl/` or `repository/impl/`
- Inject interfaces, never concrete implementations
- RowMappers: `{Entity}Mapper.java` in `repository/impl/`

### Error Handling

- Never implement silent fallback mechanisms
- Fail fast — throw exceptions, do not suppress errors
- Use module-specific unchecked exceptions extending `MedExpertMatchException`
- Never expose PHI or patient data in error messages
- Never catch generic `Exception`

### Logging

- Use `@Slf4j` (Lombok) for all logging
- Never log patient identifiers or PHI
- Log levels: ERROR for failures, WARN for degraded operations, INFO for milestones, DEBUG for diagnostics

### Lombok Rules

- **Use on**: service implementations, repository implementations, utility classes
- **Annotations**: `@Slf4j`, `@RequiredArgsConstructor`, `@Getter`, `@Setter`, `@Builder`
- **Do NOT use on**: domain entities (they are Java records)

### Comments

- Max 3 lines per comment/JavaDoc
- Interface methods: JavaDoc required (params, returns, throws)
- Implementation methods: no duplicate JavaDoc if interface has it
- All code, comments, docs in English
- No emojis in any project files

### Transaction Management

- `@Transactional` on service methods, never on controllers or repositories
- Use `readOnly = true` for read-only operations
- Separate insert/update repository methods — never `createOrUpdate`

## Boundaries
- Do NOT reformat entire files without human approval
- Do NOT change comment style conventions
- Do NOT disable or suppress error handling rules
