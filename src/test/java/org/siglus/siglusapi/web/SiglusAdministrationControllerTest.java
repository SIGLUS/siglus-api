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

import java.util.UUID;
import javax.servlet.http.HttpServletResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.dto.FacilitySearchParamDto;
import org.siglus.siglusapi.dto.SiglusFacilityDto;
import org.siglus.siglusapi.service.SiglusAdministrationsService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.mock.web.MockHttpServletResponse;


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

  private static final UUID facilityId = UUID.randomUUID();

  @Test
  public void shouldDisplayFacilitiesWithIsAndroid() {
    // when
    FacilitySearchParamDto facilitySearchParamDto = mockFacilitySearchParamDto();
    siglusAdministrationsController.showFacilitiesInfos(facilitySearchParamDto, pageable);

    // then
    verify(siglusAdministrationsService).searchForFacilities(facilitySearchParamDto, pageable);
  }

  @Test
  public void eraseAndroidByFacilityId() {
    // when
    siglusAdministrationsController.eraseAndroidDeviceInfo(facilityCode);

    // then
    verify(siglusAdministrationsService).eraseDeviceInfoByFacilityId(facilityCode);
  }

  @Test
  public void shouldGetFacilityInfo() {
    // when
    siglusAdministrationsController.getFacility(facilityId);

    // then
    verify(siglusAdministrationsService).getFacility(facilityId);
  }

  @Test
  public void shouldSaveFacilityInfo() {
    // when
    siglusAdministrationsController.updateFacility(facilityId, mockSiglusFacilityDto());

    // then
    verify(siglusAdministrationsService).updateFacility(facilityId, mockSiglusFacilityDto());
  }

  @Test
  public void shouldExportLocationManagementExcel() {
    // when
    HttpServletResponse httpServletResponse = new MockHttpServletResponse();
    siglusAdministrationsController.exportLocationManagementTemplate(facilityId, httpServletResponse);

    // then
    verify(siglusAdministrationsService).exportLocationInfo(facilityId, httpServletResponse);
  }

  @Test
  public void shouldCreateNewFacility() {
    // given
    SiglusFacilityDto siglusFacilityDto = mockSiglusFacilityDto();

    // when
    siglusAdministrationsController.createFacility(siglusFacilityDto);

    // then
    verify(siglusAdministrationsService).createFacility(siglusFacilityDto);
  }

  @Test
  public void shouldUpgradeAndroidFacilityToWeb() {
    // when
    siglusAdministrationsController.upgradeAndroidFacilityToWeb(facilityId);

    // then
    verify(siglusAdministrationsService).upgradeAndroidFacilityToWeb(facilityId);
  }

  private FacilitySearchParamDto mockFacilitySearchParamDto() {
    FacilitySearchParamDto facilitySearchParamDto = new FacilitySearchParamDto();
    facilitySearchParamDto.setName(Name);
    return facilitySearchParamDto;
  }

  private SiglusFacilityDto mockSiglusFacilityDto() {
    SiglusFacilityDto siglusFacilityDto = new SiglusFacilityDto();
    siglusFacilityDto.setEnableLocationManagement(true);
    siglusFacilityDto.setId(facilityId);
    return siglusFacilityDto;
  }
}
