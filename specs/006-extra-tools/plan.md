# Implementation Plan: 5 个扩展内置工具

> Created by `/speckit-plan`. Fill all sections before proceeding to tasks.

## Metadata

- **Feature**: 5 个扩展内置工具——对齐业界主流 Agent 产品的基础工具面
- **Spec**: `specs/006-extra-tools/spec.md`
- **Date**: 2026-10-14
- **Status**: draft

---

## 1. Tech Stack

JDK 21 + Spring Boot 3.x + Spring AI Alibaba (verify target dependency exists in locked BOM via `mvn dependency:tree` before coding) + SQLite + Spring Data JPA. Credentials via environment variable placeholders, no plaintext. SQLite uses manual schema scripts, not `hibernate.ddl-auto=update`.

---

## 2. Module Placement

| Class / Package | Target Module |
|----------------|---------------|
| `UserInteraction` interface | ibios-core |
| `SearchProvider` interface + `SearchResult` record | ibios-core |
| `ConsoleUserInteraction` | ibios-tool |
| `UnsupportedUserInteraction` | ibios-tool |
| `DuckDuckGoSearchProvider` | ibios-tool |
| `InteractionTools` (ask_user) | ibios-tool |
| `WebSearchTools` (web_search) | ibios-tool |
| `EditFileTool` / `GrepTool` / `GlobTool` (expand FileTools) | ibios-tool |

**Dependency Direction**: ibios-tool → ibios-core (interfaces in core, implementations in tool).

---

## 3. Design Decisions

### Decision 1: Interface-First for Channel-Dependent Tools

**Description**: `ask_user` and `web_search` depend on runtime context (channel type, search provider). Following Principle 7 (Sandbox interface-first), define interfaces in ibios-core and implementations in ibios-tool.

**Alternatives Considered**:
- Hardcode ConsoleUserInteraction: violates interface-first, breaks scheduled/web channels
- Put interfaces in ibios-tool: breaks dependency direction (core should not depend on tool)

**Rationale**: Matches existing patterns (NotifyChannelAdapter, Sandbox). Allows channel-specific implementations without changing tool interface. Core defines "what", tool defines "how".

### Decision 2: edit_file Unique Match Constraint

**Description**: `edit_file` requires old_text to appear exactly once in the file. Zero matches → error. Multiple matches → error. File unchanged on error.

**Alternatives Considered**:
- Replace all occurrences: dangerous, could corrupt config files
- Replace first occurrence: ambiguous, hard to debug
- Require line number: verbose, LLM must track line numbers

**Rationale**: Matches Claude Code / Cursor edit tool semantics. Forces LLM to provide sufficient context for unique identification. Safer than ambiguous replacements.

### Decision 3: grep/glob Result Limit

**Description**: Both tools cap results at 200 entries. If exceeded, truncate and append note "... (N more results truncated)".

**Alternatives Considered**:
- No limit: could overwhelm LLM context window
- Configurable limit: adds complexity, not needed in core stage
- Stream results: violates sync model (Principle 6)

**Rationale**: 200 is sufficient for most use cases. Prevents context overflow. Truncation note informs LLM to refine search.

### Decision 4: DuckDuckGo Instant Answer API

**Description**: `DuckDuckGoSearchProvider` uses DuckDuckGo's free Instant Answer API (no API key required). Returns title, URL, snippet.

**Alternatives Considered**:
- Google Custom Search: requires API key, violates "no plaintext secrets" principle
- Bing Web Search: requires API key
- Tavily: requires API key
- SerpAPI: requires API key

**Rationale**: DuckDuckGo Instant Answer is free, no key needed, sufficient for basic web search. Matches "credentials via env var" principle (no credentials needed). Can be extended to other providers in extension stage.

---

## 4. Test Strategy

Tests follow the lesson's acceptance harness:

- **FileToolsTest** (扩展): edit_file 唯一匹配成功；旧文本找不到报错；旧文本出现多次报错；grep 正常搜索返回格式正确；grep 超 200 条截断；grep 二进制文件跳过；glob 通配符匹配正确；glob 超 200 条截断
- **InteractionToolsTest**: ConsoleUserInteraction 能读终端（mock stdin）；UnsupportedUserInteraction 抛异常
- **WebSearchToolsTest**: DuckDuckGoSearchProvider 返回结果格式正确；web_search 过 Sandbox HTTP_REQUEST 检查
- **IbiosToolContractTest**: 参数化测试自动覆盖新增的 5 个工具（name/description/inputSchema 非空）

**Key Regression Points**:
- edit_file 唯一匹配约束（找不到/多次报错，文件不动）
- grep/glob 上限截断（200 条）
- ask_user 渠道适配（Console vs Unsupported）
- web_search Sandbox 检查（HTTP_REQUEST）

Unit tests run by default; integration smoke tests tagged `@Tag("integration")` skipped in CI. Implementation is done when `mvn clean verify` is all green.

---

## 5. Syntax Restrictions

Avoid Java 18+ syntax that P3C/ASM cannot parse (e.g., enhanced switch `default ->` form). Static analysis is a build gate.

---

## 6. Constitution Check

- [x] All principles from spec verified
- [x] Module placement follows §10 dependency direction (interfaces in core, impls in tool)
- [x] No reactive imports (sync execution, Principle 6)
- [x] No Spring AI auto-execution (only @Tool schema, Principle 4)
- [x] Audit tables updated if applicable (tool_invocations via ToolExecutor, Principle 8)
- [x] Sandbox enforcement noted for IO paths (all 5 tools first line calls Sandbox.enforce, Principle 7)

---

## 7. Implementation Phases

### Phase 1: Core Interfaces (ibios-core)

1. Define `UserInteraction` interface in `com.ibios.core.tool`
2. Define `SearchProvider` interface + `SearchResult` record in `com.ibios.core.tool`

### Phase 2: File Tools Extension (ibios-tool)

3. Implement `EditFileTool` in `FileTools.java` (unique match constraint)
4. Implement `GrepTool` in `FileTools.java` (regex search, 200 limit, binary skip)
5. Implement `GlobTool` in `FileTools.java` (pattern match, 200 limit)

### Phase 3: Interaction Tools (ibios-tool)

6. Implement `ConsoleUserInteraction` (read stdin)
7. Implement `UnsupportedUserInteraction` (throw exception)
8. Implement `InteractionTools` with `AskUserTool`

### Phase 4: Web Search Tools (ibios-tool)

9. Implement `DuckDuckGoSearchProvider` (HTTP GET to Instant Answer API)
10. Implement `WebSearchTools` with `WebSearchTool`

### Phase 5: Tests

11. Extend `FileToolsTest` with edit_file/grep/glob tests
12. Create `InteractionToolsTest`
13. Create `WebSearchToolsTest`
14. Verify `IbiosToolContractTest` covers all 5 new tools

### Phase 6: Validation

15. Run `mvn clean verify` — all green
16. Verify H4 global invariants (Sandbox first, audit, no plaintext, no reactive, no auto-exec)

---

## 8. Risk Assessment

| Risk | Mitigation |
|------|-----------|
| DuckDuckGo API rate limit / unavailable | Mock in tests; document as limitation; extension stage adds fallback providers |
| edit_file unique match too strict | Matches Claude Code / Cursor; document constraint; LLM learns to provide context |
| grep/glob 200 limit too low for large repos | Document limit; extension stage adds pagination or streaming |
| ConsoleUserInteraction blocks virtual thread | Acceptable for CLI; UnsupportedUserInteraction for non-interactive channels |

---

## 9. Out of Scope (Deferred to Extension Stage)

- Tool Policy (allow/deny rules per Profile)
- Tool lazy loading / on-demand registration
- IBIOS as MCP server
- Full container sandbox (Lesson 24)
- Parallel tool execution
- Additional search providers (Google, Bing, Tavily)
- Rich interaction (multi-choice, file upload) beyond ask_user text prompt
