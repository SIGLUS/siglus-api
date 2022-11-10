-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

-- set these table to original
ALTER TABLE auth.oauth_access_token REPLICA IDENTITY default;
ALTER TABLE auth.oauth_approvals REPLICA IDENTITY default;
ALTER TABLE auth.oauth_client_token REPLICA IDENTITY default;
ALTER TABLE auth.oauth_code REPLICA IDENTITY default;
ALTER TABLE auth.oauth_refresh_token REPLICA IDENTITY default;
ALTER TABLE siglusintegration.previous_adjusted_consumptions_draft REPLICA IDENTITY default;

-- add unique constraint
alter table report.jaspertemplate_requiredrights add constraint unq_jaspertemplateid_requiredrights UNIQUE(jaspertemplateid, requiredrights);
alter table report.jaspertemplateparameter_options add constraint unq_jaspertemplateparameterid_options UNIQUE(jaspertemplateparameterid, options);
alter table report.jasper_template_parameter_dependencies add constraint unq_rep_jasper_template_parameter_dependencies_id UNIQUE(id);
alter table report.jasper_templates_report_images add constraint unq_jaspertemplateid_reportimageid UNIQUE(jaspertemplateid, reportimageid);

alter table requisition.jasper_template_parameter_dependencies add constraint unq_req_jasper_template_parameter_dependencies_id UNIQUE(id);
alter table requisition.jaspertemplateparameter_options add constraint unq_req_jaspertemplateparameterid_options UNIQUE(jaspertemplateparameterid,options);
alter table requisition.available_requisition_column_sources add constraint unq_columnid_value UNIQUE(columnid, value);

alter table stockmanagement.jasper_templates add constraint unq_stock_jasper_templates_id UNIQUE(id);

-- ALTER REPLICA IDENTITY using index
ALTER TABLE report.jasper_template_parameter_dependencies REPLICA IDENTITY using index unq_rep_jasper_template_parameter_dependencies_id;
ALTER TABLE report.jasper_templates_report_images REPLICA IDENTITY using index unq_jaspertemplateid_reportimageid;

ALTER TABLE requisition.jasper_template_parameter_dependencies REPLICA IDENTITY using index unq_req_jasper_template_parameter_dependencies_id;

ALTER TABLE stockmanagement.jasper_templates REPLICA IDENTITY using index unq_stock_jasper_templates_id;
