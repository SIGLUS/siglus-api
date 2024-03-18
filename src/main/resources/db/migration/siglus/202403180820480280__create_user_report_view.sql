-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.
drop view if exists dashboard.vw_user_report;
create view dashboard.vw_user_report as
select
case when
	gz.province is null then gz.district
	else gz.province
	end as province
, gz.district, f.name as "facility name", concat(u.firstname, ' ', u.lastname) as name, u.username, ucd.email
from referencedata.users u
left join notification.user_contact_details ucd on u.id = ucd.referencedatauserid
left join referencedata.facilities f on u.homefacilityid = f.id
left join
(
	select gz1.id, gz1.name as district, gz2.name as province from referencedata.geographic_zones gz1
	inner join referencedata.geographic_zones gz2
	on gz1.parentid = gz2.id
	union
	select id, name, null from referencedata.geographic_zones where parentid is null
) gz on f.geographiczoneid = gz.id
where homefacilityid is not null
union
select null as province, null as district, null as "facility name",
concat(firstname, ' ', lastname) as name, username, ucd.email
from referencedata.users u
left join notification.user_contact_details ucd on u.id = ucd.referencedatauserid
where homefacilityid is null;
