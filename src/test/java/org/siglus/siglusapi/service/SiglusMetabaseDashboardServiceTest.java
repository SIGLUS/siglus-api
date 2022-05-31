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

package org.siglus.siglusapi.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.domain.FacilityLevel;
import org.siglus.siglusapi.domain.FacilityType;
import org.siglus.siglusapi.domain.MetaBaseConfig;
import org.siglus.siglusapi.dto.FacilityDto;
import org.siglus.siglusapi.dto.FacilityTypeDto;
import org.siglus.siglusapi.dto.GeographicLevelDto;
import org.siglus.siglusapi.dto.GeographicZoneDto;
import org.siglus.siglusapi.repository.MetabaseDashboardRepository;

@SuppressWarnings("PMD.TooManyMethods")
@RunWith(MockitoJUnitRunner.class)
public class SiglusMetabaseDashboardServiceTest {

  @InjectMocks
  private SiglusMetabaseDashboardService siglusMetabaseDashboardService;

  @Mock
  private MetabaseDashboardRepository metabaseDashboardRepository;

  private FacilityDto emptyFacilityDto;
  private FacilityDto testFacilityDto;
  private String level;
  private String dashboardName;
  private String typeCode;

  @Before
  public void prepare() {
    level = FacilityLevel.DISTRICT.getFacilityLevelName();
    dashboardName = "system_version_report";
    typeCode = FacilityType.CS.getFacilityType();
    emptyFacilityDto = FacilityDto.builder().build();

    GeographicLevelDto province = GeographicLevelDto.builder().code("province").build();
    GeographicZoneDto partProvinceZoneDto = GeographicZoneDto.builder().code("01").level(province)
        .build();

    GeographicLevelDto district = GeographicLevelDto.builder().code("district").build();
    GeographicZoneDto geographicZoneDto = GeographicZoneDto.builder().code("0101").level(district)
        .parent(partProvinceZoneDto).build();
    FacilityTypeDto type = new FacilityTypeDto();
    type.setCode(typeCode);
    testFacilityDto = FacilityDto.builder().type(type).geographicZone(geographicZoneDto).build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldThrowIllegalArgumentExceptionWhenThereIsNoMappingLevelCodeToLevel() {
    // when
    siglusMetabaseDashboardService.getCorrespondindGeographicCodeByLevel(
        FacilityLevel.DISTRICT.getFacilityLevelName(), emptyFacilityDto);
  }

  @Test
  public void shouldReturnCorrespondingGeographicCodeWhenThereIsMappingLevelCodeToLevel() {
    // when
    String correspondindGeographicCodeByLevel = siglusMetabaseDashboardService.getCorrespondindGeographicCodeByLevel(
        FacilityLevel.DISTRICT.getFacilityLevelName(), testFacilityDto);
    // then
    assertEquals(testFacilityDto.getGeographicZone().getCode(), correspondindGeographicCodeByLevel);

  }

  @Test
  public void shouldReturnRightParamWhenThereIsMappingParamByLevel() {
    // when
    String requestParamKey = siglusMetabaseDashboardService.getRequestParamKeyByLevel(
        level);
    // then
    assertEquals(FacilityType.DDM.getFacilityLevel().getMetabaseRequestParamKey(), requestParamKey);
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldThrowIllegalArgumentExceptionWhenThereIsNotMappingParamByLevel() {
    // given
    String level = "testLevel";
    // when
    siglusMetabaseDashboardService.getRequestParamKeyByLevel(level);
  }

  @Test
  public void shouldReturnEmptyFormatBodyWhenLevelIsNational() {
    // given
    String level = FacilityLevel.NATIONAL.getFacilityLevelName();
    // when
    String requestParam = siglusMetabaseDashboardService.getRequestParamByLevel(level,
        emptyFacilityDto);
    // then
    assertEquals("", requestParam);
  }

  @Test
  public void shouldReturnRightFormatBodyWhenLevelAndFacilityDtoIsMapping() {
    // when
    String requestParam = siglusMetabaseDashboardService.getRequestParamByLevel(level,
        testFacilityDto);
    // then
    assertEquals(
        "\"" + FacilityType.DDM.getFacilityLevel().getMetabaseRequestParamKey() + "\": \""
            + testFacilityDto.getGeographicZone().getCode() + "\"", requestParam);
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldThrowIllegalArgumentExceptionWhenLevelAndFacilityDtoIsNotMapping() {
    // given
    level = "testLevel";
    // when
    siglusMetabaseDashboardService.getRequestParamByLevel(level, testFacilityDto);
  }

  @Test
  public void shouldReturnDashboardIdWhenThereIsCorrespondingDataInDatabase() {
    // when
    when(metabaseDashboardRepository.findByDashboardName(dashboardName)).thenReturn(
        Optional.ofNullable(MetaBaseConfig.builder().dashboardId(5).build()));
    // then
    Integer reponseDashboardId = siglusMetabaseDashboardService.getDashboardIdByDashboardName(
        dashboardName);
    assertEquals(Integer.valueOf(5), reponseDashboardId);
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldThrowIllegalArgumentExceptionWhenThereIsNotCorrespondingDataInDatabase() {
    // when
    when(metabaseDashboardRepository.findByDashboardName(dashboardName)).thenReturn(
        Optional.empty());
    siglusMetabaseDashboardService.getDashboardIdByDashboardName(dashboardName);
  }

  @Test
  public void shouldReturnLevelWhenThereIsCorrespondingLevelByTypeCode() {
    // when
    String level = siglusMetabaseDashboardService.getLevelByTypeCode(typeCode);
    // then
    assertEquals("site", level);
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldReturnLevelWhenThereIsNotCorrespondingLevelByTypeCode() {
    // given
    String typeCpde = "CSTEST";
    // when
    siglusMetabaseDashboardService.getLevelByTypeCode(typeCpde);
  }

}
