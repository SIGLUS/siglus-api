create view dashboard.vw_system_update_report as
select
    fac.code as FacilityCode,
    fac.name as FacilityName,
    ftm.category as facilitymergetype,
    ftm.facilitytypecode as facilitytype,
    gz.code as ProvinceCode,
    gz.name as ProvinceName,
    gz_dist.code as DistrictCode,
    gz_dist."name" as DistrictName ,
    vfs.districtfacilitycode,
    vfs.provincefacilitycode,
    r.latestupdatetime,
    ai.username,
    CASE
        WHEN r.latestupdatetime >= (now() - '1 day'::interval) THEN 'last update in the last 24 hours'
        WHEN r.latestupdatetime < (now() - '1 day'::interval) AND r.latestupdatetime >= (now() + '3 day'::interval) THEN 'last update in the last 3 days'
        WHEN r.latestupdatetime < (now() - '3 day'::interval) THEN 'last update more than 3 days ago'
        ELSE 'null'
        END AS updateStatus
from
    (
        select
            distinct sc.facilityid,
                     first_value(csoh.processeddate) over (partition by sc.facilityid
	order by
		csoh.processeddate desc ) as latestupdatetime
        from stockmanagement.calculated_stocks_on_hand csoh
                 left join stockmanagement.stock_cards sc on
                sc.id = csoh.stockcardid
    ) r
        left join referencedata.facilities fac on
            fac.id = r.facilityid
        left join referencedata.geographic_zones gz on
            gz.id = fac .geographiczoneid
        left join referencedata.geographic_zones gz_dist on
            gz_dist.id = gz.parentid
        left join dashboard.vw_facility_supplier vfs on
            vfs.facilitycode = fac.code
        left join referencedata.facility_types ft on
            ft.id = fac.typeid
        left join siglusintegration.facility_type_mapping ftm on
            ftm.facilitytypecode = ft.code
        left join siglusintegration.app_info ai on
            ai.facilitycode=fac.code
where ftm.category is not null;