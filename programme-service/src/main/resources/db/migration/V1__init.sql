-- programme-service schema (CONTRACTS.md 7.8)

CREATE TABLE programmes (
    id              BIGSERIAL PRIMARY KEY,
    code            VARCHAR(30)   NOT NULL UNIQUE,
    name            VARCHAR(150)  NOT NULL,
    programme_type  VARCHAR(30)   NOT NULL,
    status          VARCHAR(20)   NOT NULL,
    objective       VARCHAR(1000) NOT NULL,
    description     VARCHAR(2000),
    start_date      DATE          NOT NULL,
    end_date        DATE          NOT NULL,
    budget_inr      NUMERIC(14, 2) CHECK (budget_inr >= 0),
    lead_staff_id   BIGINT,                  -- resource-service (no FK across services)
    CONSTRAINT ck_programme_dates CHECK (end_date >= start_date)
);

CREATE TABLE programme_areas (
    programme_id  BIGINT NOT NULL REFERENCES programmes (id) ON DELETE CASCADE,
    area_id       BIGINT NOT NULL,           -- habitat-service
    PRIMARY KEY (programme_id, area_id)
);

CREATE TABLE programme_species (
    programme_id  BIGINT NOT NULL REFERENCES programmes (id) ON DELETE CASCADE,
    species_id    BIGINT NOT NULL,           -- wildlife-service
    PRIMARY KEY (programme_id, species_id)
);

CREATE TABLE programme_activities (
    id                 BIGSERIAL PRIMARY KEY,
    programme_id       BIGINT       NOT NULL REFERENCES programmes (id),
    title              VARCHAR(150) NOT NULL,
    description        VARCHAR(1000),
    planned_start      DATE         NOT NULL,
    planned_end        DATE         NOT NULL,
    status             VARCHAR(20)  NOT NULL,
    assigned_staff_id  BIGINT,               -- resource-service
    area_id            BIGINT                -- habitat-service
);
CREATE INDEX ix_activities_programme ON programme_activities (programme_id);

CREATE TABLE programme_targets (
    id               BIGSERIAL PRIMARY KEY,
    programme_id     BIGINT           NOT NULL REFERENCES programmes (id),
    metric_name      VARCHAR(150)     NOT NULL,
    unit             VARCHAR(30)      NOT NULL,
    baseline_value   DOUBLE PRECISION NOT NULL,
    target_value     DOUBLE PRECISION NOT NULL,
    current_value    DOUBLE PRECISION NOT NULL,
    due_date         DATE,
    last_updated_at  TIMESTAMP
);
CREATE INDEX ix_targets_programme ON programme_targets (programme_id);

CREATE TABLE progress_updates (
    id                   BIGSERIAL PRIMARY KEY,
    programme_id         BIGINT        NOT NULL REFERENCES programmes (id),
    target_id            BIGINT        REFERENCES programme_targets (id),
    update_date          DATE          NOT NULL,
    previous_value       DOUBLE PRECISION,
    new_value            DOUBLE PRECISION,
    note                 VARCHAR(2000) NOT NULL,
    reported_by_user_id  BIGINT,
    reported_by_name     VARCHAR(120),
    created_at           TIMESTAMP     NOT NULL DEFAULT LOCALTIMESTAMP
);
CREATE INDEX ix_progress_programme ON progress_updates (programme_id);

-- ---------------------------------------------------------------- seed (IDs fixed by CONTRACTS.md 12)
INSERT INTO programmes (id, code, name, programme_type, status, objective, description, start_date, end_date,
                        budget_inr, lead_staff_id) VALUES
 (1, 'KNH-ASD-' || to_char(CURRENT_DATE, 'YYYY'), 'Kanha Anti-Snare Drive', 'ANTI_POACHING', 'ACTIVE',
     'Reduce snares found in Mukki and Khatia ranges to zero and keep them there.',
     'Joint sweeps by patrol teams, informant network in buffer villages, metal-detector training.',
     date_trunc('year', CURRENT_DATE)::date, (date_trunc('year', CURRENT_DATE) + interval '1 year - 1 day')::date,
     1850000.00, 1),
 (2, 'KZR-WET-' || to_char(CURRENT_DATE, 'YYYY'), 'Kaziranga Wetland Restoration', 'HABITAT_RESTORATION', 'ACTIVE',
     'Clear invasive water hyacinth from Sohola and Mihimukh beels and restore grazing grassland.',
     NULL, (CURRENT_DATE - 120), (CURRENT_DATE + 245), 3200000.00, 8),
 (3, 'PRY-CTR-' || to_char(CURRENT_DATE, 'YYYY'), 'Periyar Camera-Trap Census', 'RESEARCH_MONITORING', 'PLANNED',
     'Estimate tiger density in Periyar using a 2 km camera-trap grid.',
     NULL, (CURRENT_DATE + 30), (CURRENT_DATE + 150), 950000.00, 6);

INSERT INTO programme_areas (programme_id, area_id) VALUES (1, 1), (2, 2), (3, 3);
INSERT INTO programme_species (programme_id, species_id) VALUES (1, 1), (1, 4), (2, 3), (2, 2), (3, 1);

INSERT INTO programme_activities (programme_id, title, description, planned_start, planned_end, status, assigned_staff_id, area_id) VALUES
 (1, 'Monthly snare sweep - Mukki',     'Grid sweep of trails within 1 km of waterholes', CURRENT_DATE - 20, CURRENT_DATE - 5,  'DONE',        2, 1),
 (1, 'Monthly snare sweep - Khatia',    NULL,                                             CURRENT_DATE - 10, CURRENT_DATE - 2,  'IN_PROGRESS', 3, 1),
 (1, 'Metal-detector training',         'Two-day training for 12 guards',                 CURRENT_DATE + 5,  CURRENT_DATE + 6,  'NOT_STARTED', 1, 1),
 (2, 'Hyacinth removal - Sohola beel',  'Manual and mechanical removal',                  CURRENT_DATE - 60, CURRENT_DATE + 30, 'IN_PROGRESS', 5, 2),
 (2, 'Grassland burning plan',          NULL,                                             CURRENT_DATE - 30, CURRENT_DATE - 1,  'BLOCKED',     8, 2),
 (3, 'Procure 60 camera traps',         NULL,                                             CURRENT_DATE + 1,  CURRENT_DATE + 25, 'NOT_STARTED', 6, 3);

INSERT INTO programme_targets (id, programme_id, metric_name, unit, baseline_value, target_value, current_value, due_date, last_updated_at) VALUES
 (1, 1, 'Snares found per month',          'snares',  40, 0,   12, (date_trunc('year', CURRENT_DATE) + interval '1 year - 1 day')::date, LOCALTIMESTAMP - interval '5 days'),
 (2, 1, 'Villages with active informants', 'villages', 3, 15,  9,  (date_trunc('year', CURRENT_DATE) + interval '1 year - 1 day')::date, LOCALTIMESTAMP - interval '12 days'),
 (3, 2, 'Beel area cleared of hyacinth',   'hectares', 0, 120, 45, (CURRENT_DATE + 245), LOCALTIMESTAMP - interval '7 days');

SELECT setval(pg_get_serial_sequence('programmes', 'id'), (SELECT MAX(id) FROM programmes));
SELECT setval(pg_get_serial_sequence('programme_targets', 'id'), (SELECT MAX(id) FROM programme_targets));

INSERT INTO progress_updates (programme_id, target_id, update_date, previous_value, new_value, note, reported_by_user_id, reported_by_name) VALUES
 (1, 1, CURRENT_DATE - 35, 40, 21, 'Joint sweeps started in all beats', 2, 'Anita Sharma'),
 (1, 1, CURRENT_DATE - 5,  21, 12, 'Down to 12 snares this month',      3, 'Ravi Uikey'),
 (1, 2, CURRENT_DATE - 12, 3,  9,  'Six more villages joined',          2, 'Anita Sharma'),
 (2, 3, CURRENT_DATE - 7,  20, 45, 'Mechanical harvester deployed',     2, 'Anita Sharma'),
 (2, NULL, CURRENT_DATE - 1, NULL, NULL, 'Burning plan blocked pending permission from the Chief Wildlife Warden', 2, 'Anita Sharma');
