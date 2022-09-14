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

import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_FACILITY_CHANGED;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_FACILITY_NOT_FOUND;

import com.google.common.annotations.VisibleForTesting;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.security.KeyPair;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.repository.FacilityRepository;
import org.siglus.siglusapi.dto.Message;
import org.siglus.siglusapi.exception.BusinessDataException;
import org.siglus.siglusapi.localmachine.Machine;
import org.siglus.siglusapi.localmachine.domain.AgentInfo;
import org.siglus.siglusapi.localmachine.repository.AgentInfoRepository;
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
    if (checkForActivation(request)) {
      Facility facility = mustGetFacility(request);
      activateWithOnlineWeb(request, facility);
    }
  }

  @VisibleForTesting
  boolean checkForActivation(LocalActivationRequest request) {
    AgentInfo agentInfo = agentInfoRepository.getLocalAgent();
    // first time of activation
    if (Objects.isNull(agentInfo)) {
      return true;
    }
    // reactivation
    if (!agentInfo.getFacilityCode().equals(request.getFacilityCode())) {
      throw new BusinessDataException(new Message(ERROR_FACILITY_CHANGED));
    }
    return !agentInfo.getActivationCode().equals(request.getActivationCode());
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
        .facilityId(facility.getId())
        .facilityCode(facility.getCode())
        .activationCode(request.getActivationCode())
        .activatedAt(ZonedDateTime.now())
        .privateKey(encodedPrivateKey)
        .publicKey(encodedPublicKey)
        .build();
  }

  private Facility mustGetFacility(LocalActivationRequest request) {
    return facilityRepository
        .findByCode(request.getFacilityCode())
        .orElseThrow(() -> new BusinessDataException(new Message(ERROR_FACILITY_NOT_FOUND)));
  }

  public Optional<AgentInfo> getCurrentAgentInfo() {
    return Optional.ofNullable(agentInfoRepository.getLocalAgent());
  }
}
