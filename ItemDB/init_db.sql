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

CREATE ROLE item_service LOGIN
  ENCRYPTED PASSWORD 'item1234'
  NOSUPERUSER INHERIT NOCREATEDB NOCREATEROLE NOREPLICATION;

CREATE DATABASE item_db OWNER postgres;
  
\connect item_db

CREATE TABLE items (
    user_id integer NOT NULL,
    location text NOT NULL,
    size integer DEFAULT 0,
    inventory json[]
);


ALTER TABLE items OWNER TO postgres;

ALTER TABLE ONLY items
    ADD CONSTRAINT user_inventory_pk PRIMARY KEY (user_id, location);


REVOKE ALL ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON SCHEMA public FROM postgres;
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO PUBLIC;


REVOKE ALL ON TABLE items FROM PUBLIC;
REVOKE ALL ON TABLE items FROM postgres;
GRANT ALL ON TABLE items TO postgres;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE items TO item_service;


