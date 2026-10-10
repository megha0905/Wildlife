-- auth-service schema (CONTRACTS.md 7.1)

CREATE TABLE app_users (
    id             BIGSERIAL PRIMARY KEY,
    username       VARCHAR(50)  NOT NULL,
    password_hash  VARCHAR(100) NOT NULL,
    full_name      VARCHAR(120) NOT NULL,
    email          VARCHAR(150) NOT NULL,
    staff_id       BIGINT,                      -- resource-service staff id (no FK across services)
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP    NOT NULL DEFAULT LOCALTIMESTAMP,
    last_login_at  TIMESTAMP
);

CREATE UNIQUE INDEX ux_app_users_username ON app_users (LOWER(username));

CREATE TABLE user_roles (
    user_id  BIGINT      NOT NULL REFERENCES app_users (id) ON DELETE CASCADE,
    role     VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN', 'FOREST_OFFICER', 'RANGER', 'VET', 'ANALYST')),
    PRIMARY KEY (user_id, role)
);

-- ---------------------------------------------------------------- seed (CONTRACTS.md 12)
-- Development passwords: admin/Admin@123, officer/Officer@123, ranger/Ranger@123, vet/Vet@123, analyst/Analyst@123
INSERT INTO app_users (id, username, password_hash, full_name, email, staff_id) VALUES
 (1, 'admin',   '$2a$10$2Ps6yyLfA/bjfMpVamHCkOWTkoOFH4BJcnoM./J.AS0KPEQNOZ1uq', 'Srishti Sao', 'admin@wildguard.local',   NULL),
 (2, 'officer', '$2a$10$fHKJb40mJ0hLrZL763ACde153EwPyIoAmK53aEDFNMP7daBLrWxFi', 'Megha Gupta',         'officer@wildguard.local', 1),
 (3, 'ranger',  '$2a$10$uA/JhpskMFoQUeSxzWac5ORHTnl47VCX8dq8VVaT.09gG8XZSHD6W', 'Soni Kumari',           'ranger@wildguard.local',  2),
 (4, 'vet',     '$2a$10$7pMiZDTUB.mv1zKc1v6dHudDLGmqTI9UPK2M7v1aufbsiw5FWY2CS', 'Dr. Neha Fatima',       'vet@wildguard.local',     4),
 (5, 'analyst', '$2a$10$K.FsCuxTpnm.53oYY7FQxOxyJhpZTkaD9HcuZCn.XMYweqnDnKwUa', 'Sakshi Rathi',          'analyst@wildguard.local', NULL);

INSERT INTO user_roles (user_id, role) VALUES
 (1, 'ADMIN'),
 (2, 'FOREST_OFFICER'),
 (3, 'RANGER'),
 (4, 'VET'),
 (5, 'ANALYST');

SELECT setval(pg_get_serial_sequence('app_users', 'id'), (SELECT MAX(id) FROM app_users));
