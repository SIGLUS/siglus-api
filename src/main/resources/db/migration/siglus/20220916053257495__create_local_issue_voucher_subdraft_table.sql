-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

CREATE TABLE siglusintegration.local_issue_voucher_sub_draft
(
    id                  uuid PRIMARY KEY,
    number              integer NOT NULL,
    status              integer,
    operatorid          uuid,
    localissuevoucherid uuid REFERENCES siglusintegration.local_issue_voucher (id)
);

-- Indices -------------------------------------------------------

CREATE UNIQUE INDEX local_issue_voucher_sub_draft_localissuevoucherid_number_idx ON siglusintegration.local_issue_voucher_sub_draft (localissuevoucherid uuid_ops, number int4_ops);
