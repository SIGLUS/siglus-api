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

package org.siglus.siglusapi.web;

import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.dto.FacilitySearchParamDto;
import org.siglus.siglusapi.service.SiglusAdministrationsService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;


@RunWith(MockitoJUnitRunner.class)
public class SiglusAdministrationControllerTest {

  @InjectMocks
  private SiglusAdministrationsController siglusAdministrationsController;

  @Mock
  private SiglusAdministrationsService siglusAdministrationsService;

  private static final Direction direction = Direction.ASC;

  private static final Sort sort = new Sort(direction, "name");

  private static final Pageable pageable = new PageRequest(0, 10, sort);

  private static final String facilityCode = "000000";

  private static final String Name = "A. Alimenticios";

  @Test
  public void shouldDisplayFacilitiesWithIsAndroid() {
    //when
    FacilitySearchParamDto facilitySearchParamDto = mockFacilitySearchParamDto();
    siglusAdministrationsController.showFacilitiesInfos(facilitySearchParamDto, pageable);

    //then
    verify(siglusAdministrationsService).searchForFacilities(facilitySearchParamDto, pageable);
  }

  @Test
  public void eraseAndroidByFacilityId() {
    //when
    siglusAdministrationsController.eraseAndroidDeviceInfo(facilityCode);

    //then
    verify(siglusAdministrationsService).eraseDeviceInfoByFacilityId(facilityCode);
  }

  private FacilitySearchParamDto mockFacilitySearchParamDto() {
    FacilitySearchParamDto facilitySearchParamDto = new FacilitySearchParamDto();
    facilitySearchParamDto.setName(Name);
    return facilitySearchParamDto;
  }
}
