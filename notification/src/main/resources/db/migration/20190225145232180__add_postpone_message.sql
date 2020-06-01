-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

CREATE TABLE postpone_message (
  id UUID PRIMARY KEY,
  configurationId UUID NOT NULL,
  body text NOT NULL,
  subject text,
  userId UUID NOT NULL,
  channel text NOT NULL,
  CONSTRAINT fKey_postpone_message_digest_configurations
    FOREIGN KEY (configurationId)
    REFERENCES digest_configurations(id),
  CONSTRAINT fKey_postpone_message_user_contact_details
    FOREIGN KEY (userId)
    REFERENCES user_contact_details(referencedatauserid)
);

CREATE INDEX idx_postpone_message_userid
  ON postpone_message (userId);
CREATE INDEX idx_postpone_message_configurationid
  ON postpone_message (configurationId);
