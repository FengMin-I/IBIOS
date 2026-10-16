CREATE TABLE IF NOT EXISTS sessions (
    session_id      TEXT PRIMARY KEY,
    profile_name    TEXT    NOT NULL,
    channel         TEXT    NOT NULL,
    user_id         TEXT    NOT NULL,
    messages_json   TEXT,
    status          TEXT    NOT NULL DEFAULT 'active',
    created_at      TEXT    NOT NULL,
    last_active_at  TEXT    NOT NULL,
    archived_at     TEXT
);

CREATE TABLE IF NOT EXISTS tool_invocations (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    session_id    TEXT    NOT NULL,
    tool_name     TEXT    NOT NULL,
    input_json    TEXT,
    output_json   TEXT,
    success       INTEGER NOT NULL,
    error_message TEXT,
    duration_ms   INTEGER,
    created_at    TEXT    NOT NULL
);

CREATE TABLE IF NOT EXISTS memory_entries (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    scope       VARCHAR(16) NOT NULL,
    content     TEXT        NOT NULL,
    created_at  TEXT        NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_memory_scope ON memory_entries (scope);

CREATE TABLE IF NOT EXISTS llm_calls (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    session_id        TEXT,
    provider          TEXT    NOT NULL,
    model             TEXT    NOT NULL,
    prompt_tokens     INTEGER,
    completion_tokens INTEGER,
    total_tokens      INTEGER,
    latency_ms        INTEGER,
    success           INTEGER NOT NULL DEFAULT 1,
    error_message     TEXT,
    called_at         TEXT    NOT NULL
);

CREATE TABLE IF NOT EXISTS scheduled_tasks (
    task_id       TEXT    PRIMARY KEY,
    profile_name  TEXT    NOT NULL,
    cron          TEXT    NOT NULL,
    zone          TEXT    NOT NULL,
    message       TEXT    NOT NULL,
    enabled       INTEGER NOT NULL DEFAULT 1,
    next_run_at   TEXT,
    last_run_at   TEXT,
    last_status   TEXT,
    run_count     INTEGER NOT NULL DEFAULT 0,
    created_at    TEXT    NOT NULL
);

CREATE TABLE IF NOT EXISTS task_executions (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    task_id       TEXT    NOT NULL,
    session_id    TEXT,
    started_at    TEXT    NOT NULL,
    success       INTEGER NOT NULL,
    error_message TEXT,
    duration_ms   INTEGER,
    FOREIGN KEY (task_id) REFERENCES scheduled_tasks(task_id)
);

CREATE INDEX IF NOT EXISTS idx_task_executions_task_id ON task_executions (task_id);
