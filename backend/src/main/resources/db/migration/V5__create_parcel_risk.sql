-- Latest weather risk result per parcel, refreshed by the scheduled risk job.
-- One row per parcel: a refresh replaces the parcel's previous result.
-- The per-disease breakdown is stored as JSON; the overall values are columns.
-- Deleting a parcel deletes its stored risk.
CREATE TABLE parcel_risk (
    parcel_id    UUID PRIMARY KEY REFERENCES parcels (id) ON DELETE CASCADE,
    level        VARCHAR(20)  NOT NULL,
    risk_index   INTEGER      NOT NULL,
    reason       TEXT         NOT NULL,
    diseases     JSONB        NOT NULL,
    generated_at TIMESTAMPTZ  NOT NULL
);
