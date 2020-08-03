-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

CREATE TABLE siglusintegration.regimen_categories (
    id uuid NOT NULL,
    code character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    displayorder integer NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (code)
);

CREATE TABLE siglusintegration.regimen_dispatch_lines (
    id uuid NOT NULL,
    code character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    displayorder integer NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (code)
);

CREATE TABLE siglusintegration.regimens (
    id uuid NOT NULL,
    programid uuid NOT NULL,
    categoryid uuid NOT NULL,
    dispatchlineid uuid NOT NULL,
    code character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    displayorder integer NOT NULL,
    active boolean NOT NULL DEFAULT TRUE,
    iscustom boolean NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    UNIQUE (code, programid),
    FOREIGN KEY(programid) REFERENCES referencedata.programs(id),
    FOREIGN KEY(categoryid) REFERENCES siglusintegration.regimen_categories(id),
    FOREIGN KEY(dispatchlineid) REFERENCES siglusintegration.regimen_dispatch_lines(id)
);
