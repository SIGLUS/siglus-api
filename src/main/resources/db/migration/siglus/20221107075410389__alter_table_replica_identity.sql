-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

Alter table siglusintegration.pod_line_items_by_location
    add primary key (id);
Alter table siglusintegration.pod_sub_draft_line_items_by_location
    add primary key (id);

ALTER TABLE auth.oauth_access_token REPLICA IDENTITY full;
ALTER TABLE auth.oauth_approvals REPLICA IDENTITY full;
ALTER TABLE auth.oauth_client_token REPLICA IDENTITY full;
ALTER TABLE auth.oauth_code REPLICA IDENTITY full;
ALTER TABLE auth.oauth_refresh_token REPLICA IDENTITY full;

ALTER TABLE referencedata.trade_item_classifications REPLICA IDENTITY using index unq_trade_item_classifications_system;

ALTER TABLE report.jaspertemplate_requiredrights REPLICA IDENTITY full;
ALTER TABLE report.jaspertemplateparameter_options REPLICA IDENTITY full;
ALTER TABLE report.jasper_template_parameter_dependencies REPLICA IDENTITY full;
ALTER TABLE report.jasper_templates_report_images REPLICA IDENTITY full;

ALTER TABLE requisition.jasper_template_parameter_dependencies REPLICA IDENTITY full;
ALTER TABLE requisition.jaspertemplateparameter_options REPLICA IDENTITY full;
ALTER TABLE requisition.available_requisition_column_sources REPLICA IDENTITY full;

ALTER TABLE siglusintegration.previous_adjusted_consumptions_draft REPLICA IDENTITY full;

ALTER TABLE stockmanagement.jasper_templates REPLICA IDENTITY full;
ALTER TABLE stockmanagement.stock_card_line_item_reason_tags REPLICA IDENTITY using index stock_card_line_item_reason_tags_unique_idx;