-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

CREATE TABLE siglusintegration.local_issue_voucher_sub_draft_line_items
(
    id                          uuid PRIMARY KEY,
    localissuevoucherid         uuid NOT NULL,
    quantityaccepted            integer,
    quantityrejected            integer,
    orderableid                 uuid not null,
    lotid                       uuid,
    notes                       text,
    quantityordered             integer,
    partialfulfilled            integer,
    quantityreturned            integer,
    rejectionreasonid           uuid,
    localissuevouchersubdraftid uuid NOT NULL
);

ALTER TABLE siglusintegration.local_issue_voucher_sub_draft_line_items
    ADD FOREIGN KEY (localissuevouchersubdraftid) REFERENCES local_issue_voucher_sub_draft (id);
