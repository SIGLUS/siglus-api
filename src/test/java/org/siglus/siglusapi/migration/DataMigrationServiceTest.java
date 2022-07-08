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

package org.siglus.siglusapi.migration;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.dto.android.request.StockCardCreateRequest;
import org.siglus.siglusapi.dto.android.response.CreateStockCardResponse;
import org.siglus.siglusapi.migration.DataMigrationService;
import org.siglus.siglusapi.service.android.MeService;
import org.siglus.siglusapi.service.client.SiglusUserReferenceDataService;
import org.siglus.siglusapi.util.SiglusSimulateUserAuthHelper;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;

@RunWith(MockitoJUnitRunner.class)
public class DataMigrationServiceTest {

  @Mock
  private MeService meService;
  @Mock
  private SiglusUserReferenceDataService userReferenceDataService;

  @SuppressWarnings("PMD.UnusedPrivateField")
  @Spy
  private SiglusSimulateUserAuthHelper simulateUserAuthHelper;

  @InjectMocks
  private DataMigrationService dataMigrationService;

  @Test
  public void shouldCallMeServiceWithTheAssumedUserAuthenticationContextWhenCreateStockCards() {
    // given
    ensureCurrentAuthenticationExists();
    UserDto assumedUser = returnAssumedUserWhenCallUserReferenceDataServiceToGetUserInfo();

    AtomicReference<Object> capturedCurrentUserId =
        captureCurrentUserIdWhenCallMeServiceToCreateStockCards();

    // when
    String facilityId = UUID.randomUUID().toString();
    List<StockCardCreateRequest> requests = emptyList();
    dataMigrationService.createStockCards(facilityId, requests);

    // then
    assertThat(capturedCurrentUserId.get()).isEqualTo(assumedUser.getId());
  }

  private UserDto returnAssumedUserWhenCallUserReferenceDataServiceToGetUserInfo() {
    UserDto assumedUser = new UserDto();
    assumedUser.setId(UUID.randomUUID());
    when(userReferenceDataService.getUserInfo(any()))
        .thenReturn(new PageImpl<>(singletonList(assumedUser)));
    return assumedUser;
  }

  private AtomicReference<Object> captureCurrentUserIdWhenCallMeServiceToCreateStockCards() {
    AtomicReference<Object> capturedCurrentUserId = new AtomicReference<>();

    when(meService.createStockCards(emptyList()))
        .thenAnswer(
          (Answer<CreateStockCardResponse>)
            invocation -> {
              capturedCurrentUserId.set(
                  SecurityContextHolder.getContext().getAuthentication().getPrincipal());
              return null;
          });
    return capturedCurrentUserId;
  }

  private void ensureCurrentAuthenticationExists() {
    OAuth2Authentication authentication = mock(OAuth2Authentication.class);
    SecurityContextHolder.getContext().setAuthentication(authentication);
    when(authentication.getOAuth2Request()).thenReturn(mock(OAuth2Request.class));
  }
}
