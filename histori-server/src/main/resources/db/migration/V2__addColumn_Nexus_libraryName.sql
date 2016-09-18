alter table nexus_archive add column library_name varchar(100) DEFAULT NULL;
alter table nexus add column library_name varchar(100) DEFAULT NULL;

update nexus set library_name = 'Wikipedia - City/Settlements' where nexus_type = 'founding';
update nexus set library_name = 'Wikipedia - Battles' where nexus_type = 'battle';
update nexus_archive set library_name = 'Wikipedia - City/Settlements' where nexus_type = 'founding';
update nexus_archive set library_name = 'Wikipedia - Battles' where nexus_type = 'battle';

