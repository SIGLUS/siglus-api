ALTER TABLE transfer_properties
    ADD transferType VARCHAR(50);

UPDATE transfer_properties
    SET transferType = 'ORDER';

ALTER TABLE transfer_properties
  ALTER COLUMN transferType SET NOT NULL;

ALTER TABLE transfer_properties
  DROP CONSTRAINT IF EXISTS transfer_properties_facility_id_unique;

CREATE UNIQUE INDEX
  transfer_properties_facility_id_transfer_type_unique
  ON transfer_properties (transferType, facilityId);

