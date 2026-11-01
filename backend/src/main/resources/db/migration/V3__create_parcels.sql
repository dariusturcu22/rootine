-- Parcels drawn by a user. Each parcel has one crop and one polygon (WGS 84, SRID 4326).
-- Deleting a user deletes their parcels.
CREATE TABLE parcels (
    id         UUID PRIMARY KEY,
    owner_id   UUID                      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name       VARCHAR(100)              NOT NULL,
    crop       VARCHAR(100)              NOT NULL,
    geometry   GEOMETRY(POLYGON, 4326)   NOT NULL,
    created_at TIMESTAMPTZ               NOT NULL
);

-- List a user's parcels quickly (the parcel list is scoped to the owner).
CREATE INDEX ix_parcels_owner_id ON parcels (owner_id);

-- Spatial index for geometry queries such as the heatmap centre lookups.
CREATE INDEX ix_parcels_geometry ON parcels USING GIST (geometry);
