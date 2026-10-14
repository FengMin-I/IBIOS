# Data Model: 5 个扩展内置工具

> Phase 1 data model for `specs/006-extra-tools`.

---

## Summary

**No new database entities or tables.** This feature adds 5 new tools that operate on existing data structures (files, HTTP, stdin/stdout). All tool invocations are recorded in the existing `tool_invocations` audit table (Lesson 17).

---

## New Interfaces & Records

### 1. UserInteraction (ibios-core)

```java
public interface UserInteraction {
    String ask(String question);
}
```

**Purpose**: Abstraction for human-in-the-loop interaction.
**Implementations**:
- `ConsoleUserInteraction` (ibios-tool): reads from stdin
- `UnsupportedUserInteraction` (ibios-tool): throws exception

**Validation**: `question` must be non-null, non-empty.

---

### 2. SearchProvider (ibios-core)

```java
public interface SearchProvider {
    record SearchResult(String title, String url, String snippet) {}
    List<SearchResult> search(String query);
}
```

**Purpose**: Abstraction for web search providers.
**Implementations**:
- `DuckDuckGoSearchProvider` (ibios-tool): uses DuckDuckGo Instant Answer API

**Validation**: `query` must be non-null, non-empty. Returns empty list if no results (not an error).

---

### 3. Tool Input Schemas

Each tool has a JSON Schema for its input parameters:

#### edit_file

```json
{
  "type": "object",
  "properties": {
    "file_path": {"type": "string", "description": "Path to the file to edit"},
    "old_text": {"type": "string", "description": "Text to replace (must appear exactly once)"},
    "new_text": {"type": "string", "description": "Replacement text"}
  },
  "required": ["file_path", "old_text", "new_text"]
}
```

**Constraints**:
- `old_text` must appear exactly once in the file
- File unchanged on error (zero matches or multiple matches)

---

#### grep

```json
{
  "type": "object",
  "properties": {
    "pattern": {"type": "string", "description": "Regex pattern to search for"},
    "path": {"type": "string", "description": "File or directory to search in"},
    "case_sensitive": {"type": "boolean", "default": true, "description": "Case-sensitive search"}
  },
  "required": ["pattern", "path"]
}
```

**Output format**: `file:line_number:content` (one per line)
**Constraints**:
- Max 200 results; truncate with "... (N more results truncated)"
- Skip binary / non-UTF-8 files (log warning, continue)

---

#### glob

```json
{
  "type": "object",
  "properties": {
    "pattern": {"type": "string", "description": "Glob pattern (e.g., **/*.yaml)"},
    "path": {"type": "string", "description": "Root directory to search from"}
  },
  "required": ["pattern", "path"]
}
```

**Output format**: One file path per line
**Constraints**:
- Max 200 results; truncate with "... (N more results truncated)"

---

#### ask_user

```json
{
  "type": "object",
  "properties": {
    "question": {"type": "string", "description": "Question to ask the user"}
  },
  "required": ["question"]
}
```

**Output**: User's text response (single line)
**Constraints**:
- Throws if `UserInteraction` implementation is `UnsupportedUserInteraction`

---

#### web_search

```json
{
  "type": "object",
  "properties": {
    "query": {"type": "string", "description": "Search query"}
  },
  "required": ["query"]
}
```

**Output format**: Rendered text with title, URL, snippet for each result
**Constraints**:
- Sandbox checks `HTTP_REQUEST` for `duckduckgo.com`
- Max 10 results (DuckDuckGo Instant Answer API limitation)

---

## Audit Trail

All tool invocations (success and failure) are recorded in the existing `tool_invocations` table:

```sql
CREATE TABLE tool_invocations (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    session_id TEXT NOT NULL,
    tool_name TEXT NOT NULL,
    input_json TEXT,
    output TEXT,
    success BOOLEAN NOT NULL,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**No schema changes needed.** The existing table accommodates all 5 new tools.

---

## State Transitions

**None.** Tools are stateless. Each invocation is independent.

---

## Relationships

**None.** Tools do not reference each other or external entities (beyond the file system and HTTP endpoints, which are not modeled as database entities).
