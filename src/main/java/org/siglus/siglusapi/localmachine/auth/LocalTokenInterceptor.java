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

package org.siglus.siglusapi.localmachine.auth;

import java.io.IOException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.siglus.siglusapi.exception.NotFoundException;
import org.siglus.siglusapi.localmachine.CommonConstants;
import org.siglus.siglusapi.localmachine.agent.Registration;
import org.siglus.siglusapi.localmachine.agent.RegistrationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LocalTokenInterceptor implements ClientHttpRequestInterceptor {
  private final RegistrationRepository registrationRepository;

  @Value("${machine.version}")
  private String localMachineVersion;

  @Override
  public ClientHttpResponse intercept(
      HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
    HttpHeaders headers = request.getHeaders();
    attachHeaders(headers);
    return execution.execute(request, body);
  }

  private void attachHeaders(HttpHeaders headers) {
    Registration registration =
        Optional.ofNullable(registrationRepository.getCurrentFacility())
            .orElseThrow(() -> new NotFoundException("registration info not found"));
    String accessToken = buildAccessToken(registration);
    headers.add(CommonConstants.VERSION, localMachineVersion);
    headers.add(CommonConstants.ACCESS_TOKEN, accessToken);
  }

  private String buildAccessToken(Registration registration) {
    return MachineToken.sign(
            registration.getAgentId(), registration.getFacilityId(), registration.getPrivateKey())
        .getPayload();
  }
}