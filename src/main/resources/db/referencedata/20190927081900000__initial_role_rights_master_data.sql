-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

INSERT INTO "referencedata"."role_rights"("roleid","rightid")
VALUES
('d8ff49b1-96e7-4710-9511-289f490f7577','31cce55f-284b-4922-81bb-d8a9edc4c623'),
('d8ff49b1-96e7-4710-9511-289f490f7577','6fb013fe-d878-43e9-bff0-fa5431e62c34'),
('d8ff49b1-96e7-4710-9511-289f490f7577','7b41c10e-5489-47a9-8a68-69ae74b8a4cf'),
('b6134c01-cb37-4303-85f0-8c8eab9d3cec','c3eb5df0-c3ac-4e70-a978-02827462f60e'),
('b6134c01-cb37-4303-85f0-8c8eab9d3cec','e101d2b8-6a0f-4af6-a5de-a9576b4ebc50'),
('b6134c01-cb37-4303-85f0-8c8eab9d3cec','feb4c0b8-f6d2-4289-b29d-811c1d0b2863'),
('185db8f7-ee35-44d0-8b40-6de12489ae77','24df2715-850c-4336-b650-90eb78c544bf'),
('185db8f7-ee35-44d0-8b40-6de12489ae77','9ade922b-3523-4582-bef4-a47701f7df14'),
('185db8f7-ee35-44d0-8b40-6de12489ae77','c3eb5df0-c3ac-4e70-a978-02827462f60e'),
('185db8f7-ee35-44d0-8b40-6de12489ae77','e101d2b8-6a0f-4af6-a5de-a9576b4ebc50');
