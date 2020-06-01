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

import static org.apache.commons.lang3.BooleanUtils.isTrue;
import static org.openlmis.notification.service.NotificationChannelRouter.EMAIL_SEND_NOW_CHANNEL;
import static org.openlmis.notification.service.NotificationToSendRetriever.IMPORTANT_HEADER;
import static org.openlmis.notification.service.NotificationToSendRetriever.RECIPIENT_HEADER;

import java.util.UUID;
import org.openlmis.notification.domain.NotificationMessage;
import org.openlmis.notification.domain.UserContactDetails;
import org.openlmis.notification.repository.UserContactDetailsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.handler.annotation.Header;

@MessageEndpoint
public class EmailNotificationChannelHandler {
  private static final Logger LOGGER = LoggerFactory
      .getLogger(EmailNotificationChannelHandler.class);

  @Autowired
  private UserContactDetailsRepository userContactDetailsRepository;

  @Autowired
  private EmailSender emailSender;

  /**
   * Tries to send a notification to a user by using email channel.
   */
  @ServiceActivator(inputChannel = EMAIL_SEND_NOW_CHANNEL)
  public void handle(NotificationMessage payload,
      @Header(RECIPIENT_HEADER) UUID recipient,
      @Header(value = IMPORTANT_HEADER, required = false) Boolean important) {
    UserContactDetails contactDetails = userContactDetailsRepository.findOne(recipient);

    if (shouldSendMessage(contactDetails, important)) {
      emailSender.sendMail(contactDetails.getEmailAddress(),
          payload.getSubject(), payload.getBody());
    }
  }

  private boolean shouldSendMessage(UserContactDetails contactDetails, Boolean important) {
    if (!contactDetails.hasEmailAddress()) {
      LOGGER.error(
          "Can't send email because user with id {} has not email address",
          contactDetails.getReferenceDataUserId());
      return false;
    }

    if (isTrue(important)) {
      LOGGER.debug("The important flag is set");
      return true;
    }

    if (contactDetails.isEmailAddressVerified()) {
      LOGGER.debug(
          "Email address of user with id {} is verified",
          contactDetails.getReferenceDataUserId());
      return true;
    }

    LOGGER.error(
        "Can't send email because email address of user with id {} was not verified",
        contactDetails.getReferenceDataUserId());

    return false;
  }

}
