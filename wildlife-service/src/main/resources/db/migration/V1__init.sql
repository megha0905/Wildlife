-- wildlife-service schema (CONTRACTS.md 7.3)

CREATE TABLE species (
    id                   BIGSERIAL PRIMARY KEY,
    common_name          VARCHAR(120) NOT NULL UNIQUE,
    scientific_name      VARCHAR(160) NOT NULL UNIQUE,
    category             VARCHAR(20)  NOT NULL,
    conservation_status  VARCHAR(30)  NOT NULL,
    protection_schedule  VARCHAR(60),
    description          VARCHAR(2000)
);

CREATE TABLE animals (
    id                    BIGSERIAL PRIMARY KEY,
    tag_id                VARCHAR(30)  NOT NULL UNIQUE,
    name                  VARCHAR(80),
    species_id            BIGINT       NOT NULL REFERENCES species (id),
    sex                   VARCHAR(10)  NOT NULL,
    birth_year            INTEGER,
    birth_year_estimated  BOOLEAN      NOT NULL DEFAULT TRUE,
    health_status         VARCHAR(20)  NOT NULL,
    status                VARCHAR(20)  NOT NULL,
    identification_marks  VARCHAR(1000),
    area_id               BIGINT,          -- habitat-service area (no FK across services)
    zone_id               BIGINT,          -- habitat-service zone (no FK across services)
    zone_name             VARCHAR(150),    -- snapshot from habitat-service
    last_seen_latitude    DOUBLE PRECISION CHECK (last_seen_latitude BETWEEN -90 AND 90),
    last_seen_longitude   DOUBLE PRECISION CHECK (last_seen_longitude BETWEEN -180 AND 180),
    last_seen_at          TIMESTAMP,
    notes                 VARCHAR(2000),
    created_at            TIMESTAMP    NOT NULL DEFAULT LOCALTIMESTAMP,
    updated_at            TIMESTAMP    NOT NULL DEFAULT LOCALTIMESTAMP
);
CREATE INDEX ix_animals_species ON animals (species_id);
CREATE INDEX ix_animals_zone ON animals (zone_id);

CREATE TABLE health_records (
    id                   BIGSERIAL PRIMARY KEY,
    animal_id            BIGINT       NOT NULL REFERENCES animals (id),
    record_date          DATE         NOT NULL,
    record_type          VARCHAR(30)  NOT NULL,
    health_status        VARCHAR(20)  NOT NULL,
    weight_kg            DOUBLE PRECISION,
    diagnosis            VARCHAR(500),
    treatment            VARCHAR(1000),
    vet_name             VARCHAR(120),
    recorded_by_user_id  BIGINT,
    recorded_by_name     VARCHAR(120),
    notes                VARCHAR(2000),
    created_at           TIMESTAMP    NOT NULL DEFAULT LOCALTIMESTAMP
);
CREATE INDEX ix_health_records_animal ON health_records (animal_id);

-- ---------------------------------------------------------------- seed (IDs fixed by CONTRACTS.md 12)
INSERT INTO species (id, common_name, scientific_name, category, conservation_status, protection_schedule, description) VALUES
 (1, 'Bengal Tiger',                  'Panthera tigris tigris', 'MAMMAL',  'ENDANGERED',            'Schedule I', 'Apex predator monitored with camera traps and pugmarks.'),
 (2, 'Asian Elephant',                'Elephas maximus',        'MAMMAL',  'ENDANGERED',            'Schedule I', 'Wide-ranging; frequent source of human-elephant conflict.'),
 (3, 'Greater One-horned Rhinoceros', 'Rhinoceros unicornis',   'MAMMAL',  'VULNERABLE',            'Schedule I', 'Grassland specialist and primary poaching target.'),
 (4, 'Indian Leopard',                'Panthera pardus fusca',  'MAMMAL',  'VULNERABLE',            'Schedule I', NULL),
 (5, 'Gharial',                       'Gavialis gangeticus',    'REPTILE', 'CRITICALLY_ENDANGERED', 'Schedule I', 'Riverine crocodilian that nests on sandbanks.');

INSERT INTO animals (id, tag_id, name, species_id, sex, birth_year, birth_year_estimated, health_status, status,
                     identification_marks, area_id, zone_id, zone_name, last_seen_latitude, last_seen_longitude,
                     last_seen_at, notes) VALUES
 (1, 'TGR-KNH-001', 'Munna',  1, 'MALE',   2013, TRUE, 'HEALTHY',         'IN_WILD',  'Forehead marking resembling the letters CAT', 1, 1, 'Mukki Core',        22.2845, 80.6410, LOCALTIMESTAMP - INTERVAL '2 days',  NULL),
 (2, 'TGR-KNH-014', 'Neelam', 1, 'FEMALE', 2017, TRUE, 'HEALTHY',         'IN_WILD',  'Notch on left ear',                         1, 1, 'Mukki Core',        22.2871, 80.6402, LOCALTIMESTAMP - INTERVAL '5 days',  NULL),
 (3, 'RHN-KZR-102', 'Bhaiti', 3, 'MALE',   2010, TRUE, 'HEALTHY',         'IN_WILD',  'Horn tip chipped',                          2, 3, 'Kohora Central',    26.5775, 93.1711, LOCALTIMESTAMP - INTERVAL '1 day',   NULL),
 (4, 'RHN-KZR-131', NULL,     3, 'MALE',   2008, TRUE, 'UNKNOWN',         'MISSING',  'Torn left ear',                             2, 4, 'Agaratoli Eastern', 26.6403, 93.4210, LOCALTIMESTAMP - INTERVAL '47 days', 'Not sighted since monsoon flooding'),
 (5, 'ELE-PRY-007', 'Kombi',  2, 'MALE',   2004, TRUE, 'UNDER_TREATMENT', 'IN_RESCUE','Tusker, left tusk broken',                  3, 5, 'Thekkady Core',      9.5916, 77.1676, LOCALTIMESTAMP - INTERVAL '9 days',  'In rescue case RSC (see rescue-service case 1)'),
 (6, 'LPD-KNH-009', NULL,     4, 'FEMALE', 2018, TRUE, 'HEALTHY',         'IN_WILD',  NULL,                                        1, 2, 'Khatia Buffer',     22.3790, 80.5680, LOCALTIMESTAMP - INTERVAL '4 days',  NULL),
 (7, 'ELE-KZR-021', 'Lakhi',  2, 'FEMALE', 1998, TRUE, 'HEALTHY',         'IN_WILD',  'Matriarch of herd K-4; torn right ear',     2, 3, 'Kohora Central',    26.5900, 93.1900, LOCALTIMESTAMP - INTERVAL '3 days',  NULL),
 (8, 'TGR-PRY-003', NULL,     1, 'MALE',   2019, TRUE, 'INJURED',         'IN_WILD',  'Limp on right foreleg',                     3, 5, 'Thekkady Core',      9.4682, 77.1620, LOCALTIMESTAMP - INTERVAL '1 day',   'Vet team informed');

INSERT INTO health_records (animal_id, record_date, record_type, health_status, weight_kg, diagnosis, treatment,
                            vet_name, recorded_by_user_id, recorded_by_name, notes) VALUES
 (1, CURRENT_DATE - 60, 'CHECKUP',           'HEALTHY',         NULL,   NULL,                       NULL,                              'Dr. Neha Fatima', 4, 'Dr. Neha Fatima', 'Camera-trap body condition assessment'),
 (5, CURRENT_DATE - 9,  'INJURY_ASSESSMENT', 'INJURED',         4100,   'Abscess on left hind leg', NULL,                              'Dr. Neha Fatima', 4, 'Dr. Neha Fatima', NULL),
 (5, CURRENT_DATE - 8,  'TREATMENT',         'UNDER_TREATMENT', 4100,   'Abscess on left hind leg', 'Drained; antibiotics started',    'Dr. Neha Fatima', 4, 'Dr. Neha Fatima', NULL),
 (8, CURRENT_DATE - 1,  'OBSERVATION',       'INJURED',         NULL,   'Possible foreleg injury',  NULL,                              NULL,             3, 'Soni Kumari',     'Limping, seen from 80 m');

SELECT setval(pg_get_serial_sequence('species', 'id'), (SELECT MAX(id) FROM species));
SELECT setval(pg_get_serial_sequence('animals', 'id'), (SELECT MAX(id) FROM animals));
