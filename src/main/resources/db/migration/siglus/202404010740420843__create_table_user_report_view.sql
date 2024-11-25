-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.
CREATE TABLE IF NOT EXISTS siglusintegration.user_report_view
(
    id                     uuid PRIMARY KEY,
    userid                 uuid NOT NULL,
    provinceid             uuid ,
    districtid             uuid
);

CREATE UNIQUE INDEX IF NOT EXISTS unq_userid_provinceid_districtid
    ON siglusintegration.user_report_view (userid uuid_ops, provinceid uuid_ops, districtid uuid_ops);
