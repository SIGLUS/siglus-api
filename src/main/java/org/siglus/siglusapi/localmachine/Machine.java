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

package org.siglus.siglusapi.localmachine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.localmachine.repository.AgentInfoRepository;
import org.siglus.siglusapi.repository.FacilityExtensionRepository;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@Data
public class Machine {
  private static final String SYSTEM_INFO_COMMAND = "dmidecode -t system";
  private static final String OS_NAME = "os.name";
  private static final String OS_VERSION = "os.version";
  private static final String NOT_SPECIFIED = "Not Specified";
  private static final String DELIMITER = " ";
  private final AgentInfoRepository agentInfoRepository;
  private final SiglusAuthenticationHelper siglusAuthenticationHelper;
  private final Environment env;
  private final FacilityExtensionRepository facilityExtensionRepository;

  private String deviceInfo;
  private boolean isConnectedOnlineWeb;

  public Set<String> fetchSupportedFacilityIds() {
    if (isOnlineWeb()) {
      return new HashSet<>(facilityExtensionRepository.findNonLocalMachineFacilityIds());
    }
    return new HashSet<>(agentInfoRepository.findRegisteredFacilityIds());
  }

  public boolean isActive() {
    return CollectionUtils.isNotEmpty(fetchSupportedFacilityIds());
  }

  public UUID getMachineId() {
    return agentInfoRepository.getMachineId().map(UUID::fromString).orElse(null);
  }

  @PostConstruct
  public void ensureMachineInfoExists() {
    UUID machineId = this.getMachineId();
    deviceInfo = getMachineDeviceInfo();
    if (Objects.nonNull(machineId)) {
      return;
    }
    generateMachineId();
  }

  public UUID evalEventSenderId() {
    return Optional.ofNullable(siglusAuthenticationHelper.getCurrentUser())
        .map(UserDto::getHomeFacilityId)
        .orElseGet(this::getLocalFacilityId);
  }

  public UUID getLocalFacilityId() {
    if (isOnlineWeb()) {
      throw new IllegalStateException("not allowed to get local facility id on onlineweb");
    }
    return UUID.fromString(
        this.fetchSupportedFacilityIds().stream()
            .findFirst()
            .orElseThrow(
                () -> new IllegalStateException("can not resolve local facility id")));
  }

  public boolean isOnlineWeb() {
    return Arrays.stream(env.getActiveProfiles()).noneMatch(it -> it.equalsIgnoreCase("localmachine"));
  }

  private void generateMachineId() {
    UUID tempMachineId = UUID.randomUUID();
    log.info("touch machine id:{}", tempMachineId);
    agentInfoRepository.touchMachineId(tempMachineId);
  }

  private String getMachineDeviceInfo() {
    String osName = System.getProperty(OS_NAME);
    String osVersion = System.getProperty(OS_VERSION);
    String systemInfo = getSystemInfo();
    return StringUtils.isBlank(systemInfo)
        ? getSystemInfoString(osName, osVersion, NOT_SPECIFIED)
        : getSystemInfoString(osName, osVersion, systemInfo);
  }

  String getSystemInfo() {
    String systemInfo = null;
    try {
      Process systemInfoProcess = getSystemInfoProcess();
      InputStreamReader inputStreamReader = new InputStreamReader(systemInfoProcess.getInputStream());
      BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
      String allSystemInfo = bufferedReader.lines().collect(Collectors.joining());
      String manufacturer = getTargetString(allSystemInfo, "Manufacturer: ", "Product Name")
          .equals(NOT_SPECIFIED)
          ? "" : getTargetString(allSystemInfo, "Manufacturer: ", "Product Name");
      String productName = getTargetString(allSystemInfo, "Product Name: ", "Version")
          .equals(NOT_SPECIFIED)
          ? "" : getTargetString(allSystemInfo, "Product Name: ", "Version");
      String version = getTargetString(allSystemInfo, "Version: ", "Serial Number")
          .equals(NOT_SPECIFIED)
          ? "" : getTargetString(allSystemInfo, "Version: ", "Serial Number");
      systemInfo = String.join(DELIMITER, manufacturer, productName, version);
      systemInfoProcess.waitFor();
      bufferedReader.close();
    } catch (Exception e) {
      log.warn("Get system info failed", e.getCause());
    }
    return Optional.ofNullable(systemInfo).map(String::trim).orElse("");
  }

  Process getSystemInfoProcess() throws IOException {
    return Runtime.getRuntime().exec(SYSTEM_INFO_COMMAND);
  }

  private String getTargetString(String target, String start, String end) {
    String result = StringUtils.substringBetween(target, start, end);
    return result.trim();
  }

  private String getSystemInfoString(String osName, String osVersion, String systemInfo) {
    return String.join(DELIMITER, "OS:", osName, osVersion, "Model:", systemInfo);
  }
}
