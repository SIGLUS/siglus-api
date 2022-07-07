-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

CREATE UNIQUE INDEX ON siglusintegration.program_extension(programid);

CREATE UNIQUE INDEX ON siglusintegration.stock_card_extension(stockcardid);

CREATE INDEX ON siglusintegration.requisition_template_associated_programs(requisitiontemplateid);

CREATE INDEX ON siglusintegration.available_usage_column_sections(category);

CREATE INDEX ON siglusintegration.available_usage_columns(sectionid);

CREATE INDEX ON siglusintegration.usage_sections_maps(requisitionTemplateId);

CREATE INDEX ON siglusintegration.usage_columns_maps(usagesectionid);

CREATE INDEX ON siglusintegration.kit_usage_line_items(requisitionid);

CREATE INDEX ON siglusintegration.base_line_items(requisitionid);

CREATE INDEX ON siglusintegration.rapid_test_consumption_line_items(requisitionid);

CREATE INDEX ON siglusintegration.usage_information_line_items(requisitionid);

CREATE INDEX ON siglusintegration.order_line_item_extension(orderlineitemid);

CREATE INDEX ON siglusintegration.physical_inventory_line_items_extension(physicalinventoryid);

CREATE UNIQUE INDEX ON siglusintegration.physical_inventory_line_items_extension
(physicalinventoryid, orderableid, lotid);

CREATE INDEX ON siglusintegration.requisitions_draft(requisitionid);

CREATE INDEX ON siglusintegration.requisition_line_items_draft(requisitiondraftid);

CREATE INDEX ON siglusintegration.kit_usage_line_items_draft(requisitionid);

CREATE INDEX ON siglusintegration.stock_adjustments_draft(draftlineitemid);

