--
-- PostgreSQL database cluster dump
--

SET default_transaction_read_only = off;

SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;

--
-- Roles
--

CREATE ROLE account_service;
ALTER ROLE account_service WITH NOSUPERUSER INHERIT NOCREATEROLE NOCREATEDB LOGIN NOREPLICATION NOBYPASSRLS PASSWORD 'md55f7e1aa4dee85bb7ae700cbac501bb72';
CREATE ROLE avatar_service;
ALTER ROLE avatar_service WITH NOSUPERUSER INHERIT NOCREATEROLE NOCREATEDB LOGIN NOREPLICATION NOBYPASSRLS PASSWORD 'md53f009fa0c69541c67d3aca38573e2e14';
CREATE ROLE item_service;
ALTER ROLE item_service WITH NOSUPERUSER INHERIT NOCREATEROLE NOCREATEDB LOGIN NOREPLICATION NOBYPASSRLS PASSWORD 'md591652768eadd0237bd0fa9e098f3af99';
CREATE ROLE region_service;
ALTER ROLE region_service WITH NOSUPERUSER INHERIT NOCREATEROLE NOCREATEDB LOGIN NOREPLICATION NOBYPASSRLS PASSWORD 'md59042f7f89a531ef324b401e1211e45a5';
CREATE ROLE shop_service;
ALTER ROLE shop_service WITH NOSUPERUSER INHERIT NOCREATEROLE NOCREATEDB LOGIN NOREPLICATION NOBYPASSRLS PASSWORD 'md59ae956683bcc14589ca168102bf3e5de';
CREATE ROLE vehicle_service;
ALTER ROLE vehicle_service WITH NOSUPERUSER INHERIT NOCREATEROLE NOCREATEDB LOGIN NOREPLICATION NOBYPASSRLS PASSWORD 'md574098c89bed5db9bcb60480a429b5053';






--
-- Database creation
--

CREATE DATABASE account_db WITH TEMPLATE = template0 OWNER = postgres;
CREATE DATABASE avatar_db WITH TEMPLATE = template0 OWNER = postgres;
CREATE DATABASE item_db WITH TEMPLATE = template0 OWNER = postgres;
CREATE DATABASE region_db WITH TEMPLATE = template0 OWNER = postgres;
CREATE DATABASE shop_db WITH TEMPLATE = template0 OWNER = postgres;
REVOKE CONNECT,TEMPORARY ON DATABASE template1 FROM PUBLIC;
GRANT CONNECT ON DATABASE template1 TO PUBLIC;
CREATE DATABASE vehicle_db WITH TEMPLATE = template0 OWNER = postgres;


\connect account_db

SET default_transaction_read_only = off;

--
-- PostgreSQL database dump
--

-- Dumped from database version 9.6.3
-- Dumped by pg_dump version 9.6.3

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;


--
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


SET search_path = public, pg_catalog;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: users; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE users (
    id integer NOT NULL,
    credentials json
);


ALTER TABLE users OWNER TO postgres;

--
-- Name: users_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE users_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE users_id_seq OWNER TO postgres;

--
-- Name: users_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE users_id_seq OWNED BY users.id;


--
-- Name: users id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY users ALTER COLUMN id SET DEFAULT nextval('users_id_seq'::regclass);


--
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY users (id, credentials) FROM stdin;
1	{"username":"Jonas","password":"test"}
\.


--
-- Name: users_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('users_id_seq', 1, true);


--
-- Name: users users_pk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY users
    ADD CONSTRAINT users_pk PRIMARY KEY (id);


--
-- Name: users; Type: ACL; Schema: public; Owner: postgres
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE users TO account_service;


--
-- Name: users_id_seq; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON SEQUENCE users_id_seq TO account_service;


--
-- PostgreSQL database dump complete
--

\connect avatar_db

SET default_transaction_read_only = off;

--
-- PostgreSQL database dump
--

-- Dumped from database version 9.6.3
-- Dumped by pg_dump version 9.6.3

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;


--
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


SET search_path = public, pg_catalog;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: avatars; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE avatars (
    user_id integer NOT NULL,
    name text NOT NULL,
    data json
);


ALTER TABLE avatars OWNER TO postgres;

--
-- Data for Name: avatars; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY avatars (user_id, name, data) FROM stdin;
1	Sepp	{"name":"Sepp","faction":{"confederateReputation":0.4,"rebelReputation":-0.3},"job":"Tank","regionID":{"types":[1],"values":[33]},"homeRegionID":{"types":[1],"values":[33]},"poiID":{"types":[],"values":[]},"landed":false,"position":{"x":0.0,"y":0.0},"credits":1000}
\.


--
-- Name: avatars avatar_name_unique; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY avatars
    ADD CONSTRAINT avatar_name_unique UNIQUE (name);


--
-- Name: avatars avatar_pk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY avatars
    ADD CONSTRAINT avatar_pk PRIMARY KEY (user_id, name);


--
-- Name: avatars; Type: ACL; Schema: public; Owner: postgres
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE avatars TO avatar_service;


--
-- PostgreSQL database dump complete
--

\connect item_db

SET default_transaction_read_only = off;

--
-- PostgreSQL database dump
--

-- Dumped from database version 9.6.3
-- Dumped by pg_dump version 9.6.3

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;


--
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


SET search_path = public, pg_catalog;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: items; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE items (
    user_id integer NOT NULL,
    location text NOT NULL,
    size integer DEFAULT 0,
    inventory json[]
);


ALTER TABLE items OWNER TO postgres;

--
-- Data for Name: items; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY items (user_id, location, size, inventory) FROM stdin;
\.


--
-- Name: items user_inventory_pk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY items
    ADD CONSTRAINT user_inventory_pk PRIMARY KEY (user_id, location);


--
-- Name: items; Type: ACL; Schema: public; Owner: postgres
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE items TO item_service;


--
-- PostgreSQL database dump complete
--

\connect postgres

SET default_transaction_read_only = off;

--
-- PostgreSQL database dump
--

-- Dumped from database version 9.6.3
-- Dumped by pg_dump version 9.6.3

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: postgres; Type: COMMENT; Schema: -; Owner: postgres
--

COMMENT ON DATABASE postgres IS 'default administrative connection database';


--
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;


--
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


--
-- Name: adminpack; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS adminpack WITH SCHEMA pg_catalog;


--
-- Name: EXTENSION adminpack; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION adminpack IS 'administrative functions for PostgreSQL';


--
-- PostgreSQL database dump complete
--

\connect region_db

SET default_transaction_read_only = off;

--
-- PostgreSQL database dump
--

-- Dumped from database version 9.6.3
-- Dumped by pg_dump version 9.6.3

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;


--
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


SET search_path = public, pg_catalog;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: master_regions; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE master_regions (
    region_id bigint NOT NULL,
    data json
);


ALTER TABLE master_regions OWNER TO postgres;

--
-- Data for Name: master_regions; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY master_regions (region_id, data) FROM stdin;
0	{"id":{"types":[1],"values":[0]},"name":"Kashyyk","worldPosition":{"x":1.422,"y":5.22},"biomName":"Blue","seed":0.45844144,"threadLevel":40,"faction":"Neutral","pois":[]}
1	{"id":{"types":[1],"values":[1]},"name":"Mendes","worldPosition":{"x":1.058,"y":6.545},"biomName":"Blue","seed":0.6334152,"threadLevel":80,"faction":"Neutral","pois":[]}
2	{"id":{"types":[1],"values":[2]},"name":"Rigel","worldPosition":{"x":-0.714,"y":5.986},"biomName":"Blue","seed":0.75177354,"threadLevel":40,"faction":"Neutral","pois":[]}
3	{"id":{"types":[1],"values":[3]},"name":"Apollo","worldPosition":{"x":-2.181,"y":7.849},"biomName":"Blue","seed":0.9880761,"threadLevel":40,"faction":"Neutral","pois":[]}
4	{"id":{"types":[1],"values":[4]},"name":"Dantuine","worldPosition":{"x":-4.25,"y":7.84},"biomName":"Blue","seed":0.07203525,"threadLevel":40,"faction":"Neutral","pois":[]}
5	{"id":{"types":[1],"values":[5]},"name":"Prelmoid","worldPosition":{"x":-8.626,"y":7.393},"biomName":"Blue","seed":0.0031926632,"threadLevel":80,"faction":"Neutral","pois":[]}
6	{"id":{"types":[1],"values":[6]},"name":"Hismonia","worldPosition":{"x":-7.83,"y":8.2},"biomName":"Blue","seed":0.17834562,"threadLevel":40,"faction":"Neutral","pois":[]}
7	{"id":{"types":[1],"values":[7]},"name":"Kabal","worldPosition":{"x":-8.03,"y":6.11},"biomName":"Blue","seed":0.27968347,"threadLevel":40,"faction":"Neutral","pois":[]}
8	{"id":{"types":[1],"values":[8]},"name":"Timosero","worldPosition":{"x":-6.34,"y":4.45},"biomName":"Blue","seed":0.539098,"threadLevel":80,"faction":"Neutral","pois":[]}
9	{"id":{"types":[1],"values":[9]},"name":"Haital","worldPosition":{"x":-7.83,"y":2.66},"biomName":"Blue","seed":0.6175031,"threadLevel":40,"faction":"Neutral","pois":[]}
10	{"id":{"types":[1],"values":[10]},"name":"Flavour","worldPosition":{"x":-5.65,"y":-6.37},"biomName":"Green","seed":0.014817715,"threadLevel":50,"faction":"Neutral","pois":[]}
11	{"id":{"types":[1],"values":[11]},"name":"Salt","worldPosition":{"x":-7.11,"y":-3.65},"biomName":"Green","seed":0.077456474,"threadLevel":80,"faction":"Neutral","pois":[]}
12	{"id":{"types":[1],"values":[12]},"name":"Dimerion","worldPosition":{"x":-7.77,"y":-6.29},"biomName":"Green","seed":0.39890718,"threadLevel":50,"faction":"Neutral","pois":[]}
13	{"id":{"types":[1],"values":[13]},"name":"Heulion","worldPosition":{"x":-5.38,"y":-2.86},"biomName":"Green","seed":0.23177344,"threadLevel":50,"faction":"Neutral","pois":[]}
14	{"id":{"types":[1],"values":[14]},"name":"Ganja","worldPosition":{"x":-5.43,"y":-8.36},"biomName":"Green","seed":0.47859138,"threadLevel":50,"faction":"Neutral","pois":[]}
15	{"id":{"types":[1],"values":[15]},"name":"Memphis","worldPosition":{"x":-1.01,"y":-5.83},"biomName":"Purple","seed":0.32190615,"threadLevel":50,"faction":"Neutral","pois":[]}
16	{"id":{"types":[1],"values":[16]},"name":"Hexania","worldPosition":{"x":1.28,"y":-8.07},"biomName":"Purple","seed":0.38687402,"threadLevel":50,"faction":"Neutral","pois":[]}
17	{"id":{"types":[1],"values":[17]},"name":"Toph","worldPosition":{"x":1.55,"y":-6.75},"biomName":"Purple","seed":0.7056373,"threadLevel":80,"faction":"Neutral","pois":[]}
18	{"id":{"types":[1],"values":[18]},"name":"Teima","worldPosition":{"x":4.4,"y":-8.15},"biomName":"Purple","seed":0.53939927,"threadLevel":50,"faction":"Neutral","pois":[]}
19	{"id":{"types":[1],"values":[19]},"name":"Amarath","worldPosition":{"x":5.64,"y":-7.2},"biomName":"Purple","seed":0.789263,"threadLevel":80,"faction":"Neutral","pois":[]}
20	{"id":{"types":[1],"values":[20]},"name":"Keimora","worldPosition":{"x":7.68,"y":-8.08},"biomName":"Purple","seed":0.050593197,"threadLevel":50,"faction":"Neutral","pois":[]}
21	{"id":{"types":[1],"values":[21]},"name":"Sodom","worldPosition":{"x":6.58,"y":-3.11},"biomName":"Red","seed":0.14388382,"threadLevel":80,"faction":"Neutral","pois":[]}
22	{"id":{"types":[1],"values":[22]},"name":"Gomorrah","worldPosition":{"x":6.07,"y":-2.14},"biomName":"Red","seed":0.36938143,"threadLevel":80,"faction":"Neutral","pois":[]}
23	{"id":{"types":[1],"values":[23]},"name":"Springfield","worldPosition":{"x":8.25,"y":2.64},"biomName":"Red","seed":0.9245384,"threadLevel":80,"faction":"Neutral","pois":[]}
24	{"id":{"types":[1],"values":[24]},"name":"Takko","worldPosition":{"x":7.27,"y":4.64},"biomName":"Red","seed":0.9403297,"threadLevel":30,"faction":"Neutral","pois":[]}
25	{"id":{"types":[1],"values":[25]},"name":"Hakkonen","worldPosition":{"x":4.81,"y":-0.22},"biomName":"Red","seed":0.16230112,"threadLevel":30,"faction":"Neutral","pois":[]}
26	{"id":{"types":[1],"values":[26]},"name":"Lanzeloto","worldPosition":{"x":2.09,"y":-1.56},"biomName":"Red","seed":0.3336135,"threadLevel":30,"faction":"Neutral","pois":[]}
27	{"id":{"types":[1],"values":[27]},"name":"Salsa","worldPosition":{"x":7.51,"y":5.7},"biomName":"Red","seed":0.4814477,"threadLevel":30,"faction":"Neutral","pois":[]}
28	{"id":{"types":[1],"values":[28]},"name":"Biedermann","worldPosition":{"x":6.5,"y":8.15},"biomName":"Plain","seed":0.013850927,"threadLevel":0,"faction":"Rebel","pois":[]}
29	{"id":{"types":[1],"values":[29]},"name":"Oddland","worldPosition":{"x":4.21,"y":7.65},"biomName":"Plain","seed":0.049887598,"threadLevel":0,"faction":"Rebel","pois":[]}
30	{"id":{"types":[1],"values":[30]},"name":"Connec","worldPosition":{"x":1.83,"y":8.49},"biomName":"Plain","seed":0.44622833,"threadLevel":0,"faction":"Rebel","pois":[]}
31	{"id":{"types":[1],"values":[31]},"name":"Hylia","worldPosition":{"x":3.1,"y":4.84},"biomName":"Plain","seed":0.41776645,"threadLevel":0,"faction":"Rebel","pois":[]}
32	{"id":{"types":[1],"values":[32]},"name":"Fuelia","worldPosition":{"x":4.9,"y":5.28},"biomName":"Plain","seed":0.4691683,"threadLevel":0,"faction":"Rebel","pois":[]}
33	{"id":{"types":[1],"values":[33]},"name":"Central","worldPosition":{"x":-1.88,"y":1.8},"biomName":"Plain","seed":0.89407396,"threadLevel":0,"faction":"Confederate","pois":[]}
34	{"id":{"types":[1],"values":[34]},"name":"Crossroads","worldPosition":{"x":4.25,"y":3.44},"biomName":"Plain","seed":0.16732198,"threadLevel":0,"faction":"Rebel","pois":[]}
35	{"id":{"types":[1],"values":[35]},"name":"Narrow","worldPosition":{"x":6.0,"y":1.79},"biomName":"Plain","seed":0.95048165,"threadLevel":0,"faction":"Neutral","pois":[]}
36	{"id":{"types":[1],"values":[36]},"name":"Edge","worldPosition":{"x":7.65,"y":-0.21},"biomName":"Plain","seed":0.9243489,"threadLevel":0,"faction":"Neutral","pois":[]}
37	{"id":{"types":[1],"values":[37]},"name":"Tango","worldPosition":{"x":0.65,"y":2.88},"biomName":"Plain","seed":0.69915926,"threadLevel":0,"faction":"Confederate","pois":[]}
38	{"id":{"types":[1],"values":[38]},"name":"Dante","worldPosition":{"x":1.72,"y":1.19},"biomName":"Plain","seed":0.6572602,"threadLevel":0,"faction":"Confederate","pois":[]}
39	{"id":{"types":[1],"values":[39]},"name":"Saulon","worldPosition":{"x":-3.22,"y":5.47},"biomName":"Plain","seed":0.672292,"threadLevel":0,"faction":"Confederate","pois":[]}
40	{"id":{"types":[1],"values":[40]},"name":"Jaul","worldPosition":{"x":-5.72,"y":6.53},"biomName":"Plain","seed":0.48410094,"threadLevel":0,"faction":"Neutral","pois":[]}
41	{"id":{"types":[1],"values":[41]},"name":"Teardown","worldPosition":{"x":-5.19,"y":1.02},"biomName":"Plain","seed":0.47646874,"threadLevel":0,"faction":"Confederate","pois":[]}
42	{"id":{"types":[1],"values":[42]},"name":"Border","worldPosition":{"x":-7.25,"y":-1.14},"biomName":"Plain","seed":0.928969,"threadLevel":0,"faction":"Neutral","pois":[]}
43	{"id":{"types":[1],"values":[43]},"name":"Hammer","worldPosition":{"x":-2.85,"y":2.82},"biomName":"Plain","seed":0.6915619,"threadLevel":0,"faction":"Confederate","pois":[]}
44	{"id":{"types":[1],"values":[44]},"name":"Twink","worldPosition":{"x":-1.25,"y":-0.08},"biomName":"Plain","seed":0.124712646,"threadLevel":0,"faction":"Confederate","pois":[]}
45	{"id":{"types":[1],"values":[45]},"name":"Gazzlow","worldPosition":{"x":-3.45,"y":-1.57},"biomName":"Plain","seed":0.7692348,"threadLevel":0,"faction":"Confederate","pois":[]}
46	{"id":{"types":[1],"values":[46]},"name":"Himalaya","worldPosition":{"x":-7.66,"y":-6.9},"biomName":"Plain","seed":0.46816975,"threadLevel":80,"faction":"Neutral","pois":[]}
47	{"id":{"types":[1],"values":[47]},"name":"Worldsend","worldPosition":{"x":-1.96,"y":-8.17},"biomName":"Plain","seed":0.74604166,"threadLevel":0,"faction":"Neutral","pois":[]}
48	{"id":{"types":[1],"values":[48]},"name":"Chronius","worldPosition":{"x":-3.55,"y":-5.46},"biomName":"Plain","seed":0.23598689,"threadLevel":0,"faction":"Neutral","pois":[]}
49	{"id":{"types":[1],"values":[49]},"name":"Sandwich","worldPosition":{"x":-0.13,"y":-3.69},"biomName":"Plain","seed":0.41342968,"threadLevel":0,"faction":"Neutral","pois":[]}
50	{"id":{"types":[1],"values":[50]},"name":"Ying","worldPosition":{"x":3.5,"y":-3.56},"biomName":"Plain","seed":0.49703407,"threadLevel":0,"faction":"Neutral","pois":[]}
51	{"id":{"types":[1],"values":[51]},"name":"Yang","worldPosition":{"x":3.29,"y":-5.29},"biomName":"Plain","seed":0.89058983,"threadLevel":0,"faction":"Neutral","pois":[]}
52	{"id":{"types":[1],"values":[52]},"name":"Cornaria","worldPosition":{"x":5.49,"y":-5.36},"biomName":"Plain","seed":0.5461825,"threadLevel":0,"faction":"Neutral","pois":[]}
53	{"id":{"types":[1],"values":[53]},"name":"Yavin","worldPosition":{"x":7.76,"y":-5.72},"biomName":"Plain","seed":0.9232553,"threadLevel":0,"faction":"Neutral","pois":[]}
\.


--
-- Name: master_regions region_pk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY master_regions
    ADD CONSTRAINT region_pk PRIMARY KEY (region_id);


--
-- Name: master_regions; Type: ACL; Schema: public; Owner: postgres
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE master_regions TO region_service;


--
-- PostgreSQL database dump complete
--

\connect shop_db

SET default_transaction_read_only = off;

--
-- PostgreSQL database dump
--

-- Dumped from database version 9.6.3
-- Dumped by pg_dump version 9.6.3

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;


--
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


SET search_path = public, pg_catalog;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: shops; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE shops (
    faction text NOT NULL,
    items json[],
    rank_restrictions integer[]
);


ALTER TABLE shops OWNER TO postgres;

--
-- Data for Name: shops; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY shops (faction, items, rank_restrictions) FROM stdin;
Confederate	{"{\\"name\\":\\"Beetle\\",\\"type\\":0,\\"quantity\\":0,\\"maxStackSize\\":0,\\"price\\":100}","{\\"name\\":\\"Scout\\",\\"type\\":0,\\"quantity\\":0,\\"maxStackSize\\":0,\\"price\\":700}","{\\"name\\":\\"Omega\\",\\"type\\":0,\\"quantity\\":0,\\"maxStackSize\\":0,\\"price\\":650}","{\\"name\\":\\"Shuttle\\",\\"type\\":0,\\"quantity\\":0,\\"maxStackSize\\":0,\\"price\\":480}","{\\"name\\":\\"Avalon\\",\\"type\\":0,\\"quantity\\":0,\\"maxStackSize\\":0,\\"price\\":2000}","{\\"name\\":\\"LaserBlaster\\",\\"type\\":1,\\"quantity\\":0,\\"maxStackSize\\":0,\\"price\\":80}","{\\"name\\":\\"RocketLauncher\\",\\"type\\":2,\\"quantity\\":0,\\"maxStackSize\\":0,\\"price\\":100}","{\\"name\\":\\"FireBlaster\\",\\"type\\":1,\\"quantity\\":0,\\"maxStackSize\\":0,\\"price\\":100}","{\\"name\\":\\"IceBlaster\\",\\"type\\":1,\\"quantity\\":0,\\"maxStackSize\\":0,\\"price\\":100}","{\\"name\\":\\"GaussBeam\\",\\"type\\":1,\\"quantity\\":0,\\"maxStackSize\\":0,\\"price\\":300}","{\\"name\\":\\"TorpedoPod\\",\\"type\\":2,\\"quantity\\":0,\\"maxStackSize\\":0,\\"price\\":100}"}	{0,1,1,1,2,1,1,2,2,2,3}
Rebel	{"{\\"name\\":\\"Mice\\",\\"type\\":0,\\"quantity\\":0,\\"maxStackSize\\":0,\\"price\\":100}","{\\"name\\":\\"Eagle\\",\\"type\\":0,\\"quantity\\":0,\\"maxStackSize\\":0,\\"price\\":600}","{\\"name\\":\\"Delta\\",\\"type\\":0,\\"quantity\\":0,\\"maxStackSize\\":0,\\"price\\":720}","{\\"name\\":\\"Worker\\",\\"type\\":0,\\"quantity\\":0,\\"maxStackSize\\":0,\\"price\\":550}","{\\"name\\":\\"Humble\\",\\"type\\":0,\\"quantity\\":0,\\"maxStackSize\\":0,\\"price\\":2000}","{\\"name\\":\\"LaserBlaster\\",\\"type\\":1,\\"quantity\\":0,\\"maxStackSize\\":0,\\"price\\":80}","{\\"name\\":\\"RocketLauncher\\",\\"type\\":2,\\"quantity\\":0,\\"maxStackSize\\":0,\\"price\\":100}","{\\"name\\":\\"FireBlaster\\",\\"type\\":1,\\"quantity\\":0,\\"maxStackSize\\":0,\\"price\\":100}","{\\"name\\":\\"IceBlaster\\",\\"type\\":1,\\"quantity\\":0,\\"maxStackSize\\":0,\\"price\\":100}","{\\"name\\":\\"GaussBeam\\",\\"type\\":1,\\"quantity\\":0,\\"maxStackSize\\":0,\\"price\\":300}","{\\"name\\":\\"TorpedoPod\\",\\"type\\":2,\\"quantity\\":0,\\"maxStackSize\\":0,\\"price\\":100}"}	{0,1,1,1,2,1,1,2,2,2,3}
Neutral	{"{\\"name\\":\\"Gaujo\\",\\"type\\":0,\\"quantity\\":0,\\"maxStackSize\\":0,\\"price\\":1200}","{\\"name\\":\\"Rhino\\",\\"type\\":0,\\"quantity\\":0,\\"maxStackSize\\":0,\\"price\\":1200}","{\\"name\\":\\"Balance\\",\\"type\\":0,\\"quantity\\":0,\\"maxStackSize\\":0,\\"price\\":2400}","{\\"name\\":\\"Drone\\",\\"type\\":0,\\"quantity\\":0,\\"maxStackSize\\":0,\\"price\\":50}","{\\"name\\":\\"Degen\\",\\"type\\":0,\\"quantity\\":0,\\"maxStackSize\\":0,\\"price\\":650}","{\\"name\\":\\"RocketLauncher\\",\\"type\\":2,\\"quantity\\":0,\\"maxStackSize\\":0,\\"price\\":100}","{\\"name\\":\\"LaserBlaster\\",\\"type\\":1,\\"quantity\\":0,\\"maxStackSize\\":0,\\"price\\":80}"}	{0,0,0,0,0,0,0}
\.


--
-- Name: shops shop_pk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY shops
    ADD CONSTRAINT shop_pk PRIMARY KEY (faction);


--
-- Name: shops; Type: ACL; Schema: public; Owner: postgres
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE shops TO shop_service;


--
-- PostgreSQL database dump complete
--

\connect template1

SET default_transaction_read_only = off;

--
-- PostgreSQL database dump
--

-- Dumped from database version 9.6.3
-- Dumped by pg_dump version 9.6.3

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: template1; Type: COMMENT; Schema: -; Owner: postgres
--

COMMENT ON DATABASE template1 IS 'default template for new databases';


--
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;


--
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


--
-- PostgreSQL database dump complete
--

\connect vehicle_db

SET default_transaction_read_only = off;

--
-- PostgreSQL database dump
--

-- Dumped from database version 9.6.3
-- Dumped by pg_dump version 9.6.3

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;


--
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


SET search_path = public, pg_catalog;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: vehicle_configurations; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE vehicle_configurations (
    name text NOT NULL,
    data json
);


ALTER TABLE vehicle_configurations OWNER TO postgres;

--
-- Name: vehicles; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE vehicles (
    user_id integer NOT NULL,
    avatar_name text NOT NULL,
    current_vehicle_index integer DEFAULT 0,
    data json[]
);


ALTER TABLE vehicles OWNER TO postgres;

--
-- Data for Name: vehicle_configurations; Type: TABLE DATA; Schema: public; Owner: postgres
--

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


--
-- Data for Name: vehicles; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY vehicles (user_id, avatar_name, current_vehicle_index, data) FROM stdin;
1	Sepp	0	{"{\\"name\\":\\"Beetle\\",\\"primaryWeapons\\":[\\"LaserBlaster\\"],\\"heavyWeapons\\":[]}"}
\.


--
-- Name: vehicle_configurations vehicle_configuration_pk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY vehicle_configurations
    ADD CONSTRAINT vehicle_configuration_pk PRIMARY KEY (name);


--
-- Name: vehicles vehicles_pk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY vehicles
    ADD CONSTRAINT vehicles_pk PRIMARY KEY (user_id, avatar_name);


--
-- Name: vehicle_configurations; Type: ACL; Schema: public; Owner: postgres
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE vehicle_configurations TO vehicle_service;


--
-- Name: vehicles; Type: ACL; Schema: public; Owner: postgres
--

GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE vehicles TO vehicle_service;


--
-- PostgreSQL database dump complete
--

--
-- PostgreSQL database cluster dump complete
--

