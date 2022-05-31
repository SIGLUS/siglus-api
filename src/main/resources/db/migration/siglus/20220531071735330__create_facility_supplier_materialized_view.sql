-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.


CREATE SCHEMA IF NOT EXISTS dashboard;

CREATE MATERIALIZED VIEW vw_facility_supplier AS
SELECT fs.facilitycode,
       fs.facilityname,
       fs.districtfacilitycode,
       fs.districtfacilityname,
       fs.provincefacilitycode,
       fs.provincefacilityname
FROM refresh_facility_supplier() fs
    WITH NO DATA;;