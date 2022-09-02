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
import org.siglus.siglusapi.localmachine.webapi.LocalActivationRequest;
import org.siglus.siglusapi.localmachine.webapi.RemoteActivationRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivationService {
  private final AgentInfoRepository agentInfoRepository;
  private final FacilityRepository facilityRepository;

  public void activate(RemoteActivationRequest request) {
    validateRequest(request);
    LocalActivationRequest localActivationRequest = request.getLocalActivationRequest();
    Facility facility = mustGetFacility(localActivationRequest.getFacilityCode());
    AgentInfo agentInfo = buildAgentInfo(request, facility);
    log.info("add agent {} for facility {}", request.getMachineId(), facility.getCode());
    agentInfoRepository.save(agentInfo);
  }

  private Facility mustGetFacility(String facilityCode) {
    return facilityRepository
        .findByCode(facilityCode)
        .orElseThrow(
            () -> new NotFoundException(String.format("facility %s not found", facilityCode)));
  }

  private void validateRequest(RemoteActivationRequest request) {
    LocalActivationRequest localActivationRequest = request.getLocalActivationRequest();
    AgentInfo agentInfo =
        agentInfoRepository.findFirstByFacilityCode(localActivationRequest.getFacilityCode());
    if (Objects.nonNull(agentInfo)) {
      throw new IllegalStateException(
          String.format(
              "facility %s has been activated with machine %s before",
              localActivationRequest.getFacilityCode(), request.getMachineId()));
    }
  }

  private AgentInfo buildAgentInfo(RemoteActivationRequest request, Facility facility) {
    AgentInfo agentInfo;
    Decoder decoder = Base64.getDecoder();
    agentInfo =
        AgentInfo.builder()
            .machineId(request.getMachineId())
            .activatedAt(ZonedDateTime.now())
            .privateKey(decoder.decode(request.getBase64EncodedPrivateKey()))
            .publicKey(decoder.decode(request.getBase64EncodedPublicKey()))
            .activationCode(request.getLocalActivationRequest().getActivationCode())
            .facilityId(facility.getId())
            .build();
    return agentInfo;
  }
}
