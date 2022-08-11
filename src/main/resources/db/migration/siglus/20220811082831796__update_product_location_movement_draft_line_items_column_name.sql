-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

ALTER TABLE siglusintegration.product_location_movement_draft_line_items RENAME COLUMN locationid TO srclocationid;
ALTER TABLE siglusintegration.product_location_movement_draft_line_items RENAME COLUMN locationcode TO srclocationcode;
ALTER TABLE siglusintegration.product_location_movement_draft_line_items RENAME COLUMN movetoid TO destlocationid;
ALTER TABLE siglusintegration.product_location_movement_draft_line_items RENAME COLUMN movetocode TO destlocationcode;