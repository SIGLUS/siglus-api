-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp IN the file name (that serves as a version) is the latest timestamp, and that no new migration have been added IN the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave IN an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

DELETE FROM "stockmanagement"."valid_destination_assignments"
WHERE nodeid IN (
    SELECT id FROM "stockmanagement"."nodes"
    WHERE referenceid IN (
        SELECT id FROM "stockmanagement"."organizations"
        WHERE name IN ('UNPACK', 'KIT PME US; Sem Dosagem; KIT', 'KIT AL/US (Artemeter+Lumefantrina); 170 Tratamentos+ 400 Testes; KIT')
    )
);

DELETE FROM "stockmanagement"."valid_source_assignments"
WHERE nodeid IN (
    SELECT id FROM "stockmanagement"."nodes"
    WHERE referenceid IN (
        SELECT id FROM "stockmanagement"."organizations"
        WHERE name IN ('UNPACK', 'KIT PME US; Sem Dosagem; KIT', 'KIT AL/US (Artemeter+Lumefantrina); 170 Tratamentos+ 400 Testes; KIT')
    )
);

DELETE FROM "stockmanagement"."nodes"
WHERE referenceid IN (
    SELECT id FROM "stockmanagement"."organizations"
    WHERE name IN ('UNPACK', 'KIT PME US; Sem Dosagem; KIT', 'KIT AL/US (Artemeter+Lumefantrina); 170 Tratamentos+ 400 Testes; KIT')
);

DELETE FROM "stockmanagement"."organizations"
WHERE name IN ('UNPACK', 'KIT PME US; Sem Dosagem; KIT', 'KIT AL/US (Artemeter+Lumefantrina); 170 Tratamentos+ 400 Testes; KIT');
