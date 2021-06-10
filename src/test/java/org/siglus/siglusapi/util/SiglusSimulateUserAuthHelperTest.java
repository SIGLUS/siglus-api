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

package org.siglus.siglusapi.util;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;

@RunWith(MockitoJUnitRunner.class)
public class SiglusSimulateUserAuthHelperTest {

  @InjectMocks
  private SiglusSimulateUserAuthHelper simulateUserAuth;

  @Mock
  private OAuth2Authentication authentication;

  @Test
  public void shouldSimulateUserAuth() {
    // given
    UUID userId = UUID.randomUUID();
    SecurityContextHolder.getContext().setAuthentication(authentication);
    when(authentication.getOAuth2Request()).thenReturn(mock(OAuth2Request.class));

    // when
    simulateUserAuth.simulateUserAuth(userId);

    // then
    Assert.assertEquals(SecurityContextHolder.getContext().getAuthentication().getPrincipal(),
        userId);
  }
}
