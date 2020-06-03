ALTER TABLE fulfillment.orders ADD COLUMN lastupdateddate timestamp with time zone;
ALTER TABLE fulfillment.orders ADD COLUMN lastupdaterid uuid;

UPDATE fulfillment.orders SET lastupdateddate = createddate;
UPDATE fulfillment.orders SET lastupdaterid = createdbyid;

ALTER TABLE fulfillment.orders ALTER COLUMN lastupdateddate SET NOT NULL;
ALTER TABLE fulfillment.orders ALTER COLUMN lastupdaterid SET NOT NULL;
