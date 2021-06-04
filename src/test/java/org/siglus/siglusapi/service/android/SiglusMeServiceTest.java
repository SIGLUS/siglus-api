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

package org.siglus.siglusapi.service.android;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.common.dto.referencedata.FacilityDto;
import org.siglus.common.dto.referencedata.SupportedProgramDto;
import org.siglus.common.dto.referencedata.UserDto;
import org.siglus.common.service.client.SiglusFacilityReferenceDataService;
import org.siglus.common.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.dto.response.android.FacilityResponse;

@RunWith(MockitoJUnitRunner.class)
public class SiglusMeServiceTest {

  @InjectMocks
  private SiglusMeService service;

  @Mock
  private SiglusFacilityReferenceDataService facilityReferenceDataService;

  @Mock
  private SiglusAuthenticationHelper authenticationHelper;

  private String facilityCode = "facilityCode";

  private String facilityName = "facilityName";

  private UUID facilityId = UUID.randomUUID();

  private List<SupportedProgramDto> programDtos = new ArrayList<>();

  @Before
  public void setUp() {
    UserDto userDto = new UserDto();
    userDto.setHomeFacilityId(facilityId);
    when(authenticationHelper.getCurrentUser()).thenReturn(userDto);
  }

  @Test
  public void shouldCallFacilityReferenceDataServiceWhenGetFacility() {
    // given
    programDtos.add(getSupportedProgramDto());
    FacilityDto facilityDto = new FacilityDto();
    facilityDto.setCode(facilityCode);
    facilityDto.setName(facilityName);
    facilityDto.setSupportedPrograms(programDtos);
    when(facilityReferenceDataService.getFacilityById(facilityId)).thenReturn(facilityDto);

    // when
    FacilityResponse response = service.getFacility();

    // then
    assertEquals(programDtos.get(0).getCode(),response.getSupportedPrograms().get(0).getCode());
  }

  private SupportedProgramDto getSupportedProgramDto() {
    SupportedProgramDto supportedProgram = new SupportedProgramDto();
    supportedProgram.setId(UUID.randomUUID());
    supportedProgram.setCode("ARV");
    supportedProgram.setName("ARV");
    supportedProgram.setDescription("description");
    supportedProgram.setProgramActive(true);
    supportedProgram.setSupportActive(true);
    supportedProgram.setSupportLocallyFulfilled(true);
    supportedProgram.setSupportStartDate(LocalDate.now());
    return supportedProgram;
  }
}
