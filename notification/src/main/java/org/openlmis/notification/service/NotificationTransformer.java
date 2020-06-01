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

import static org.openlmis.notification.service.AllowNotifyFilter.ALLOW_NOTIFY_CHANNEL;
import static org.openlmis.notification.service.ChannelFilter.FILTER_CHANNEL;
import static org.openlmis.notification.service.NotificationToSendRetriever.CHANNEL_TO_USE_HEADER;

import java.util.Objects;
import org.openlmis.notification.domain.Notification;
import org.openlmis.notification.domain.NotificationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

@MessageEndpoint
public class NotificationTransformer {

  private static final Logger LOGGER = LoggerFactory.getLogger(NotificationTransformer.class);

  static final String CHANNEL_HEADER = "channel";
  static final String TAG_HEADER = "tag";

  /**
   * Split single notification into several messages. Skips messages that have been sent.
   */
  @Transformer(inputChannel = ALLOW_NOTIFY_CHANNEL, outputChannel = FILTER_CHANNEL)
  public Message extractNotificationMessage(Message<?> message) {
    NotificationChannel channel = message
        .getHeaders()
        .get(CHANNEL_TO_USE_HEADER, NotificationChannel.class);

    Notification notification = (Notification) message.getPayload();
    NotificationMessage notificationMessage = notification
        .getMessages()
        .stream()
        .filter(item -> Objects.equals(channel, item.getChannel()))
        .findFirst()
        .orElse(null);

    if (null == notificationMessage) {
      LOGGER.warn("Can't extract a notification message for channel: {}", channel);
      return null;
    }

    return MessageBuilder
        .withPayload(notificationMessage)
        .copyHeaders(message.getHeaders())
        .setHeader(CHANNEL_HEADER, notificationMessage.getChannel())
        .setHeader(TAG_HEADER, notificationMessage.getTag())
        .removeHeader(CHANNEL_TO_USE_HEADER)
        .build();
  }

}
