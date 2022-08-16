-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

ALTER TABLE siglusintegration.physical_inventories_extension ALTER COLUMN locationoption TYPE CHARACTER VARYING(255);
ALTER TABLE siglusintegration.physical_inventory_line_items_extension ALTER COLUMN locationcode TYPE CHARACTER VARYING(255);
ALTER TABLE siglusintegration.physical_inventory_line_items_extension ALTER COLUMN area TYPE CHARACTER VARYING(255);

CREATE TABLE siglusintegration.calculated_stocks_on_hand_locations
(
    id                 UUID PRIMARY KEY NOT NULL,
    stockcardid        UUID             NOT NULL,
    locationcode       CHARACTER VARYING(255) NOT NULL,
    area               CHARACTER VARYING (255) NOT NULL,
    occurreddate       DATE             NOT NULL,
    stockonhand        INTEGER NOT NULL,
    calculatedstockonhandid UUID NOT NULL
);

CREATE INDEX ON siglusintegration.calculated_stocks_on_hand_locations(stockcardid);
CREATE INDEX ON siglusintegration.calculated_stocks_on_hand_locations(locationcode);
CREATE INDEX ON siglusintegration.calculated_stocks_on_hand_locations(area);
CREATE INDEX ON siglusintegration.calculated_stocks_on_hand_locations(calculatedstockonhandid);