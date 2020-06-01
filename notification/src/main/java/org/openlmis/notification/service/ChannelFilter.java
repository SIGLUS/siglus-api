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

import static org.openlmis.notification.service.NotificationToSendRetriever.RECIPIENT_HEADER;
import static org.openlmis.notification.service.NotificationTransformer.CHANNEL_HEADER;
import static org.openlmis.notification.service.NotificationTransformer.TAG_HEADER;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import org.openlmis.notification.domain.DigestSubscription;
import org.openlmis.notification.repository.DigestSubscriptionRepository;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.annotation.Filter;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.messaging.handler.annotation.Header;

@MessageEndpoint
public class ChannelFilter {

  private static final XLogger XLOGGER = XLoggerFactory.getXLogger(ChannelFilter.class);


  static final String READY_TO_SEND_CHANNEL = "notificationToSend.readyToSend";
  static final String FILTER_CHANNEL = "notificationToSend.filter";

  @Autowired
  private DigestSubscriptionRepository repository;

  /**
   * Checks if user prefers the channel for the given notification.
   */
  @Filter(inputChannel = FILTER_CHANNEL, outputChannel = READY_TO_SEND_CHANNEL)
  public boolean accept(@Header(RECIPIENT_HEADER) UUID recipient,
      @Header(CHANNEL_HEADER) NotificationChannel channel,
      @Header(value = TAG_HEADER, required = false) String messageTag) {
    XLOGGER.entry(recipient, channel, messageTag);

    Optional<DigestSubscription> subscriptionForTag = repository.getUserSubscriptions(recipient)
        .stream()
        .filter(buildTagMatcher(messageTag))
        .findFirst();

    if (subscriptionForTag.isPresent()) {
      XLOGGER.exit(subscriptionForTag.get().getPreferredChannel().equals(channel));
      return subscriptionForTag.get().getPreferredChannel().equals(channel);
    }
    XLOGGER.exit(NotificationChannel.EMAIL.equals(channel));
    return NotificationChannel.EMAIL.equals(channel);
  }

  private Predicate<DigestSubscription> buildTagMatcher(String messageTag) {
    return digestSubscription -> digestSubscription.getDigestConfiguration().getTag()
      .equals(messageTag);
  }

}
