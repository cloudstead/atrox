alter table nexus_archive add column book varchar(100) DEFAULT NULL;
alter table nexus add column book varchar(100) DEFAULT NULL;

update nexus set book = 'Wikipedia - City/Settlements' where nexus_type = 'founding';
update nexus set book = 'Wikipedia - Battles' where nexus_type = 'battle';
update nexus_archive set book = 'Wikipedia - City/Settlements' where nexus_type = 'founding';
update nexus_archive set book = 'Wikipedia - Battles' where nexus_type = 'battle';

-- add missing indexes
CREATE INDEX nexus_idx_authoritative ON nexus(authoritative);
