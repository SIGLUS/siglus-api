-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

create index IF NOT EXISTS requisitions_modifieddate_index
    on requisition.requisitions (modifieddate);

create index IF NOT EXISTS proofs_of_delivery_receiveddate_index
    on fulfillment.proofs_of_delivery (receiveddate);
