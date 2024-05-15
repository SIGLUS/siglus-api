-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.
CREATE INDEX calculated_stocks_on_hand_stockcardid_idx ON stockmanagement.calculated_stocks_on_hand USING btree (stockcardid, processeddate DESC);
CREATE INDEX calculated_stocks_on_hand_stockcardid_occureddate_idx ON stockmanagement.calculated_stocks_on_hand USING btree (stockcardid, occurreddate DESC);
CREATE INDEX stock_cards_facilityid_idx ON stockmanagement.stock_cards USING btree (facilityid);
CREATE INDEX stock_cards_lotid_idx ON stockmanagement.stock_cards USING btree (lotid);
CREATE INDEX hf_cmms_facilitycode_productcode_end_idx ON siglusintegration.hf_cmms USING btree (facilitycode, productcode, periodend DESC);