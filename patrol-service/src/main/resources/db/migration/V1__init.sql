-- patrol-service schema (CONTRACTS.md 7.5)

CREATE TABLE patrol_teams (
    id               BIGSERIAL PRIMARY KEY,
    name             VARCHAR(100) NOT NULL UNIQUE,
    area_id          BIGINT       NOT NULL,   -- habitat-service area (no FK across services)
    leader_staff_id  BIGINT       NOT NULL,   -- resource-service staff
    leader_name      VARCHAR(120),
    active           BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE patrol_team_members (
    team_id      BIGINT       NOT NULL REFERENCES patrol_teams (id) ON DELETE CASCADE,
    staff_id     BIGINT       NOT NULL,
    staff_name   VARCHAR(120),
    designation  VARCHAR(20)
);
CREATE INDEX ix_team_members_team ON patrol_team_members (team_id);

CREATE TABLE patrol_routes (
    id               BIGSERIAL PRIMARY KEY,
    name             VARCHAR(120)     NOT NULL,
    area_id          BIGINT           NOT NULL,
    zone_id          BIGINT           NOT NULL,
    zone_name        VARCHAR(150),
    distance_km      DOUBLE PRECISION NOT NULL CHECK (distance_km > 0),
    estimated_hours  DOUBLE PRECISION,
    difficulty       VARCHAR(20)      NOT NULL,
    description      VARCHAR(1000)
);

CREATE TABLE patrol_route_waypoints (
    route_id   BIGINT           NOT NULL REFERENCES patrol_routes (id) ON DELETE CASCADE,
    sequence   INTEGER          NOT NULL,
    latitude   DOUBLE PRECISION NOT NULL,
    longitude  DOUBLE PRECISION NOT NULL,
    label      VARCHAR(100)
);
CREATE INDEX ix_route_waypoints_route ON patrol_route_waypoints (route_id);

CREATE TABLE patrol_schedules (
    id               BIGSERIAL PRIMARY KEY,
    team_id          BIGINT      NOT NULL REFERENCES patrol_teams (id),
    route_id         BIGINT      NOT NULL REFERENCES patrol_routes (id),
    patrol_date      DATE        NOT NULL,
    shift            VARCHAR(10) NOT NULL,
    start_time       TIME        NOT NULL,
    end_time         TIME        NOT NULL,
    status           VARCHAR(20) NOT NULL,
    vehicle_id       BIGINT,                -- resource-service vehicle
    objective        VARCHAR(500),
    started_at       TIMESTAMP,
    completed_at     TIMESTAMP,
    created_by_name  VARCHAR(120)
);
CREATE INDEX ix_schedules_date ON patrol_schedules (patrol_date);

CREATE TABLE patrol_schedule_allocations (
    schedule_id    BIGINT NOT NULL REFERENCES patrol_schedules (id) ON DELETE CASCADE,
    allocation_id  BIGINT NOT NULL            -- resource-service allocation
);

CREATE TABLE patrol_logs (
    id                   BIGSERIAL PRIMARY KEY,
    schedule_id          BIGINT           NOT NULL REFERENCES patrol_schedules (id),
    logged_at            TIMESTAMP        NOT NULL,
    observation_type     VARCHAR(30)      NOT NULL,
    latitude             DOUBLE PRECISION NOT NULL,
    longitude            DOUBLE PRECISION NOT NULL,
    description          VARCHAR(2000)    NOT NULL,
    animal_tag_id        VARCHAR(30),
    severity_hint        VARCHAR(10),
    reported_by_user_id  BIGINT,
    reported_by_name     VARCHAR(120),
    incident_id          BIGINT,          -- incident-service, set on escalation
    created_at           TIMESTAMP        NOT NULL DEFAULT LOCALTIMESTAMP
);
CREATE INDEX ix_logs_schedule ON patrol_logs (schedule_id);

-- ---------------------------------------------------------------- seed (IDs fixed by CONTRACTS.md 12)
INSERT INTO patrol_teams (id, name, area_id, leader_staff_id, leader_name, active) VALUES
 (1, 'Mukki Tigers',       1, 2, 'Soni Kumari',  TRUE),
 (2, 'Kohora Rhino Watch', 2, 5, 'Sakshi Rathi', TRUE);

INSERT INTO patrol_team_members (team_id, staff_id, staff_name, designation) VALUES
 (1, 2, 'Soni Kumari',  'RANGE_OFFICER'),
 (1, 3, 'Srishti Sao',   'FOREST_GUARD'),
 (2, 5, 'Sakshi Rathi', 'FOREST_GUARD');

INSERT INTO patrol_routes (id, name, area_id, zone_id, zone_name, distance_km, estimated_hours, difficulty, description) VALUES
 (1, 'Mukki core loop',       1, 1, 'Mukki Core',     18.0, 6.0, 'MODERATE', 'Gate - Sondar tank - waterhole 3 - back to gate'),
 (2, 'Kohora grassland beat', 2, 3, 'Kohora Central', 12.0, 4.5, 'EASY',     'Kohora camp to Mihimukh beel and back');

INSERT INTO patrol_route_waypoints (route_id, sequence, latitude, longitude, label) VALUES
 (1, 1, 22.2830, 80.6420, 'Mukki gate'),
 (1, 2, 22.2950, 80.6380, 'Sondar tank'),
 (1, 3, 22.2810, 80.6450, 'Waterhole 3'),
 (1, 4, 22.2830, 80.6420, 'Mukki gate'),
 (2, 1, 26.5775, 93.1711, 'Kohora camp'),
 (2, 2, 26.5850, 93.1750, 'Mihimukh beel'),
 (2, 3, 26.5775, 93.1711, 'Kohora camp');

INSERT INTO patrol_schedules (id, team_id, route_id, patrol_date, shift, start_time, end_time, status, vehicle_id,
                              objective, started_at, completed_at, created_by_name) VALUES
 (1, 1, 1, CURRENT_DATE,     'MORNING', TIME '06:00', TIME '12:00', 'SCHEDULED', 1, 'Check waterhole 3 pump and snare sweep near the gate', NULL, NULL, 'Megha Gupta'),
 (2, 1, 1, CURRENT_DATE - 1, 'EVENING', TIME '15:00', TIME '19:00', 'COMPLETED', NULL, 'Evening loop', CURRENT_DATE - 1 + TIME '15:05', CURRENT_DATE - 1 + TIME '19:10', 'Megha Gupta'),
 (3, 2, 2, CURRENT_DATE + 1, 'MORNING', TIME '06:00', TIME '11:00', 'SCHEDULED', NULL, 'Rhino census support', NULL, NULL, 'Megha Gupta');

-- allocation ids created in resource-service seed
INSERT INTO patrol_schedule_allocations (schedule_id, allocation_id) VALUES
 (1, 1), (1, 2), (1, 3),
 (2, 4), (2, 5),
 (3, 6);

INSERT INTO patrol_logs (id, schedule_id, logged_at, observation_type, latitude, longitude, description,
                         animal_tag_id, severity_hint, reported_by_user_id, reported_by_name, incident_id) VALUES
 (1, 2, CURRENT_DATE - 1 + TIME '16:20', 'ANIMAL_SIGHTING', 22.2900, 80.6400, 'Munna resting near Sondar tank',
     'TGR-KNH-001', NULL, 3, 'Soni Kumari', NULL),
 (2, 2, CURRENT_DATE - 1 + TIME '17:45', 'SNARE_FOUND',     22.2815, 80.6455, 'Wire snare on game trail 200 m from waterhole 3; removed',
     NULL, 'HIGH', 3, 'Soni Kumari', 3);

SELECT setval(pg_get_serial_sequence('patrol_teams', 'id'), (SELECT MAX(id) FROM patrol_teams));
SELECT setval(pg_get_serial_sequence('patrol_routes', 'id'), (SELECT MAX(id) FROM patrol_routes));
SELECT setval(pg_get_serial_sequence('patrol_schedules', 'id'), (SELECT MAX(id) FROM patrol_schedules));
SELECT setval(pg_get_serial_sequence('patrol_logs', 'id'), (SELECT MAX(id) FROM patrol_logs));
