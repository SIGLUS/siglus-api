-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

CREATE SCHEMA IF NOT EXISTS dashboard;

CREATE MATERIALIZED VIEW dashboard.vw_facility_supplier AS
SELECT uuid_in(md5(random()::text || now()::text)::cstring) AS uuid,
       fs.facilitycode,
       fs.facilityname,
       fs.districtfacilitycode,
       fs.districtfacilityname,
       fs.provincefacilitycode,
       fs.provincefacilityname
FROM refresh_facility_supplier() fs;

CREATE UNIQUE INDEX idx_vw_facility_supplier ON dashboard.vw_facility_supplier(uuid uuid_ops);
