# Quickstart: 5 个扩展内置工具验证指南

> Validation scenarios for `specs/006-extra-tools`.

---

## Prerequisites

- JDK 21 installed
- Maven 3.8+
- IBIOS project built: `mvn clean install -DskipTests`
- A test Agent profile with tools enabled

---

## Scenario 1: edit_file 唯一匹配

**Goal**: Verify edit_file replaces unique text and rejects ambiguous edits.

### Setup

Create a test file:
```bash
mkdir -p /tmp/ibios-test
cat > /tmp/ibios-test/config.yaml << 'EOF'
server:
  port: 8080
  timeout: 30

database:
  host: localhost
  timeout: 60
EOF
```

### Test 1a: Unique match (success)

**Input**:
```json
{
  "file_path": "/tmp/ibios-test/config.yaml",
  "old_text": "port: 8080",
  "new_text": "port: 9090"
}
```

**Expected**: File modified, `port: 9090` appears once.

**Verify**:
```bash
grep "port:" /tmp/ibios-test/config.yaml
# Expected: port: 9090
```

### Test 1b: Zero matches (failure)

**Input**:
```json
{
  "file_path": "/tmp/ibios-test/config.yaml",
  "old_text": "port: 7070",
  "new_text": "port: 9090"
}
```

**Expected**: Error "Text not found in file". File unchanged.

**Verify**:
```bash
grep "port:" /tmp/ibios-test/config.yaml
# Expected: port: 9090 (unchanged)
```

### Test 1c: Multiple matches (failure)

**Input**:
```json
{
  "file_path": "/tmp/ibios-test/config.yaml",
  "old_text": "timeout: 30",
  "new_text": "timeout: 45"
}
```

**Expected**: Error "Text appears 2 times at lines [3, 7]". File unchanged.

**Verify**:
```bash
grep "timeout:" /tmp/ibios-test/config.yaml
# Expected: timeout: 30 (unchanged, appears twice)
```

---

## Scenario 2: grep 内容搜索

**Goal**: Verify grep searches files and respects 200-result limit.

### Setup

Create test files:
```bash
mkdir -p /tmp/ibios-test/logs
for i in {1..250}; do
  echo "ERROR: something went wrong at line $i" > /tmp/ibios-test/logs/app-$i.log
done
```

### Test 2a: Normal search

**Input**:
```json
{
  "pattern": "ERROR.*line 100",
  "path": "/tmp/ibios-test/logs"
}
```

**Expected**: One result: `/tmp/ibios-test/logs/app-100.log:1:ERROR: something went wrong at line 100`

### Test 2b: >200 results (truncation)

**Input**:
```json
{
  "pattern": "ERROR",
  "path": "/tmp/ibios-test/logs"
}
```

**Expected**: 200 results + `"\n... (50 more results truncated)"`

**Verify**:
```bash
# Count lines in output
wc -l
# Expected: 201 (200 results + 1 truncation note)
```

---

## Scenario 3: glob 文件查找

**Goal**: Verify glob finds files by pattern.

### Test 3a: Find all YAML files

**Input**:
```json
{
  "pattern": "**/*.yaml",
  "path": "/tmp/ibios-test"
}
```

**Expected**: List of all `.yaml` files under `/tmp/ibios-test`.

**Verify**:
```bash
# Compare with find command
find /tmp/ibios-test -name "*.yaml" | wc -l
# Should match glob output line count
```

---

## Scenario 4: ask_user 人机交互（CLI 渠道）

**Goal**: Verify ask_user reads from stdin in CLI channel.

### Setup

Run IBIOS in CLI mode:
```bash
java -jar ibios-boot/target/ibios-boot.jar chat
```

### Test

**Agent prompt**: "I need to delete a file. Can you confirm?"
**Agent calls**: `ask_user` with `{"question": "Delete /tmp/ibios-test/config.yaml? (yes/no)"}`

**Expected**: 
- Terminal displays question
- User types "yes" and presses Enter
- Agent receives "yes" as response

**Verify**: Manual observation of terminal interaction.

---

## Scenario 5: web_search 网页搜索

**Goal**: Verify web_search queries DuckDuckGo and returns results.

### Test

**Input**:
```json
{
  "query": "Spring Boot 3 virtual threads"
}
```

**Expected**: Rendered text with 1-10 results, each containing title, URL, snippet.

**Verify**:
```bash
# Check output format
grep -E "^[0-9]+\. " output.txt
# Expected: numbered list of results
```

**Note**: Requires internet access. DuckDuckGo Instant Answer API may not return results for all queries.

---

## Automated Validation

Run all unit tests:
```bash
mvn test -pl ibios-tool
```

**Expected**: All tests pass, including:
- `FileToolsTest` (edit_file/grep/glob)
- `InteractionToolsTest` (ask_user)
- `WebSearchToolsTest` (web_search)
- `IbiosToolContractTest` (all 5 tools)

Run full build with quality gates:
```bash
mvn clean verify
```

**Expected**: BUILD SUCCESS, no P3C/SpotBugs/PMD violations.

---

## H4 Global Invariants Verification

### 1. Sandbox first

```bash
grep -n "sandbox.enforce" ibios-tool/src/main/java/com/ibios/tool/builtin/FileTools.java
# Expected: edit_file, grep, glob all call sandbox.enforce as first line
```

### 2. Audit completeness

```bash
# Run a test that invokes each tool, then check tool_invocations table
sqlite3 ibios-storage/ibios_test.db "SELECT tool_name, success FROM tool_invocations ORDER BY id DESC LIMIT 5;"
# Expected: 5 rows, one for each tool invocation
```

### 3. No plaintext secrets

```bash
grep -r "sk-" ibios-tool/src/main/java/
# Expected: no output
```

### 4. No reactive

```bash
grep -r "Mono\|Flux\|CompletableFuture" ibios-tool/src/main/java/com/ibios/tool/builtin/
# Expected: no output
```

### 5. No Spring AI auto-execution

```bash
grep -r "autoExecuteTools" ibios-tool/src/main/java/
# Expected: no output (only ibios-provider uses this, set to false)
```

---

## Cleanup

```bash
rm -rf /tmp/ibios-test
```

---

## Troubleshooting

| Issue | Cause | Solution |
|-------|-------|----------|
| edit_file "Text appears N times" | old_text not unique | Provide more context in old_text |
| grep/glob truncated | >200 results | Refine pattern or narrow path |
| ask_user "not supported" | Wrong channel | Use CLI channel, not web/scheduled |
| web_search "No results" | DuckDuckGo API limitation | Try different query; check internet |
| Sandbox violation | Path/domain not in whitelist | Update `application.yaml` allowed lists |
