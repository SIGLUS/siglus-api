CREATE TABLE siglusintegration.program_extension
(
    id UUID PRIMARY KEY,
    programid UUID UNIQUE NOT NULL,
    code character varying(255),
    name text,
    isvirtual BOOLEAN DEFAULT False,
    parentid UUID,
    issupportemergency BOOLEAN DEFAULT FALSE
);
