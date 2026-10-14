# Research: 5 个扩展内置工具

> Phase 0 research findings for `specs/006-extra-tools`.

---

## Research Task 1: DuckDuckGo Instant Answer API

**Question**: What is the DuckDuckGo Instant Answer API, and what are its limitations?

**Findings**:
- **Endpoint**: `https://api.duckduckgo.com/?q={query}&format=json`
- **Response format**: JSON with `Abstract`, `AbstractURL`, `RelatedTopics` array
- **Rate limit**: No official rate limit, but aggressive use may trigger blocking
- **Limitations**:
  - Returns instant answers, not full web search results
  - Limited coverage compared to Google/Bing
  - No pagination
  - May not return results for all queries

**Decision**: Use DuckDuckGo Instant Answer API for core stage. Document limitations. Extension stage adds Google/Bing/Tavily as alternative `SearchProvider` implementations.

**Alternatives Considered**:
- Google Custom Search API: requires API key, 100 free queries/day, then $5 per 1000 queries
- Bing Web Search API: requires API key, pricing varies
- Tavily: requires API key, $0.003 per search
- SerpAPI: requires API key, $0.01 per search

**Rationale**: DuckDuckGo is free, no key needed, aligns with "no plaintext secrets" principle. Sufficient for demo and basic use cases.

---

## Research Task 2: edit_file Unique Match Semantics

**Question**: How do Claude Code and Cursor implement edit_file unique match?

**Findings**:
- **Claude Code**: Requires old_text to appear exactly once. Zero matches → error. Multiple matches → error with list of line numbers where text appears.
- **Cursor**: Similar semantics. Requires unique match. Provides diff preview before applying.
- **Rationale**: Prevents accidental bulk replacements. Forces user/LLM to provide sufficient context.

**Decision**: Match Claude Code / Cursor semantics exactly. Error messages should be clear:
- Zero matches: "Text not found in file"
- Multiple matches: "Text appears N times at lines [X, Y, Z]. Provide more context to uniquely identify the edit location."

**Alternatives Considered**:
- Replace all: dangerous, could corrupt files
- Replace first: ambiguous, hard to debug
- Require line number: verbose, LLM must track line numbers

**Rationale**: Industry standard for AI-assisted editing. Safest default.

---

## Research Task 3: grep/glob Result Limit

**Question**: What is a reasonable result limit for grep/glob in Agent tools?

**Findings**:
- **Claude Code**: No explicit limit, but truncates very large results
- **Cursor**: Limits to ~100 results in some cases
- **LLM context window**: Typical context is 4K-128K tokens. 200 results × ~50 tokens each = ~10K tokens, reasonable.

**Decision**: Cap at 200 results. If exceeded, truncate and append "... (N more results truncated)".

**Rationale**:
- 200 is sufficient for most use cases
- Prevents context overflow
- Truncation note informs LLM to refine search (e.g., more specific pattern, narrower path)
- Extension stage can add pagination or streaming

---

## Research Task 4: UserInteraction Interface Design

**Question**: How should UserInteraction handle different channels (CLI vs web vs scheduled)?

**Findings**:
- **CLI channel**: Can read from stdin, write to stdout
- **Web channel**: No persistent connection, cannot block waiting for user input
- **Scheduled channel**: No user present, cannot interact

**Decision**: Interface-first approach:
```java
public interface UserInteraction {
    String ask(String question);  // throws if cannot interact
}
```

Two implementations:
- `ConsoleUserInteraction`: reads from stdin (for CLI)
- `UnsupportedUserInteraction`: throws `UnsupportedOperationException` (for web/scheduled)

**Rationale**: Matches existing patterns (NotifyChannelAdapter, Sandbox). Channel-specific behavior without changing tool interface. Fails fast if interaction not supported.

---

## Research Task 5: Sandbox Enforcement for New Tools

**Question**: What Sandbox ActionType should each new tool use?

**Findings**:
- `edit_file`: writes to file → `FILE_WRITE`
- `grep`: reads files → `FILE_READ`
- `glob`: reads directory/file metadata → `FILE_READ`
- `ask_user`: no IO (just stdin/stdout) → no Sandbox check needed
- `web_search`: HTTP request → `HTTP_REQUEST`

**Decision**: Each tool calls `sandbox.enforce()` as first line:
- `EditFileTool`: `sandbox.enforce(new SandboxAction(ActionType.FILE_WRITE, filePath))`
- `GrepTool`: `sandbox.enforce(new SandboxAction(ActionType.FILE_READ, searchPath))`
- `GlobTool`: `sandbox.enforce(new SandboxAction(ActionType.FILE_READ, searchPath))`
- `AskUserTool`: no Sandbox check (no external IO)
- `WebSearchTool`: `sandbox.enforce(new SandboxAction(ActionType.HTTP_REQUEST, "duckduckgo.com"))`

**Rationale**: Matches Principle 7 (Sandbox interface-first) and Principle 10.1 (Sandbox first on all IO paths).

---

## Summary

All research tasks completed. No unresolved "NEEDS CLARIFICATION" items. Key decisions:
1. DuckDuckGo Instant Answer API (free, no key)
2. edit_file unique match (Claude Code / Cursor semantics)
3. grep/glob 200 result limit (prevents context overflow)
4. UserInteraction interface-first (matches existing patterns)
5. Sandbox enforcement on all IO paths (Principle 7, 10.1)

Ready to proceed to Phase 1 (data-model.md, contracts/, quickstart.md).
