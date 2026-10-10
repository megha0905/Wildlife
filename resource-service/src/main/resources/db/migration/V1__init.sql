-- resource-service schema (CONTRACTS.md 7.4)

CREATE TABLE staff (
    id             BIGSERIAL PRIMARY KEY,
    employee_code  VARCHAR(20)  NOT NULL UNIQUE,
    full_name      VARCHAR(120) NOT NULL,
    designation    VARCHAR(20)  NOT NULL,
    status         VARCHAR(20)  NOT NULL,
    phone          VARCHAR(20),
    email          VARCHAR(150),
    home_area_id   BIGINT,          -- habitat-service area (no FK across services)
    joined_on      DATE
);

CREATE TABLE vehicles (
    id                   BIGSERIAL PRIMARY KEY,
    registration_number  VARCHAR(20) NOT NULL UNIQUE,
    vehicle_type         VARCHAR(20) NOT NULL,
    status               VARCHAR(20) NOT NULL,
    make                 VARCHAR(60),
    model                VARCHAR(60),
    seating_capacity     INTEGER CHECK (seating_capacity > 0),
    home_area_id         BIGINT,
    last_serviced_on     DATE
);

CREATE TABLE equipment (
    id              BIGSERIAL PRIMARY KEY,
    asset_tag       VARCHAR(30)  NOT NULL UNIQUE,
    name            VARCHAR(120) NOT NULL,
    equipment_type  VARCHAR(30)  NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    home_area_id    BIGINT,
    purchased_on    DATE,
    notes           VARCHAR(1000)
);

CREATE TABLE allocations (
    id                    BIGSERIAL PRIMARY KEY,
    resource_kind         VARCHAR(20)  NOT NULL,
    resource_id           BIGINT       NOT NULL,   -- staff/vehicles/equipment id depending on resource_kind
    resource_label        VARCHAR(160) NOT NULL,
    purpose               VARCHAR(20)  NOT NULL,
    reference_id          BIGINT,                  -- record in another service (patrol schedule, incident...)
    reference_label       VARCHAR(200),
    start_at              TIMESTAMP    NOT NULL,
    end_at                TIMESTAMP    NOT NULL,
    status                VARCHAR(20)  NOT NULL,
    allocated_by_user_id  BIGINT,
    allocated_by_name     VARCHAR(120),
    notes                 VARCHAR(1000),
    created_at            TIMESTAMP    NOT NULL DEFAULT LOCALTIMESTAMP,
    CONSTRAINT ck_allocation_window CHECK (end_at > start_at)
);
-- speeds up the overlap check (CONTRACTS.md 6.4)
CREATE INDEX ix_allocations_resource ON allocations (resource_kind, resource_id, start_at, end_at);

-- ---------------------------------------------------------------- seed (IDs fixed by CONTRACTS.md 12)
INSERT INTO staff (id, employee_code, full_name, designation, status, phone, email, home_area_id, joined_on) VALUES
 (1, 'EMP-001', 'Megha Gupta',   'FOREST_OFFICER', 'ACTIVE',   '+91 94250 11001', 'megha.gupta@forest.gov.in',  1, DATE '2012-06-01'),
 (2, 'EMP-002', 'Soni Kumari',     'RANGE_OFFICER',  'ACTIVE',   '+91 94250 11002', 'soni.kumari@forest.gov.in',    1, DATE '2015-03-15'),
 (3, 'EMP-003', 'Srishti Sao',      'FOREST_GUARD',   'ACTIVE',   '+91 94250 11003', NULL,                          1, DATE '2018-09-10'),
 (4, 'EMP-004', 'Dr. Neha Fatima', 'VETERINARIAN',   'ACTIVE',   '+91 94470 22004', 'neha.fatima@forest.gov.in',    3, DATE '2014-01-20'),
 (5, 'EMP-005', 'Sakshi Rathi',    'FOREST_GUARD',   'ACTIVE',   '+91 94350 33005', NULL,                          2, DATE '2019-11-01'),
 (6, 'EMP-006', 'Suresh Nair',    'RANGE_OFFICER',  'ACTIVE',   '+91 94470 22006', 'suresh.nair@forest.gov.in',   3, DATE '2013-07-07'),
 (7, 'EMP-007', 'Imran Khan',     'DRIVER',         'ACTIVE',   '+91 94250 11007', NULL,                          1, DATE '2016-02-12'),
 (8, 'EMP-008', 'Lakshmi Pillai', 'RESEARCHER',     'ON_LEAVE', '+91 94350 33008', 'lakshmi.pillai@wii.gov.in',   2, DATE '2021-08-23');

INSERT INTO vehicles (id, registration_number, vehicle_type, status, make, model, seating_capacity, home_area_id, last_serviced_on) VALUES
 (1, 'MP-51-G-1021', 'JEEP',       'AVAILABLE',   'Mahindra', 'Bolero', 7, 1, CURRENT_DATE - 40),
 (2, 'AS-05-G-2207', 'JEEP',       'AVAILABLE',   'Maruti',   'Gypsy',  5, 2, CURRENT_DATE - 75),
 (3, 'KL-06-G-0310', 'RESCUE_VAN', 'MAINTENANCE', 'Force',    'Traveller', 4, 3, CURRENT_DATE - 200);

INSERT INTO equipment (id, asset_tag, name, equipment_type, status, home_area_id, purchased_on, notes) VALUES
 (1, 'CT-KNH-017', 'Camera trap 17',        'CAMERA_TRAP',      'IN_USE',    1, DATE '2023-02-01', 'Mukki range, grid cell 17'),
 (2, 'CT-KNH-018', 'Camera trap 18',        'CAMERA_TRAP',      'AVAILABLE', 1, DATE '2023-02-01', NULL),
 (3, 'GPS-KZR-03', 'Handheld GPS 3',        'GPS_UNIT',         'AVAILABLE', 2, DATE '2022-05-14', NULL),
 (4, 'RAD-KNH-11', 'VHF radio 11',          'RADIO',            'AVAILABLE', 1, DATE '2021-10-30', NULL),
 (5, 'TQ-PRY-01',  'Tranquilizer rifle 1',  'TRANQUILIZER_GUN', 'AVAILABLE', 3, DATE '2020-03-03', 'Vet team only');

-- Patrol allocations line up with patrol-service seed schedules 1-3 (purpose PATROL, reference_id = schedule id)
INSERT INTO allocations (id, resource_kind, resource_id, resource_label, purpose, reference_id, reference_label,
                         start_at, end_at, status, allocated_by_user_id, allocated_by_name, notes) VALUES
 (1, 'STAFF',   2, 'Soni Kumari',   'PATROL', 1, 'Patrol PS-1, Mukki Core, today morning',
     CURRENT_DATE + TIME '06:00', CURRENT_DATE + TIME '12:00', 'PLANNED',   2, 'Megha Gupta', NULL),
 (2, 'STAFF',   3, 'Srishti Sao',    'PATROL', 1, 'Patrol PS-1, Mukki Core, today morning',
     CURRENT_DATE + TIME '06:00', CURRENT_DATE + TIME '12:00', 'PLANNED',   2, 'Megha Gupta', NULL),
 (3, 'VEHICLE', 1, 'MP-51-G-1021', 'PATROL', 1, 'Patrol PS-1, Mukki Core, today morning',
     CURRENT_DATE + TIME '06:00', CURRENT_DATE + TIME '12:00', 'PLANNED',   2, 'Megha Gupta', NULL),
 (4, 'STAFF',   2, 'Soni Kumari',   'PATROL', 2, 'Patrol PS-2, Mukki Core, yesterday evening',
     CURRENT_DATE - 1 + TIME '15:00', CURRENT_DATE - 1 + TIME '19:00', 'COMPLETED', 2, 'Megha Gupta', NULL),
 (5, 'STAFF',   3, 'Srishti Sao',    'PATROL', 2, 'Patrol PS-2, Mukki Core, yesterday evening',
     CURRENT_DATE - 1 + TIME '15:00', CURRENT_DATE - 1 + TIME '19:00', 'COMPLETED', 2, 'Megha Gupta', NULL),
 (6, 'STAFF',   5, 'Sakshi Rathi',  'PATROL', 3, 'Patrol PS-3, Kohora Central, tomorrow morning',
     CURRENT_DATE + 1 + TIME '06:00', CURRENT_DATE + 1 + TIME '11:00', 'PLANNED', 2, 'Megha Gupta', NULL),
 (7, 'EQUIPMENT', 5, 'TQ-PRY-01 Tranquilizer rifle 1', 'RESCUE', 1, 'Rescue RSC case 1 (Kombi)',
     CURRENT_DATE - 9 + TIME '08:00', CURRENT_DATE + 5 + TIME '18:00', 'ACTIVE', 4, 'Dr. Neha Fatima', NULL);

SELECT setval(pg_get_serial_sequence('staff', 'id'), (SELECT MAX(id) FROM staff));
SELECT setval(pg_get_serial_sequence('vehicles', 'id'), (SELECT MAX(id) FROM vehicles));
SELECT setval(pg_get_serial_sequence('equipment', 'id'), (SELECT MAX(id) FROM equipment));
SELECT setval(pg_get_serial_sequence('allocations', 'id'), (SELECT MAX(id) FROM allocations));
