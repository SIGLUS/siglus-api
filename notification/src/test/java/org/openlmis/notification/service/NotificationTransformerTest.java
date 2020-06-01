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

import static org.assertj.core.api.Assertions.assertThat;
import static org.openlmis.notification.service.NotificationToSendRetriever.CHANNEL_TO_USE_HEADER;
import static org.openlmis.notification.service.NotificationTransformer.CHANNEL_HEADER;

import org.junit.Test;
import org.openlmis.notification.domain.Notification;
import org.openlmis.notification.domain.NotificationMessage;
import org.openlmis.notification.util.NotificationDataBuilder;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

public class NotificationTransformerTest {

  private NotificationTransformer transformer = new NotificationTransformer();

  private Notification notification = new NotificationDataBuilder()
      .withEmptyMessage(NotificationChannel.EMAIL)
      .build();

  private Message<?> message = MessageBuilder
      .withPayload(notification)
      .setHeader(CHANNEL_TO_USE_HEADER, NotificationChannel.EMAIL)
      .build();

  @Test
  public void shouldExtractNotificationMessages() {
    // when
    Message<?> newMessage = transformer.extractNotificationMessage(message);

    // then
    NotificationMessage existingMessage = notification.getMessages().get(0);

    assertThat(newMessage.getPayload()).isEqualTo(existingMessage);
    assertThat(newMessage.getHeaders())
        .containsEntry(CHANNEL_HEADER, existingMessage.getChannel())
        .doesNotContainKey(CHANNEL_TO_USE_HEADER);
  }

  @Test
  public void shouldReturnNullIfThereIsNoMessageForGivenChannel() {
    // given
    message = MessageBuilder
        .fromMessage(message)
        .setHeader(CHANNEL_TO_USE_HEADER, NotificationChannel.SMS)
        .build();

    // when
    Message<?> newMessage = transformer.extractNotificationMessage(message);

    // then
    assertThat(newMessage).isNull();

  }
}
