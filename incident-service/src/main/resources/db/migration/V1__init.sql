-- incident-service schema (CONTRACTS.md 7.6)

CREATE SEQUENCE incident_code_seq START WITH 1;

CREATE TABLE incidents (
    id                    BIGSERIAL PRIMARY KEY,
    incident_code         VARCHAR(20)      NOT NULL UNIQUE,
    title                 VARCHAR(150)     NOT NULL,
    incident_type         VARCHAR(30)      NOT NULL,
    severity              VARCHAR(10)      NOT NULL,
    status                VARCHAR(20)      NOT NULL,
    description           VARCHAR(4000)    NOT NULL,
    area_id               BIGINT           NOT NULL,   -- habitat-service (no FK across services)
    zone_id               BIGINT           NOT NULL,   -- habitat-service
    zone_name             VARCHAR(150),
    latitude              DOUBLE PRECISION NOT NULL CHECK (latitude BETWEEN -90 AND 90),
    longitude             DOUBLE PRECISION NOT NULL CHECK (longitude BETWEEN -180 AND 180),
    location_description  VARCHAR(300),
    occurred_at           TIMESTAMP        NOT NULL,
    reported_at           TIMESTAMP        NOT NULL,
    reported_by_user_id   BIGINT,
    reported_by_name      VARCHAR(120),
    verified_at           TIMESTAMP,
    resolved_at           TIMESTAMP,
    closed_at             TIMESTAMP,
    resolution_notes      VARCHAR(2000),
    animal_id             BIGINT,                      -- wildlife-service
    species_id            BIGINT,                      -- wildlife-service
    source_patrol_log_id  BIGINT                       -- patrol-service
);
-- the monthly zone review filters on zone + occurred_at
CREATE INDEX ix_incidents_zone_occurred ON incidents (zone_id, occurred_at);
CREATE INDEX ix_incidents_status ON incidents (status);

CREATE TABLE incident_assignments (
    id                BIGSERIAL PRIMARY KEY,
    incident_id       BIGINT      NOT NULL REFERENCES incidents (id),
    staff_id          BIGINT      NOT NULL,   -- resource-service
    staff_name        VARCHAR(120),
    designation       VARCHAR(20),
    assignment_role   VARCHAR(20) NOT NULL,
    assigned_at       TIMESTAMP   NOT NULL,
    assigned_by_name  VARCHAR(120),
    active            BOOLEAN     NOT NULL DEFAULT TRUE,
    note              VARCHAR(500),
    allocation_id     BIGINT                  -- resource-service
);
CREATE INDEX ix_assignments_incident ON incident_assignments (incident_id);

CREATE TABLE incident_status_history (
    id                  BIGSERIAL PRIMARY KEY,
    incident_id         BIGINT      NOT NULL REFERENCES incidents (id),
    from_status         VARCHAR(20),
    to_status           VARCHAR(20) NOT NULL,
    changed_by_user_id  BIGINT,
    changed_by_name     VARCHAR(120),
    changed_at          TIMESTAMP   NOT NULL,
    note                VARCHAR(2000)
);
CREATE INDEX ix_history_incident ON incident_status_history (incident_id);

-- ---------------------------------------------------------------- seed
-- Dates for incidents 1-7 are a fraction of the time elapsed since the 1st of the CURRENT month, so they always
-- fall inside "this month" and never in the future, whichever day the database is created (CONTRACTS.md 12).
CREATE TEMPORARY TABLE seed_clock AS
SELECT date_trunc('month', LOCALTIMESTAMP) AS m0, LOCALTIMESTAMP - date_trunc('month', LOCALTIMESTAMP) AS elapsed;

INSERT INTO incidents (id, incident_code, title, incident_type, severity, status, description, area_id, zone_id,
                       zone_name, latitude, longitude, location_description, occurred_at, reported_at,
                       reported_by_user_id, reported_by_name, verified_at, resolved_at, closed_at, resolution_notes,
                       animal_id, species_id, source_patrol_log_id)
SELECT v.id, 'INC-' || to_char(LOCALTIMESTAMP, 'YYYY') || '-' || lpad(v.id::text, 4, '0'),
       v.title, v.incident_type, v.severity, v.status, v.description, v.area_id, v.zone_id, v.zone_name,
       v.latitude, v.longitude, v.location_description,
       c.m0 + c.elapsed * v.f,                                   -- occurred_at
       c.m0 + c.elapsed * v.f + interval '1 minute' * least(30, extract(epoch from c.elapsed) / 60 * 0.01),
       v.reported_by, v.reported_name,
       CASE WHEN v.status <> 'REPORTED' THEN c.m0 + c.elapsed * (v.f + (1 - v.f) * 0.2) END,
       CASE WHEN v.status IN ('RESOLVED', 'CLOSED') THEN c.m0 + c.elapsed * (v.f + (1 - v.f) * 0.6) END,
       CASE WHEN v.status = 'CLOSED' THEN c.m0 + c.elapsed * (v.f + (1 - v.f) * 0.8) END,
       v.resolution_notes, v.animal_id, v.species_id, v.patrol_log
FROM seed_clock c,
(VALUES
 (1, 'Cattle kill reported near Mukki gate', 'HUMAN_ANIMAL_CONFLICT', 'HIGH', 'IN_PROGRESS',
     'Villagers report a cow killed by a tiger 300 m inside the boundary. Compensation claim expected.',
     1, 1, 'Mukki Core', 22.2860, 80.6390, '300 m north of Mukki gate', 0.10, 3, 'Soni Kumari', NULL, NULL, 1, NULL),
 (2, 'Limping tigress seen near Sondar tank', 'WILDLIFE_INJURY', 'MEDIUM', 'VERIFIED',
     'Tigress TGR-KNH-014 seen limping on the right hind leg; vet assessment requested.',
     1, 1, 'Mukki Core', 22.2950, 80.6380, 'Sondar tank, east bank', 0.25, 3, 'Soni Kumari', NULL, 2, 1, NULL),
 (3, 'Wire snare found on game trail', 'POACHING', 'HIGH', 'REPORTED',
     'Wire snare on game trail 200 m from waterhole 3; removed by patrol. Escalated from patrol log.',
     1, 1, 'Mukki Core', 22.2815, 80.6455, '200 m from waterhole 3', 0.40, 3, 'Soni Kumari', NULL, NULL, NULL, 2),
 (4, 'Ground fire near compartment 412', 'FOREST_FIRE', 'MEDIUM', 'RESOLVED',
     'Small ground fire spotted by fire watcher; contained with beaters.',
     1, 1, 'Mukki Core', 22.2790, 80.6510, 'Compartment 412', 0.55, 2, 'Megha Gupta',
     'Fire line cut and blaze put out within two hours; about 0.5 ha of grass burnt.', NULL, NULL, NULL),
 (5, 'Fresh teak stumps found', 'ILLEGAL_LOGGING', 'LOW', 'CLOSED',
     'Three freshly cut teak stumps found during beat patrol.',
     1, 1, 'Mukki Core', 22.2880, 80.6300, 'Beat 7, near nala crossing', 0.15, 3, 'Soni Kumari',
     'Timber seized from nearby village; offence case registered.', NULL, NULL, NULL),
 (6, 'Gunshots heard near Mihimukh', 'POACHING', 'CRITICAL', 'IN_PROGRESS',
     'Two gunshots heard at night from the direction of Mihimukh beel. Anti-poaching camp alerted.',
     2, 3, 'Kohora Central', 26.5850, 93.1750, 'Mihimukh beel, north side', 0.70, 2, 'Megha Gupta', NULL, NULL, 3, NULL),
 (7, 'Elephants raiding paddy at forest edge', 'HUMAN_ANIMAL_CONFLICT', 'MEDIUM', 'REPORTED',
     'Herd of about 12 elephants raided paddy fields; no injuries.',
     2, 3, 'Kohora Central', 26.5700, 93.1650, 'Kohora village fields', 0.85, 2, 'Megha Gupta', NULL, NULL, 2, NULL)
) AS v(id, title, incident_type, severity, status, description, area_id, zone_id, zone_name, latitude, longitude,
       location_description, f, reported_by, reported_name, resolution_notes, animal_id, species_id, patrol_log);

-- Incident 8 is older (linked from rescue case 1); its date is fixed relative to today, not to the month
INSERT INTO incidents (id, incident_code, title, incident_type, severity, status, description, area_id, zone_id,
                       zone_name, latitude, longitude, location_description, occurred_at, reported_at,
                       reported_by_user_id, reported_by_name, verified_at, resolved_at, resolution_notes,
                       animal_id, species_id)
VALUES (8, 'INC-' || to_char(LOCALTIMESTAMP, 'YYYY') || '-0008', 'Injured tusker near Thekkady lake', 'WILDLIFE_INJURY',
        'HIGH', 'RESOLVED', 'Tusker Kombi seen with a swollen left hind leg; rescue team called.',
        3, 5, 'Thekkady Core', 9.5916, 77.1676, 'Lake edge near boat landing',
        LOCALTIMESTAMP - interval '9 days 6 hours', LOCALTIMESTAMP - interval '9 days 5 hours', 4, 'Dr. Neha Fatima',
        LOCALTIMESTAMP - interval '9 days 4 hours', LOCALTIMESTAMP - interval '9 days',
        'Animal tranquilised and moved to the rescue centre (rescue case 1).', 5, 2);

SELECT setval('incident_code_seq', 8);
SELECT setval(pg_get_serial_sequence('incidents', 'id'), (SELECT MAX(id) FROM incidents));

INSERT INTO incident_assignments (incident_id, staff_id, staff_name, designation, assignment_role, assigned_at,
                                  assigned_by_name, active)
SELECT i.id, v.staff_id, v.staff_name, v.designation, v.role, i.reported_at + interval '5 minutes', 'Megha Gupta', TRUE
FROM incidents i
JOIN (VALUES
  (1, 2, 'Soni Kumari',     'RANGE_OFFICER', 'LEAD'),
  (1, 3, 'Srishti Sao',      'FOREST_GUARD',  'SUPPORT'),
  (2, 2, 'Soni Kumari',     'RANGE_OFFICER', 'INVESTIGATOR'),
  (2, 4, 'Dr. Neha Fatima', 'VETERINARIAN',  'VETERINARY'),
  (4, 3, 'Srishti Sao',      'FOREST_GUARD',  'LEAD'),
  (5, 2, 'Soni Kumari',     'RANGE_OFFICER', 'LEAD'),
  (6, 5, 'Sakshi Rathi',    'FOREST_GUARD',  'LEAD'),
  (8, 4, 'Dr. Neha Fatima', 'VETERINARIAN',  'VETERINARY')
) AS v(incident_id, staff_id, staff_name, designation, role) ON v.incident_id = i.id;

-- Workflow history consistent with each incident's current status
INSERT INTO incident_status_history (incident_id, from_status, to_status, changed_by_user_id, changed_by_name, changed_at, note)
SELECT id, NULL, 'REPORTED', reported_by_user_id, reported_by_name, reported_at, 'Incident reported' FROM incidents;
INSERT INTO incident_status_history (incident_id, from_status, to_status, changed_by_user_id, changed_by_name, changed_at, note)
SELECT id, 'REPORTED', 'VERIFIED', 2, 'Megha Gupta', verified_at, NULL FROM incidents WHERE verified_at IS NOT NULL;
INSERT INTO incident_status_history (incident_id, from_status, to_status, changed_by_user_id, changed_by_name, changed_at, note)
SELECT id, 'VERIFIED', 'IN_PROGRESS', 2, 'Megha Gupta', verified_at + (COALESCE(resolved_at, LOCALTIMESTAMP) - verified_at) / 4, NULL
FROM incidents WHERE status IN ('IN_PROGRESS', 'RESOLVED', 'CLOSED');
INSERT INTO incident_status_history (incident_id, from_status, to_status, changed_by_user_id, changed_by_name, changed_at, note)
SELECT id, 'IN_PROGRESS', 'RESOLVED', 2, 'Megha Gupta', resolved_at, resolution_notes FROM incidents WHERE resolved_at IS NOT NULL;
INSERT INTO incident_status_history (incident_id, from_status, to_status, changed_by_user_id, changed_by_name, changed_at, note)
SELECT id, 'RESOLVED', 'CLOSED', 2, 'Megha Gupta', closed_at, NULL FROM incidents WHERE closed_at IS NOT NULL;

DROP TABLE seed_clock;
