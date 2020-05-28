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

import org.openlmis.stockmanagement.service.StockmanagementAuthService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.stereotype.Service;

@Service
public class SiglusAuthService extends StockmanagementAuthService {

  public String obtainAccessToken(boolean obtainUserToken) {
    if (obtainUserToken) {
      OAuth2Authentication authentication = (OAuth2Authentication) SecurityContextHolder
          .getContext()
          .getAuthentication();
      if (!authentication.isClientOnly()) {
        OAuth2AuthenticationDetails authenticationDetails =
            (OAuth2AuthenticationDetails) authentication.getDetails();
        return authenticationDetails.getTokenValue();
      }
    }
    return super.obtainAccessToken();
  }
}
