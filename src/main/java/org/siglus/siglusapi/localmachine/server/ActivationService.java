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

package org.siglus.siglusapi.localmachine.server;

import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.repository.FacilityRepository;
import org.siglus.siglusapi.exception.NotFoundException;
import org.siglus.siglusapi.localmachine.domain.ActivationCode;
import org.siglus.siglusapi.localmachine.domain.AgentInfo;
import org.siglus.siglusapi.localmachine.repository.ActivationCodeRepository;
import org.siglus.siglusapi.localmachine.repository.AgentInfoRepository;
import org.siglus.siglusapi.localmachine.webapi.LocalActivationRequest;
import org.siglus.siglusapi.localmachine.webapi.RemoteActivationRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivationService {
  private final AgentInfoRepository agentInfoRepository;
  private final FacilityRepository facilityRepository;
  private final ActivationCodeRepository activationCodeRepository;

  @Transactional
  public void activate(RemoteActivationRequest request) {
    validateRequest(request);
    LocalActivationRequest localActivationRequest = request.getLocalActivationRequest();
    Facility facility = mustGetFacility(localActivationRequest.getFacilityCode());
    doActivation(localActivationRequest);
    AgentInfo agentInfo = buildAgentInfo(request, facility);
    log.info("add agent {} for facility {}", request.getMachineId(), facility.getCode());
    agentInfoRepository.save(agentInfo);
  }

  void doActivation(LocalActivationRequest localActivationRequest) {
    ActivationCode activationCode =
        activationCodeRepository
            .findFirstByFacilityCodeAndActivationCode(
                localActivationRequest.getFacilityCode(),
                localActivationRequest.getActivationCode())
            .orElseThrow(() -> new NotFoundException("activation code not found"));
    if (activationCode.isUsed()) {
      throw new IllegalStateException("activation code has been used");
    }
    activationCode.isUsed(Boolean.TRUE);
    activationCode.isUsedAt(ZonedDateTime.now());
    log.info("mark activation code as used, id:{}", activationCode.getId());
    activationCodeRepository.save(activationCode);
  }

  void validateRequest(RemoteActivationRequest request) {
    LocalActivationRequest localActivationRequest = request.getLocalActivationRequest();
    String facilityCode = localActivationRequest.getFacilityCode();
    AgentInfo agentInfo = agentInfoRepository.findFirstByFacilityCode(facilityCode);
    if (Objects.nonNull(agentInfo)) {
      throw new IllegalStateException(
          String.format(
              "facility %s has been activated with machine %s before",
              facilityCode, request.getMachineId()));
    }
  }

  private Facility mustGetFacility(String facilityCode) {
    return facilityRepository
        .findByCode(facilityCode)
        .orElseThrow(
            () -> new NotFoundException(String.format("facility %s not found", facilityCode)));
  }

  private AgentInfo buildAgentInfo(RemoteActivationRequest request, Facility facility) {
    AgentInfo agentInfo;
    Decoder decoder = Base64.getDecoder();
    agentInfo =
        AgentInfo.builder()
            .machineId(request.getMachineId())
            .activatedAt(ZonedDateTime.now())
            .facilityCode(facility.getCode())
            .privateKey(decoder.decode(request.getBase64EncodedPrivateKey()))
            .publicKey(decoder.decode(request.getBase64EncodedPublicKey()))
            .activationCode(request.getLocalActivationRequest().getActivationCode())
            .facilityId(facility.getId())
            .build();
    return agentInfo;
  }
}
