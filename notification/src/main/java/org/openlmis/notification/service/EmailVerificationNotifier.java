/*
 * This program is part of the OpenLMIS logistics management information system platform software.
 * Copyright © 2017 VillageReach
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details. You should have received a copy of
 * the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses.  For additional information contact info@OpenLMIS.org.
 */

package org.openlmis.notification.service;

import static org.openlmis.notification.i18n.MessageKeys.EMAIL_VERIFICATION_EMAIL_BODY;
import static org.openlmis.notification.i18n.MessageKeys.EMAIL_VERIFICATION_EMAIL_SUBJECT;

import java.time.ZonedDateTime;
import java.util.Locale;
import org.openlmis.notification.domain.EmailVerificationToken;
import org.openlmis.notification.domain.UserContactDetails;
import org.openlmis.notification.i18n.ExposedMessageSource;
import org.openlmis.notification.repository.EmailVerificationTokenRepository;
import org.openlmis.notification.service.referencedata.UserDto;
import org.openlmis.notification.service.referencedata.UserReferenceDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailVerificationNotifier {
  private static final long TOKEN_VALIDITY_HOURS = 12;

  @Autowired
  private EmailVerificationTokenRepository emailVerificationTokenRepository;

  @Autowired
  private ExposedMessageSource messageSource;

  @Autowired
  private EmailSender emailSender;

  @Autowired
  private UserReferenceDataService userReferenceDataService;
  
  @Value("${publicUrl}")
  private String publicUrl;

  /**
   * Sends email verification notification.
   *
   * @param contactDetails the contactDetails whose email is being verified
   * @param emailAddress recipient's new email address
   */
  @Async
  public void sendNotification(UserContactDetails contactDetails, String emailAddress, 
      Locale locale) {
    EmailVerificationToken token = createToken(contactDetails, emailAddress);
    sendEmail(contactDetails, emailAddress, token, locale);
  }

  EmailVerificationToken createToken(UserContactDetails contactDetails,
      String emailAddress) {
    EmailVerificationToken token = emailVerificationTokenRepository
        .findOneByUserContactDetails(contactDetails);

    if (token != null) {
      emailVerificationTokenRepository.delete(token);
      // the JPA provider feels free to reorganize and/or optimize the database writes of the
      // pending changes from the persistent context, in particular the JPA provider does not
      // feel obliged to perform the database writes in the ordering and form implicated by
      // the individual changes of the persistent context.

      // the flush() flushes the changes to the database so when the flush() is executed after
      // delete(), sql gets executed and the following save will have no problems.
      emailVerificationTokenRepository.flush();
    }

    token = new EmailVerificationToken();
    token.setUserContactDetails(contactDetails);
    token.setExpiryDate(ZonedDateTime.now().plusHours(TOKEN_VALIDITY_HOURS));
    token.setEmailAddress(emailAddress);

    return emailVerificationTokenRepository.save(token);
  }

  private void sendEmail(UserContactDetails contactDetails, String email,
      EmailVerificationToken token, Locale locale) {
    UserDto referenceDataUser = userReferenceDataService
        .findOne(contactDetails.getReferenceDataUserId());

    String[] bodyMsgArgs = {
        referenceDataUser.getFirstName(),
        referenceDataUser.getLastName(),
        getVerificationPath(contactDetails, token)
    };
    String[] subjectMsgArgs = {};
    
    String subject = messageSource
        .getMessage(EMAIL_VERIFICATION_EMAIL_SUBJECT, subjectMsgArgs, locale);
    String body = messageSource
        .getMessage(EMAIL_VERIFICATION_EMAIL_BODY, bodyMsgArgs, locale);

    emailSender.sendMail(email, subject, body);
  }

  private String getVerificationPath(UserContactDetails contactDetails,
      EmailVerificationToken token) {
    return publicUrl
        + "/api/userContactDetails/"
        + contactDetails.getId()
        + "/verifications/"
        + token.getId();
  }

}
