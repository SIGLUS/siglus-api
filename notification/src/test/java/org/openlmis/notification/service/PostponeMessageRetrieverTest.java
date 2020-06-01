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
import static org.openlmis.notification.service.NotificationToSendRetriever.RECIPIENT_HEADER;
import static org.openlmis.notification.service.NotificationTransformer.CHANNEL_HEADER;
import static org.openlmis.notification.service.PostponeMessageRetriever.CONFIGURATION_ID_HEADER;

import java.util.List;
import java.util.UUID;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.openlmis.notification.domain.PostponeMessage;
import org.openlmis.notification.testutils.PostponeMessageDataBuilder;
import org.springframework.integration.jpa.core.JpaExecutor;
import org.springframework.messaging.Message;

public class PostponeMessageRetrieverTest {

  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock
  private JpaExecutor jpaExecutor;

  private PostponeMessageRetriever retriever;

  private NotificationChannel channel = NotificationChannel.EMAIL;
  private UUID configurationId = UUID.randomUUID();
  private UUID userId = UUID.randomUUID();

  private PostponeMessage postpone = new PostponeMessageDataBuilder().build();

  @Before
  public void setUp() {
    retriever = new PostponeMessageRetriever(jpaExecutor, channel, configurationId, userId);
  }

  @Test
  public void shouldHandleSinglePostponeNotifications() {
    // given
    given(jpaExecutor.poll()).willReturn(postpone);

    // when
    Message<List<PostponeMessage>> message = retriever.receive();

    // then
    assertThat(message).isNotNull();
    assertThat(message.getPayload())
        .hasSize(1)
        .contains(postpone);
    assertThat(message.getHeaders())
        .containsEntry(RECIPIENT_HEADER, userId)
        .containsEntry(CONFIGURATION_ID_HEADER, configurationId)
        .containsEntry(CHANNEL_HEADER, channel);
  }

  @Test
  public void shouldHandleListOfPostponeNotifications() {
    // given
    given(jpaExecutor.poll()).willReturn(Lists.newArrayList(postpone));

    // when
    Message<List<PostponeMessage>> message = retriever.receive();

    // then
    assertThat(message).isNotNull();
    assertThat(message.getPayload())
        .hasSize(1)
        .contains(postpone);
    assertThat(message.getHeaders())
        .containsEntry(RECIPIENT_HEADER, userId)
        .containsEntry(CONFIGURATION_ID_HEADER, configurationId)
        .containsEntry(CHANNEL_HEADER, channel);
  }

  @Test
  public void shouldReturnNullValueIfThereIsNoPostponeNotification() {
    // given
    given(jpaExecutor.poll()).willReturn(null);

    // when
    Message<List<PostponeMessage>> message = retriever.receive();

    // then
    assertThat(message).isNull();
  }

  @Test
  public void shouldReturnNullValueIfCreatedCollectionIsEmpty() {
    // given
    given(jpaExecutor.poll()).willReturn(Lists.newArrayList());

    // when
    Message<List<PostponeMessage>> message = retriever.receive();

    // then
    assertThat(message).isNull();
  }
}
