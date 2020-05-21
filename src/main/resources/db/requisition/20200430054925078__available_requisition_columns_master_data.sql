-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

INSERT INTO "requisition"."available_requisition_columns"("id","canbechangedbyuser","canchangeorder","columntype","definition","indicator","isdisplayrequired","label","mandatory","name","supportstag")
VALUES
('96bd0839-4a83-4588-8d78-5566ef80dc89',FALSE,TRUE,'NUMERIC','Difference = Inventory -(Stock at Beginning of Period + Sum of Entries - Issues).','DI',FALSE,'Difference',FALSE,'difference',FALSE),
('03d4fe58-12a1-41ce-80b8-d6374342d7ae',FALSE,TRUE,'NUMERIC','Final authorized quantity. This is quantified in dispensing units','QA',FALSE,'Quantity Authorized',FALSE,'authorizedQuantity',FALSE),
('d546e757-5276-4806-87fe-aa10017467d5',FALSE,TRUE,'NUMERIC','Theoretical Quantity to Request = 2 * issues - inventory','TQ',FALSE,'Theoretical Quantity to Request',FALSE,'theoreticalQuantityToRequest',FALSE),
('7e022648-52da-46ab-8001-a0e6388a0964',FALSE,TRUE,'NUMERIC','Theoretical Stock at End of Period = stock at beginning of period + sum of entries -issues','TS',FALSE,'Theoretical Stock at End of Period',FALSE,'theoreticalStockAtEndofPeriod',FALSE);

INSERT INTO "requisition"."available_requisition_column_sources"("columnid","value")
VALUES
('7e022648-52da-46ab-8001-a0e6388a0964','CALCULATED'),
('d546e757-5276-4806-87fe-aa10017467d5','CALCULATED'),
('03d4fe58-12a1-41ce-80b8-d6374342d7ae','USER_INPUT'),
('96bd0839-4a83-4588-8d78-5566ef80dc89','CALCULATED');
