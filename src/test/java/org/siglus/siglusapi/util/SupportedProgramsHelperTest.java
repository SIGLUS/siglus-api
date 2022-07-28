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

package org.siglus.siglusapi.util;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.dto.FacilityDto;
import org.siglus.siglusapi.dto.FacilityTypeDto;
import org.siglus.siglusapi.dto.SupportedProgramDto;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.service.client.SiglusFacilityReferenceDataService;

@RunWith(MockitoJUnitRunner.class)
public class SupportedProgramsHelperTest {

  @InjectMocks
  private SupportedProgramsHelper supportedProgramsHelper;

  @Mock
  private SiglusAuthenticationHelper authenticationHelper;

  @Mock
  private SiglusFacilityReferenceDataService facilityReferenceDataService;

  @Mock
  private SiglusDateHelper dateHelper;

  private final UUID facilityId = UUID.randomUUID();

  @Test
  public void shouldFilterSupportedProgramsByUserPermission() {
    // given
    UserDto userDto = new UserDto();
    userDto.setHomeFacilityId(facilityId);
    when(authenticationHelper.getCurrentUser()).thenReturn(userDto);
    FacilityDto facilityDto = new FacilityDto();
    FacilityTypeDto typeDto = new FacilityTypeDto();
    typeDto.setCode("DDM");
    facilityDto.setType(typeDto);
    SupportedProgramDto supportedProgramDto = SupportedProgramDto.builder().programActive(true).supportActive(true)
        .supportStartDate(LocalDate.of(2022, 3, 2)).build();
    SupportedProgramDto supportedProgramDto1 = SupportedProgramDto.builder().programActive(false).supportActive(true)
        .supportStartDate(LocalDate.of(2022, 5, 2)).build();
    facilityDto.setSupportedPrograms(Arrays.asList(supportedProgramDto1, supportedProgramDto));

    when(facilityReferenceDataService.findOne(facilityId)).thenReturn(facilityDto);
    when(dateHelper.getCurrentDate()).thenReturn(LocalDate.of(2022, 4, 2));
    // when
    List<SupportedProgramDto> homeFacilitySupportedPrograms = supportedProgramsHelper
        .findHomeFacilitySupportedPrograms();

    // then
    assertEquals(Collections.singletonList(supportedProgramDto), homeFacilitySupportedPrograms);
  }

}