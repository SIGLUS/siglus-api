-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

create table siglusintegration.local_receipt_voucher
(
    id                   uuid         not null
        constraint orders_extension_pk primary key,
    ordercode            text         not null,
    status               varchar(255) not null,
    processingperiodid   uuid,
    programid            uuid         not null,
    requestingfacilityid uuid         not null,
    supplyingfacilityid  uuid         not null,
    createdbyid          uuid,
    createddate          timestamp with time zone,
    emergency            boolean,
);