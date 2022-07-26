-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

DROP VIEW IF EXISTS dashboard.vw_master_data;
CREATE VIEW dashboard.vw_master_data AS
SELECT rg.code rgcode,
       rg.NAME rgname,
       p.NAME programname,
       ps.NAME scheduler,
       sn.code sncode,
       sn.NAME snname,
       sn2.code parentsncode,
       sn2.NAME parentsnname,
       f.code snfacilitycode,
       f.NAME snfacilityname,
       f2.code membersfacilitycode,
       f2.NAME membersfacilityname,
       ft.code membersfacilitytype,
       gz.code geographiczonecode,
       gz.NAME geographiczonename,
       gl.NAME geographiczonetype
FROM   referencedata.requisition_group_members rgm
       JOIN referencedata.requisition_groups rg
         ON rgm.requisitiongroupid = rg.id
       JOIN referencedata.supervisory_nodes sn
         ON rg.supervisorynodeid = sn.id
       JOIN referencedata.supervisory_nodes sn2
         ON sn.parentid = sn2.id
       JOIN referencedata.facilities f
         ON sn.facilityid = f.id
       JOIN referencedata.facilities f2
         ON rgm.facilityid = f2.id
       JOIN referencedata.facility_types ft
         ON ft.id = f2.typeid
       JOIN referencedata.requisition_group_program_schedules rgps
         ON rgps.requisitiongroupid = rg.id
       JOIN referencedata.processing_schedules ps
         ON ps.id = rgps.processingscheduleid
       JOIN referencedata.programs p
         ON p.id = rgps.programid
       JOIN referencedata.geographic_zones gz
         ON gz.id = f2.geographiczoneid
       JOIN referencedata.geographic_levels gl
         ON gz.levelid = gl.id
ORDER  BY rg.code, membersfacilitycode;
