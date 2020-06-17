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

package org.siglus.siglusapi.service;

import static org.apache.commons.lang3.RandomUtils.nextBoolean;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.stockmanagement.service.StockmanagementAuthService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;

@RunWith(MockitoJUnitRunner.class)
public class AuthServiceTest {

  @InjectMocks
  private AuthService service;

  @Mock
  private StockmanagementAuthService stockmanagementAuthService;

  @Mock
  private OAuth2Authentication authentication;

  @Mock
  private OAuth2AuthenticationDetails authenticationDetails;

  @Before
  public void prepare() {
    SecurityContextHolder.getContext().setAuthentication(authentication);
    when(authentication.getDetails()).thenReturn(authenticationDetails);
  }

  @Test
  public void shouldReadFromTokenWhenObtainAccessTokenGivenObtainUserTokenAndNotClientOnly() {
    //given
    when(authentication.isClientOnly()).thenReturn(false);
    final boolean obtainUserToken = true;

    //when
    service.obtainAccessToken(obtainUserToken);

    //then
    verify(authenticationDetails).getTokenValue();
  }

  @Test
  public void shouldReadFromTokenWhenObtainAccessTokenGivenObtainUserTokenAndClientOnly() {
    //given
    when(authentication.isClientOnly()).thenReturn(true);
    final boolean obtainUserToken = true;

    //when
    service.obtainAccessToken(obtainUserToken);

    //then
    verify(stockmanagementAuthService).obtainAccessToken();
  }

  @Test
  public void shouldReadFromTokenWhenObtainAccessTokenGivenNotObtainUserToken() {
    //given
    when(authentication.isClientOnly()).thenReturn(nextBoolean());
    final boolean obtainUserToken = false;

    //when
    service.obtainAccessToken(obtainUserToken);

    //then
    verify(stockmanagementAuthService).obtainAccessToken();
  }

  @Test
  public void shouldCallStockAuthServiceWhenObtainAccessToken() {
    //when
    service.obtainAccessToken();

    //then
    verify(stockmanagementAuthService).obtainAccessToken();
  }

  @Test
  public void shouldCallStockAuthServiceWhenClearTokenCache() {
    //when
    service.clearTokenCache();

    //then
    verify(stockmanagementAuthService).clearTokenCache();
  }

}
