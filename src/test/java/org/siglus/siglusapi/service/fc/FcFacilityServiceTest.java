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

package org.siglus.siglusapi.service.fc;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.siglus.siglusapi.constant.FcConstants.STATUS_ACTIVE;
import static org.siglus.siglusapi.service.fc.FcVariables.LAST_UPDATED_AT;
import static org.siglus.siglusapi.service.fc.FcVariables.START_DATE;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.openlmis.referencedata.web.FacilityController;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.openlmis.stockmanagement.domain.sourcedestination.Node;
import org.openlmis.stockmanagement.repository.NodeRepository;
import org.powermock.modules.junit4.PowerMockRunner;
import org.siglus.siglusapi.domain.ProgramRealProgram;
import org.siglus.siglusapi.dto.FacilityDto;
import org.siglus.siglusapi.dto.FacilityTypeDto;
import org.siglus.siglusapi.dto.GeographicLevelDto;
import org.siglus.siglusapi.dto.GeographicZoneDto;
import org.siglus.siglusapi.dto.SupportedProgramDto;
import org.siglus.siglusapi.dto.fc.FcAreaDto;
import org.siglus.siglusapi.dto.fc.FcFacilityDto;
import org.siglus.siglusapi.dto.fc.FcIntegrationResultDto;
import org.siglus.siglusapi.repository.ProgramRealProgramRepository;
import org.siglus.siglusapi.repository.SiglusFacilityRepository;
import org.siglus.siglusapi.service.client.SiglusFacilityReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusFacilityTypeReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusGeographicZoneReferenceDataService;
import org.siglus.siglusapi.util.SiglusSimulateUserAuthHelper;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.validation.BeanPropertyBindingResult;

@RunWith(PowerMockRunner.class)
public class FcFacilityServiceTest {

  public static final String FACILITY = "facility";
  public static final UUID FACILITY_ID = UUID.randomUUID();
  public static final String TYPE_1 = "type1";
  public static final String TYPE_2 = "type2";
  public static final String PTV = "PTV";
  public static final String TYPE_NAME = "typeName";
  public static final String DESCRIPTION = "description 1";
  @Mock
  private SiglusFacilityReferenceDataService facilityService;

  @Mock
  private SiglusGeographicZoneReferenceDataService geographicZoneService;

  @Mock
  private FacilityController facilityController;

  @Mock
  private ProgramReferenceDataService programReferenceDataService;

  @Mock
  private ProgramRealProgramRepository programRealProgramRepository;

  @Mock
  private SiglusFacilityTypeReferenceDataService facilityTypeService;

  @Mock
  private NodeRepository nodeRepository;

  @Mock
  private SiglusSimulateUserAuthHelper siglusSimulateUserAuthHelper;

  @Mock
  private SiglusFacilityRepository siglusFacilityRepository;

  @InjectMocks
  private FcFacilityService fcFacilityService;

  @Captor
  private ArgumentCaptor<org.openlmis.referencedata.dto.FacilityDto> rfFacilityCaptor;

  @Captor
  private ArgumentCaptor<FacilityDto> facilityCaptor;

  private final UUID programId = UUID.randomUUID();

  private final UUID roleAdminId = UUID.randomUUID();

  private final UUID adminUserId = UUID.randomUUID();

  @Before
  public void setup() {
    ReflectionTestUtils.setField(fcFacilityService, "roleAdminId", roleAdminId.toString());
    when(siglusFacilityRepository.findAdminUserIdByAdminRoleId(roleAdminId)).thenReturn(adminUserId.toString());
  }

  @Test
  public void shouldReturnNullGivenEmptyFcResult() {
    // when
    FcIntegrationResultDto result = fcFacilityService.processData(emptyList(), START_DATE, LAST_UPDATED_AT);

    // then
    assertNull(result);
  }

  @Test
  public void shouldIgnoreFacilityWhenValidateFacilityFailed() {
    // given
    ProgramDto programDto = getProgramDto();
    when(programReferenceDataService.findAll()).thenReturn(Collections.singletonList(programDto));
    when(facilityService.findAll()).thenReturn(emptyList());
    when(geographicZoneService.searchAllGeographicZones()).thenReturn(getGeographicZones());
    FcFacilityDto facilityDto = getFcFacilityDto(FACILITY, TYPE_1);
    facilityDto.setAreas(singletonList(new FcAreaDto("ABC", "ABC Description")));
    when(facilityService.createFacility(any(FacilityDto.class))).thenThrow(new RuntimeException());

    // when
    FcIntegrationResultDto result = fcFacilityService.processData(Collections.singletonList(facilityDto), START_DATE,
        LAST_UPDATED_AT);

    // then
    assertNotNull(result);
    assertTrue(result.getFinalSuccess());
    verify(facilityService, times(0)).createFacility(any());
  }

  @Test
  public void shouldFinalSuccessFalseWhenSaveFacilityFailed() {
    // given
    ProgramDto programDto = getProgramDto();
    when(programReferenceDataService.findAll()).thenReturn(Collections.singletonList(programDto));
    when(programRealProgramRepository.findAll()).thenReturn(
        Collections.singletonList(mockRealProgram(UUID.randomUUID(), "PT", programDto.getCode(), PTV, true)));
    when(facilityService.findAll()).thenReturn(emptyList());
    when(geographicZoneService.searchAllGeographicZones()).thenReturn(getGeographicZones());
    FacilityTypeDto typeDto1 = mockFacilityTypeDto(TYPE_1, TYPE_NAME, true);
    when(facilityTypeService.searchAllFacilityTypes()).thenReturn(Collections.singletonList(typeDto1));
    FcFacilityDto facilityDto = getFcFacilityDto(FACILITY, TYPE_1);
    when(facilityService.createFacility(any(FacilityDto.class))).thenThrow(new RuntimeException());

    // when
    FcIntegrationResultDto result = fcFacilityService.processData(Collections.singletonList(facilityDto), START_DATE,
        LAST_UPDATED_AT);

    // then
    assertNotNull(result);
    assertFalse(result.getFinalSuccess());
  }

  @Test
  public void shouldNewAddedFacility() {
    // given
    ProgramDto programDto = getProgramDto();
    when(programReferenceDataService.findAll()).thenReturn(Collections.singletonList(programDto));
    when(programRealProgramRepository.findAll()).thenReturn(
        Collections.singletonList(mockRealProgram(UUID.randomUUID(), "PT", programDto.getCode(), PTV, true)));
    when(facilityService.findAll()).thenReturn(emptyList());
    when(geographicZoneService.searchAllGeographicZones()).thenReturn(getGeographicZones());
    FacilityTypeDto typeDto1 = mockFacilityTypeDto(TYPE_1, TYPE_NAME, true);
    when(facilityTypeService.searchAllFacilityTypes()).thenReturn(Collections.singletonList(typeDto1));
    FcFacilityDto facilityDto = getFcFacilityDto(FACILITY, TYPE_1);
    when(facilityService.createFacility(any())).thenReturn(new FacilityDto());

    // when
    FcIntegrationResultDto result = fcFacilityService.processData(Collections.singletonList(facilityDto), START_DATE,
        LAST_UPDATED_AT);

    // then
    assertNotNull(result);
    verify(facilityService).createFacility(facilityCaptor.capture());
    verify(nodeRepository).save(any(Node.class));
    verify(siglusSimulateUserAuthHelper).simulateUserAuth(adminUserId);
    assertEquals(DESCRIPTION, facilityCaptor.getValue().getGeographicZone().getName());
    assertEquals(FACILITY, facilityCaptor.getValue().getCode());
    assertEquals(TYPE_1, facilityCaptor.getValue().getType().getCode());
    assertEquals("TARV", facilityCaptor.getValue().getSupportedPrograms().get(0).getName());
  }

  @Test
  public void shouldUpdateFacilityWhenHaveChanges() {
    // given
    ProgramDto programDto = getProgramDto();
    when(programReferenceDataService.findAll()).thenReturn(Collections.singletonList(programDto));
    when(programRealProgramRepository.findAll()).thenReturn(
        Collections.singletonList(mockRealProgram(UUID.randomUUID(), "PT", programDto.getCode(), PTV, true)));
    when(facilityService.findAll()).thenReturn(singletonList(getFacilityDto()));
    when(facilityService.findOne(FACILITY_ID)).thenReturn(getFacilityDto());
    when(geographicZoneService.searchAllGeographicZones()).thenReturn(getGeographicZones());
    FacilityTypeDto typeDto1 = mockFacilityTypeDto(TYPE_1, TYPE_NAME, true);
    when(facilityTypeService.searchAllFacilityTypes())
        .thenReturn(Collections.singletonList(typeDto1));
    FcFacilityDto facilityDto = getFcFacilityDto(FACILITY, TYPE_1);

    // when
    FcIntegrationResultDto result = fcFacilityService.processData(Collections.singletonList(facilityDto), START_DATE,
        LAST_UPDATED_AT);

    // then
    assertNotNull(result);
    verify(facilityController)
        .saveFacility(rfFacilityCaptor.capture(), eq(FACILITY_ID), any(BeanPropertyBindingResult.class));
    assertEquals(DESCRIPTION, rfFacilityCaptor.getValue().getGeographicZone().getName());
    assertEquals("facilityName", rfFacilityCaptor.getValue().getName());
    assertEquals("description", rfFacilityCaptor.getValue().getDescription());
    assertEquals(FACILITY, rfFacilityCaptor.getValue().getCode());
    assertEquals(TYPE_1, rfFacilityCaptor.getValue().getType().getCode());
    assertTrue(rfFacilityCaptor.getValue().getActive());
    assertEquals("T", rfFacilityCaptor.getValue().getSupportedPrograms().iterator().next().getCode());
  }

  @Test
  public void shouldSaveFacilityWhenSupportProgramNotExit() {
    // given
    ProgramDto programDto = getProgramDto();
    when(programReferenceDataService.findAll()).thenReturn(Collections.singletonList(programDto));
    when(programRealProgramRepository.findAll()).thenReturn(
        Collections.singletonList(
            mockRealProgram(UUID.randomUUID(), "PT", programDto.getCode(),
                PTV, true)));
    FacilityDto originFacility = new FacilityDto();
    originFacility.setId(UUID.randomUUID());
    originFacility.setName(FACILITY);
    originFacility.setCode("facility2");
    originFacility.setActive(true);
    originFacility.setDescription("description");
    originFacility.setSupportedPrograms(emptyList());
    FacilityTypeDto typeDto = new FacilityTypeDto();
    typeDto.setCode(TYPE_2);
    originFacility.setType(typeDto);
    GeographicZoneDto zoneDto = new GeographicZoneDto();
    zoneDto.setCode("nationalGeographicZone");
    originFacility.setGeographicZone(zoneDto);
    when(facilityService.findOne(originFacility.getId())).thenReturn(originFacility);
    when(facilityService.findAll()).thenReturn(Collections.singletonList(originFacility));
    when(geographicZoneService.searchAllGeographicZones()).thenReturn(getGeographicZones());
    FacilityTypeDto typeDto1 = mockFacilityTypeDto(typeDto.getCode(), TYPE_NAME, true);
    when(facilityTypeService.searchAllFacilityTypes())
        .thenReturn(Collections.singletonList(typeDto1));
    FcFacilityDto facilityDto = getFcFacilityDto("facility2", TYPE_2);

    // when
    fcFacilityService.processData(Collections.singletonList(facilityDto), START_DATE, LAST_UPDATED_AT);

    // then
    verify(siglusSimulateUserAuthHelper).simulateUserAuth(adminUserId);
    verify(facilityController)
        .saveFacility(rfFacilityCaptor.capture(), eq(originFacility.getId()), any(BeanPropertyBindingResult.class));
    assertEquals(DESCRIPTION, rfFacilityCaptor.getValue().getGeographicZone().getName());
    assertEquals("facility2", rfFacilityCaptor.getValue().getCode());
    assertEquals(TYPE_2, rfFacilityCaptor.getValue().getType().getCode());
    assertEquals("TARV", rfFacilityCaptor.getValue().getSupportedPrograms().iterator().next().getName());
  }

  public FcFacilityDto getFcFacilityDto(String code, String typeCode) {
    FcFacilityDto facilityDto = new FcFacilityDto();
    facilityDto.setCode(code);
    facilityDto.setName("facilityName");
    facilityDto.setDescription("description");
    facilityDto.setStatus(STATUS_ACTIVE);
    facilityDto.setDistrictCode("nationalGeographicZone");
    facilityDto.setClientTypeCode(typeCode);
    FcAreaDto areaDto = new FcAreaDto();
    areaDto.setAreaCode("PT");
    facilityDto.setAreas(Collections.singletonList(areaDto));
    return facilityDto;
  }

  public FacilityDto getFacilityDto() {
    SupportedProgramDto supportedProgramDto = SupportedProgramDto.builder().code("VC").build();
    GeographicZoneDto geographicZoneDto = GeographicZoneDto.builder()
        .code("province")
        .name("originalGeographicZone")
        .build();
    return FacilityDto.builder()
        .id(FACILITY_ID)
        .code(FACILITY)
        .name("originalName")
        .description("originalDescription")
        .active(false)
        .supportedPrograms(singletonList(supportedProgramDto))
        .geographicZone(geographicZoneDto)
        .type(mockFacilityTypeDto(TYPE_2, TYPE_2, false))
        .build();
  }

  private ProgramDto getProgramDto() {
    ProgramDto programDto = new ProgramDto();
    programDto.setId(programId);
    programDto.setCode("T");
    programDto.setName("TARV");
    programDto.setActive(true);
    return programDto;
  }

  private ProgramRealProgram mockRealProgram(UUID id, String code, String programCode, String name, boolean active) {
    ProgramRealProgram program = new ProgramRealProgram();
    program.setId(id);
    program.setRealProgramCode(code);
    program.setRealProgramName(name);
    program.setProgramCode(programCode);
    program.setActive(active);
    return program;
  }

  private List<GeographicZoneDto> getGeographicZones() {
    GeographicLevelDto level1 = GeographicLevelDto.builder()
        .code("national")
        .name("National")
        .levelNumber(1)
        .build();
    GeographicLevelDto level2 = GeographicLevelDto.builder()
        .code("province")
        .name("Province")
        .levelNumber(2)
        .build();
    GeographicZoneDto national1 = GeographicZoneDto.builder()
        .code("nationalGeographicZone")
        .name(DESCRIPTION)
        .level(level1)
        .parent(null)
        .build();
    GeographicZoneDto province1 = GeographicZoneDto.builder()
        .code("provinceGeographicZone")
        .name("description 11")
        .level(level2)
        .parent(national1)
        .build();
    return Arrays.asList(national1, province1);
  }

  private FacilityTypeDto mockFacilityTypeDto(String code, String name, boolean active) {
    FacilityTypeDto facilityTypeDto = new FacilityTypeDto();
    facilityTypeDto.setId(UUID.randomUUID());
    facilityTypeDto.setCode(code);
    facilityTypeDto.setName(name);
    facilityTypeDto.setActive(active);
    return facilityTypeDto;
  }
}
