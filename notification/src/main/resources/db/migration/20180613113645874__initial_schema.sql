CREATE TABLE user_contact_details (
    allownotify boolean DEFAULT true,
    email character varying(255),
    emailVerified boolean DEFAULT false,
    phoneNumber CHARACTER VARYING(255),
    referencedatauserid uuid PRIMARY KEY
);

CREATE UNIQUE INDEX unq_contact_details_email
ON user_contact_details(email)
WHERE email IS NOT NULL;
