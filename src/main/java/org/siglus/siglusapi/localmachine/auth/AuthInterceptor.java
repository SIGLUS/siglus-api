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

import static org.siglus.siglusapi.localmachine.auth.AuthenticationArgumentResolver.LOCAL_MACHINE_token;
import static org.springframework.util.StringUtils.isEmpty;

import java.util.Optional;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.siglus.siglusapi.localmachine.CommonConstants;
import org.siglus.siglusapi.localmachine.server.AgentInfo;
import org.siglus.siglusapi.localmachine.server.AgentInfoRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthInterceptor extends HandlerInterceptorAdapter {
  private final AgentInfoRepository agentInfoRepository;

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    if (!supported(request)) {
      return true;
    }
    String tokenValue = mustGetTokenValue(request);
    MachineToken machineToken = authenticate(tokenValue);
    bindRequestAttribute(request, machineToken);
    return true;
  }

  private boolean supported(HttpServletRequest request) {
    String version = request.getHeader(CommonConstants.VERSION);
    return !isEmpty(version);
  }

  private String mustGetTokenValue(HttpServletRequest request) throws IllegalAccessException {
    String tokenValue = request.getHeader(CommonConstants.ACCESS_TOKEN);
    if (isEmpty(tokenValue)) {
      throw new IllegalAccessException("access token is required");
    }
    return tokenValue;
  }

  private MachineToken authenticate(String tokenValue) throws IllegalAccessException {
    MachineToken machineToken = MachineToken.parse(tokenValue);
    AgentInfo agentInfo = mustGetAgentInfo(machineToken);
    if (!machineToken.verify(agentInfo.getPublicKey())) {
      throw new IllegalAccessException("invalid token");
    }
    return machineToken;
  }

  private AgentInfo mustGetAgentInfo(MachineToken machineToken) throws IllegalAccessException {
    UUID agentId = machineToken.getAgentId();
    UUID facilityId = machineToken.getFacilityId();
    log.info(String.format("auth check, agentId = %s, facilityId=%s", agentId, facilityId));
    return Optional.ofNullable(
            agentInfoRepository.findOneByAgentIdAndFacilityId(agentId, facilityId))
        .orElseThrow(
            () -> new IllegalAccessException("agentInfo not exists. agentId = " + agentId));
  }

  private void bindRequestAttribute(HttpServletRequest request, MachineToken machineToken) {
    request.setAttribute(LOCAL_MACHINE_token, machineToken);
  }
}
