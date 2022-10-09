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

import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_ACTIVATION_CODE_USED_ALREADY;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_FACILITY_NOT_FOUND;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_INVALID_ACTIVATION_CODE;

import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.Base64.Decoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.repository.FacilityRepository;
import org.siglus.siglusapi.dto.Message;
import org.siglus.siglusapi.exception.BusinessDataException;
import org.siglus.siglusapi.localmachine.domain.ActivationCode;
import org.siglus.siglusapi.localmachine.domain.AgentInfo;
import org.siglus.siglusapi.localmachine.repository.ActivationCodeRepository;
import org.siglus.siglusapi.localmachine.repository.AgentInfoRepository;
import org.siglus.siglusapi.localmachine.webapi.ActivationResponse;
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
  public ActivationResponse activate(RemoteActivationRequest request) {
    LocalActivationRequest localActivationRequest = request.getLocalActivationRequest();
    Facility facility = mustGetFacility(localActivationRequest.getFacilityCode());
    doActivation(localActivationRequest);
    AgentInfo agentInfo = buildAgentInfo(request, facility);
    log.info("add agent {} for facility {}", request.getMachineId(), facility.getCode());
    agentInfoRepository.save(agentInfo);
    return new ActivationResponse(facility.getId(), facility.getCode(), agentInfo.getActivationCode());
  }

  void doActivation(LocalActivationRequest localActivationRequest) {
    ActivationCode activationCode =
        activationCodeRepository
            .findFirstByFacilityCodeAndActivationCode(
                localActivationRequest.getFacilityCode(),
                localActivationRequest.getActivationCode())
            .orElseThrow(
                () -> new BusinessDataException(new Message(ERROR_INVALID_ACTIVATION_CODE)));
    if (activationCode.isUsed()) {
      throw new BusinessDataException(new Message(ERROR_ACTIVATION_CODE_USED_ALREADY));
    }
    activationCode.isUsed(Boolean.TRUE);
    activationCode.isUsedAt(ZonedDateTime.now());
    log.info("mark activation code as used, id:{}", activationCode.getId());
    activationCodeRepository.save(activationCode);
  }

  private Facility mustGetFacility(String facilityCode) {
    return facilityRepository
        .findByCode(facilityCode)
        .orElseThrow(() -> new BusinessDataException(new Message(ERROR_FACILITY_NOT_FOUND)));
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
