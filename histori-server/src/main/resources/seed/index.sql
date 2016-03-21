--------------------------
-- Account indexes -------
--------------------------
CREATE UNIQUE INDEX account_idx_name ON account(name);

--------------------------
-- Nexus indexes ---------
--------------------------
CREATE INDEX nexus_idx_canonical_name ON nexus(canonical_name);
CREATE INDEX nexus_idx_owner ON nexus(owner);
CREATE INDEX nexus_idx_origin ON nexus(origin);
CREATE INDEX nexus_idx_visibility ON nexus(visibility);
CREATE INDEX nexus_idx_east ON nexus(east);
CREATE INDEX nexus_idx_north ON nexus(north);
CREATE INDEX nexus_idx_south ON nexus(south);
CREATE INDEX nexus_idx_west ON nexus(west);
CREATE INDEX nexus_idx_nexus_type ON nexus(nexus_type);
CREATE INDEX nexus_idx_end_instant ON nexus(end_instant);
CREATE INDEX nexus_idx_end_year ON nexus(end_year);
CREATE INDEX nexus_idx_start_instant ON nexus(start_instant);
CREATE INDEX nexus_idx_start_year ON nexus(start_year);

--------------------------
-- NexusArchive indexes --
--------------------------
CREATE INDEX nexus_archive_idx_owner ON nexus_archive(owner);
CREATE INDEX nexus_archive_idx_identifier ON nexus_archive(identifier);
CREATE INDEX nexus_archive_idx_version ON nexus_archive(version);
CREATE INDEX nexus_archive_idx_visibility ON nexus_archive(visibility);

--------------------------
-- SuperNexus indexes ----
--------------------------
CREATE UNIQUE INDEX super_nexus_idx_canonical_name ON super_nexus(canonical_name) WHERE owner IS NULL;
CREATE UNIQUE INDEX super_nexus_idx_canonical_name_owner_visibility ON super_nexus(canonical_name, owner, visibility) WHERE owner IS NOT NULL;
CREATE INDEX super_nexus_idx_owner ON super_nexus(owner);
CREATE INDEX super_nexus_idx_visibility ON super_nexus(visibility);
CREATE INDEX super_nexus_idx_east ON super_nexus(east);
CREATE INDEX super_nexus_idx_north ON super_nexus(north);
CREATE INDEX super_nexus_idx_south ON super_nexus(south);
CREATE INDEX super_nexus_idx_west ON super_nexus(west);
CREATE INDEX super_nexus_idx_end_instant ON super_nexus(end_instant);
CREATE INDEX super_nexus_idx_end_year ON super_nexus(end_year);
CREATE INDEX super_nexus_idx_start_instant ON super_nexus(start_instant);
CREATE INDEX super_nexus_idx_start_year ON super_nexus(start_year);
CREATE INDEX super_nexus_idx_dirty ON super_nexus(dirty);

--------------------------
-- Tag/TagType indexes ---
--------------------------
CREATE INDEX tag_idx_canonical_name ON tag(canonical_name);
CREATE INDEX tag_idx_tag_type ON tag(tag_type);
CREATE INDEX tag_type_idx_name ON tag_type(name);

---------------------------
-- Vote/VoteArchive indexes
---------------------------
CREATE INDEX vote_idx_owner ON vote(owner);
CREATE INDEX vote_idx_entity ON vote(entity);

CREATE INDEX vote_archive_idx_owner ON vote_archive(owner);
CREATE INDEX vote_archive_idx_entity ON vote_archive(entity);
CREATE INDEX vote_archive_idx_identifier ON vote_archive(identifier);

--------------------------
-- Misc indexes ----------
--------------------------
CREATE INDEX bookmark_idx_owner ON bookmark(owner);
CREATE INDEX map_image_idx_owner ON map_image(owner);
CREATE INDEX permalink_idx_name ON permalink(name);
