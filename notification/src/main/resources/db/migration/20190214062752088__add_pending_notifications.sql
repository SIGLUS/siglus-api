-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

CREATE TABLE pending_notifications (
  notificationId UUID NOT NULL,
  channel VARCHAR(255) NOT NULL,
  createdDate timestamptz DEFAULT now(),
  CONSTRAINT pKey_pending_notifications
    PRIMARY KEY (notificationId, channel),
  CONSTRAINT fKey_pending_notifications_notifications
    FOREIGN KEY (notificationId)
    REFERENCES notifications(id)
);

CREATE UNIQUE INDEX pending_notifications_unique_idx
  ON pending_notifications (channel, notificationId);
