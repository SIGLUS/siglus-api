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

import static java.util.Collections.emptyList;
import static org.siglus.siglusapi.security.CustomUserAuthenticationConverter.REFERENCE_DATA_USER_ID;
import static org.springframework.security.oauth2.provider.token.AccessTokenConverter.AUTHORITIES;

import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.siglus.siglusapi.domain.Notification;
import org.siglus.siglusapi.security.CustomUserAuthenticationConverter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.UserAuthenticationConverter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SiglusSimulateUserAuthHelper {

  public void simulateUserAuth(UUID userId) {
    simulateUserAuth(userId, emptyList());
  }

  public void simulateUserAuth(UUID userId, Collection<String> authorities) {
    UserAuthenticationConverter userAuthenticationConverter = new CustomUserAuthenticationConverter();
    Authentication authentication = userAuthenticationConverter.extractAuthentication(
        ImmutableMap.of(REFERENCE_DATA_USER_ID, userId.toString(), AUTHORITIES, authorities));
    OAuth2Authentication originAuth = (OAuth2Authentication) SecurityContextHolder.getContext().getAuthentication();
    OAuth2Authentication newAuth = new OAuth2Authentication(originAuth.getOAuth2Request(), authentication);
    SecurityContextHolder.getContext().setAuthentication(newAuth);
  }

  public void simulateNewUserAuth(UUID userId) {
    simulateNewUserAuth(userId, emptyList());
  }

  public void simulateNewUserAuth(UUID userId, Collection<String> authorities) {
    UserAuthenticationConverter userAuthenticationConverter = new CustomUserAuthenticationConverter();
    Authentication authentication = userAuthenticationConverter.extractAuthentication(
        ImmutableMap.of(REFERENCE_DATA_USER_ID, userId.toString(), AUTHORITIES, authorities));
    OAuth2Authentication newAuth = new OAuth2Authentication(null, authentication);
    SecurityContextHolder.getContext().setAuthentication(newAuth);
  }

  public void simulateNewUserThenRollbackAuth(UUID userId, Notification notification,
      Consumer<Notification> notificationConsumer) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    try {
      simulateNewUserAuth(userId);
      notificationConsumer.accept(notification);
    } finally {
      // reset
      SecurityContextHolder.getContext().setAuthentication(authentication);
    }
  }
}
