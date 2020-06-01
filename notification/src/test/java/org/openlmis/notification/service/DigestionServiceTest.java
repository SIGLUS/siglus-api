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
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.openlmis.notification.service.NotificationChannel.EMAIL;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.stubbing.Answer;
import org.openlmis.notification.domain.DigestConfiguration;
import org.openlmis.notification.domain.DigestSubscription;
import org.openlmis.notification.domain.NotificationMessage;
import org.openlmis.notification.domain.PostponeMessage;
import org.openlmis.notification.repository.DigestConfigurationRepository;
import org.openlmis.notification.repository.DigestSubscriptionRepository;
import org.openlmis.notification.repository.PostponeMessageRepository;
import org.openlmis.notification.testutils.DigestConfigurationDataBuilder;
import org.openlmis.notification.testutils.DigestSubscriptionDataBuilder;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;

public class DigestionServiceTest {

  private static final Table<UUID, String, SourcePollingChannelAdapter> ADAPTERS =
      HashBasedTable.create();

  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock
  private DigestConfigurationRepository digestConfigurationRepository;

  @Mock
  private DigestSubscriptionRepository digestSubscriptionRepository;

  @Mock
  private PostponeMessageRepository postponeMessageRepository;

  @InjectMocks
  @Spy
  private DigestionService service = new DigestionService(ADAPTERS);

  private DigestConfiguration configuration = new DigestConfigurationDataBuilder()
      .withMessage("This is digest configuration message: {count}")
      .build();
  private DigestSubscription subscription = new DigestSubscriptionDataBuilder().build();
  private NotificationMessage message = new NotificationMessage(EMAIL, "body", "subject");
  private NotificationChannel channel = message.getChannel();
  private UUID recipient = UUID.randomUUID();
  private String tag = configuration.getTag();
  private PostponeMessage postpone = new PostponeMessage(configuration, message.getBody(),
      message.getSubject(), recipient, channel);

  @Before
  public void setUp() {
    willAnswer(new MockSetPollingAdapter())
        .given(service)
        .setPollingAdapter(any(), any(), any(), any());

    given(digestConfigurationRepository.findByTag(tag)).willReturn(configuration);

    given(digestSubscriptionRepository.findBy(recipient, configuration)).willReturn(subscription);
  }

  @After
  public void tearDown() {
    ADAPTERS.clear();
  }

  @Test
  public void shouldDoNothingIfDigestConfigurationDoesNotExist() {
    // given
    given(digestConfigurationRepository.findByTag(tag)).willReturn(null);

    // when
    service.handleMessage(message, channel, recipient, tag);

    // then
    verifyZeroInteractions(digestSubscriptionRepository, postponeMessageRepository);
  }

  @Test
  public void shouldDoNothingIfDigestSubscriptionDoesNotExist() {
    // given
    given(digestSubscriptionRepository.findBy(recipient, configuration)).willReturn(null);

    // when
    service.handleMessage(message, channel, recipient, tag);

    // then
    verifyZeroInteractions(postponeMessageRepository);
  }

  @Test
  public void shouldSetAdapterIfAllResourcesAreAvailable() {
    // when
    service.handleMessage(message, channel, recipient, tag);

    // then
    verify(postponeMessageRepository).saveAndFlush(postpone);
    verify(service).setPollingAdapter(channel, configuration,
        recipient, subscription.getCronExpression());
  }

  @Test
  public void shouldNotSetAnotherAdapterForSameSettings() {
    // given
    DigestConfiguration anotherConfiguration = new DigestConfigurationDataBuilder().build();
    given(digestConfigurationRepository.findByTag(anotherConfiguration.getTag()))
        .willReturn(anotherConfiguration);
    given(digestSubscriptionRepository.findBy(recipient, anotherConfiguration))
        .willReturn(subscription);

    // when
    service.handleMessage(message, channel, recipient, tag);
    service.handleMessage(message, channel, recipient, anotherConfiguration.getTag());
    service.handleMessage(message, channel, recipient, tag);

    // then
    verify(service, times(1))
        .setPollingAdapter(channel, configuration, recipient, subscription.getCronExpression());
    verify(service, times(1))
        .setPollingAdapter(channel, anotherConfiguration,
            recipient, subscription.getCronExpression());
  }

  @Test
  public void shouldDropExistingPollingAdapters() {
    // given
    ADAPTERS.put(recipient, tag, new SourcePollingChannelAdapter());
    ADAPTERS.put(recipient, "another-tag", new SourcePollingChannelAdapter());

    // when
    service.dropExistingPollingAdapters(recipient);

    // then
    assertThat(ADAPTERS.size()).isEqualTo(0);
  }

  private static final class MockSetPollingAdapter implements Answer<Object> {

    @Override
    public Object answer(InvocationOnMock invocation) {
      UUID recipient = invocation.getArgumentAt(2, UUID.class);
      DigestConfiguration configuration = invocation.getArgumentAt(1, DigestConfiguration.class);
      SourcePollingChannelAdapter adapter = mock(SourcePollingChannelAdapter.class);

      ADAPTERS.put(recipient, configuration.getTag(), adapter);
      return null;
    }

  }
}
