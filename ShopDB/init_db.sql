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

CREATE ROLE shop_service LOGIN
  ENCRYPTED PASSWORD 'shop1234'
  NOSUPERUSER INHERIT NOCREATEDB NOCREATEROLE NOREPLICATION;

CREATE DATABASE shop_db OWNER postgres;
  
\connect shop_db

CREATE TABLE shops (
    faction text NOT NULL,
    items json[],
    rank_restrictions integer[]
);


ALTER TABLE shops OWNER TO postgres;


COPY shops (faction, items, rank_restrictions) FROM stdin;
Confederate	{"{\\"name\\":\\"Beetle\\",\\"type\\":0,\\"quantity\\":0,\\"maxStackSize\\":0,\\"price\\":100}","{\\"name\\":\\"Scout\\",\\"type\\":0,\\"quantity\\":0,\\"maxStackSize\\":0,\\"price\\":700}","{\\"name\\":\\"Omega\\",\\"type\\":0,\\"quantity\\":0,\\"maxStackSize\\":0,\\"price\\":650}","{\\"name\\":\\"Shuttle\\",\\"type\\":0,\\"quantity\\":0,\\"maxStackSize\\":0,\\"price\\":480}","{\\"name\\":\\"Avalon\\",\\"type\\":0,\\"quantity\\":0,\\"maxStackSize\\":0,\\"price\\":2000}","{\\"name\\":\\"LaserBlaster\\",\\"type\\":1,\\"quantity\\":0,\\"maxStackSize\\":0,\\"price\\":80}","{\\"name\\":\\"RocketLauncher\\",\\"type\\":2,\\"quantity\\":0,\\"maxStackSize\\":0,\\"price\\":100}","{\\"name\\":\\"FireBlaster\\",\\"type\\":1,\\"quantity\\":0,\\"maxStackSize\\":0,\\"price\\":100}","{\\"name\\":\\"IceBlaster\\",\\"type\\":1,\\"quantity\\":0,\\"maxStackSize\\":0,\\"price\\":100}","{\\"name\\":\\"GaussBeam\\",\\"type\\":1,\\"quantity\\":0,\\"maxStackSize\\":0,\\"price\\":300}","{\\"name\\":\\"TorpedoPod\\",\\"type\\":2,\\"quantity\\":0,\\"maxStackSize\\":0,\\"price\\":100}"}	{0,1,1,1,2,1,1,2,2,2,3}
Rebel	{"{\\"name\\":\\"Mice\\",\\"type\\":0,\\"quantity\\":0,\\"maxStackSize\\":0,\\"price\\":100}","{\\"name\\":\\"Eagle\\",\\"type\\":0,\\"quantity\\":0,\\"maxStackSize\\":0,\\"price\\":600}","{\\"name\\":\\"Delta\\",\\"type\\":0,\\"quantity\\":0,\\"maxStackSize\\":0,\\"price\\":720}","{\\"name\\":\\"Worker\\",\\"type\\":0,\\"quantity\\":0,\\"maxStackSize\\":0,\\"price\\":550}","{\\"name\\":\\"Humble\\",\\"type\\":0,\\"quantity\\":0,\\"maxStackSize\\":0,\\"price\\":2000}","{\\"name\\":\\"LaserBlaster\\",\\"type\\":1,\\"quantity\\":0,\\"maxStackSize\\":0,\\"price\\":80}","{\\"name\\":\\"RocketLauncher\\",\\"type\\":2,\\"quantity\\":0,\\"maxStackSize\\":0,\\"price\\":100}","{\\"name\\":\\"FireBlaster\\",\\"type\\":1,\\"quantity\\":0,\\"maxStackSize\\":0,\\"price\\":100}","{\\"name\\":\\"IceBlaster\\",\\"type\\":1,\\"quantity\\":0,\\"maxStackSize\\":0,\\"price\\":100}","{\\"name\\":\\"GaussBeam\\",\\"type\\":1,\\"quantity\\":0,\\"maxStackSize\\":0,\\"price\\":300}","{\\"name\\":\\"TorpedoPod\\",\\"type\\":2,\\"quantity\\":0,\\"maxStackSize\\":0,\\"price\\":100}"}	{0,1,1,1,2,1,1,2,2,2,3}
Neutral	{"{\\"name\\":\\"Gaujo\\",\\"type\\":0,\\"quantity\\":0,\\"maxStackSize\\":0,\\"price\\":1200}","{\\"name\\":\\"Rhino\\",\\"type\\":0,\\"quantity\\":0,\\"maxStackSize\\":0,\\"price\\":1200}","{\\"name\\":\\"Balance\\",\\"type\\":0,\\"quantity\\":0,\\"maxStackSize\\":0,\\"price\\":2400}","{\\"name\\":\\"Drone\\",\\"type\\":0,\\"quantity\\":0,\\"maxStackSize\\":0,\\"price\\":50}","{\\"name\\":\\"Degen\\",\\"type\\":0,\\"quantity\\":0,\\"maxStackSize\\":0,\\"price\\":650}","{\\"name\\":\\"RocketLauncher\\",\\"type\\":2,\\"quantity\\":0,\\"maxStackSize\\":0,\\"price\\":100}","{\\"name\\":\\"LaserBlaster\\",\\"type\\":1,\\"quantity\\":0,\\"maxStackSize\\":0,\\"price\\":80}"}	{0,0,0,0,0,0,0}
\.



ALTER TABLE ONLY shops
    ADD CONSTRAINT shop_pk PRIMARY KEY (faction);


REVOKE ALL ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON SCHEMA public FROM postgres;
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO PUBLIC;



REVOKE ALL ON TABLE shops FROM PUBLIC;
REVOKE ALL ON TABLE shops FROM postgres;
GRANT ALL ON TABLE shops TO postgres;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE shops TO shop_service;

