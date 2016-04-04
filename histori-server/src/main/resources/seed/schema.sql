--
-- PostgreSQL database dump
--

SET statement_timeout = 0;
SET lock_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;

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
-- Name: account; Type: TABLE; Schema: public; Owner: histori; Tablespace: 
--

CREATE TABLE account (
    uuid character varying(100) NOT NULL,
    ctime bigint NOT NULL,
    mtime bigint NOT NULL,
    name character varying(100),
    admin boolean NOT NULL,
    auth_id character varying(30),
    canonical_email character varying(200) NOT NULL,
    email character varying(300),
    email_verification_code character varying(100),
    email_verification_code_created_at bigint,
    email_verified boolean NOT NULL,
    first_name character varying(200) NOT NULL,
    hashed_password character varying(200) NOT NULL,
    reset_token character varying(30),
    reset_token_ctime bigint,
    last_login bigint,
    last_name character varying(200) NOT NULL,
    locale character varying(140),
    mobile_phone character varying(130) NOT NULL,
    mobile_phone_country_code character varying(50) NOT NULL,
    suspended boolean NOT NULL,
    two_factor boolean NOT NULL,
    anonymous boolean NOT NULL,
    subscriber boolean NOT NULL
);


ALTER TABLE account OWNER TO histori;

--
-- Name: audit_log; Type: TABLE; Schema: public; Owner: histori; Tablespace: 
--

CREATE TABLE audit_log (
    uuid character varying(100) NOT NULL,
    ctime bigint NOT NULL,
    mtime bigint NOT NULL,
    context character varying(200) NOT NULL,
    name character varying(100) NOT NULL,
    notes character varying(16000) NOT NULL,
    user_agent character varying(1000) NOT NULL
);


ALTER TABLE audit_log OWNER TO histori;

--
-- Name: bookmark; Type: TABLE; Schema: public; Owner: histori; Tablespace: 
--

CREATE TABLE bookmark (
    uuid character varying(100) NOT NULL,
    ctime bigint NOT NULL,
    mtime bigint NOT NULL,
    owner character varying(100) NOT NULL,
    json character varying(16000) NOT NULL,
    name character varying(1024) NOT NULL
);


ALTER TABLE bookmark OWNER TO histori;

--
-- Name: map_image; Type: TABLE; Schema: public; Owner: histori; Tablespace: 
--

CREATE TABLE map_image (
    uuid character varying(100) NOT NULL,
    ctime bigint NOT NULL,
    mtime bigint NOT NULL,
    owner character varying(100) NOT NULL,
    file_name character varying(1024) NOT NULL,
    uri character varying(1024) NOT NULL
);


ALTER TABLE map_image OWNER TO histori;

--
-- Name: nexus; Type: TABLE; Schema: public; Owner: histori; Tablespace: 
--

CREATE TABLE nexus (
    uuid character varying(100) NOT NULL,
    ctime bigint NOT NULL,
    mtime bigint NOT NULL,
    owner character varying(100) NOT NULL,
    markdown character varying(100000),
    origin character varying(100),
    version integer NOT NULL,
    visibility character varying(20) NOT NULL,
    authoritative boolean NOT NULL,
    east double precision NOT NULL,
    north double precision NOT NULL,
    south double precision NOT NULL,
    west double precision NOT NULL,
    canonical_name character varying(1024) NOT NULL,
    geo_json character varying(64000) NOT NULL,
    name character varying(1024) NOT NULL,
    nexus_type character varying(1024),
    tags_json character varying(1048576),
    end_day smallint,
    end_hour smallint,
    end_instant numeric(29,0),
    end_minute smallint,
    end_month smallint,
    end_second smallint,
    end_year bigint,
    start_day smallint,
    start_hour smallint,
    start_instant numeric(29,0) NOT NULL,
    start_minute smallint,
    start_month smallint,
    start_second smallint,
    start_year bigint
);


ALTER TABLE nexus OWNER TO histori;

--
-- Name: nexus_archive; Type: TABLE; Schema: public; Owner: histori; Tablespace: 
--

CREATE TABLE nexus_archive (
    uuid character varying(100) NOT NULL,
    ctime bigint NOT NULL,
    mtime bigint NOT NULL,
    owner character varying(100) NOT NULL,
    markdown character varying(100000),
    origin character varying(100),
    version integer NOT NULL,
    visibility character varying(20) NOT NULL,
    authoritative boolean NOT NULL,
    east double precision NOT NULL,
    north double precision NOT NULL,
    south double precision NOT NULL,
    west double precision NOT NULL,
    canonical_name character varying(1024) NOT NULL,
    geo_json character varying(64000) NOT NULL,
    name character varying(1024) NOT NULL,
    nexus_type character varying(1024),
    tags_json character varying(1048576),
    end_day smallint,
    end_hour smallint,
    end_instant numeric(29,0),
    end_minute smallint,
    end_month smallint,
    end_second smallint,
    end_year bigint,
    start_day smallint,
    start_hour smallint,
    start_instant numeric(29,0) NOT NULL,
    start_minute smallint,
    start_month smallint,
    start_second smallint,
    start_year bigint,
    identifier character varying(1000) NOT NULL
);


ALTER TABLE nexus_archive OWNER TO histori;

--
-- Name: permalink; Type: TABLE; Schema: public; Owner: histori; Tablespace: 
--

CREATE TABLE permalink (
    name character varying(100) NOT NULL,
    ctime bigint NOT NULL,
    mtime bigint NOT NULL,
    json character varying(16000) NOT NULL
);


ALTER TABLE permalink OWNER TO histori;

--
-- Name: search_query; Type: TABLE; Schema: public; Owner: histori; Tablespace: 
--

CREATE TABLE search_query (
    uuid character varying(100) NOT NULL,
    ctime bigint NOT NULL,
    mtime bigint NOT NULL,
    blocked_owners character varying(255),
    east double precision NOT NULL,
    north double precision NOT NULL,
    south double precision NOT NULL,
    west double precision NOT NULL,
    nexus_sort_order character varying(255),
    preferred_owners character varying(255),
    query character varying(1024),
    summary_sort_order character varying(255),
    end_day smallint,
    end_hour smallint,
    end_instant numeric(29,0),
    end_minute smallint,
    end_month smallint,
    end_second smallint,
    end_year bigint,
    start_day smallint,
    start_hour smallint,
    start_instant numeric(29,0) NOT NULL,
    start_minute smallint,
    start_month smallint,
    start_second smallint,
    start_year bigint,
    use_cache boolean NOT NULL,
    visibility character varying(255)
);


ALTER TABLE search_query OWNER TO histori;

--
-- Name: shard; Type: TABLE; Schema: public; Owner: histori; Tablespace: 
--

CREATE TABLE shard (
    uuid character varying(100) NOT NULL,
    ctime bigint NOT NULL,
    mtime bigint NOT NULL,
    allow_read boolean NOT NULL,
    allow_write boolean NOT NULL,
    logical_end integer NOT NULL,
    logical_start integer NOT NULL,
    shard_set character varying(1024),
    url character varying(1024)
);


ALTER TABLE shard OWNER TO histori;

--
-- Name: super_nexus; Type: TABLE; Schema: public; Owner: histori; Tablespace: 
--

CREATE TABLE super_nexus (
    uuid character varying(100) NOT NULL,
    ctime bigint NOT NULL,
    mtime bigint NOT NULL,
    east double precision NOT NULL,
    north double precision NOT NULL,
    south double precision NOT NULL,
    west double precision NOT NULL,
    canonical_name character varying(1024) NOT NULL,
    dirty boolean NOT NULL,
    name character varying(1024) NOT NULL,
    owner character varying(100),
    end_day smallint,
    end_hour smallint,
    end_instant numeric(29,0),
    end_minute smallint,
    end_month smallint,
    end_second smallint,
    end_year bigint,
    start_day smallint,
    start_hour smallint,
    start_instant numeric(29,0) NOT NULL,
    start_minute smallint,
    start_month smallint,
    start_second smallint,
    start_year bigint,
    visibility character varying(20) NOT NULL,
    down_votes bigint NOT NULL,
    tally bigint NOT NULL,
    up_votes bigint NOT NULL,
    vote_count bigint NOT NULL
);


ALTER TABLE super_nexus OWNER TO histori;

--
-- Name: tag; Type: TABLE; Schema: public; Owner: histori; Tablespace: 
--

CREATE TABLE tag (
    canonical_name character varying(1024) NOT NULL,
    alias_for character varying(1024),
    name character varying(1024) NOT NULL,
    tag_type character varying(1024)
);


ALTER TABLE tag OWNER TO histori;

--
-- Name: tag_type; Type: TABLE; Schema: public; Owner: histori; Tablespace: 
--

CREATE TABLE tag_type (
    canonical_name character varying(1024) NOT NULL,
    alias_for character varying(1024),
    name character varying(1024) NOT NULL,
    schema_json character varying(32000)
);


ALTER TABLE tag_type OWNER TO histori;

--
-- Name: vote; Type: TABLE; Schema: public; Owner: histori; Tablespace: 
--

CREATE TABLE vote (
    uuid character varying(100) NOT NULL,
    ctime bigint NOT NULL,
    mtime bigint NOT NULL,
    owner character varying(100) NOT NULL,
    entity character varying(100) NOT NULL,
    version integer NOT NULL,
    vote smallint NOT NULL,
    CONSTRAINT vote_vote_check CHECK (((vote >= (-1)) AND (vote <= 1)))
);


ALTER TABLE vote OWNER TO histori;

--
-- Name: vote_archive; Type: TABLE; Schema: public; Owner: histori; Tablespace: 
--

CREATE TABLE vote_archive (
    uuid character varying(100) NOT NULL,
    ctime bigint NOT NULL,
    mtime bigint NOT NULL,
    owner character varying(100) NOT NULL,
    entity character varying(100) NOT NULL,
    version integer NOT NULL,
    vote smallint NOT NULL,
    identifier character varying(1000) NOT NULL,
    CONSTRAINT vote_archive_vote_check CHECK (((vote >= (-1)) AND (vote <= 1)))
);


ALTER TABLE vote_archive OWNER TO histori;

--
-- Data for Name: account; Type: TABLE DATA; Schema: public; Owner: histori
--

COPY account (uuid, ctime, mtime, name, admin, auth_id, canonical_email, email, email_verification_code, email_verification_code_created_at, email_verified, first_name, hashed_password, reset_token, reset_token_ctime, last_login, last_name, locale, mobile_phone, mobile_phone_country_code, suspended, two_factor, anonymous, subscriber) FROM stdin;
\.


--
-- Data for Name: audit_log; Type: TABLE DATA; Schema: public; Owner: histori
--

COPY audit_log (uuid, ctime, mtime, context, name, notes, user_agent) FROM stdin;
\.


--
-- Data for Name: bookmark; Type: TABLE DATA; Schema: public; Owner: histori
--

COPY bookmark (uuid, ctime, mtime, owner, json, name) FROM stdin;
\.


--
-- Data for Name: map_image; Type: TABLE DATA; Schema: public; Owner: histori
--

COPY map_image (uuid, ctime, mtime, owner, file_name, uri) FROM stdin;
\.


--
-- Data for Name: nexus; Type: TABLE DATA; Schema: public; Owner: histori
--

COPY nexus (uuid, ctime, mtime, owner, markdown, origin, version, visibility, authoritative, east, north, south, west, canonical_name, geo_json, name, nexus_type, tags_json, end_day, end_hour, end_instant, end_minute, end_month, end_second, end_year, start_day, start_hour, start_instant, start_minute, start_month, start_second, start_year) FROM stdin;
\.


--
-- Data for Name: nexus_archive; Type: TABLE DATA; Schema: public; Owner: histori
--

COPY nexus_archive (uuid, ctime, mtime, owner, markdown, origin, version, visibility, authoritative, east, north, south, west, canonical_name, geo_json, name, nexus_type, tags_json, end_day, end_hour, end_instant, end_minute, end_month, end_second, end_year, start_day, start_hour, start_instant, start_minute, start_month, start_second, start_year, identifier) FROM stdin;
\.


--
-- Data for Name: permalink; Type: TABLE DATA; Schema: public; Owner: histori
--

COPY permalink (name, ctime, mtime, json) FROM stdin;
\.


--
-- Data for Name: search_query; Type: TABLE DATA; Schema: public; Owner: histori
--

COPY search_query (uuid, ctime, mtime, blocked_owners, east, north, south, west, nexus_sort_order, preferred_owners, query, summary_sort_order, end_day, end_hour, end_instant, end_minute, end_month, end_second, end_year, start_day, start_hour, start_instant, start_minute, start_month, start_second, start_year, use_cache, visibility) FROM stdin;
\.


--
-- Data for Name: shard; Type: TABLE DATA; Schema: public; Owner: histori
--

COPY shard (uuid, ctime, mtime, allow_read, allow_write, logical_end, logical_start, shard_set, url) FROM stdin;
\.


--
-- Data for Name: super_nexus; Type: TABLE DATA; Schema: public; Owner: histori
--

COPY super_nexus (uuid, ctime, mtime, east, north, south, west, canonical_name, dirty, name, owner, end_day, end_hour, end_instant, end_minute, end_month, end_second, end_year, start_day, start_hour, start_instant, start_minute, start_month, start_second, start_year, visibility, down_votes, tally, up_votes, vote_count) FROM stdin;
\.


--
-- Data for Name: tag; Type: TABLE DATA; Schema: public; Owner: histori
--

COPY tag (canonical_name, alias_for, name, tag_type) FROM stdin;
\.


--
-- Data for Name: tag_type; Type: TABLE DATA; Schema: public; Owner: histori
--

COPY tag_type (canonical_name, alias_for, name, schema_json) FROM stdin;
world-actor	\N	World Actor	{\n  "fields" : [ {\n    "name" : "name",\n    "fieldType" : "world_actor",\n    "required" : false,\n    "multiple" : false\n  }, {\n    "name" : "role",\n    "fieldType" : "role_type",\n    "required" : false,\n    "multiple" : true\n  } ]\n}
person	\N	Person	{\n  "fields" : [ {\n    "name" : "name",\n    "fieldType" : "person",\n    "required" : false,\n    "multiple" : false\n  }, {\n    "name" : "role",\n    "fieldType" : "role_type",\n    "required" : false,\n    "multiple" : true\n  }, {\n    "name" : "world_actor",\n    "fieldType" : "world_actor",\n    "required" : false,\n    "multiple" : true\n  } ]\n}
event	\N	Event	{\n  "fields" : [ {\n    "name" : "name",\n    "fieldType" : "event",\n    "required" : false,\n    "multiple" : false\n  }, {\n    "name" : "relationship",\n    "fieldType" : "relationship_type",\n    "required" : false,\n    "multiple" : true\n  } ]\n}
result	\N	Result	{\n  "fields" : [ {\n    "name" : "name",\n    "fieldType" : "result",\n    "required" : false,\n    "multiple" : false\n  }, {\n    "name" : "world_actor",\n    "fieldType" : "world_actor",\n    "required" : false,\n    "multiple" : true\n  } ]\n}
impact	\N	Impact	{\n  "fields" : [ {\n    "name" : "name",\n    "fieldType" : "impact",\n    "required" : false,\n    "multiple" : false\n  }, {\n    "name" : "world_actor",\n    "fieldType" : "world_actor",\n    "required" : false,\n    "multiple" : true\n  }, {\n    "name" : "low_estimate",\n    "fieldType" : "integer",\n    "required" : false,\n    "multiple" : false\n  }, {\n    "name" : "estimate",\n    "fieldType" : "integer",\n    "required" : false,\n    "multiple" : false\n  }, {\n    "name" : "high_estimate",\n    "fieldType" : "integer",\n    "required" : false,\n    "multiple" : false\n  } ]\n}
citation	\N	Citation	{\n  "fields" : [ {\n    "name" : "name",\n    "fieldType" : "citation",\n    "required" : false,\n    "multiple" : false\n  }, {\n    "name" : "excerpt",\n    "fieldType" : "string",\n    "required" : false,\n    "multiple" : true\n  } ]\n}
idea	\N	Idea	{\n  "fields" : [ {\n    "name" : "name",\n    "fieldType" : "idea",\n    "required" : false,\n    "multiple" : false\n  } ]\n}
event-type	\N	Event Type	{\n  "fields" : [ {\n    "name" : "name",\n    "fieldType" : "event_type",\n    "required" : false,\n    "multiple" : false\n  } ]\n}
meta	\N	Meta	{\n  "fields" : [ {\n    "name" : "name",\n    "fieldType" : "string",\n    "required" : false,\n    "multiple" : false\n  }, {\n    "name" : "value",\n    "fieldType" : "string",\n    "required" : false,\n    "multiple" : true\n  } ]\n}
\.


--
-- Data for Name: vote; Type: TABLE DATA; Schema: public; Owner: histori
--

COPY vote (uuid, ctime, mtime, owner, entity, version, vote) FROM stdin;
\.


--
-- Data for Name: vote_archive; Type: TABLE DATA; Schema: public; Owner: histori
--

COPY vote_archive (uuid, ctime, mtime, owner, entity, version, vote, identifier) FROM stdin;
\.


--
-- Name: account_canonical_email_key; Type: CONSTRAINT; Schema: public; Owner: histori; Tablespace: 
--

ALTER TABLE ONLY account
    ADD CONSTRAINT account_canonical_email_key UNIQUE (canonical_email);


--
-- Name: account_pkey; Type: CONSTRAINT; Schema: public; Owner: histori; Tablespace: 
--

ALTER TABLE ONLY account
    ADD CONSTRAINT account_pkey PRIMARY KEY (uuid);


--
-- Name: audit_log_pkey; Type: CONSTRAINT; Schema: public; Owner: histori; Tablespace: 
--

ALTER TABLE ONLY audit_log
    ADD CONSTRAINT audit_log_pkey PRIMARY KEY (uuid);


--
-- Name: bookmark_pkey; Type: CONSTRAINT; Schema: public; Owner: histori; Tablespace: 
--

ALTER TABLE ONLY bookmark
    ADD CONSTRAINT bookmark_pkey PRIMARY KEY (uuid);


--
-- Name: map_image_pkey; Type: CONSTRAINT; Schema: public; Owner: histori; Tablespace: 
--

ALTER TABLE ONLY map_image
    ADD CONSTRAINT map_image_pkey PRIMARY KEY (uuid);


--
-- Name: nexus_archive_pkey; Type: CONSTRAINT; Schema: public; Owner: histori; Tablespace: 
--

ALTER TABLE ONLY nexus_archive
    ADD CONSTRAINT nexus_archive_pkey PRIMARY KEY (uuid);


--
-- Name: nexus_archive_uniq; Type: CONSTRAINT; Schema: public; Owner: histori; Tablespace: 
--

ALTER TABLE ONLY nexus_archive
    ADD CONSTRAINT nexus_archive_uniq UNIQUE (owner, name, version);


--
-- Name: nexus_pkey; Type: CONSTRAINT; Schema: public; Owner: histori; Tablespace: 
--

ALTER TABLE ONLY nexus
    ADD CONSTRAINT nexus_pkey PRIMARY KEY (uuid);


--
-- Name: nexus_uniq; Type: CONSTRAINT; Schema: public; Owner: histori; Tablespace: 
--

ALTER TABLE ONLY nexus
    ADD CONSTRAINT nexus_uniq UNIQUE (owner, name);


--
-- Name: permalink_pkey; Type: CONSTRAINT; Schema: public; Owner: histori; Tablespace: 
--

ALTER TABLE ONLY permalink
    ADD CONSTRAINT permalink_pkey PRIMARY KEY (name);


--
-- Name: search_query_pkey; Type: CONSTRAINT; Schema: public; Owner: histori; Tablespace: 
--

ALTER TABLE ONLY search_query
    ADD CONSTRAINT search_query_pkey PRIMARY KEY (uuid);


--
-- Name: shard_pkey; Type: CONSTRAINT; Schema: public; Owner: histori; Tablespace: 
--

ALTER TABLE ONLY shard
    ADD CONSTRAINT shard_pkey PRIMARY KEY (uuid);


--
-- Name: super_nexus_pkey; Type: CONSTRAINT; Schema: public; Owner: histori; Tablespace: 
--

ALTER TABLE ONLY super_nexus
    ADD CONSTRAINT super_nexus_pkey PRIMARY KEY (uuid);


--
-- Name: tag_pkey; Type: CONSTRAINT; Schema: public; Owner: histori; Tablespace: 
--

ALTER TABLE ONLY tag
    ADD CONSTRAINT tag_pkey PRIMARY KEY (canonical_name);


--
-- Name: tag_type_pkey; Type: CONSTRAINT; Schema: public; Owner: histori; Tablespace: 
--

ALTER TABLE ONLY tag_type
    ADD CONSTRAINT tag_type_pkey PRIMARY KEY (canonical_name);


--
-- Name: uk_1wdpsed5kna2y38hnbgrnhi5b; Type: CONSTRAINT; Schema: public; Owner: histori; Tablespace: 
--

ALTER TABLE ONLY tag
    ADD CONSTRAINT uk_1wdpsed5kna2y38hnbgrnhi5b UNIQUE (name);


--
-- Name: uk_9evqakbsdoc6885a0qwja6kwu; Type: CONSTRAINT; Schema: public; Owner: histori; Tablespace: 
--

ALTER TABLE ONLY tag_type
    ADD CONSTRAINT uk_9evqakbsdoc6885a0qwja6kwu UNIQUE (name);


--
-- Name: uk_jgxk02jby9few6x04oqy8swtt; Type: CONSTRAINT; Schema: public; Owner: histori; Tablespace: 
--

ALTER TABLE ONLY bookmark
    ADD CONSTRAINT uk_jgxk02jby9few6x04oqy8swtt UNIQUE (owner, name);


--
-- Name: vote_archive_pkey; Type: CONSTRAINT; Schema: public; Owner: histori; Tablespace: 
--

ALTER TABLE ONLY vote_archive
    ADD CONSTRAINT vote_archive_pkey PRIMARY KEY (uuid);


--
-- Name: vote_archive_uniq; Type: CONSTRAINT; Schema: public; Owner: histori; Tablespace: 
--

ALTER TABLE ONLY vote_archive
    ADD CONSTRAINT vote_archive_uniq UNIQUE (owner, entity, version);


--
-- Name: vote_pkey; Type: CONSTRAINT; Schema: public; Owner: histori; Tablespace: 
--

ALTER TABLE ONLY vote
    ADD CONSTRAINT vote_pkey PRIMARY KEY (uuid);


--
-- Name: vote_uniq; Type: CONSTRAINT; Schema: public; Owner: histori; Tablespace: 
--

ALTER TABLE ONLY vote
    ADD CONSTRAINT vote_uniq UNIQUE (owner, entity);


--
-- Name: public; Type: ACL; Schema: -; Owner: postgres
--

REVOKE ALL ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON SCHEMA public FROM postgres;
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO PUBLIC;


--
-- PostgreSQL database dump complete
--

