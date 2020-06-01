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

import static org.openlmis.notification.service.DigestFilter.SEND_NOW_PREPARE_CHANNEL;
import static org.openlmis.notification.service.NotificationTransformer.CHANNEL_HEADER;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.Router;
import org.springframework.messaging.handler.annotation.Header;

@MessageEndpoint
public class NotificationChannelRouter {

  private static final Logger LOGGER = LoggerFactory.getLogger(NotificationChannelRouter.class);

  static final String EMAIL_SEND_NOW_CHANNEL = "notificationToSend.sendNow.readyToSend.email";
  static final String SMS_SEND_NOW_CHANNEL = "notificationToSend.sendNow.readyToSend.sms";

  /**
   * Defines which handler should be used for notification.
   */
  @Router(inputChannel = SEND_NOW_PREPARE_CHANNEL)
  public String route(@Header(CHANNEL_HEADER) NotificationChannel channel) {
    if (NotificationChannel.EMAIL == channel) {
      return EMAIL_SEND_NOW_CHANNEL;
    } else if (NotificationChannel.SMS == channel) {
      return SMS_SEND_NOW_CHANNEL;
    }

    LOGGER.warn("Unknown notification channel: {}", channel);
    return null;
  }

}
