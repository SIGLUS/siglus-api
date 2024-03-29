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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.Optional;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.repository.FacilityRepository;
import org.siglus.siglusapi.domain.FacilityExtension;
import org.siglus.siglusapi.exception.BusinessDataException;
import org.siglus.siglusapi.localmachine.domain.ActivationCode;
import org.siglus.siglusapi.localmachine.domain.AgentInfo;
import org.siglus.siglusapi.localmachine.repository.ActivationCodeRepository;
import org.siglus.siglusapi.localmachine.repository.AgentInfoRepository;
import org.siglus.siglusapi.localmachine.webapi.ActivationResponse;
import org.siglus.siglusapi.localmachine.webapi.LocalActivationRequest;
import org.siglus.siglusapi.localmachine.webapi.RemoteActivationRequest;
import org.siglus.siglusapi.repository.FacilityExtensionRepository;

@RunWith(MockitoJUnitRunner.class)
public class ActivationServiceTest {
  @Mock FacilityRepository facilityRepository;
  @Mock AgentInfoRepository agentInfoRepository;
  @Mock ActivationCodeRepository activationCodeRepository;
  @Mock FacilityExtensionRepository facilityExtensionRepository;
  @InjectMocks private ActivationService activationService;

  @Before
  public void setup() {
    given(facilityExtensionRepository.findByFacilityId(any()))
        .willReturn(FacilityExtension.builder().isLocalMachine(Boolean.TRUE).build());
  }

  @Test(expected = BusinessDataException.class)
  public void shouldThrowWhenCheckFacilityTypeGivenFacilityExtensionNotExists() {
    // given
    Facility facility = mock(Facility.class);
    given(facility.getId()).willReturn(UUID.randomUUID());
    given(facilityExtensionRepository.findByFacilityId(any())).willReturn(null);
    // then
    activationService.checkFacilityType(facility);
  }

  @Test(expected = BusinessDataException.class)
  public void shouldThrowWhenCheckFacilityTypeGivenNotLocalMachine() {
    // given
    Facility facility = mock(Facility.class);
    given(facility.getId()).willReturn(UUID.randomUUID());
    FacilityExtension nonLocalMachineExtension = FacilityExtension.builder().isLocalMachine(Boolean.FALSE).build();
    given(facilityExtensionRepository.findByFacilityId(any())).willReturn(nonLocalMachineExtension);
    // then
    activationService.checkFacilityType(facility);
  }

  @Test
  public void shouldActivateAgentSuccessfullyGivenAgentNotActivated() {
    // given
    String facilityCode = "facility-code";
    Facility facility = new Facility(UUID.randomUUID());
    given(facilityRepository.findByCode(facilityCode)).willReturn(Optional.of(facility));
    given(agentInfoRepository.findFirstByFacilityCode(facilityCode)).willReturn(null);
    ActivationCode activationCode =
        ActivationCode.builder().id(UUID.randomUUID()).isUsed(Boolean.FALSE).build();
    given(activationCodeRepository.findFirstByFacilityCodeAndActivationCode(any(), any()))
        .willReturn(Optional.of(activationCode));
    Encoder encoder = Base64.getEncoder();
    RemoteActivationRequest request =
        RemoteActivationRequest.builder()
            .machineId(UUID.randomUUID())
            .localActivationRequest(new LocalActivationRequest("activation code", facilityCode))
            .base64EncodedPrivateKey(encoder.encodeToString("privatekey".getBytes()))
            .base64EncodedPublicKey(encoder.encodeToString("publickey".getBytes()))
            .build();
    // when
    ActivationResponse resp = activationService.activate(request);
    // then
    ArgumentCaptor<AgentInfo> capture = ArgumentCaptor.forClass(AgentInfo.class);
    verify(agentInfoRepository).save(capture.capture());
    AgentInfo agentInfo = capture.getValue();
    assertThat(agentInfo.getMachineId()).isEqualTo(request.getMachineId());
    assertThat(agentInfo.getPublicKey()).isEqualTo("publickey".getBytes());
    assertThat(agentInfo.getPrivateKey()).isEqualTo("privatekey".getBytes());
    assertThat(resp.getFacilityId()).isEqualTo(facility.getId());
  }

  @Test(expected = BusinessDataException.class)
  public void shouldThrowWhenDoActivationGivenUsedActivationCode() {
    // given
    ActivationCode activationCode =
        ActivationCode.builder().id(UUID.randomUUID()).isUsed(Boolean.TRUE).build();
    given(activationCodeRepository.findFirstByFacilityCodeAndActivationCode(any(), any()))
        .willReturn(Optional.of(activationCode));
    // when
    activationService.doActivation(new LocalActivationRequest());
  }
}
