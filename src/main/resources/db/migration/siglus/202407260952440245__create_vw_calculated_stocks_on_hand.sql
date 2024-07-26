-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

DROP MATERIALIZED VIEW IF EXISTS dashboard.vw_calculated_stocks_on_hand;

CREATE MATERIALIZED VIEW dashboard.vw_calculated_stocks_on_hand AS
SELECT DISTINCT ON (calculated_stocks_on_hand.stockcardid) uuid_generate_v4() AS id,
    calculated_stocks_on_hand.stockonhand,
    calculated_stocks_on_hand.occurreddate,
    calculated_stocks_on_hand.stockcardid,
    now() AS processeddate
FROM stockmanagement.calculated_stocks_on_hand
ORDER BY calculated_stocks_on_hand.stockcardid, calculated_stocks_on_hand.occurreddate DESC;

-- Indices -------------------------------------------------------
CREATE UNIQUE INDEX vw_calculated_stocks_on_hand_id_idx ON dashboard.vw_calculated_stocks_on_hand(id uuid_ops);
CREATE UNIQUE INDEX vw_calculated_stocks_on_hand_stockcardid_idx ON dashboard.vw_calculated_stocks_on_hand(stockcardid uuid_ops);
