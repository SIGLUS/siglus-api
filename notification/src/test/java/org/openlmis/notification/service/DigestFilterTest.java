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
import org.openlmis.notification.domain.DigestSubscription;
import org.openlmis.notification.repository.DigestConfigurationRepository;
import org.openlmis.notification.repository.DigestSubscriptionRepository;
import org.openlmis.notification.service.referencedata.TogglzFeatureDto;
import org.openlmis.notification.service.referencedata.TogglzReferenceDataService;
import org.openlmis.notification.testutils.DigestConfigurationDataBuilder;
import org.openlmis.notification.testutils.DigestSubscriptionDataBuilder;

public class DigestFilterTest {

  private static final UUID RECIPIENT = UUID.randomUUID();

  private static final String CORRECT_TAG = "correct-tag";
  private static final String INCORRECT_TAG = "in" + CORRECT_TAG;
  private static final String CORRECT_NON_DIGEST_TAG = "correct-non-digest-tag";
  private static final String EMPTY_TAG = "";

  private static final boolean IMPORTANT = true;
  private static final boolean UNIMPORTANT = false;

  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock
  private TogglzReferenceDataService togglzReferenceDataService;

  @Mock
  private DigestConfigurationRepository digestConfigurationRepository;

  @Mock
  private DigestSubscriptionRepository digestSubscriptionRepository;

  @InjectMocks
  private DigestFilter filter = new DigestFilter();

  private DigestConfiguration digestConfiguration = new DigestConfigurationDataBuilder()
      .withTag(CORRECT_TAG)
      .build();

  private DigestConfiguration nonDigestConfiguration = new DigestConfigurationDataBuilder()
      .withTag(CORRECT_NON_DIGEST_TAG)
      .build();

  private DigestSubscription digestSubscription = new DigestSubscriptionDataBuilder()
      .withDigestConfiguration(digestConfiguration)
      .withUseDigest(true)
      .build();

  private DigestSubscription nonDigestSubscription = new DigestSubscriptionDataBuilder()
      .withDigestConfiguration(nonDigestConfiguration)
      .withUseDigest(false)
      .build();

  private TogglzFeatureDto digestFeature =
      new TogglzFeatureDto(DigestFilter.CONSOLIDATE_NOTIFICATIONS, true, null, null);

  @Before
  public void setUp() {
    given(togglzReferenceDataService.findAll()).willReturn(Lists.newArrayList(digestFeature));
    given(digestConfigurationRepository.findByTag(CORRECT_TAG)).willReturn(digestConfiguration);
    given(digestConfigurationRepository.findByTag(CORRECT_NON_DIGEST_TAG))
        .willReturn(nonDigestConfiguration);
    given(digestSubscriptionRepository.findBy(RECIPIENT, digestConfiguration))
        .willReturn(digestSubscription);
    given(digestSubscriptionRepository.findBy(RECIPIENT, nonDigestConfiguration))
        .willReturn(nonDigestSubscription);
  }

  @Test
  public void shouldReturnSendNowChannelIfImportantFlagIsSet() {
    // when
    String channelName = filter.route(RECIPIENT, IMPORTANT, EMPTY_TAG);

    // then
    assertThat(channelName).isEqualTo(DigestFilter.SEND_NOW_PREPARE_CHANNEL);
  }

  @Test
  public void shouldReturnSendNowChannelIfDigestFeatureIsOff() {
    // given
    digestFeature.setEnabled(false);

    // when
    String channelName = filter.route(RECIPIENT, UNIMPORTANT, CORRECT_TAG);

    // then
    assertThat(channelName).isEqualTo(DigestFilter.SEND_NOW_PREPARE_CHANNEL);
  }

  @Test
  public void shouldReturnSendNowChannelIfTagIsEmpty() {
    // when
    String channelName = filter.route(RECIPIENT, UNIMPORTANT, EMPTY_TAG);

    // then
    assertThat(channelName).isEqualTo(DigestFilter.SEND_NOW_PREPARE_CHANNEL);
  }

  @Test
  public void shouldReturnSendNowChannelIfConfigurationNotExistForTag() {
    // when
    String channelName = filter.route(RECIPIENT, UNIMPORTANT, INCORRECT_TAG);

    // then
    assertThat(channelName).isEqualTo(DigestFilter.SEND_NOW_PREPARE_CHANNEL);
  }

  @Test
  public void shouldReturnSendNowChannelIfUserIsNotSubscribedForTag() {
    // given
    given(digestSubscriptionRepository
        .findBy(RECIPIENT, digestConfiguration))
        .willReturn(null);

    // when
    String channelName = filter.route(RECIPIENT, UNIMPORTANT, CORRECT_TAG);

    // then
    assertThat(channelName).isEqualTo(DigestFilter.SEND_NOW_PREPARE_CHANNEL);
  }

  @Test
  public void shouldReturnPostponeChannelIfUserIsSubscribedForTag() {
    // when
    String channelName = filter.route(RECIPIENT, UNIMPORTANT, CORRECT_TAG);

    // then
    assertThat(channelName).isEqualTo(DigestFilter.SEND_NOW_POSTPONE_CHANNEL);
  }

  @Test
  public void shouldReturnSendNowChannelIfDigestSubscriptionDoesNotUseDigest() {
    // when
    String channelName = filter.route(RECIPIENT, UNIMPORTANT, CORRECT_NON_DIGEST_TAG);

    // then
    assertThat(channelName).isEqualTo(DigestFilter.SEND_NOW_PREPARE_CHANNEL);
  }

}
