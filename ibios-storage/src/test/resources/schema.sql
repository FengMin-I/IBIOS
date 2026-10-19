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

CREATE TABLE IF NOT EXISTS notify_channels (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    name        VARCHAR(255) NOT NULL UNIQUE,
    type        VARCHAR(255) NOT NULL,
    url         VARCHAR(1024),
    description VARCHAR(1024),
    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP    NOT NULL
);

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
