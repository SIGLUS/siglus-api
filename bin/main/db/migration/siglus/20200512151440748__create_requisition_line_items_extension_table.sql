CREATE TABLE siglusintegration.requisition_line_items_extension
(
    id UUID PRIMARY KEY,
    requisitionlineitemid UUID UNIQUE NOT NULL,
    authorizedQuantity integer NOT NULL
);
