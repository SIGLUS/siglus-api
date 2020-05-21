CREATE TABLE siglusintegration.stock_card_extension
(
    id UUID PRIMARY KEY NOT NULL,
    stockcardid UUID NOT NULL,
    createdate date
);