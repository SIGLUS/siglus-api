-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

drop view if exists dashboard.vw_system_version;
create view dashboard.vw_system_version
            (number, provincecode, provincename, districtcode, districtname, facilitytype, facilitymergetype,
             districtfacilitycode, provincefacilitycode, facilitycode, facilityname, versioncode, username, devicetype, deviceinfo)
as
SELECT row_number() OVER (ORDER BY a.facilitycode) AS number,
       pgz.code                                    AS provincecode,
       pgz.name                                    AS provincename,
       gz.code                                     AS districtcode,
       gz.name                                     AS districtname,
       ft.code                                     AS facilitytype,
       ftm.category                                AS facilitymergetype,
       vfs.districtfacilitycode,
       vfs.provincefacilitycode,
       a.facilitycode,
       f.name                                      AS facilityname,
       a.versioncode,
       a.username,
       CASE
           WHEN fe.isandroid IS TRUE THEN 'Tablet'::text
           ELSE 'Computador'::text
           END                                     AS devicetype,
       a.deviceinfo
FROM siglusintegration.app_info a
         JOIN referencedata.facilities f ON f.code = a.facilitycode::text
         LEFT JOIN referencedata.facility_types ft ON ft.id = f.typeid
         LEFT JOIN siglusintegration.facility_type_mapping ftm ON ftm.facilitytypecode = ft.code
         LEFT JOIN dashboard.vw_facility_supplier vfs ON vfs.facilitycode = f.code
         LEFT JOIN referencedata.geographic_zones gz ON gz.id = f.geographiczoneid
         LEFT JOIN referencedata.geographic_zones pgz ON gz.parentid = pgz.id
         LEFT JOIN siglusintegration.facility_extension fe ON fe.facilityid = f.id
WHERE ftm.category IS NOT NULL;