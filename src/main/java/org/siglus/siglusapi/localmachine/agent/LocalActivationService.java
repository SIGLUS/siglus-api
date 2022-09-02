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

package org.siglus.siglusapi.localmachine.agent;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.security.KeyPair;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.repository.FacilityRepository;
import org.siglus.siglusapi.exception.NotFoundException;
import org.siglus.siglusapi.localmachine.Machine;
import org.siglus.siglusapi.localmachine.server.AgentInfo;
import org.siglus.siglusapi.localmachine.server.AgentInfoRepository;
import org.siglus.siglusapi.localmachine.webapi.LocalActivationRequest;
import org.siglus.siglusapi.localmachine.webapi.RemoteActivationRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocalActivationService {
  private final FacilityRepository facilityRepository;
  private final AgentInfoRepository agentInfoRepository;
  private final OnlineWebClient onlineWebClient;
  private final Machine machine;

  @Transactional
  public void activate(LocalActivationRequest request) {
    AgentInfo agentInfo = agentInfoRepository.findFirstByFacilityCode(request.getFacilityCode());
    if (Objects.nonNull(agentInfo)) {
      log.info("Facility {} has been activated before", request.getFacilityCode());
      return;
    }
    Facility facility =
        facilityRepository
            .findByCode(request.getFacilityCode())
            .orElseThrow(
                () ->
                    new NotFoundException(
                        (String.format("Facility %s not exists", request.getFacilityCode()))));
    activateWithOnlineWeb(request, facility);
  }

  private void activateWithOnlineWeb(LocalActivationRequest request, Facility facility) {
    KeyPair keyPair = Keys.keyPairFor(SignatureAlgorithm.RS256);
    byte[] encodedPrivateKey = keyPair.getPrivate().getEncoded();
    byte[] encodedPublicKey = keyPair.getPublic().getEncoded();
    AgentInfo agentInfo = buildAgentInfo(request, facility, encodedPrivateKey, encodedPublicKey);
    RemoteActivationRequest remoteActivationRequest =
        buildRemoteActivationRequest(request, encodedPrivateKey, encodedPublicKey);
    onlineWebClient.activate(remoteActivationRequest);
    log.info("save agentInfo info for facility:{}", facility.getCode());
    agentInfoRepository.save(agentInfo);
  }

  private RemoteActivationRequest buildRemoteActivationRequest(
      LocalActivationRequest request, byte[] encodedPrivateKey, byte[] encodedPublicKey) {
    Encoder base64Encoder = Base64.getEncoder();
    return RemoteActivationRequest.builder()
        .localActivationRequest(request)
        .base64EncodedPublicKey(base64Encoder.encodeToString(encodedPublicKey))
        .base64EncodedPrivateKey(base64Encoder.encodeToString(encodedPrivateKey))
        .machineId(machine.getMachineId())
        .build();
  }

  private AgentInfo buildAgentInfo(
      LocalActivationRequest request,
      Facility facility,
      byte[] encodedPrivateKey,
      byte[] encodedPublicKey) {
    return AgentInfo.builder()
        .machineId(machine.getMachineId())
        .id(UUID.randomUUID())
        .facilityId(facility.getId())
        .facilityCode(facility.getCode())
        .activationCode(request.getActivationCode())
        .activatedAt(ZonedDateTime.now())
        .privateKey(encodedPrivateKey)
        .publicKey(encodedPublicKey)
        .build();
  }
}
