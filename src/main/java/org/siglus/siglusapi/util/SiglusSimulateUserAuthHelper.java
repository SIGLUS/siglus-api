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

import static org.siglus.siglusapi.security.CustomUserAuthenticationConverter.REFERENCE_DATA_USER_ID;

import com.google.common.collect.ImmutableMap;
import java.util.UUID;
import org.siglus.siglusapi.security.CustomUserAuthenticationConverter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.UserAuthenticationConverter;
import org.springframework.stereotype.Component;

@Component
public class SiglusSimulateUserAuthHelper {

  public void simulateUserAuth(UUID userId) {
    UserAuthenticationConverter userAuthenticationConverter
        = new CustomUserAuthenticationConverter();
    Authentication authentication = userAuthenticationConverter.extractAuthentication(
        ImmutableMap.of(REFERENCE_DATA_USER_ID, userId.toString()));

    OAuth2Authentication orignAuth = (OAuth2Authentication) SecurityContextHolder
        .getContext()
        .getAuthentication();
    OAuth2Authentication newAuth = new OAuth2Authentication(orignAuth.getOAuth2Request(),
        authentication);

    SecurityContextHolder
        .getContext().setAuthentication(newAuth);
  }
}
