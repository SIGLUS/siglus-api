-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

alter table siglusintegration.stock_card_location_movement_draft_line_items
    add column iskit boolean;

alter table siglusintegration.stock_card_location_movement_draft_line_items
    add column iskit boolean;

alter table siglusintegration.stock_card_location_movement_draft_line_items
    drop column occurredDate;

alter table siglusintegration.stock_card_location_movement_draft_line_items
    alter column orderableid drop not null;

alter table siglusintegration.stock_card_location_movement_draft_line_items
    alter column srcarea drop not null;

alter table siglusintegration.stock_card_location_movement_draft_line_items
    alter column destarea drop not null;
