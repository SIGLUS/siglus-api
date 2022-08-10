-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

ALTER TABLE siglusintegration.physical_inventories_extension
    ADD COLUMN locationoption VARCHAR(20);

ALTER TABLE siglusintegration.physical_inventory_line_items_extension
    ADD COLUMN locationcode VARCHAR(20),
    ADD COLUMN area VARCHAR(20);