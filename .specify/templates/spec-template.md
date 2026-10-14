# Feature Specification: [FEATURE_NAME]

> Created by `/speckit-specify`. Fill all sections before proceeding to clarify.

## Metadata

- **Feature**: [FEATURE_NAME]
- **Lesson**: [LESSON_NUMBER]
- **Author**: [AUTHOR]
- **Date**: [DATE]
- **Status**: draft | clarified | approved

---

## 1. Background & Value

[Why this feature is needed. One paragraph max.]

## 2. User Scenarios

[2-3 concrete scenarios describing how a user/system interacts with this feature.]

### Scenario 1: [Title]

[Description]

### Scenario 2: [Title]

[Description]

## 3. Functional Requirements

Each requirement MUST be testable.

- **FR1**: [Requirement]
- **FR2**: [Requirement]
- ...

## 4. Out of Scope (Boundary)

Explicitly list what this feature does NOT do.

- [Item 1]
- [Item 2]

## 5. Acceptance Criteria

### Automated (Harness)

[Tests that `mvn test` / `mvn clean verify` will run. List test classes and what they verify.]

### Manual

[Items requiring human verification — real model calls, real webhooks, etc.]

## 6. Dependencies & Assumptions

### Dependencies on Prior Lessons

- [Lesson N]: [What it provides]

### External Dependencies

- [BOM versions, MCP servers, API keys, etc.]

---

## Constitution Check

Verify this spec complies with `.specify/memory/constitution.md`:

- [ ] Principle 1: JDK 21 + Spring Boot 3.x monolith
- [ ] Principle 2: Five core capabilities priority
- [ ] Principle 3: Self-implemented ReAct loop
- [ ] Principle 4: Spring AI protocol only, auto-exec DISABLED
- [ ] Principle 5: Plugin tool three-tier access
- [ ] Principle 6: Synchronous + virtual threads
- [ ] Principle 7: Sandbox interface-first
- [ ] Principle 8: SQLite + audit day-one
- [ ] Principle 9: Explicit provider mapping
- [ ] Principle 10: Global invariants
