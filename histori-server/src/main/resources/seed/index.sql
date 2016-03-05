
CREATE INDEX nexus_idx_owner ON nexus(owner);
CREATE INDEX nexus_idx_origin ON nexus(origin);
CREATE INDEX nexus_idx_visibility ON nexus(visibility);
CREATE INDEX nexus_idx_east ON nexus(east);
CREATE INDEX nexus_idx_north ON nexus(north);
CREATE INDEX nexus_idx_south ON nexus(south);
CREATE INDEX nexus_idx_west ON nexus(west);
CREATE INDEX nexus_idx_name ON nexus(name);
CREATE INDEX nexus_idx_nexus_type ON nexus(nexus_type);
CREATE INDEX nexus_idx_end_instant ON nexus(end_instant);
CREATE INDEX nexus_idx_end_month ON nexus(end_month);
CREATE INDEX nexus_idx_end_year ON nexus(end_year);
CREATE INDEX nexus_idx_start_instant ON nexus(start_instant);
CREATE INDEX nexus_idx_start_month ON nexus(start_month);
CREATE INDEX nexus_idx_start_year ON nexus(start_year);

CREATE INDEX nexus_tag_idx_owner ON nexus_tag(owner);
CREATE INDEX nexus_tag_visibility ON nexus_tag(visibility);
CREATE INDEX nexus_tag_nexus ON nexus_tag(nexus);
CREATE INDEX nexus_tag_schema_values ON nexus_tag(schema_values);
CREATE INDEX nexus_tag_tag_name ON nexus_tag(tag_name);
CREATE INDEX nexus_tag_tag_type ON nexus_tag(tag_type);

