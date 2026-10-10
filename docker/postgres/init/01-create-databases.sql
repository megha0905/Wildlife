-- Creates one database per service (see CONTRACTS.md section 1).
-- Runs automatically the first time the PostgreSQL container starts with an empty volume.
-- Tables are created later by each service's Flyway migration (V1__init.sql).
-- The databases are owned by the POSTGRES_USER from docker-compose, which the services log in as.

CREATE DATABASE auth_db;
CREATE DATABASE habitat_db;
CREATE DATABASE wildlife_db;
CREATE DATABASE resource_db;
CREATE DATABASE patrol_db;
CREATE DATABASE incident_db;
CREATE DATABASE rescue_db;
CREATE DATABASE programme_db;
