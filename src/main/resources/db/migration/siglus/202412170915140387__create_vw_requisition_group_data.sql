-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

DROP VIEW IF EXISTS dashboard.vw_master_data;
DROP VIEW IF EXISTS dashboard.vw_requisition_group_data;
CREATE VIEW dashboard.vw_requisition_group_data AS
SELECT
    gz.code AS districtcode,
    gz.name AS districtname,
    ft2.code AS facilitytype,
    f2.code AS facilitycode,
    f2.name AS facilityname,
    p.name AS programname,
    ft.code AS suppliertype,
    f.code AS suppliercode,
    f.name AS suppliername,
    rg.code AS rgcode,
    rg.name AS rgname,
    ps.name AS scheduler,
    sn.code AS sncode,
    sn.name AS snname
FROM referencedata.requisition_group_members rgm
         JOIN referencedata.requisition_groups rg ON rgm.requisitiongroupid = rg.id
         JOIN referencedata.supervisory_nodes sn ON rg.supervisorynodeid = sn.id
         JOIN referencedata.facilities f ON sn.facilityid = f.id
         JOIN referencedata.facility_types ft ON ft.id = f.typeid
         JOIN referencedata.facilities f2 ON rgm.facilityid = f2.id
         JOIN referencedata.facility_types ft2 ON ft2.id = f2.typeid
         JOIN referencedata.requisition_group_program_schedules rgps ON rgps.requisitiongroupid = rg.id
         JOIN referencedata.processing_schedules ps ON ps.id = rgps.processingscheduleid
         JOIN referencedata.programs p ON p.id = rgps.programid
         JOIN referencedata.geographic_zones gz ON gz.id = f2.geographiczoneid
         JOIN referencedata.geographic_levels gl ON gz.levelid = gl.id
ORDER BY gz.code, f2.code, p.code DESC;
