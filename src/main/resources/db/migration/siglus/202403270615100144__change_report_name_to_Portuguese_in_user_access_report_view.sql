-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.
drop view if exists dashboard.vw_user_access_report;
create view dashboard.vw_user_access_report as
select case when
	gz.province is null then gz.district
	else gz.province
	end as province
, gz.district, f.name as facilityname, ftm.category as facilitytype, u.username as user,
CASE WHEN rar.reportname = 'STOCK_STATUS' THEN 'Relatório do estado de stock'
WHEN rar.reportname = 'SOH_BY_LOT' THEN 'Relatório de stock existente por lote'
WHEN rar.reportname = 'EXPIRED_PRODUCTS' THEN 'Relatório de produtos prestes a expirar'
WHEN rar.reportname = 'EXPIRING_PRODUCTS' THEN 'Relatório de produtos expirados'
WHEN rar.reportname = 'TRACER_DRUG' THEN 'Relatório de produtos sentinela'
WHEN rar.reportname = 'HISTORICAL_DATA' THEN 'Relatório de dados históricos'
WHEN rar.reportname = 'REQUISITIONS_MONTHLY' THEN 'Requisições & Mapas Mensais'
WHEN rar.reportname = 'REQUISITION_DATA' THEN 'Relatório de dados da requisição'
WHEN rar.reportname = 'MMIA_REGIMENS' THEN 'Relatório de Regimes MMIA'
WHEN rar.reportname = 'MMTB_REGIMENS' THEN 'Relatório de Regimes MMTB'
WHEN rar.reportname = 'MALARIA_CONSUMPTION_DATA' THEN 'Relatório de dados de consumo de Malária'
WHEN rar.reportname = 'RAPID_TEST_CONSUMPTION_DATA' THEN 'Relatório de dados de consumo do MMIT'
WHEN rar.reportname = 'FULFILLMENT' THEN 'Relatório de grau de satisfação'
WHEN rar.reportname = 'SYSTEM_VERSION' THEN 'Relatório da Versão do Sistema'
WHEN rar.reportname = 'SYSTEM_UPDATE' THEN 'Relatório de actualização do Sistema'
WHEN rar.reportname = 'USER' THEN 'Relatório do Utilizador'
WHEN rar.reportname = 'USER_ACCESS' THEN 'Relatório de Acesso do Usuário'
ELSE rar.reportname
END AS reportname,
rar.accessdate
from siglusintegration.report_access_record rar
left join referencedata.users u on rar.userid = u.id
left join notification.user_contact_details ucd on u.id = ucd.referencedatauserid
left join referencedata.facilities f on u.homefacilityid = f.id
left join referencedata.facility_types ft ON f.typeid = ft.id
left join siglusintegration.facility_type_mapping ftm ON ft.code = ftm.facilitytypecode
left join
(
	select gz1.id, gz1.name as district, gz2.name as province from referencedata.geographic_zones gz1
	inner join referencedata.geographic_zones gz2
	on gz1.parentid = gz2.id
	union
	select id, name, null from referencedata.geographic_zones where parentid is null
) gz on f.geographiczoneid = gz.id
where ucd.email is null or ucd.email not like '%@thoughtworks.com%';
