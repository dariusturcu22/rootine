-- Diagnoses: the result of one photo sent to the AI service for one parcel.
-- Only the path to the stored photo is kept, never the image itself.
-- The model's result is stored as JSON; the model version is kept in its own column.
-- Deleting a parcel deletes its diagnoses.
CREATE TABLE diagnoses (
    id            UUID PRIMARY KEY,
    parcel_id     UUID         NOT NULL REFERENCES parcels (id) ON DELETE CASCADE,
    photo_path    VARCHAR(512) NOT NULL,
    result        JSONB        NOT NULL,
    model_version VARCHAR(100) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL
);

-- List the diagnoses of one parcel quickly.
CREATE INDEX ix_diagnoses_parcel_id ON diagnoses (parcel_id);
