CREATE TABLE siglusintegration.requisition_extension
(
    id UUID PRIMARY KEY,
    requisitionid UUID UNIQUE NOT NULL,
    actualstartdate DATE NOT NULL,
    actualenddate DATE NOT NULL
);
