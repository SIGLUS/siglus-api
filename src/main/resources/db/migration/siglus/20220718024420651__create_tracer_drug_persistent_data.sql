-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

-- auto-generated definition
create table tracer_drug_persistent_data
(
    id              serial
        constraint tracer_drug_persistent_data_pk
            primary key,
    productcode     text,
    cmm             double precision,
    facilitycode    text,
    computationtime date,
    stockonhand     integer
);

alter table tracer_drug_persistent_data
    owner to postgres;

create unique index tracer_drug_persistent_data_id_uindex
    on tracer_drug_persistent_data (id);

create unique index tracer_drug_persistent_data_productcode_facilitycode_computatio
    on tracer_drug_persistent_data (productcode, facilitycode, computationtime);