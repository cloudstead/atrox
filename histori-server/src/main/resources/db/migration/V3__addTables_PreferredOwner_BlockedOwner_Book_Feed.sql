CREATE TABLE preferred_owner (
  uuid character varying(100) NOT NULL,
  ctime bigint NOT NULL,
  mtime bigint NOT NULL,
  owner character varying(100) NOT NULL,
  preferred character varying(100) NOT NULL,
  active boolean NOT NULL,
  priority integer NOT NULL
);

ALTER TABLE ONLY preferred_owner ADD CONSTRAINT preferred_owner_pkey PRIMARY KEY (uuid);
ALTER TABLE ONLY preferred_owner ADD CONSTRAINT preferred_owner_uniq_owner_preferred UNIQUE (owner, preferred);

CREATE TABLE blocked_owner (
  uuid character varying(100) NOT NULL,
  ctime bigint NOT NULL,
  mtime bigint NOT NULL,
  owner character varying(100) NOT NULL,
  blocked character varying(100) NOT NULL,
  active boolean NOT NULL
);

ALTER TABLE ONLY blocked_owner ADD CONSTRAINT blocked_owner_pkey PRIMARY KEY (uuid);
ALTER TABLE ONLY blocked_owner ADD CONSTRAINT blocked_owner_uniq_owner_blocked UNIQUE (owner, blocked);

ALTER TABLE search_query ALTER COLUMN preferred_owners TYPE varchar(10000);
ALTER TABLE search_query ALTER COLUMN blocked_owners TYPE varchar(10000);
ALTER TABLE search_query ALTER COLUMN nexus_sort_order TYPE varchar(50);
ALTER TABLE search_query ALTER COLUMN summary_sort_order TYPE varchar(50);

CREATE TABLE book (
  uuid character varying(100) NOT NULL,
  ctime bigint NOT NULL,
  mtime bigint NOT NULL,
  owner character varying(100) NOT NULL,
  name character varying(1024) NOT NULL,
  short_name character varying(100) NOT NULL
);
ALTER TABLE ONLY book ADD CONSTRAINT book_pkey PRIMARY KEY (uuid);
ALTER TABLE ONLY book ADD CONSTRAINT book_uniq_name UNIQUE (name);
ALTER TABLE ONLY book ADD CONSTRAINT book_uniq_short_name UNIQUE (short_name);

INSERT INTO book (uuid, name, short_name, owner, ctime, mtime) VALUES (
  uuid_generate_v4(),
  'Wikipedia - City and Settlement Foundings',
  'w.city',
  (select uuid from account where admin = true limit 1),
  (select (1000*extract(epoch from now())::bigint)),
  (select (1000*extract(epoch from now())::bigint))
);

INSERT INTO book (uuid, name, short_name, owner, ctime, mtime) VALUES (
  uuid_generate_v4(),
  'Wikipedia - Battles',
  'w.battle',
  (select uuid from account where admin = true limit 1),
  (select (1000*extract(epoch from now())::bigint)),
  (select (1000*extract(epoch from now())::bigint))
);

CREATE TABLE feed (
  uuid character varying(100) NOT NULL,
  ctime bigint NOT NULL,
  mtime bigint NOT NULL,
  owner character varying(100) NOT NULL,
  book character varying(1024),
  match character varying(1024),
  name character varying(1024) NOT NULL,
  active boolean NOT NULL,
  reader character varying(1024) NOT NULL,
  nexus jsonb,
  path character varying(1024) NOT NULL,
  poll character varying(30) NOT NULL,
  source character varying(1024) NOT NULL
);

ALTER TABLE ONLY feed ADD CONSTRAINT feed_pkey PRIMARY KEY (uuid);
ALTER TABLE ONLY feed ADD CONSTRAINT feed_uniq_owner_name UNIQUE (owner, name);

UPDATE tag_type SET schema_json =
'{
  "fields": [
    {"name": "name", "fieldType": "citation", "multiple": false},
    {"name": "last_accessed", "fieldType": "date", "multiple": false},
    {"name": "excerpt", "fieldType": "string", "multiple": true},
    {"name": "see_also", "fieldType": "string", "multiple": true},
    {"name": "comment", "fieldType": "string", "multiple": true}
  ]
}'
WHERE canonical_name = 'citation';

UPDATE tag_type SET schema_json =
'{
  "fields": [
    {"name": "name", "fieldType": "impact", "multiple": false},
    {"name": "world_actor", "fieldType": "world_actor", "multiple": true},
    {"name": "low_estimate", "fieldType": "integer", "multiple": false},
    {"name": "estimate", "fieldType": "integer", "multiple": false},
    {"name": "value", "fieldType": "integer", "multiple": false},
    {"name": "high_estimate", "fieldType": "integer", "multiple": false},
    {"name": "see_also", "fieldType": "string", "multiple": true},
    {"name": "comment", "fieldType": "string", "multiple": true}
  ]
}'
WHERE canonical_name = 'impact';
