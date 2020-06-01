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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.notification.domain.DigestSubscription;
import org.openlmis.notification.repository.DigestSubscriptionRepository;
import org.openlmis.notification.testutils.DigestConfigurationDataBuilder;
import org.openlmis.notification.testutils.DigestSubscriptionDataBuilder;
import org.openlmis.notification.util.UserContactDetailsDataBuilder;

@RunWith(MockitoJUnitRunner.class)
public class ChannelFilterTest {

  private static final String MESSAGE_TAG = "messageTag";
  private static final String MESSAGE_TAG_TWO = "messageTagTwo";
  private static final String MESSAGE_TAG_THREE = "messageTagThree";

  @Mock
  private DigestSubscriptionRepository repository;

  @InjectMocks
  private ChannelFilter channelFilter;

  private UUID userId;

  @Before
  public void setUp() {
    userId = UUID.randomUUID();

    DigestSubscription digestSubscriptionForSmsChannel = new DigestSubscriptionDataBuilder()
        .withPreferredChannel(NotificationChannel.SMS)
        .withDigestConfiguration(
          new DigestConfigurationDataBuilder()
              .withTag(MESSAGE_TAG_TWO)
              .build()
        )
        .withUserContactDetails(
          new UserContactDetailsDataBuilder()
              .withReferenceDataUserId(userId)
              .build()
        )
        .build();

    DigestSubscription digestSubscriptionForEmailChannel = new DigestSubscriptionDataBuilder()
        .withPreferredChannel(NotificationChannel.EMAIL)
        .withDigestConfiguration(
          new DigestConfigurationDataBuilder()
              .withTag(MESSAGE_TAG)
              .build()
        )
        .withUserContactDetails(
          new UserContactDetailsDataBuilder()
              .withReferenceDataUserId(userId)
              .build()
        )
        .build();

    when(repository.getUserSubscriptions(userId))
        .thenReturn(Arrays.asList(digestSubscriptionForEmailChannel,
            digestSubscriptionForSmsChannel));
  }

  @Test
  public void acceptShouldReturnTrueIfMessageUsesUserPreferredChannel() {
    assertTrue(channelFilter.accept(userId, NotificationChannel.EMAIL, MESSAGE_TAG));
  }

  @Test
  public void acceptShouldReturnFalseIfMessageUsesNonUserPreferredChannel() {
    assertFalse(channelFilter.accept(userId, NotificationChannel.SMS, MESSAGE_TAG));
  }

  @Test
  public void acceptShouldReturnTrueIfUserHasNotDigestSubscriptionAndChannelIsEmail() {
    assertTrue(channelFilter.accept(userId, NotificationChannel.EMAIL, MESSAGE_TAG_THREE));
  }

  @Test
  public void acceptShouldReturnTrueIfMessageHasNoTagAndUsesEmailChannel() {
    assertTrue(channelFilter.accept(userId, NotificationChannel.EMAIL, null));
  }

}