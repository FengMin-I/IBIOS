# Tool Contracts: 5 个扩展内置工具

> Interface contracts for the 5 new tools in `specs/006-extra-tools`.

---

## Contract 1: edit_file

**Tool Name**: `edit_file`
**Module**: `ibios-tool` (extends `FileTools`)
**Description**: 把文件里一段唯一出现的旧文本换成新文本

### Input Schema

```json
{
  "type": "object",
  "properties": {
    "file_path": {
      "type": "string",
      "description": "Path to the file to edit"
    },
    "old_text": {
      "type": "string",
      "description": "Text to replace (must appear exactly once)"
    },
    "new_text": {
      "type": "string",
      "description": "Replacement text"
    }
  },
  "required": ["file_path", "old_text", "new_text"]
}
```

### Output

**Success**: Empty string (file modified in place)
**Failure**: Error message (file unchanged)

### Error Cases

| Condition | Error Message | File State |
|-----------|--------------|------------|
| File not found | "File not found: {file_path}" | N/A |
| `old_text` not found | "Text not found in file" | Unchanged |
| `old_text` appears N>1 times | "Text appears {N} times at lines [X, Y, Z]. Provide more context to uniquely identify the edit location." | Unchanged |
| Sandbox violation | "Sandbox violation: path not in allowed list" | Unchanged |

### Sandbox Enforcement

```java
sandbox.enforce(new SandboxAction(ActionType.FILE_WRITE, file_path));
```

**First line of execute method.**

### Example

**Input**:
```json
{
  "file_path": "/path/to/config.yaml",
  "old_text": "timeout: 30",
  "new_text": "timeout: 60"
}
```

**Output** (success): `""`
**Output** (failure): `"Text appears 2 times at lines [15, 42]. Provide more context to uniquely identify the edit location."`

---

## Contract 2: grep

**Tool Name**: `grep`
**Module**: `ibios-tool` (extends `FileTools`)
**Description**: 在路径下按正则搜索内容，返回"文件:行号:内容"

### Input Schema

```json
{
  "type": "object",
  "properties": {
    "pattern": {
      "type": "string",
      "description": "Regex pattern to search for"
    },
    "path": {
      "type": "string",
      "description": "File or directory to search in"
    },
    "case_sensitive": {
      "type": "boolean",
      "default": true,
      "description": "Case-sensitive search"
    }
  },
  "required": ["pattern", "path"]
}
```

### Output

**Success**: One match per line in format `file:line_number:content`
**Truncation**: If >200 results, append `"\n... (N more results truncated)"`

### Error Cases

| Condition | Error Message |
|-----------|--------------|
| Path not found | "Path not found: {path}" |
| Invalid regex | "Invalid regex pattern: {error}" |
| Sandbox violation | "Sandbox violation: path not in allowed list" |

### Sandbox Enforcement

```java
sandbox.enforce(new SandboxAction(ActionType.FILE_READ, path));
```

**First line of execute method.**

### Example

**Input**:
```json
{
  "pattern": "timeout:\\s*\\d+",
  "path": "/path/to/config",
  "case_sensitive": false
}
```

**Output** (success):
```
/path/to/config/app.yaml:15:timeout: 30
/path/to/config/db.yaml:42:timeout: 60
```

**Output** (>200 results):
```
/path/to/file1.yaml:1:timeout: 30
...
/path/to/file200.yaml:99:timeout: 60
... (150 more results truncated)
```

---

## Contract 3: glob

**Tool Name**: `glob`
**Module**: `ibios-tool` (extends `FileTools`)
**Description**: 按通配符查找文件路径

### Input Schema

```json
{
  "type": "object",
  "properties": {
    "pattern": {
      "type": "string",
      "description": "Glob pattern (e.g., **/*.yaml)"
    },
    "path": {
      "type": "string",
      "description": "Root directory to search from"
    }
  },
  "required": ["pattern", "path"]
}
```

### Output

**Success**: One file path per line
**Truncation**: If >200 results, append `"\n... (N more results truncated)"`

### Error Cases

| Condition | Error Message |
|-----------|--------------|
| Path not found | "Path not found: {path}" |
| Invalid pattern | "Invalid glob pattern: {error}" |
| Sandbox violation | "Sandbox violation: path not in allowed list" |

### Sandbox Enforcement

```java
sandbox.enforce(new SandboxAction(ActionType.FILE_READ, path));
```

**First line of execute method.**

### Example

**Input**:
```json
{
  "pattern": "**/*.yaml",
  "path": "/path/to/project"
}
```

**Output** (success):
```
/path/to/project/config.yaml
/path/to/project/src/main/resources/application.yaml
```

---

## Contract 4: ask_user

**Tool Name**: `ask_user`
**Module**: `ibios-tool` (new `InteractionTools`)
**Description**: 向用户提问并获取回答

### Input Schema

```json
{
  "type": "object",
  "properties": {
    "question": {
      "type": "string",
      "description": "Question to ask the user"
    }
  },
  "required": ["question"]
}
```

### Output

**Success**: User's text response (single line)
**Failure**: Error message if interaction not supported

### Error Cases

| Condition | Error Message |
|-----------|--------------|
| `UserInteraction` is `UnsupportedUserInteraction` | "User interaction not supported in this channel" |

### Sandbox Enforcement

**None.** No external IO (stdin/stdout only).

### Example

**Input**:
```json
{
  "question": "Are you sure you want to delete this file? (yes/no)"
}
```

**Output** (success): `"yes"`
**Output** (failure): `"User interaction not supported in this channel"`

---

## Contract 5: web_search

**Tool Name**: `web_search`
**Module**: `ibios-tool` (new `WebSearchTools`)
**Description**: 用搜索引擎检索网页

### Input Schema

```json
{
  "type": "object",
  "properties": {
    "query": {
      "type": "string",
      "description": "Search query"
    }
  },
  "required": ["query"]
}
```

### Output

**Success**: Rendered text with title, URL, snippet for each result (max 10)
**Format**:
```
1. {title}
   URL: {url}
   {snippet}

2. {title}
   URL: {url}
   {snippet}
```

### Error Cases

| Condition | Error Message |
|-----------|--------------|
| No results | "No results found for query: {query}" |
| HTTP error | "Search request failed: {error}" |
| Sandbox violation | "Sandbox violation: domain not in allowed list" |

### Sandbox Enforcement

```java
sandbox.enforce(new SandboxAction(ActionType.HTTP_REQUEST, "duckduckgo.com"));
```

**First line of execute method.**

### Example

**Input**:
```json
{
  "query": "Spring Boot 3.x virtual threads"
}
```

**Output** (success):
```
1. Virtual Threads in Spring Boot 3
   URL: https://example.com/spring-boot-virtual-threads
   Spring Boot 3 introduces support for Java 21 virtual threads...

2. Getting Started with Virtual Threads
   URL: https://example.com/virtual-threads-guide
   A comprehensive guide to using virtual threads in Java 21...
```

---

## Common Constraints

All 5 tools:
- Implement `IbiosTool` interface
- Registered in `ToolRegistry`
- Subject to `tool_invocations` audit (success/failure)
- Subject to Profile `tools` field filtering
- Test method names in English (P3C requirement)
- `@DisplayName` preserves Chinese descriptions from courseware
