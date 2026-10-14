<!--
Sync Impact Report
==================
Version change: N/A → 1.0.0 (initial ratification)
Modified principles: N/A (initial version)
Added sections: All (initial version)
Removed sections: None
Templates requiring updates:
  - .specify/templates/spec-template.md — ✅ updated (aligned with principles)
  - .specify/templates/plan-template.md — ✅ updated (Constitution Check section added)
  - .specify/templates/tasks-template.md — ✅ updated (task types aligned)
Follow-up TODOs: None
-->

# IBIOS Project Constitution

> This constitution defines the non-negotiable principles for the IBIOS project.
> All specs, plans, tasks, and implementations MUST comply with these principles.
> Any deviation requires a constitution amendment through the governance process below.

- **Version**: 1.0.0
- **Ratified**: 2026-10-13
- **Last Amended**: 2026-10-13

---

## Principle 1: JDK 21 + Spring Boot 3.x Monolith

IBIOS is a single Spring Boot 3.x application running on JDK 21, packaged as a
Maven multi-module project (9 modules), deployable as a single executable JAR.

- MUST use JDK 21 as the minimum and target Java version.
- MUST use Spring Boot 3.x as the application framework.
- MUST maintain Maven multi-module structure (ibios-core, ibios-provider,
  ibios-memory, ibios-tool, ibios-web, ibios-storage, ibios-boot, ibios-cli,
  ibios-channel-cli).
- MUST produce a single executable JAR from ibios-boot.
- MUST NOT introduce a second runtime (e.g., Node.js, Python sidecar) in core
  stage.

**Rationale**: Single-binary deployment matches the "install and run" positioning.
Java is the de facto enterprise backend stack; Spring Boot ecosystem reuse
minimizes adoption friction.

---

## Principle 2: Five Core Capabilities First

Core stage delivers the Agent OS runtime kernel aligned with open-source Agent OS
foundations. Enterprise governance (multi-tenant, SSO, full audit, Tool Policy
enforcement) belongs to the extension stage.

- MUST prioritize: Provider (LLM), ReAct Loop, Memory, Plugin Tool, Web Service.
- MUST NOT implement governance-layer features (multi-tenant, SSO, Tool Policy
  enforcement) in core stage.
- MUST architect for extension-point insertion so governance can be added later
  without refactoring core modules.

**Rationale**: 4-week delivery rhythm (12 hours total) demands strict scope
control. Kernel first, governance later.

---

## Principle 3: Self-Implemented ReAct Loop

IBIOS implements its own ReAct loop (`ReActLoop` + `ToolExecutor` +
`PromptBuilder`). Does NOT use Spring AI's Agent abstraction or its built-in
tool execution engine.

- MUST implement `ReActLoop` in ibios-core.
- MUST implement `ToolExecutor` in ibios-core for all tool dispatch.
- MUST NOT delegate loop control to Spring AI's `ChatClient` tool-calling
  pipeline.
- MUST NOT use Spring AI's `Agent` or `AgentExecutor` abstractions.

**Rationale**: Full control over the Agent's core decision cycle. Preserves
room for custom loop behaviors (retry, rollback, sandbox enforcement) that
Spring AI's built-in loop cannot accommodate.

---

## Principle 4: Spring AI Used for Protocol Only — Auto Tool Execution DISABLED

This is the most error-prone principle. Spring AI MUST be used ONLY for:

1. Provider abstraction and protocol conversion (OpenAI/Anthropic/Gemini formats)
2. `@Tool` annotation JSON Schema generation

Spring AI's automatic tool execution mechanism MUST be disabled in every
`ChatModel.call()`. Tool dispatch is exclusively controlled by IBIOS's own
`ReActLoop` + `ToolExecutor`.

- MUST pass `autoExecuteTools=false` (or equivalent) in every LLM call.
- MUST NOT allow Spring AI to auto-execute any tool.
- MUST verify this constraint with a regression test
  (`autoExecuteTools` assertion in `ProviderServiceTest`).
- MUST NOT import or use Spring AI's tool execution pipeline classes.

**Rationale**: If Spring AI auto-execution is left enabled, tools get called
twice (once by Spring AI, once by IBIOS ToolExecutor), and execution bypasses
sandbox checks. This is the single most likely source of critical bugs.

---

## Principle 5: Plugin Tool Three-Tier Access

Tools are accessible via three integration tiers, from zero-code to heavy-code:

| Tier | Method | Target Users |
|------|--------|-------------|
| Zero-code | SKILL.md + MCP server reuse | Business users describe intent |
| Light-code | MCP server in any language | Enterprise system integration |
| Heavy-code | `@Tool` annotated Java Spring Bean | Deep integration, best perf |

- MUST provide `IbiosTool` abstraction layer unifying built-in and MCP tools.
- MUST NOT force ReAct loop to distinguish tool origin.
- MUST support MCP Java SDK for external MCP server integration.

**Rationale**: Lowering the barrier to tool creation is IBIOS's differentiator
for enterprise adoption.

---

## Principle 6: Synchronous Execution + Virtual Threads

Core stage uses synchronous blocking execution model with JDK 21 virtual
threads. No reactive programming (Reactor, CompletableFuture, custom thread
pools).

- MUST use `spring.threads.virtual.enabled=true`.
- MUST NOT use Reactor (`Mono`/`Flux`) in core stage.
- MUST NOT use `CompletableFuture` for agent execution flow.
- MUST NOT create custom thread pools.
- MUST use Spring MVC (not WebFlux) for HTTP endpoints.

**Rationale**: Synchronous code is直观 and debuggable. Virtual threads provide
high concurrency without reactive complexity. Extension stage adds SSE via
`SseEmitter` when streaming is needed.

---

## Principle 7: Sandbox Interface-First

Sandbox MUST be defined as an abstract interface (`Sandbox`) before any
implementation. Core stage implements only `WhitelistSandbox` (application-level
path/pattern whitelisting). No `SecurityManager` (deprecated since JDK 17,
unavailable in JDK 21).

- MUST define `Sandbox` interface in ibios-core.
- MUST enforce `Sandbox.enforce()` as the first line in any external IO path.
- MUST NOT use Java `SecurityManager`.
- MUST NOT embed container/VM-specific concepts in the Sandbox interface.
- Extension stage: container isolation → microVM, adding implementation classes
  without changing the interface.

**Rationale**: Interface-first design prevents the core-stage implementation
choice from locking the architecture. WhitelistSandbox is sufficient for
trusted-code scenarios; heavier isolation is added by signal.

---

## Principle 8: SQLite + Spring Data JPA, Audit Day-One

Persistence uses SQLite + Spring Data JPA. Audit tables (`tool_invocations`,
`llm_calls`) MUST be written from day one. Schema managed by Flyway
(`db/migration/{vendor}/` dual-track directories). SQLite is the default
zero-config profile; PostgreSQL becomes a deployment option via URL detection.

- MUST use SQLite as default database (zero-config).
- MUST create audit tables (`tool_invocations`, `llm_calls`) at project init.
- MUST record both success AND failure for LLM calls and tool invocations.
- MUST use Flyway for schema migrations (no `hibernate.ddl-auto=update` in
  production).
- MUST NOT store plaintext API keys in code or config files.
- Credentials MUST come from environment variables.

**Rationale**: Audit-on-day-one prevents costly reverse-engineering from logs
later. SQLite keeps the single-binary promise; Flyway ensures reproducible
schema evolution.

---

## Principle 9: Explicit Provider-to-ChatModel Mapping

Multiple LLM providers coexist as `ChatModel` beans in the Spring container.
Since bean types are identical, IBIOS MUST maintain an explicit
`Map<String, ChatModel>` mapping by provider name. MUST NOT rely on type-based
scanning to distinguish providers.

- MUST build the mapping at startup from `application.yaml` provider list.
- MUST throw `ProviderNotFoundException` when a Profile references an unknown
  provider name.
- MUST validate provider name against the global registry at Profile load time.

**Rationale**: Type scanning is unreliable when multiple beans share the same
type. Explicit mapping eliminates ambiguity in multi-provider scenarios.

---

## Principle 10: No Reactive, No Auto-Exec, No Plaintext Keys (Global Invariants)

These six invariants MUST hold across the entire codebase at all times:

1. **Sandbox first**: Every external IO path starts with `Sandbox.enforce()`.
   (If Sandbox is not yet implemented for the current lesson, leave the call
   site with a comment noting "wire in lesson 24".)
2. **Audit completeness**: LLM call success/failure → `llm_calls` table;
   tool invocation success/failure → `tool_invocations` table.
3. **No plaintext secrets**: `grep -r "sk-"` (or key prefix) finds nothing in
   code or config.
4. **Session ID containment**: `session_id` is assembled only inside
   `SessionManager`, never constructed elsewhere.
5. **No reactive**: No `Mono`/`Flux`/`CompletableFuture`/custom thread pools.
6. **No Spring AI auto tool execution**: No import of Spring AI tool execution
   pipeline classes.

**Rationale**: These are the most common failure modes. Encoding them as
grep-verifiable invariants makes them machine-checkable in CI.

---

## Governance

### Amendment Procedure

1. Propose change via PR modifying this constitution.
2. State which principle(s) change and the rationale.
3. Version bump follows semantic versioning:
   - MAJOR: principle removal or backward-incompatible redefinition
   - MINOR: new principle added or materially expanded guidance
   - PATCH: clarification, wording, non-semantic refinement
4. All dependent templates (spec, plan, tasks) MUST be checked for consistency.

### Compliance Review

- `mvn clean verify` MUST pass (includes P3C, SpotBugs, FindSecBugs, PMD).
- Global invariants (Principle 10) MUST be verifiable via grep commands.
- Each principle SHOULD have at least one regression test anchoring it.

### Dependencies Between Principles

- Principle 3 (ReAct) depends on Principle 4 (Spring AI boundary).
- Principle 6 (sync model) reinforces Principle 10.5 (no reactive).
- Principle 7 (Sandbox) reinforces Principle 10.1 (sandbox first).
- Principle 8 (audit) reinforces Principle 10.2 (audit completeness).
