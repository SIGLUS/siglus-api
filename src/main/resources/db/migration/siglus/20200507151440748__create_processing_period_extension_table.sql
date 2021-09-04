CREATE TABLE siglusintegration.processing_period_extension
(
    id UUID PRIMARY KEY,
    processingperiodid UUID UNIQUE NOT NULL,
    submitstartdate DATE NOT NULL,
    submitenddate DATE NOT NULL
);
