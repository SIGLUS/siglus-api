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

package org.siglus.common.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.stereotype.Component;

@Component
public class SimulateAuthenticationHelper {

  @Value("${auth.server.clientId}")
  private String serviceTokenClientId;

  public OAuth2Authentication simulateCrossServiceAuth() {
    OAuth2Authentication orignAuth = (OAuth2Authentication) SecurityContextHolder
        .getContext()
        .getAuthentication();
    OAuth2Request auth2Request = orignAuth.getOAuth2Request();
    OAuth2Request newAuth2Request = new OAuth2Request(auth2Request.getRequestParameters(),
        serviceTokenClientId,
        auth2Request.getAuthorities(),
        true,
        auth2Request.getScope(),
        auth2Request.getResourceIds(),
        auth2Request.getRedirectUri(),
        auth2Request.getResponseTypes(),
        auth2Request.getExtensions());
    OAuth2Authentication newAuth = new OAuth2Authentication(newAuth2Request,
        null);
    SecurityContextHolder
        .getContext().setAuthentication(newAuth);
    return orignAuth;
  }

  public void recoveryAuth(OAuth2Authentication orignAuth) {
    SecurityContextHolder
        .getContext().setAuthentication(orignAuth);
  }

}
