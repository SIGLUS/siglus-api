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

package org.siglus.siglusapi.service.task.report;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.siglus.siglusapi.constant.FieldConstants.DISTRICT;
import static org.siglus.siglusapi.constant.FieldConstants.PROVINCE;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.dto.AssociatedGeographicZoneDto;
import org.siglus.siglusapi.dto.FacilityDto;
import org.siglus.siglusapi.dto.RequisitionGeographicZonesDto;
import org.siglus.siglusapi.dto.TracerDrugDto;
import org.siglus.siglusapi.dto.TracerDrugExportDto;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.repository.TracerDrugRepository;
import org.siglus.siglusapi.service.client.SiglusFacilityReferenceDataService;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;

@RunWith(MockitoJUnitRunner.class)
public class TracerDrugReportServiceTest {

  @Mock
  private TracerDrugRepository tracerDrugRepository;

  @InjectMocks
  private TracerDrugReportService tracerDrugReportService;

  @Mock
  private SiglusFacilityReferenceDataService siglusFacilityReferenceDataService;

  @Mock
  private SiglusAuthenticationHelper authenticationHelper;

  private final UUID facilityId = UUID.randomUUID();

  private List<RequisitionGeographicZonesDto> requisitionGeographicZonesDto;
  private RequisitionGeographicZonesDto districtZone1;
  private RequisitionGeographicZonesDto districtZone2;
  private List<TracerDrugDto> tracerDrugDtos;
  private AssociatedGeographicZoneDto expectedAssociatedGeographicZone1;
  private AssociatedGeographicZoneDto expectedAssociatedGeographicZone2;
  private AssociatedGeographicZoneDto expectedAssociatedGeographicZone3;
  private AssociatedGeographicZoneDto expectedAssociatedGeographicZone4;

  @Before
  public void prepareMockDate() {
    districtZone1 = RequisitionGeographicZonesDto
        .builder()
        .districtName("CIDADE DE NAMPULA")
        .districtCode("0301")
        .provinceName("NAMPULA")
        .provinceCode("03")
        .districtLevel(DISTRICT)
        .districtLevelCode(3)
        .provinceLevel(PROVINCE)
        .provinceLevelCode(2)
        .facilityCode("03030101")
        .provinceFacilityCode("03030101")
        .build();
    districtZone2 = RequisitionGeographicZonesDto
        .builder()
        .districtName("CIDADE DE QUELIMANE")
        .districtCode("0401")
        .provinceName("ZAMBEZIA")
        .provinceCode("04")
        .districtLevel(DISTRICT)
        .districtLevelCode(3)
        .provinceLevel(PROVINCE)
        .provinceLevelCode(2)
        .facilityCode("02040101")
        .districtFacilityCode("02040101")
        .provinceFacilityCode("03040101")
        .build();

    expectedAssociatedGeographicZone1 = AssociatedGeographicZoneDto.builder()
        .code("0301")
        .name("CIDADE DE NAMPULA")
        .parentCode("03")
        .level(DISTRICT)
        .levelCode(3)
        .build();

    expectedAssociatedGeographicZone2 = AssociatedGeographicZoneDto.builder()
        .code("0401")
        .name("CIDADE DE QUELIMANE")
        .parentCode("04")
        .level(DISTRICT)
        .levelCode(3)
        .build();

    expectedAssociatedGeographicZone3 = AssociatedGeographicZoneDto.builder()
        .code("03")
        .name("NAMPULA")
        .level(PROVINCE)
        .levelCode(2)
        .build();

    expectedAssociatedGeographicZone4 = AssociatedGeographicZoneDto.builder()
        .code("04")
        .name("ZAMBEZIA")
        .level(PROVINCE)
        .levelCode(2)
        .build();
    requisitionGeographicZonesDto = Arrays.asList(districtZone1, districtZone2, districtZone2);

    TracerDrugDto drug1 = TracerDrugDto.builder().productName("drug1").productCode("2A061").build();
    TracerDrugDto drug2 = TracerDrugDto.builder().productName("drug2").productCode("2A062").build();

    tracerDrugDtos = Arrays.asList(drug1, drug2);
    when(tracerDrugRepository.getTracerDrugInfo()).thenReturn(tracerDrugDtos);

    UserDto userDto = new UserDto();
    userDto.setHomeFacilityId(facilityId);
    when(authenticationHelper.getCurrentUser()).thenReturn(userDto);

    when(tracerDrugRepository.getAllRequisitionGeographicZones()).thenReturn(
        requisitionGeographicZonesDto);
  }


  @Test
  public void shouldGetALlGeographicZonesWhenFacilityLevelIsAdmin() {
    // given

    when(siglusFacilityReferenceDataService.findOne(facilityId)).thenReturn(FacilityDto.builder().build());
    when(authenticationHelper.getFacilityGeographicZoneLevel()).thenReturn("");
    when(authenticationHelper.isTheCurrentUserAdmin()).thenReturn(true);

    TracerDrugExportDto expectedTracerDrugExportDto = TracerDrugExportDto.builder().tracerDrugs(tracerDrugDtos)
        .geographicZones(Arrays.asList(expectedAssociatedGeographicZone3,
            expectedAssociatedGeographicZone1,
            expectedAssociatedGeographicZone4,
            expectedAssociatedGeographicZone2)).build();
    // when
    TracerDrugExportDto tracerDrugExportDto = tracerDrugReportService.getTracerDrugExportDto();

    // then
    assertEquals(expectedTracerDrugExportDto, tracerDrugExportDto);
  }

  @Test
  public void shouldGetPartialGeographicZonesWhenFacilityLevelIsProvince() {
    // given
    when(siglusFacilityReferenceDataService.findOne(facilityId)).thenReturn(
        FacilityDto.builder().code("03040101").build());
    when(authenticationHelper.getFacilityGeographicZoneLevel()).thenReturn("PROVINCE");
    when(authenticationHelper.isTheCurrentUserAdmin()).thenReturn(false);
    TracerDrugExportDto expectedTracerDrugExportDto = TracerDrugExportDto.builder().tracerDrugs(tracerDrugDtos)
        .geographicZones(Arrays.asList(expectedAssociatedGeographicZone4, expectedAssociatedGeographicZone2)).build();
    // when
    TracerDrugExportDto tracerDrugExportDto = tracerDrugReportService.getTracerDrugExportDto();

    // then
    assertEquals(expectedTracerDrugExportDto, tracerDrugExportDto);
  }

  @Test
  public void shouldGetPartialGeographicZonesWhenFacilityLevelIsDistrict() {
    // given
    when(siglusFacilityReferenceDataService.findOne(facilityId)).thenReturn(
        FacilityDto.builder().code("02040101").build());
    when(authenticationHelper.getFacilityGeographicZoneLevel()).thenReturn("DISTRICT");
    when(authenticationHelper.isTheCurrentUserAdmin()).thenReturn(false);
    TracerDrugExportDto expectedTracerDrugExportDto = TracerDrugExportDto.builder().tracerDrugs(tracerDrugDtos)
        .geographicZones(Arrays.asList(expectedAssociatedGeographicZone4, expectedAssociatedGeographicZone2)).build();
    // when
    TracerDrugExportDto tracerDrugExportDto = tracerDrugReportService.getTracerDrugExportDto();

    // then
    assertEquals(expectedTracerDrugExportDto, tracerDrugExportDto);
  }

  @Test
  public void shouldGetPartialGeographicZonesWhenFacilityLevelIsSite() {
    // given
    when(siglusFacilityReferenceDataService.findOne(facilityId)).thenReturn(
        FacilityDto.builder().code("03030101").build());
    when(authenticationHelper.getFacilityGeographicZoneLevel()).thenReturn("SITE");
    when(authenticationHelper.isTheCurrentUserAdmin()).thenReturn(false);
    TracerDrugExportDto expectedTracerDrugExportDto = TracerDrugExportDto.builder().tracerDrugs(tracerDrugDtos)
        .geographicZones(Arrays.asList(expectedAssociatedGeographicZone3, expectedAssociatedGeographicZone1)).build();
    // when
    TracerDrugExportDto tracerDrugExportDto = tracerDrugReportService.getTracerDrugExportDto();

    // then
    assertEquals(expectedTracerDrugExportDto, tracerDrugExportDto);
  }
}