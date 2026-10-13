CREATE TABLE IF NOT EXISTS tool_invocations (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    tool_name   TEXT    NOT NULL,
    input       TEXT,
    output      TEXT,
    status      TEXT    NOT NULL,
    started_at  TEXT    NOT NULL,
    finished_at TEXT
);

CREATE TABLE IF NOT EXISTS llm_calls (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    provider      TEXT    NOT NULL,
    model         TEXT    NOT NULL,
    prompt_tokens INTEGER,
    completion_tokens INTEGER,
    total_tokens  INTEGER,
    latency_ms    INTEGER,
    status        TEXT    NOT NULL,
    called_at     TEXT    NOT NULL
);
