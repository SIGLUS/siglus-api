-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

CREATE TABLE digest_subscriptions (
  id uuid NOT NULL PRIMARY KEY,
  userContactDetailsId uuid NOT NULL,
  digestConfigurationId uuid NOT NULL,
  cronExpression VARCHAR(255) NOT NULL,
  CONSTRAINT fKey_digest_subscriptions_user_contact_details
    FOREIGN KEY (userContactDetailsId)
    REFERENCES user_contact_details(referencedatauserid),
  CONSTRAINT fKey_digest_subscriptions_digest_configurations
    FOREIGN KEY (digestConfigurationId)
    REFERENCES digest_configurations(id));
