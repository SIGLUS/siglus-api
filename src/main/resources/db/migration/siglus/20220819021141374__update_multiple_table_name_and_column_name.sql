-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

alter table if exists siglusintegration.product_location_movement_drafts
    rename to stock_card_location_movement_drafts;

alter table if exists siglusintegration.product_location_movement_draft_line_items
    rename to stock_card_location_movement_draft_line_items;

alter table if exists siglusintegration.product_location_movement_line_items
    rename to stock_card_location_movement_line_items;

alter table if exists siglusintegration.stock_card_line_item_extension
    rename to stock_card_line_items_by_location;

alter table if exists siglusintegration.calculated_stocks_on_hand_locations
    rename to calculated_stocks_on_hand_by_location;


alter table if exists siglusintegration.stock_card_location_movement_drafts
drop
column if exists createddate;

alter table if exists siglusintegration.stock_card_location_movement_drafts
drop
column if exists signature;

alter table if exists siglusintegration.stock_card_location_movement_draft_line_items
    rename column createddate to occurreddate;

alter table if exists siglusintegration.stock_card_location_movement_line_items
    rename column createddate to occurreddate;

alter table if exists siglusintegration.stock_card_location_movement_line_items
alter
column signature drop
not null;

alter table siglusintegration.stock_card_location_movement_draft_line_items
drop constraint product_location_movement_dra_productlocationmovementdraft_fkey;

alter table siglusintegration.stock_card_location_movement_draft_line_items
    rename column productlocationmovementdraftId to stockcardlocationmovementdraftId;

alter table siglusintegration.stock_card_location_movement_draft_line_items
    add foreign key (stockCardlocationmovementdraftId) references stock_card_location_movement_drafts (id);
