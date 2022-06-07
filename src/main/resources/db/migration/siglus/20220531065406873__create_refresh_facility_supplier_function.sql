-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

create function refresh_facility_supplier() returns SETOF facility_supplier
    language plpgsql
as
$$
DECLARE
    facilityCode text;
    districtFacilityCode text;
    provinceFacilityCode text;
    facilityName text;
    districtFacilityName text;
    provinceFacilityName text;
    supplier1Code text;
    supplier2Code text;
    facilityLevel text;
    supplier1Level text;
    supplier2level text;
    supplier1Name text;
    supplier2Name text;
BEGIN
for facilityCode,supplier1Code,supplier2Code IN (
    select tmp.code as facilityCode,split_part(tmp.pfCode,',',1) as supplier1Code,split_part(tmp.pfCode,',',2) as supplier2Code from
        (
            select f.code,array_to_string(array_agg(DISTINCT pf.code),',') as pfCode
            from referencedata.facilities f
                     left join referencedata.requisition_group_members rgm on rgm.facilityid = f.id
                     left join referencedata.requisition_groups rg on rg.id = rgm.requisitiongroupid
                     left join referencedata.supervisory_nodes sn on sn.id = rg.supervisorynodeid
                     left join referencedata.facilities pf on pf.id = sn.facilityid
            group by f.code
        ) tmp
)
LOOP
select fsl.level,f.name from referencedata.facilities f
                          left join referencedata.facility_types ft on ft.id = f.typeid
                          left join siglusintegration.facility_supplier_level fsl on fsl.facilitytypecode = ft.code
where f.code = facilityCode
    into facilityLevel,facilityName;

if supplier1Code is not null
THEN
select fsl.level,f.name from referencedata.facilities f
                          left join referencedata.facility_types ft on ft.id = f.typeid
                          left join siglusintegration.facility_supplier_level fsl on fsl.facilitytypecode = ft.code
where f.code = supplier1Code
    into supplier1Level,supplier1Name;
END IF;

if supplier2Code is not null
THEN
select fsl.level,f.name from referencedata.facilities f
                          left join referencedata.facility_types ft on ft.id = f.typeid
                          left join siglusintegration.facility_supplier_level fsl on fsl.facilitytypecode = ft.code
where f.code = supplier2Code
    into supplier2Level,supplier2Name;
END IF;

districtFacilityCode = null;
provinceFacilityCode = null;
districtFacilityName = null;
provinceFacilityName = null;

if supplier1Level = 'DISTRICT'
then
districtFacilityCode = supplier1Code;
districtFacilityName = supplier1Name;
elseif supplier1Level = 'PROVINCE'
then
provinceFacilityCode = supplier1Code;
provinceFacilityName = supplier1Name;
END IF;

if supplier2level = 'DISTRICT'
then
districtFacilityCode = supplier2Code;
districtFacilityName = supplier2Name;
elseif supplier2level = 'PROVINCE'
then
provinceFacilityCode = supplier2Code;
provinceFacilityName = supplier2Name;
END IF;

if facilityLevel = 'DISTRICT'
then
districtFacilityCode = facilityCode;
districtFacilityName = facilityName;
elseif facilityLevel = 'PROVINCE'
then
provinceFacilityCode = facilityCode;
provinceFacilityName = facilityName;
END IF;

RETURN NEXT (facilityCode, facilityName, districtFacilityCode, districtFacilityName, provinceFacilityCode, provinceFacilityName);
END LOOP;

END;
$$;