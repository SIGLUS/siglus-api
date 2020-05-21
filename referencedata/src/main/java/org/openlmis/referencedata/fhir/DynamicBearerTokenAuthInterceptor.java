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

package org.openlmis.referencedata.fhir;

import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import lombok.AllArgsConstructor;
import org.openlmis.referencedata.service.AuthService;

@AllArgsConstructor
final class DynamicBearerTokenAuthInterceptor extends BearerTokenAuthInterceptor {
  private AuthService authService;

  @Override
  public void interceptRequest(IHttpRequest theRequest) {
    String token = authService.obtainAccessToken();
    theRequest.addHeader(
        Constants.HEADER_AUTHORIZATION,
        Constants.HEADER_AUTHORIZATION_VALPREFIX_BEARER + token);
  }

}
