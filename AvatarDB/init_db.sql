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
  
------------- ADD YOUR DATABASE INITIALIZATION HERE ---------------

CREATE ROLE avatar_service LOGIN
  ENCRYPTED PASSWORD 'avatar1234'
  NOSUPERUSER INHERIT NOCREATEDB NOCREATEROLE NOREPLICATION;

CREATE DATABASE avatar_db OWNER postgres;
  
\connect avatar_db

CREATE TABLE avatars (
    user_id integer NOT NULL,
    name text NOT NULL,
    data json
);

ALTER TABLE avatars OWNER TO postgres;

ALTER TABLE ONLY avatars
    ADD CONSTRAINT avatar_name_unique UNIQUE (name);

ALTER TABLE ONLY avatars
    ADD CONSTRAINT avatar_pk PRIMARY KEY (user_id, name);

REVOKE ALL ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON SCHEMA public FROM postgres;
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO PUBLIC;

REVOKE ALL ON TABLE avatars FROM PUBLIC;
REVOKE ALL ON TABLE avatars FROM postgres;
GRANT ALL ON TABLE avatars TO postgres;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE avatars TO avatar_service;

-------------------------------------------------------------------