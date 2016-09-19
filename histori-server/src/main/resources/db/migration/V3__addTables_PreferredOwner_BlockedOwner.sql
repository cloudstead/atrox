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
