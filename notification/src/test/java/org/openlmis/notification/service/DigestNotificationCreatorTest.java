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

import java.util.List;
import java.util.UUID;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.openlmis.notification.domain.DigestConfiguration;
import org.openlmis.notification.domain.PostponeMessage;
import org.openlmis.notification.i18n.MessageService;
import org.openlmis.notification.repository.DigestConfigurationRepository;
import org.openlmis.notification.testutils.DigestConfigurationDataBuilder;
import org.openlmis.notification.testutils.PostponeMessageDataBuilder;
import org.springframework.messaging.Message;
import org.springframework.test.util.ReflectionTestUtils;

public class DigestNotificationCreatorTest {

  private static final String SERVICE_URL = "http://localhost";

  private static final String MSG_TEMPLATE = "There are ${count} notifications. ${serviceUrl}";
  private static final String EXPECTED_MSG = "There are 2 notifications. " + SERVICE_URL;

  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock
  private DigestConfigurationRepository digestConfigurationRepository;

  @Mock
  private MessageService messageService;

  @InjectMocks
  private DigestNotificationCreator creator;

  private DigestConfiguration configuration = new DigestConfigurationDataBuilder()
      .withMessage(MSG_TEMPLATE)
      .build();
  private List<PostponeMessage> postpones = Lists.newArrayList(
      new PostponeMessageDataBuilder().build(),
      new PostponeMessageDataBuilder().build());
  private UUID recipient = UUID.randomUUID();
  private UUID configurationId = configuration.getId();
  private NotificationChannel channel = NotificationChannel.EMAIL;

  private org.openlmis.notification.i18n.Message message =
      new org.openlmis.notification.i18n.Message(MSG_TEMPLATE);

  @Before
  public void setUp() {
    given(digestConfigurationRepository.findOne(configurationId)).willReturn(configuration);
    given(messageService.localize(message)).willReturn(message.localMessage(MSG_TEMPLATE));

    ReflectionTestUtils.setField(creator, "serviceUrl", SERVICE_URL);
  }

  @Test
  public void shouldReturnNullIfConfigurationDoesNotExist() {
    // given
    given(digestConfigurationRepository.findOne(configurationId)).willReturn(null);

    // when
    Message message = creator
        .createDigestNotification(postpones, recipient, configurationId, channel);

    // then
    assertThat(message).isNull();
  }

  @Test
  public void shouldCreateDigestNotification() {
    // when
    Message message = creator
        .createDigestNotification(postpones, recipient, configurationId, channel);

    // then
    assertThat(message).isNotNull();
    assertThat(message.getPayload())
        .hasFieldOrPropertyWithValue("channel", channel)
        .hasFieldOrPropertyWithValue("body", EXPECTED_MSG)
        .hasFieldOrPropertyWithValue("subject", postpones.get(0).getSubject());
    assertThat(message.getHeaders())
        .containsEntry(RECIPIENT_HEADER, recipient)
        .containsEntry(CHANNEL_HEADER, channel);
  }
}
