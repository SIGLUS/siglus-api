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
import static org.mockito.BDDMockito.given;
import static org.openlmis.notification.service.NotificationToSendRetriever.CHANNEL_TO_USE_HEADER;
import static org.openlmis.notification.service.NotificationToSendRetriever.IMPORTANT_HEADER;
import static org.openlmis.notification.service.NotificationToSendRetriever.RECIPIENT_HEADER;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.openlmis.notification.domain.Notification;
import org.openlmis.notification.domain.PendingNotification;
import org.openlmis.notification.util.NotificationDataBuilder;
import org.springframework.integration.jpa.core.JpaExecutor;
import org.springframework.messaging.Message;

public class NotificationToSendRetrieverTest {

  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock
  private JpaExecutor jpaExecutor;

  private NotificationToSendRetriever retriever;

  private Notification notification = new NotificationDataBuilder()
      .withEmptyMessage(NotificationChannel.EMAIL)
      .buildAsNew();

  private PendingNotification pendingNotification =
      new PendingNotification(notification, NotificationChannel.EMAIL);

  @Before
  public void setUp() {
    retriever = new NotificationToSendRetriever(jpaExecutor);
  }

  @Test
  public void shouldReturnFirstPendingNotificationReadyToSend() {
    // given
    given(jpaExecutor.poll()).willReturn(pendingNotification);

    // when
    Message<Notification> message = retriever.retrieve();

    // then
    assertThat(message).isNotNull();
    assertThat(message.getPayload()).isEqualTo(notification);
    assertThat(message.getHeaders())
        .containsEntry(RECIPIENT_HEADER, notification.getUserId())
        .containsEntry(IMPORTANT_HEADER, notification.getImportant())
        .containsEntry(CHANNEL_TO_USE_HEADER, pendingNotification.getChannel());
  }

  @Test
  public void shouldReturnNullValueIfThereIsNoPendingNotification() {
    // given
    given(jpaExecutor.poll()).willReturn(null);

    // when
    Message<Notification> message = retriever.retrieve();

    // then
    assertThat(message).isNull();
  }
}
