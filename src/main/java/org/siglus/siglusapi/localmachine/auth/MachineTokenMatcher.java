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
import org.codehaus.plexus.util.StringUtils;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.repository.FacilityRepository;
import org.siglus.siglusapi.domain.AppInfo;
import org.siglus.siglusapi.localmachine.CommonConstants;
import org.siglus.siglusapi.localmachine.domain.AgentInfo;
import org.siglus.siglusapi.localmachine.repository.AgentInfoRepository;
import org.siglus.siglusapi.repository.AppInfoRepository;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MachineTokenMatcher implements RequestMatcher {
  private final AgentInfoRepository agentInfoRepository;
  private final AppInfoRepository appInfoRepository;
  private final FacilityRepository facilityRepository;

  @Override
  public boolean matches(HttpServletRequest request) {
    try {
      String tokenValue = mustGetTokenValue(request);
      MachineToken machineToken = authenticate(tokenValue);
      bindRequestAttribute(request, machineToken);
      if (StringUtils.isNotBlank(request.getHeader(CommonConstants.DEVICE_INFO))
          && (HttpMethod.PUT.name().equals(request.getMethod())
          || HttpMethod.POST.name().equals(request.getMethod()))) {
        updateMachineDeviceInfo(request, machineToken);
      }
    } catch (IllegalAccessException e) {
      log.error("illegal access", e);
      return false;
    }
    return true;
  }

  private String mustGetTokenValue(HttpServletRequest request) throws IllegalAccessException {
    String tokenValue = request.getHeader(CommonConstants.ACCESS_TOKEN);
    if (isEmpty(tokenValue)) {
      throw new IllegalAccessException("machine access token is required");
    }
    return tokenValue;
  }

  private MachineToken authenticate(String tokenValue) throws IllegalAccessException {
    MachineToken machineToken = MachineToken.parse(tokenValue);
    AgentInfo agentInfo = mustGetAgentInfo(machineToken);
    if (!machineToken.verify(agentInfo.getPublicKey())) {
      throw new IllegalAccessException("invalid machine token");
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

  void updateMachineDeviceInfo(HttpServletRequest request, MachineToken machineToken) {
    String deviceInfo = request.getHeader(CommonConstants.DEVICE_INFO);
    UUID facilityId = machineToken.getFacilityId();
    Facility facility = facilityRepository.findOne(facilityId);
    AppInfo appInfo = appInfoRepository.findByFacilityCode(facility.getCode());
    if (null != appInfo && !appInfo.getUniqueId().equals(machineToken.getMachineId().toString())) {
      return;
    }
    if (null != appInfo && deviceInfo.equals(appInfo.getDeviceInfo())) {
      return;
    }
    if (null == appInfo) {
      appInfo = buildForAppInfo(facility, machineToken.getMachineId());
    }
    appInfo.setVersionCode(request.getHeader(CommonConstants.VERSION));
    appInfo.setDeviceInfo(deviceInfo);
    log.info("deviceInfo updated, facilityId: {}; new deviceInfo: {}", facilityId, deviceInfo);
    appInfoRepository.save(appInfo);
  }

  private AppInfo buildForAppInfo(Facility facility, UUID machineId) {
    return AppInfo
        .builder()
        .facilityCode(facility.getCode())
        .facilityName(facility.getName())
        .uniqueId(machineId.toString())
        .build();
  }
}
