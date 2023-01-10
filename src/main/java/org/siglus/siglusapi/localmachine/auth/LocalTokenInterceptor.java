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

import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_NETWORK_ERROR;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_NOT_ACTIVATED_YET;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.BooleanUtils;
import org.siglus.siglusapi.dto.Message;
import org.siglus.siglusapi.exception.BusinessDataException;
import org.siglus.siglusapi.localmachine.CommonConstants;
import org.siglus.siglusapi.localmachine.Machine;
import org.siglus.siglusapi.localmachine.domain.AgentInfo;
import org.siglus.siglusapi.localmachine.repository.AgentInfoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LocalTokenInterceptor implements ClientHttpRequestInterceptor {

  private final AgentInfoRepository agentInfoRepository;

  @Value("${machine.version}")
  private String localMachineVersion;

  private final Machine machine;

  @Setter
  private Function<HttpRequest, Boolean> acceptFunc = (httpRequest -> true);

  @Override
  public ClientHttpResponse intercept(
      HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
    if (BooleanUtils.isTrue(acceptFunc.apply(request))) {
      HttpHeaders headers = request.getHeaders();
      attachHeaders(headers);
    }
    try {
      ClientHttpResponse httpResponse = execution.execute(request, body);
      machine.setConnectedOnlineWeb(true);
      logClientHttpResponse(request, httpResponse);
      return httpResponse;
    } catch (IOException e) {
      machine.setConnectedOnlineWeb(false);
      throw new BusinessDataException(e, new Message(ERROR_NETWORK_ERROR));
    }
  }

  @SneakyThrows
  private void logClientHttpResponse(HttpRequest request, ClientHttpResponse httpResponse) {
    HttpStatus statusCode = httpResponse.getStatusCode();
    if (!statusCode.is2xxSuccessful()) {
      StringBuilder bodyStringBuilder = new StringBuilder();
      BufferedReader inputStreamReader = new BufferedReader(new InputStreamReader(httpResponse.getBody(),
          StandardCharsets.UTF_8));
      String line = inputStreamReader.readLine();
      while (line != null) {
        bodyStringBuilder.append(line).append('\n');
        line = inputStreamReader.readLine();
      }
      log.error("localmachine agent error response, request = {}, status code = {}, headers = {} ,body = {}",
          request.getURI(), statusCode, httpResponse.getHeaders(), bodyStringBuilder);
    }
    if (HttpStatus.PRECONDITION_FAILED.equals(statusCode)) {
      log.info("unauthorized access, delete local machine table");
      agentInfoRepository.deleteLocalMachineAgents();
    }
  }

  private void attachHeaders(HttpHeaders headers) {
    AgentInfo agentInfo =
        Optional.ofNullable(agentInfoRepository.getLocalAgent())
            .orElseThrow(() -> new BusinessDataException(new Message(ERROR_NOT_ACTIVATED_YET)));
    String accessToken = buildAccessToken(agentInfo);
    headers.add(CommonConstants.VERSION, localMachineVersion);
    headers.add(CommonConstants.ACCESS_TOKEN, accessToken);
    headers.add(CommonConstants.DEVICE_INFO, machine.getDeviceInfo());
  }

  private String buildAccessToken(AgentInfo agentInfo) {
    return MachineToken.sign(
            agentInfo.getMachineId(), agentInfo.getFacilityId(), agentInfo.getPrivateKey())
        .getPayload();
  }
}
