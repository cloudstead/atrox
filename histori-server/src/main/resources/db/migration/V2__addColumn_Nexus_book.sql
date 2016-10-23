alter table nexus_archive add column book varchar(1024) DEFAULT NULL;
alter table nexus_archive add column feed varchar(1024);
alter table nexus_archive add column in_owner_book boolean default false;
alter table nexus add column book varchar(100) DEFAULT NULL;
alter table nexus add column feed varchar(1024);
alter table nexus add column in_owner_book boolean default false;

update nexus set book = 'w.city' where nexus_type = 'founding';
update nexus set book = 'w.battle' where nexus_type = 'battle';
update nexus_archive set book = 'w.city' where nexus_type = 'founding';
update nexus_archive set book = 'w.battle' where nexus_type = 'battle';
update nexus set in_owner_book = true;
update nexus_archive set in_owner_book = true;

-- add missing indexes
CREATE INDEX nexus_idx_authoritative ON nexus(authoritative);
