-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

DELETE FROM referencedata.requisition_group_program_schedules
WHERE programid IN ('10845cb9-d365-4aaa-badd-b4fa39c6a26a', 'a24f19a8-3743-4a1a-a919-e8f97b5719ad')
AND requisitiongroupid IN (SELECT id FROM referencedata.requisition_groups WHERE name not like 'MMIA%');

DELETE FROM referencedata.requisition_group_program_schedules
WHERE programid IN ('dce17f2e-af3e-40ad-8e00-3496adef44c3', 'dfbbd880-cfd2-11e9-9535-0242ac130005')
AND requisitiongroupid IN (SELECT id FROM referencedata.requisition_groups WHERE name like 'MMIA%');
