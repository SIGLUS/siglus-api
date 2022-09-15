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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.siglus.siglusapi.constant.FieldConstants.ALL_GEOGRAPHIC_ZONE;
import static org.siglus.siglusapi.constant.FieldConstants.CMM;
import static org.siglus.siglusapi.constant.FieldConstants.DISTRICT;
import static org.siglus.siglusapi.constant.FieldConstants.DISTRICT_PORTUGUESE;
import static org.siglus.siglusapi.constant.FieldConstants.DRUG_CODE_PORTUGUESE;
import static org.siglus.siglusapi.constant.FieldConstants.DRUG_NAME_PORTUGUESE;
import static org.siglus.siglusapi.constant.FieldConstants.EMPTY_VALUE;
import static org.siglus.siglusapi.constant.FieldConstants.FACILITY_PORTUGUESE;
import static org.siglus.siglusapi.constant.FieldConstants.PROGRAM_PORTUGUESE;
import static org.siglus.siglusapi.constant.FieldConstants.PROVINCE;
import static org.siglus.siglusapi.constant.FieldConstants.PROVINCE_PORTUGUESE;
import static org.siglus.siglusapi.constant.FieldConstants.REPORT_GENERATED_FOR_PORTUGUESE;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
import org.siglus.siglusapi.dto.TracerDrugExcelDto;
import org.siglus.siglusapi.dto.TracerDrugExportDto;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.repository.TracerDrugRepository;
import org.siglus.siglusapi.service.client.SiglusFacilityReferenceDataService;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.springframework.test.util.ReflectionTestUtils;

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
  private final String dateFormat = "yyyy-MM-dd";
  private final String startDate = "2022-03-02";
  private final String endDate = "2022-04-02";
  private List<RequisitionGeographicZonesDto> requisitionGeographicZonesDto;
  private RequisitionGeographicZonesDto districtZone1;
  private RequisitionGeographicZonesDto districtZone2;
  private List<TracerDrugDto> tracerDrugDtos;
  private AssociatedGeographicZoneDto expectedAssociatedGeographicZone1;
  private AssociatedGeographicZoneDto expectedAssociatedGeographicZone2;
  private AssociatedGeographicZoneDto expectedAssociatedGeographicZone3;
  private AssociatedGeographicZoneDto expectedAssociatedGeographicZone4;
  private Map<String, List<TracerDrugExcelDto>> tracerDrugMap;

  @Before
  public void prepareMockDate() throws ParseException {
    ReflectionTestUtils.setField(tracerDrugReportService, "dateFormat", "dd/MM/yyyy");
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

    tracerDrugMap = new HashMap<>();
    SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
    tracerDrugMap.put("1", Arrays.asList(
        TracerDrugExcelDto.builder().computationTime(sdf.parse("2022-03-04"))
            .stockOnHand(2).stockStatusColorCode(1).build(),
        TracerDrugExcelDto.builder().computationTime(sdf.parse("2022-04-04"))
            .stockStatusColorCode(0).build())
    );

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

  @Test
  public void shouldGetAllAuthorizedFacilityCodeWhenUserChooseAllProvince() {
    // given
    when(siglusFacilityReferenceDataService.findOne(facilityId)).thenReturn(FacilityDto.builder().build());
    when(authenticationHelper.getFacilityGeographicZoneLevel()).thenReturn("");
    when(authenticationHelper.isTheCurrentUserAdmin()).thenReturn(true);
    List<String> expectedRequisitionFacilityCode = Stream.of(districtZone1,
        districtZone2,
        districtZone2).map(RequisitionGeographicZonesDto::getFacilityCode).collect(Collectors.toList());

    // when
    List<String> requisitionFacilityCode = tracerDrugReportService.getRequisitionFacilityCode(ALL_GEOGRAPHIC_ZONE,
        ALL_GEOGRAPHIC_ZONE);

    // then
    assertEquals(expectedRequisitionFacilityCode, requisitionFacilityCode);
  }

  @Test
  public void shouldGetPartOfAuthorizedFacilityCodeWhenUserChooseAllDistrict() {
    // given
    when(siglusFacilityReferenceDataService.findOne(facilityId)).thenReturn(FacilityDto.builder().build());
    when(authenticationHelper.getFacilityGeographicZoneLevel()).thenReturn("");
    when(authenticationHelper.isTheCurrentUserAdmin()).thenReturn(true);
    List<String> expectedRequisitionFacilityCode = Stream.of(districtZone2,
        districtZone2).map(RequisitionGeographicZonesDto::getFacilityCode).collect(Collectors.toList());

    // when
    List<String> requisitionFacilityCode = tracerDrugReportService.getRequisitionFacilityCode(ALL_GEOGRAPHIC_ZONE,
        "04");

    // then
    assertEquals(expectedRequisitionFacilityCode, requisitionFacilityCode);
  }

  @Test
  public void shouldGetPartOfAuthorizedFacilityCodeWhenUserChooseSpecifiedDistrict() {
    // given
    when(siglusFacilityReferenceDataService.findOne(facilityId)).thenReturn(FacilityDto.builder().build());
    when(authenticationHelper.getFacilityGeographicZoneLevel()).thenReturn("");
    when(authenticationHelper.isTheCurrentUserAdmin()).thenReturn(true);
    List<String> expectedRequisitionFacilityCode = Stream.of(districtZone1)
        .map(RequisitionGeographicZonesDto::getFacilityCode).collect(Collectors.toList());

    // when
    List<String> requisitionFacilityCode = tracerDrugReportService.getRequisitionFacilityCode("0301",
        "03");

    // then
    assertEquals(expectedRequisitionFacilityCode, requisitionFacilityCode);
  }

  @Test
  public void shouldConvertManyRowsToOneRowWIthManyColumnsByTracerDrugList() throws ParseException {
    // given
    int[][] colorArrays = new int[1][2];
    List<Object> expectedDataRow = new LinkedList<>();
    expectedDataRow.add(null);
    expectedDataRow.add(null);
    expectedDataRow.add(null);
    expectedDataRow.add(null);
    expectedDataRow.add(null);
    expectedDataRow.add(null);
    expectedDataRow.add(null);
    expectedDataRow.add(startDate + "-" + endDate);
    expectedDataRow.add(2);
    expectedDataRow.add(EMPTY_VALUE);
    List<List<Object>> expectedDataRows = Collections.singletonList(expectedDataRow);

    // when
    List<List<Object>> dataRows = tracerDrugReportService.getDataRows(startDate, endDate, tracerDrugMap, colorArrays);

    // then
    assertEquals(expectedDataRows, dataRows);
    assertEquals(1, colorArrays[0][0]);
    assertEquals(0, colorArrays[0][1]);
  }

  @Test
  public void shouldGetExcelHeadColumnsByTracerDrugList() throws ParseException {
    // given
    List<List<String>> expectedHeadColumns = new LinkedList<>();
    expectedHeadColumns.add(Collections.singletonList(DRUG_CODE_PORTUGUESE));
    expectedHeadColumns.add(Collections.singletonList(PROGRAM_PORTUGUESE));
    expectedHeadColumns.add(Collections.singletonList(DRUG_NAME_PORTUGUESE));
    expectedHeadColumns.add(Collections.singletonList(PROVINCE_PORTUGUESE));
    expectedHeadColumns.add(Collections.singletonList(DISTRICT_PORTUGUESE));
    expectedHeadColumns.add(Collections.singletonList(FACILITY_PORTUGUESE));
    expectedHeadColumns.add(Collections.singletonList(CMM));
    expectedHeadColumns.add(Collections.singletonList(REPORT_GENERATED_FOR_PORTUGUESE));
    expectedHeadColumns.add(Collections.singletonList("04/03/2022"));
    expectedHeadColumns.add(Collections.singletonList("04/04/2022"));

    // when
    List<List<String>> headRow = tracerDrugReportService.getHeadRow(tracerDrugMap);

    // then
    assertEquals(expectedHeadColumns, headRow);
  }

  @Test
  public void shouldCallInsertDataWithinSpecifiedTime() {
    // given
    when(tracerDrugRepository.insertDataWithinSpecifiedTime(startDate, endDate)).thenReturn(1);
    // when
    tracerDrugReportService.refreshTracerDrugPersistentData(startDate, endDate);
    // then
    verify(tracerDrugRepository).insertDataWithinSpecifiedTime(startDate, endDate);
  }

  @Test
  public void shouldCallInsertDataWithinSpecifiedTimeWithConfiguredStartDate() {
    // given
    when(tracerDrugRepository.insertDataWithinSpecifiedTime(startDate, endDate)).thenReturn(1);
    ReflectionTestUtils.setField(tracerDrugReportService, "tracerDrugInitializeDate", "2021-08-01");
    // when
    tracerDrugReportService.initializeTracerDrugPersistentData();
    // then
    verify(tracerDrugRepository).insertDataWithinSpecifiedTime("2021-08-01", LocalDate.now().toString());
  }

  @Test
  public void shouldCallInsertDataWithinSpecifiedTimeByFacilityIds() {
    // given
    List<UUID> facilityIds = Collections.singletonList(facilityId);
    when(tracerDrugRepository.insertDataWithinSpecifiedTimeByFacilityIds(startDate, endDate, facilityIds))
        .thenReturn(1);
    // when
    tracerDrugReportService.refreshTracerDrugPersistentDataByFacility(facilityIds, startDate, endDate);
    // then
    verify(tracerDrugRepository).insertDataWithinSpecifiedTimeByFacilityIds(startDate, endDate, facilityIds);
  }

}