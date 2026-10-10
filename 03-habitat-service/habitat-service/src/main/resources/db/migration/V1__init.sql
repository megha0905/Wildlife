-- habitat-service schema (CONTRACTS.md 7.2)

CREATE TABLE areas (
    id                BIGSERIAL PRIMARY KEY,
    name              VARCHAR(150)     NOT NULL,
    code              VARCHAR(20)      NOT NULL UNIQUE,
    area_type         VARCHAR(30)      NOT NULL,
    state             VARCHAR(80)      NOT NULL,
    district          VARCHAR(80),
    total_area_sq_km  DOUBLE PRECISION NOT NULL CHECK (total_area_sq_km > 0),
    established_year  INTEGER,
    center_latitude   DOUBLE PRECISION NOT NULL CHECK (center_latitude BETWEEN -90 AND 90),
    center_longitude  DOUBLE PRECISION NOT NULL CHECK (center_longitude BETWEEN -180 AND 180),
    description       VARCHAR(2000)
);

CREATE TABLE zones (
    id                BIGSERIAL PRIMARY KEY,
    area_id           BIGINT           NOT NULL REFERENCES areas (id),
    name              VARCHAR(150)     NOT NULL,
    code              VARCHAR(20)      NOT NULL UNIQUE,
    zone_type         VARCHAR(20)      NOT NULL,
    area_sq_km        DOUBLE PRECISION,
    center_latitude   DOUBLE PRECISION NOT NULL CHECK (center_latitude BETWEEN -90 AND 90),
    center_longitude  DOUBLE PRECISION NOT NULL CHECK (center_longitude BETWEEN -180 AND 180),
    description       VARCHAR(2000)
);
CREATE INDEX ix_zones_area ON zones (area_id);

CREATE TABLE habitats (
    id                   BIGSERIAL PRIMARY KEY,
    zone_id              BIGINT        NOT NULL REFERENCES zones (id),
    name                 VARCHAR(150)  NOT NULL,
    habitat_type         VARCHAR(30)   NOT NULL,
    habitat_condition    VARCHAR(20)   NOT NULL,
    area_sq_km           DOUBLE PRECISION,
    latitude             DOUBLE PRECISION,
    longitude            DOUBLE PRECISION,
    dominant_vegetation  VARCHAR(300),
    last_survey_date     DATE,
    notes                VARCHAR(2000)
);
CREATE INDEX ix_habitats_zone ON habitats (zone_id);

CREATE TABLE water_resources (
    id                 BIGSERIAL PRIMARY KEY,
    zone_id            BIGINT           NOT NULL REFERENCES zones (id),
    name               VARCHAR(150)     NOT NULL,
    source_type        VARCHAR(30)      NOT NULL,
    status             VARCHAR(20)      NOT NULL,
    perennial          BOOLEAN          NOT NULL DEFAULT FALSE,
    latitude           DOUBLE PRECISION NOT NULL,
    longitude          DOUBLE PRECISION NOT NULL,
    capacity_cubic_m   DOUBLE PRECISION,
    last_inspected_at  TIMESTAMP,
    notes              VARCHAR(2000)
);
CREATE INDEX ix_water_zone ON water_resources (zone_id);

-- ---------------------------------------------------------------- seed (IDs fixed by CONTRACTS.md 12)
INSERT INTO areas (id, name, code, area_type, state, district, total_area_sq_km, established_year,
                   center_latitude, center_longitude, description) VALUES
 (1, 'Kanha Tiger Reserve',     'KNH', 'TIGER_RESERVE', 'Madhya Pradesh', 'Mandla',   1945.0, 1955, 22.3345, 80.6115,
  'Sal and bamboo forests with large meadows; stronghold of the hard-ground barasingha.'),
 (2, 'Kaziranga National Park', 'KZR', 'NATIONAL_PARK', 'Assam',          'Golaghat',  1055.0, 1974, 26.6667, 93.3667,
  'Floodplain grasslands of the Brahmaputra; largest population of greater one-horned rhinoceros.'),
 (3, 'Periyar Tiger Reserve',   'PRY', 'TIGER_RESERVE', 'Kerala',         'Idukki',    925.0, 1978,  9.4682, 77.1620,
  'Tropical evergreen and moist deciduous forest around Periyar lake.');

INSERT INTO zones (id, area_id, name, code, zone_type, area_sq_km, center_latitude, center_longitude, description) VALUES
 (1, 1, 'Mukki Core',        'KNH-MUK', 'CORE',   420.0, 22.2830, 80.6420, 'Southern core range around Mukki gate.'),
 (2, 1, 'Khatia Buffer',     'KNH-KHT', 'BUFFER', 310.0, 22.3760, 80.5700, 'Buffer villages and forest edge near Khatia.'),
 (3, 2, 'Kohora Central',    'KZR-KOH', 'CORE',   280.0, 26.5775, 93.1711, 'Central range, densest rhino habitat.'),
 (4, 2, 'Agaratoli Eastern', 'KZR-AGR', 'CORE',   230.0, 26.6403, 93.4210, 'Eastern range with wetlands and beels.'),
 (5, 3, 'Thekkady Core',     'PRY-THK', 'CORE',   350.0,  9.5916, 77.1676, 'Lake-side core area.'),
 (6, 3, 'Kumily Buffer',     'PRY-KUM', 'BUFFER', 180.0,  9.6050, 77.1800, 'Plantation and settlement interface.');

INSERT INTO habitats (zone_id, name, habitat_type, habitat_condition, area_sq_km, latitude, longitude,
                      dominant_vegetation, last_survey_date, notes) VALUES
 (1, 'Mukki sal forest',          'TROPICAL_DRY_FOREST',   'GOOD',     180.0, 22.2900, 80.6300, 'Sal, bamboo',            CURRENT_DATE - 40, NULL),
 (1, 'Kanha meadows',             'GRASSLAND',             'FAIR',      35.0, 22.3000, 80.6500, 'Themeda, Saccharum',     CURRENT_DATE - 75, 'Lantana spreading on edges'),
 (2, 'Khatia scrub belt',         'SCRUBLAND',             'DEGRADED',  40.0, 22.3800, 80.5600, 'Lantana, Zizyphus',      CURRENT_DATE - 120, 'Grazing pressure from buffer villages'),
 (3, 'Kohora tall grassland',     'GRASSLAND',             'GOOD',     120.0, 26.5800, 93.1800, 'Elephant grass',         CURRENT_DATE - 30, NULL),
 (4, 'Agaratoli wetlands',        'WETLAND',               'FAIR',      60.0, 26.6400, 93.4300, 'Water hyacinth, reeds',  CURRENT_DATE - 60, 'Invasive hyacinth in Sohola beel'),
 (5, 'Periyar evergreen belt',    'TROPICAL_MOIST_FOREST', 'GOOD',     200.0,  9.5800, 77.1500, 'Evergreen and semi-evergreen', CURRENT_DATE - 90, NULL),
 (6, 'Kumily plantation edge',    'SCRUBLAND',             'CRITICAL',  25.0,  9.6100, 77.1850, 'Cardamom, scrub',        CURRENT_DATE - 20, 'Encroachment reported');

INSERT INTO water_resources (zone_id, name, source_type, status, perennial, latitude, longitude,
                             capacity_cubic_m, last_inspected_at, notes) VALUES
 (1, 'Sondar tank',           'LAKE',                 'AVAILABLE',    TRUE,  22.2950, 80.6380, 90000, LOCALTIMESTAMP - INTERVAL '5 days',  NULL),
 (1, 'Mukki waterhole 3',     'ARTIFICIAL_WATERHOLE', 'LOW',          FALSE, 22.2810, 80.6450,   400, LOCALTIMESTAMP - INTERVAL '2 days',  'Solar pump needs repair'),
 (2, 'Banjar river bend',     'RIVER',                'AVAILABLE',    TRUE,  22.3700, 80.5650,  NULL, LOCALTIMESTAMP - INTERVAL '10 days', NULL),
 (3, 'Mihimukh beel',         'LAKE',                 'AVAILABLE',    TRUE,  26.5850, 93.1750, 150000, LOCALTIMESTAMP - INTERVAL '7 days', NULL),
 (4, 'Sohola beel',           'POND',                 'CONTAMINATED', TRUE,  26.6420, 93.4250,  8000, LOCALTIMESTAMP - INTERVAL '3 days',  'Run-off from tea garden'),
 (5, 'Periyar lake inlet',    'LAKE',                 'AVAILABLE',    TRUE,   9.5900, 77.1650, NULL,  LOCALTIMESTAMP - INTERVAL '14 days', NULL),
 (6, 'Kumily seasonal stream','STREAM',               'DRY',          FALSE,  9.6080, 77.1820, NULL,  LOCALTIMESTAMP - INTERVAL '1 day',   'Dry since early September');

SELECT setval(pg_get_serial_sequence('areas', 'id'), (SELECT MAX(id) FROM areas));
SELECT setval(pg_get_serial_sequence('zones', 'id'), (SELECT MAX(id) FROM zones));
