CREATE TABLE email_verification_tokens (
    id uuid PRIMARY KEY,
    expirydate timestamp with time zone NOT NULL,
    emailAddress CHARACTER VARYING NOT NULL,
    userContactDetailsId uuid NOT NULL
);

CREATE UNIQUE INDEX unq_email_verification_tokens_emailaddress
ON email_verification_tokens(emailAddress);

CREATE UNIQUE INDEX unq_email_verification_tokens_usercontactdetailsid
ON email_verification_tokens(userContactDetailsId);

ALTER TABLE ONLY email_verification_tokens
ADD CONSTRAINT email_verification_tokens_usercontactdetailsid_fk
FOREIGN KEY (userContactDetailsId)
REFERENCES user_contact_details(referencedatauserid);
