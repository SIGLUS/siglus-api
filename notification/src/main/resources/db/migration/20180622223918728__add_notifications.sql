CREATE TABLE notifications (
    id uuid NOT NULL PRIMARY KEY,
    userid uuid NOT NULL,
    important bool DEFAULT false,
    createddate timestamptz DEFAULT now()
);

CREATE INDEX ON notification.notifications (userid);

CREATE TABLE notification_messages (
    id uuid NOT NULL PRIMARY KEY,
    notificationid uuid NOT NULL,
    channel text NOT NULL,
    body text NOT NULL,
    subject text,
    FOREIGN KEY (notificationid) REFERENCES notifications
);

ALTER TABLE notification_messages
  ADD CONSTRAINT unq_notification_messages_notificationid_channel UNIQUE (notificationid, channel);