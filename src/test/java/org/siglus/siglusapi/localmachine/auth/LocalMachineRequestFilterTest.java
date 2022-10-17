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

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.repository.FacilityRepository;
import org.siglus.siglusapi.domain.AppInfo;
import org.siglus.siglusapi.localmachine.CommonConstants;
import org.siglus.siglusapi.localmachine.auth.LocalMachineRequestFilter.AuthorizeException;
import org.siglus.siglusapi.localmachine.repository.AgentInfoRepository;
import org.siglus.siglusapi.repository.AppInfoRepository;
import org.springframework.mock.web.MockHttpServletRequest;

@RunWith(MockitoJUnitRunner.class)
public class LocalMachineRequestFilterTest {
  @InjectMocks private LocalMachineRequestFilter localMachineRequestFilter;
  @Mock private AgentInfoRepository agentInfoRepository;
  @Mock private FacilityRepository facilityRepository;
  @Mock private AppInfoRepository appInfoRepository;

  private final UUID facilityId = UUID.randomUUID();
  private final String facilityCode = "facilityCode";

  @Before
  public void setup() {
    given(agentInfoRepository.findOneByMachineIdAndFacilityId(any(), any())).willReturn(null);
  }

  @Test(expected = AuthorizeException.class)
  public void shouldThrowGivenTokenHeaderIsEmpty() {
    // given
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(CommonConstants.VERSION, "1.0");
    request.removeHeader(CommonConstants.ACCESS_TOKEN);
    // when
    localMachineRequestFilter.authenticate(request);
  }

  @Test
  public void shouldSaveMachineDeviceInfoWhenNewRequestIn() {
    // given
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(CommonConstants.VERSION, "1.0");
    request.addHeader(CommonConstants.DEVICE_INFO, "deviceInfo");
    when(facilityRepository.findOne((UUID) any())).thenReturn(mockFacility());
    when(appInfoRepository.findByFacilityCode(facilityCode)).thenReturn(null);
    MachineToken machineToken = new MachineToken();
    machineToken.setMachineId(UUID.randomUUID());
    machineToken.setFacilityId(UUID.randomUUID());

    // when
    localMachineRequestFilter.updateMachineDeviceInfo(request, machineToken);

    // then
    verify(appInfoRepository, times(1)).save((AppInfo) any());
  }

  private Facility mockFacility() {
    Facility facility = new Facility();
    facility.setId(facilityId);
    facility.setCode(facilityCode);
    return facility;
  }
}
