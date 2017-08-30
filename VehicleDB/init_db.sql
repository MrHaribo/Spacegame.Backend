SET statement_timeout = 0;
SET lock_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;
SET row_security = off;

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;
COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';
SET search_path = public, pg_catalog;
SET default_tablespace = '';
SET default_with_oids = false;

CREATE ROLE vehicle_service LOGIN
  ENCRYPTED PASSWORD 'vehicle1234'
  NOSUPERUSER INHERIT NOCREATEDB NOCREATEROLE NOREPLICATION;

CREATE DATABASE vehicle_db OWNER postgres;
  
\connect vehicle_db

CREATE TABLE vehicle_configurations (
    name text NOT NULL,
    data json
);


ALTER TABLE vehicle_configurations OWNER TO postgres;

CREATE TABLE vehicles (
    user_id integer NOT NULL,
    avatar_name text NOT NULL,
    current_vehicle_index integer DEFAULT 0,
    data json[]
);


ALTER TABLE vehicles OWNER TO postgres;

COPY vehicle_configurations (name, data) FROM stdin;
Avalon	{"name":"Avalon","primaryWeapons":["LaserBlaster","LaserBlaster","LaserBlaster","LaserBlaster","LaserBlaster","LaserBlaster"],"heavyWeapons":["RocketLauncher"]}
Balance	{"name":"Balance","primaryWeapons":["LaserBlaster","LaserBlaster","LaserBlaster","LaserBlaster"],"heavyWeapons":[]}
Beetle	{"name":"Beetle","primaryWeapons":["LaserBlaster"],"heavyWeapons":[]}
Degen	{"name":"Degen","primaryWeapons":["LaserBlaster","LaserBlaster"],"heavyWeapons":[]}
Delta	{"name":"Delta","primaryWeapons":["LaserBlaster"],"heavyWeapons":[]}
Drone	{"name":"Drone","primaryWeapons":["LaserBlaster"],"heavyWeapons":[]}
Eagle	{"name":"Eagle","primaryWeapons":["LaserBlaster","LaserBlaster"],"heavyWeapons":["RocketLauncher"]}
Gaujo	{"name":"Gaujo","primaryWeapons":["LaserBlaster"],"heavyWeapons":[]}
Humble	{"name":"Humble","primaryWeapons":["LaserBlaster","LaserBlaster","LaserBlaster","LaserBlaster","LaserBlaster","LaserBlaster"],"heavyWeapons":["RocketLauncher"]}
Mice	{"name":"Mice","primaryWeapons":["LaserBlaster","LaserBlaster"],"heavyWeapons":[]}
Omega	{"name":"Omega","primaryWeapons":["LaserBlaster","LaserBlaster"],"heavyWeapons":["RocketLauncher","RocketLauncher"]}
Rhino	{"name":"Rhino","primaryWeapons":["LaserBlaster"],"heavyWeapons":[]}
Scout	{"name":"Scout","primaryWeapons":["LaserBlaster","LaserBlaster"],"heavyWeapons":[]}
Shuttle	{"name":"Shuttle","primaryWeapons":["LaserBlaster","LaserBlaster"],"heavyWeapons":[]}
Worker	{"name":"Worker","primaryWeapons":["LaserBlaster"],"heavyWeapons":[]}
\.

ALTER TABLE ONLY vehicle_configurations
    ADD CONSTRAINT vehicle_configuration_pk PRIMARY KEY (name);


ALTER TABLE ONLY vehicles
    ADD CONSTRAINT vehicles_pk PRIMARY KEY (user_id, avatar_name);


REVOKE ALL ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON SCHEMA public FROM postgres;
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO PUBLIC;


REVOKE ALL ON TABLE vehicle_configurations FROM PUBLIC;
REVOKE ALL ON TABLE vehicle_configurations FROM postgres;
GRANT ALL ON TABLE vehicle_configurations TO postgres;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE vehicle_configurations TO vehicle_service;


REVOKE ALL ON TABLE vehicles FROM PUBLIC;
REVOKE ALL ON TABLE vehicles FROM postgres;
GRANT ALL ON TABLE vehicles TO postgres;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE vehicles TO vehicle_service;


