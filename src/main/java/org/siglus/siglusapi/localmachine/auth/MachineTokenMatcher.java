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

import static org.siglus.siglusapi.localmachine.auth.AuthenticationArgumentResolver.LOCAL_MACHINE_TOKEN;
import static org.springframework.util.StringUtils.isEmpty;

import java.util.Optional;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.siglus.siglusapi.localmachine.CommonConstants;
import org.siglus.siglusapi.localmachine.server.AgentInfo;
import org.siglus.siglusapi.localmachine.server.AgentInfoRepository;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MachineTokenMatcher implements RequestMatcher {
  private final AgentInfoRepository agentInfoRepository;

  @Override
  public boolean matches(HttpServletRequest request) {
    try {
      String tokenValue = mustGetTokenValue(request);
      MachineToken machineToken = authenticate(tokenValue);
      bindRequestAttribute(request, machineToken);
    } catch (IllegalAccessException e) {
      log.error("illegal access", e);
      return false;
    }
    return true;
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
    UUID machineId = machineToken.getMachineId();
    UUID facilityId = machineToken.getFacilityId();
    log.info(String.format("auth check, machineId = %s, facilityId=%s", machineId, facilityId));
    return Optional.ofNullable(
            agentInfoRepository.findOneByMachineIdAndFacilityId(machineId, facilityId))
        .orElseThrow(
            () -> new IllegalAccessException("agentInfo not exists. machineId = " + machineId));
  }

  private void bindRequestAttribute(HttpServletRequest request, MachineToken machineToken) {
    request.setAttribute(LOCAL_MACHINE_TOKEN, machineToken);
  }
}
