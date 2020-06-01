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
import static org.openlmis.notification.service.NotificationToSendRetriever.IMPORTANT_HEADER;
import static org.openlmis.notification.service.NotificationToSendRetriever.RECIPIENT_HEADER;
import static org.openlmis.notification.service.NotificationToSendRetriever.START_CHANNEL;

import java.util.UUID;
import org.openlmis.notification.domain.UserContactDetails;
import org.openlmis.notification.repository.UserContactDetailsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.annotation.Filter;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.messaging.handler.annotation.Header;

@MessageEndpoint
public class AllowNotifyFilter {

  private static final Logger LOGGER = LoggerFactory.getLogger(AllowNotifyFilter.class);

  static final String ALLOW_NOTIFY_CHANNEL = "notificationToSend.allowNotify";

  @Autowired
  private UserContactDetailsRepository userContactDetailsRepository;

  /**
   * Checks if user should get a notification.
   */
  @Filter(inputChannel = START_CHANNEL, outputChannel = ALLOW_NOTIFY_CHANNEL)
  public boolean accept(@Header(RECIPIENT_HEADER) UUID recipient,
      @Header(value = IMPORTANT_HEADER, required = false) Boolean important) {
    UserContactDetails userContactDetails = userContactDetailsRepository.findOne(recipient);

    if (null == userContactDetails) {
      LOGGER.error("Can't send notification to a user with id {}"
          + " because user contact details does not exist", recipient);
      return false;
    }

    if (isTrue(important)) {
      LOGGER.debug("The important flag is set");
      return true;
    }

    if (userContactDetails.isAllowNotify()) {
      LOGGER.debug("User {} has set allowNotify flag", recipient);
      return true;
    }

    LOGGER.warn("Can't send notification to a user with id {}"
        + " because the important flag was not set and"
        + " the user unset the allowNotify flag", recipient);

    return false;
  }

}
