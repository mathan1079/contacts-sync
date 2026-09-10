CREATE TABLE IF NOT EXISTS devices (
    device_id TEXT PRIMARY KEY,
    first_seen_at INTEGER NOT NULL,
    last_seen_at INTEGER NOT NULL,
    last_client_sync_timestamp INTEGER,
    total_syncs INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS contacts (
    device_id TEXT NOT NULL,
    contact_id INTEGER NOT NULL,
    lookup_key TEXT,
    display_name TEXT,
    phones_json TEXT NOT NULL DEFAULT '[]',
    emails_json TEXT NOT NULL DEFAULT '[]',
    company TEXT,
    designation TEXT,
    contact_last_updated_timestamp INTEGER,
    server_created_at INTEGER NOT NULL,
    server_updated_at INTEGER NOT NULL,

    PRIMARY KEY (
        device_id,
        contact_id
    ),

    FOREIGN KEY (device_id)
        REFERENCES devices(device_id)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS sync_logs (
    id TEXT PRIMARY KEY,
    device_id TEXT NOT NULL,
    client_sync_timestamp INTEGER,
    server_sync_timestamp INTEGER NOT NULL,
    upsert_count INTEGER NOT NULL DEFAULT 0,
    delete_count INTEGER NOT NULL DEFAULT 0,
    status TEXT NOT NULL,

    FOREIGN KEY (device_id)
        REFERENCES devices(device_id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_contacts_device
ON contacts(device_id);

CREATE INDEX IF NOT EXISTS idx_contacts_name
ON contacts(display_name);

CREATE INDEX IF NOT EXISTS idx_sync_logs_device
ON sync_logs(device_id);