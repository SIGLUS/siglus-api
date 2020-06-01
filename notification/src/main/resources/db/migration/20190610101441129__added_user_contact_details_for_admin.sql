-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

INSERT INTO user_contact_details (referencedatauserid, allownotify, email, emailVerified)
    SELECT '35316636-6264-6331-2d34-3933322d3462', true, 'example@mail.com', true
WHERE
    NOT EXISTS (
        SELECT referencedatauserid FROM user_contact_details WHERE referencedatauserid = '35316636-6264-6331-2d34-3933322d3462'
    );