-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

DROP VIEW IF EXISTS dashboard.vw_requisition_monthly_report;
DROP TABLE IF EXISTS siglusintegration.requisition_monthly_not_submit_report;
CREATE TABLE siglusintegration.requisition_monthly_not_submit_report
(
    id                     uuid,
    programid              uuid,
    processingperiodid     uuid,
    district               text,
    province               text,
    requisitionperiod      date,
    ficilityname           text,
    ficilitycode           text,
    inventorydate          text,
    statusdetail           character varying(255),
    submittedstatus        text,
    reporttype             text,
    reportname             character varying(255),
    originalperiod         text,
    submittedtime          timestamp WITH TIME ZONE,
    synctime               timestamp WITH TIME ZONE,
    facilityid             uuid,
    facilitytype           text,
    facilitymergetype      text,
    districtfacilitycode   text,
    provincefacilitycode   text,
    submitteduser          text,
    clientsubmittedtime    text,
    requisitioncreateddate timestamp WITH TIME ZONE,
    statuslastcreateddate  timestamp WITH TIME ZONE,
    submitstartdate        date,
    submitenddate          date
);


CREATE VIEW dashboard.vw_requisition_monthly_report
            (id, programid, processingperiodid, district, province, requisitionperiod, facilityname, ficilitycode,
             inventorydate, statusdetail, submittedstatus,
             reporttype, reportname, originalperiod, submittedtime, synctime, facilityid, facilitytype,
             facilitymergetype, districtfacilitycode, provincefacilitycode, submitteduser, clientSubmittedTime,
             requisitionCreateddate, statusLastcreateddate, submitstartdate, submitenddate)
AS
select
    r.id id,
    p.id programid,
    pp.id processingperiodid,
    fz.District district,
    fz.Province province,
    pp.enddate requisitionperiod,
    fz.name ficilityname,
    rf.code ficilitycode,
    r.extradata::json ->> 'actualEndDate' inventorydate,
        r.status statusdetail,
        (case
        when r.status in ('INITIATED', 'SUBMITTED', 'AUTHORIZED', 'REJECTED') then 'Não Submetido'
        when r.status in ('IN_APPROVAL', 'APPROVED', 'RELEASED', 'RELEASED_WITHOUT_ORDER') then (
        select
        (case
        when ((case
        when r.extradata::json ->> 'clientSubmittedTime' is not null then cast(r.extradata::json ->> 'clientSubmittedTime' as date)
        else cast(scfc.lastcreateddate as date)
        end) between p.submitstartdate and p.submitenddate) then 'A tempo'
        else 'Tarde'
        end
        )
        from
        requisition.requisitions rr
        left join requisition.status_changes s on
        rr.id = s.requisitionid
        left join siglusintegration.processing_period_extension p on
        rr.processingperiodid = p.processingperiodid
        where
        s.status = 'SUBMITTED'
        and rr.id = r.id)
        end
        ) submittedstatus,
        (case
        when r.emergency is true then 'Emergência'
        else 'Regular'
        end) reporttype,
        case pnm.requisitionname
        when 'Balance Requisition' then 'Requisição Balancete'
        else pnm.requisitionname
end as reportname,
	CONCAT(TO_CHAR(pp.startdate, 'DD'), ' ', (case TO_CHAR(pp.startdate, 'MM')
                                                     when '01' then 'Jan'
                                                     when '02' then 'Fev'
                                                     when '03' then 'Mar'
                                                     when '04' then 'Abr'
                                                     when '05' then 'Mai'
                                                     when '06' then 'Jun'
                                                     when '07' then 'Jul'
                                                     when '08' then 'Ago'
                                                     when '09' then 'Set'
                                                     when '10' then 'Out'
                                                     when '11' then 'Nov'
                                                     when '12' then 'Dez' end), ' ',
              TO_CHAR(pp.startdate, 'YYYY'), ' - ', TO_CHAR(pp.enddate, 'DD'), ' ',
              (case TO_CHAR(pp.enddate, 'MM')
                   when '01' then 'Jan'
                   when '02' then 'Fev'
                   when '03' then 'Mar'
                   when '04' then 'Abr'
                   when '05' then 'Mai'
                   when '06' then 'Jun'
                   when '07' then 'Jul'
                   when '08' then 'Ago'
                   when '09' then 'Set'
                   when '10' then 'Out'
                   when '11' then 'Nov'
                   when '12' then 'Dez' end), ' ', TO_CHAR(pp.enddate, 'YYYY'))
                                                  originalperiod,
	(case
		when r.status in ('IN_APPROVAL', 'APPROVED', 'RELEASED', 'RELEASED_WITHOUT_ORDER') then (case
			when r.extradata::json ->> 'clientSubmittedTime' is not null
                                                                                                             then cast(r.extradata::json ->> 'clientSubmittedTime' as TIMESTAMP with TIME zone)
			else scfc.lastcreateddate
		end)
	end) submittedtime,
	(case
		when r.status in ('IN_APPROVAL', 'APPROVED', 'RELEASED', 'RELEASED_WITHOUT_ORDER') then (case
			when r.extradata::json ->> 'clientSubmittedTime' is not null
                                                                                                             then r.createddate
			else scfc.lastcreateddate
		end)
	end) synctime,
	-- extra info
	r.facilityid facilityid,
	ft.code facilitytype,
	ftm.category facilitymergetype,
	vfs.districtfacilitycode,
	vfs.provincefacilitycode,
	u.username submitteduser,
	r.extradata::json ->> 'clientSubmittedTime' clientsubmittedtime,
	r.createddate requisitioncreateddate,
	scfc.lastcreateddate statuslastcreateddate,
	ppe.submitstartdate,
	ppe.submitenddate
from
	requisition.requisitions r
left join
     (
	select
		f.id,
		f.name,
		zz.District District,
		zz.Province Province
	from
		referencedata.facilities f
	left join
           (
		select
			z1.id id,
			z1.name District,
			z2.name Province
		from
			referencedata.geographic_zones z1
		left join referencedata.geographic_zones z2 on
			z1.parentid = z2.id) zz
           on
		f.geographiczoneid = zz.id) fz
     on
	r.facilityid = fz.id
left join referencedata.programs p on
	r.programid = p.id
left join siglusintegration.program_requisition_name_mapping pnm on
	pnm.programid = p.id
left join siglusintegration.processing_period_extension ppe on
	r.processingperiodid = ppe.processingperiodid
left join referencedata.processing_periods pp on
	r.processingperiodid = pp.id
left join referencedata.facilities rf on
	r.facilityid = rf.id
left join dashboard.vw_facility_supplier vfs on
	vfs.facilitycode = rf.code
left join referencedata.facility_types ft on
	rf.typeid = ft.id
left join siglusintegration.facility_type_mapping ftm on
	ftm.facilitytypecode = ft.code
left join (
	select
		distinct MAX(sc.createddate)
                                    over (partition by sc.requisitionid) lastcreateddate,
		sc.requisitionid
	from
		requisition.status_changes sc
	where
		status = 'IN_APPROVAL') scfc on
	r.id = scfc.requisitionid
left join (
	select
		distinct first_value(sc.authorid)
                                    over (partition by sc.requisitionid
	order by
		sc.createddate desc) lastauthorid,
		sc.requisitionid
	from
		requisition.status_changes sc
	where
		status = 'SUBMITTED') scfa on
	r.id = scfa.requisitionid
left join referencedata.users u on
	scfa.lastauthorid = u.id
union all
select
    id,
    programid,
    processingperiodid,
    district,
    province,
    requisitionperiod,
    ficilityname,
    ficilitycode,
    inventorydate,
    statusdetail,
    'Não Submetido' as submittedstatus,
    reporttype,
    case reportname
        when 'Balance Requisition' then 'Requisição Balancete'
        else reportname
        end,
    originalperiod,
    submittedtime,
    synctime,
    facilityid,
    facilitytype,
    facilitymergetype,
    districtfacilitycode,
    provincefacilitycode,
    submitteduser,
    clientsubmittedtime,
    requisitioncreateddate,
    statuslastcreateddate,
    submitstartdate,
    submitenddate
from
    siglusintegration.requisition_monthly_not_submit_report;