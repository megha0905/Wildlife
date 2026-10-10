-- rescue-service schema (CONTRACTS.md 7.7)

CREATE SEQUENCE rescue_case_seq START WITH 1;

CREATE TABLE rescue_cases (
    id                     BIGSERIAL PRIMARY KEY,
    case_number            VARCHAR(20)      NOT NULL UNIQUE,
    animal_id              BIGINT,                  -- wildlife-service (no FK across services)
    animal_tag_id          VARCHAR(30),
    species_id             BIGINT,                  -- wildlife-service
    species_name           VARCHAR(120)     NOT NULL,
    animal_description     VARCHAR(500),
    cause                  VARCHAR(20)      NOT NULL,
    intake_condition       VARCHAR(20)      NOT NULL,
    status                 VARCHAR(20)      NOT NULL,
    rescued_at             TIMESTAMP        NOT NULL,
    latitude               DOUBLE PRECISION NOT NULL,
    longitude              DOUBLE PRECISION NOT NULL,
    area_id                BIGINT,                  -- habitat-service
    zone_id                BIGINT,                  -- habitat-service
    incident_id            BIGINT,                  -- incident-service
    facility_name          VARCHAR(150)     NOT NULL,
    registered_by_user_id  BIGINT,
    registered_by_name     VARCHAR(120),
    notes                  VARCHAR(2000),
    transfer_destination   VARCHAR(150),
    transfer_reason        VARCHAR(500),
    transfer_date          DATE,
    release_date           DATE,
    release_area_id        BIGINT,
    release_zone_id        BIGINT,
    release_latitude       DOUBLE PRECISION,
    release_longitude      DOUBLE PRECISION,
    release_notes          VARCHAR(1000),
    created_at             TIMESTAMP        NOT NULL DEFAULT LOCALTIMESTAMP,
    updated_at             TIMESTAMP        NOT NULL DEFAULT LOCALTIMESTAMP
);
CREATE INDEX ix_rescue_status ON rescue_cases (status);

CREATE TABLE treatments (
    id              BIGSERIAL PRIMARY KEY,
    rescue_id       BIGINT        NOT NULL REFERENCES rescue_cases (id),
    treatment_date  DATE          NOT NULL,
    diagnosis       VARCHAR(500)  NOT NULL,
    procedure       VARCHAR(1000) NOT NULL,
    medications     VARCHAR(500),
    vet_staff_id    BIGINT,                         -- resource-service
    vet_name        VARCHAR(120)  NOT NULL,
    notes           VARCHAR(1000),
    created_at      TIMESTAMP     NOT NULL DEFAULT LOCALTIMESTAMP
);
CREATE INDEX ix_treatments_rescue ON treatments (rescue_id);

CREATE TABLE rehab_progress (
    id               BIGSERIAL PRIMARY KEY,
    rescue_id        BIGINT        NOT NULL REFERENCES rescue_cases (id),
    assessed_on      DATE          NOT NULL,
    enclosure        VARCHAR(100),
    behaviour_notes  VARCHAR(1000) NOT NULL,
    weight_kg        DOUBLE PRECISION,
    fit_for_release  BOOLEAN       NOT NULL,
    assessed_by      VARCHAR(120)  NOT NULL,
    created_at       TIMESTAMP     NOT NULL DEFAULT LOCALTIMESTAMP
);
CREATE INDEX ix_rehab_rescue ON rehab_progress (rescue_id);

CREATE TABLE rescue_status_history (
    id                  BIGSERIAL PRIMARY KEY,
    rescue_id           BIGINT      NOT NULL REFERENCES rescue_cases (id),
    from_status         VARCHAR(20),
    to_status           VARCHAR(20) NOT NULL,
    changed_by_user_id  BIGINT,
    changed_by_name     VARCHAR(120),
    changed_at          TIMESTAMP   NOT NULL,
    note                VARCHAR(2000)
);
CREATE INDEX ix_rescue_history_rescue ON rescue_status_history (rescue_id);

-- ---------------------------------------------------------------- seed (IDs fixed by CONTRACTS.md 12)
INSERT INTO rescue_cases (id, case_number, animal_id, animal_tag_id, species_id, species_name, animal_description,
                          cause, intake_condition, status, rescued_at, latitude, longitude, area_id, zone_id,
                          incident_id, facility_name, registered_by_user_id, registered_by_name, notes,
                          created_at, updated_at) VALUES
 (1, 'RSC-' || to_char(LOCALTIMESTAMP, 'YYYY') || '-0001', 5, 'ELE-PRY-007', 2, 'Asian Elephant',
     'Adult tusker, left tusk broken', 'INJURY', 'INJURED', 'UNDER_TREATMENT',
     LOCALTIMESTAMP - interval '9 days', 9.5916, 77.1676, 3, 5, 8,
     'Periyar Elephant Rescue Centre', 4, 'Dr. Arun Menon', 'Tranquilised at lake edge; transported by truck.',
     LOCALTIMESTAMP - interval '9 days', LOCALTIMESTAMP - interval '8 days'),
 (2, 'RSC-' || to_char(LOCALTIMESTAMP, 'YYYY') || '-0002', NULL, NULL, 4, 'Indian Leopard',
     'Female cub, about 4 months, separated from mother', 'ORPHANED', 'SICK', 'REHABILITATING',
     LOCALTIMESTAMP - interval '75 days', 22.3790, 80.5680, 1, 2, NULL,
     'Mukki Wildlife Transit Centre', 2, 'Anita Sharma', 'Found in a sugarcane field after mother not seen for 3 days.',
     LOCALTIMESTAMP - interval '75 days', LOCALTIMESTAMP - interval '20 days');

SELECT setval('rescue_case_seq', 2);
SELECT setval(pg_get_serial_sequence('rescue_cases', 'id'), (SELECT MAX(id) FROM rescue_cases));

INSERT INTO treatments (rescue_id, treatment_date, diagnosis, procedure, medications, vet_staff_id, vet_name, notes) VALUES
 (1, CURRENT_DATE - 8, 'Abscess on left hind leg', 'Abscess drained and flushed', 'Enrofloxacin, meloxicam', 4, 'Dr. Arun Menon', NULL),
 (1, CURRENT_DATE - 4, 'Abscess healing',          'Wound dressing changed',      'Enrofloxacin',           4, 'Dr. Arun Menon', 'Eating well'),
 (2, CURRENT_DATE - 74, 'Dehydration, mild worm load', 'Rehydration and deworming', 'Oral fluids, fenbendazole', 4, 'Dr. Arun Menon', NULL);

INSERT INTO rehab_progress (rescue_id, assessed_on, enclosure, behaviour_notes, weight_kg, fit_for_release, assessed_by) VALUES
 (2, CURRENT_DATE - 40, 'Soft-release enclosure A', 'Hunting live prey; avoids keepers', 14.5, FALSE, 'Dr. Arun Menon'),
 (2, CURRENT_DATE - 5,  'Soft-release enclosure A', 'Independent, wary of people, good body condition', 19.0, TRUE, 'Dr. Arun Menon');

INSERT INTO rescue_status_history (rescue_id, from_status, to_status, changed_by_user_id, changed_by_name, changed_at, note) VALUES
 (1, NULL,              'REGISTERED',      4, 'Dr. Arun Menon', LOCALTIMESTAMP - interval '9 days',  'Rescue case registered'),
 (1, 'REGISTERED',      'UNDER_TREATMENT', 4, 'Dr. Arun Menon', LOCALTIMESTAMP - interval '8 days',  NULL),
 (2, NULL,              'REGISTERED',      2, 'Anita Sharma',   LOCALTIMESTAMP - interval '75 days', 'Rescue case registered'),
 (2, 'REGISTERED',      'UNDER_TREATMENT', 4, 'Dr. Arun Menon', LOCALTIMESTAMP - interval '74 days', NULL),
 (2, 'UNDER_TREATMENT', 'REHABILITATING',  4, 'Dr. Arun Menon', LOCALTIMESTAMP - interval '60 days', 'Weaned onto meat');
