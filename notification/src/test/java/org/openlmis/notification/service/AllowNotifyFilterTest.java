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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.openlmis.notification.domain.UserContactDetails;
import org.openlmis.notification.repository.UserContactDetailsRepository;
import org.openlmis.notification.util.UserContactDetailsDataBuilder;

public class AllowNotifyFilterTest {

  private static final boolean IMPORTANT = true;
  private static final boolean UNIMPORTANT = false;

  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock
  private UserContactDetailsRepository userContactDetailsRepository;

  @InjectMocks
  private AllowNotifyFilter filter;

  private UserContactDetails contactDetails = new UserContactDetailsDataBuilder().build();
  private UUID recipient = contactDetails.getId();

  @Before
  public void setUp() {
    given(userContactDetailsRepository.findOne(contactDetails.getId()))
        .willReturn(contactDetails);
  }

  @Test
  public void shouldAcceptImportantMessage() {
    // when
    boolean accepted = filter.accept(recipient, IMPORTANT);

    // then
    assertThat(accepted).isTrue();
  }

  @Test
  public void shouldAcceptStandardMessageWhenUserWantToGetIt() {
    // given
    contactDetails.setAllowNotify(true);

    // when
    boolean accepted = filter.accept(recipient, UNIMPORTANT);

    // then
    assertThat(accepted).isTrue();
  }

  @Test
  public void shouldDeclineStandardMessageWhenUserDoesNotWantToGetIt() {
    // given
    contactDetails.setAllowNotify(false);

    // when
    boolean accepted = filter.accept(recipient, UNIMPORTANT);

    // then
    assertThat(accepted).isFalse();
  }

  @Test
  public void shouldDeclineStandardMessageWhenUserDoesNotExist() {
    // given
    given(userContactDetailsRepository.findOne(contactDetails.getId())).willReturn(null);

    // when
    boolean accepted = filter.accept(recipient, UNIMPORTANT);

    // then
    assertThat(accepted).isFalse();
  }
}
