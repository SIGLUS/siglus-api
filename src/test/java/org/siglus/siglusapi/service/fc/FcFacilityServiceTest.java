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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.siglus.siglusapi.constant.FcConstants.STATUS_ACTIVE;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.powermock.modules.junit4.PowerMockRunner;
import org.siglus.common.dto.referencedata.FacilityDto;
import org.siglus.common.dto.referencedata.FacilityTypeDto;
import org.siglus.common.dto.referencedata.GeographicLevelDto;
import org.siglus.common.dto.referencedata.OpenLmisGeographicZoneDto;
import org.siglus.siglusapi.service.client.SiglusFacilityReferenceDataService;
import org.siglus.siglusapi.domain.ProgramRealProgram;
import org.siglus.siglusapi.dto.fc.FcAreaDto;
import org.siglus.siglusapi.dto.fc.FcFacilityDto;
import org.siglus.siglusapi.repository.ProgramRealProgramRepository;
import org.siglus.siglusapi.service.client.SiglusFacilityTypeReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusGeographicZoneReferenceDataService;

@RunWith(PowerMockRunner.class)
public class FcFacilityServiceTest {

  @Mock
  private SiglusFacilityReferenceDataService facilityService;

  @Mock
  private SiglusGeographicZoneReferenceDataService geographicZoneService;

  @Mock
  private ProgramReferenceDataService programReferenceDataService;

  @Mock
  private ProgramRealProgramRepository programRealProgramRepository;

  @Mock
  private SiglusFacilityTypeReferenceDataService facilityTypeService;

  @Mock
  private FcSourceDestinationService fcSourceDestinationService;

  @InjectMocks
  private FcFacilityService fcFacilityService;

  @Captor
  private ArgumentCaptor<FacilityDto> facilityCaptor;

  private final UUID programId = UUID.randomUUID();

  @Test
  public void shouldReturnFalseGivenEmptyFcResult() {
    // when
    boolean result = fcFacilityService.processFacility(Collections.emptyList());

    // then
    assertFalse(result);
  }

  @Test
  public void shouldNewAddedFacility() {
    // given
    ProgramDto programDto = getProgramDto();
    when(programReferenceDataService.findAll()).thenReturn(Collections.singletonList(programDto));
    when(programRealProgramRepository.findAll()).thenReturn(
        Collections.singletonList(
            mockRealProgram(UUID.randomUUID(), "PT", programDto.getCode(),
                "PTV", true)));
    when(facilityService.findAll()).thenReturn(Collections.emptyList());
    when(geographicZoneService.searchAllGeographicZones()).thenReturn(getGeographicZones());
    FacilityTypeDto typeDto1 = mockFacilityTypeDto("type1", "typeName", true);
    when(facilityTypeService.searchAllFacilityTypes())
        .thenReturn(Collections.singletonList(typeDto1));
    FcFacilityDto facilityDto = getFcFacilityDto("facility", "type1");

    // when
    boolean isProcess = fcFacilityService.processFacility(Collections.singletonList(facilityDto));

    // then
    assertTrue(isProcess);
    verify(facilityService).createFacility(facilityCaptor.capture());
    verify(fcSourceDestinationService).createSourceAndDestination(any());
    assertEquals("description 1",
        facilityCaptor.getValue().getGeographicZone().getName());
    assertEquals("facility", facilityCaptor.getValue().getCode());
    assertEquals("type1", facilityCaptor.getValue().getType().getCode());
    assertEquals("TARV", facilityCaptor.getValue()
        .getSupportedPrograms().get(0).getName());
  }

  @Test
  public void shouldSaveFacilityWhenSupportProgramNotExit() {
    // given
    ProgramDto programDto = getProgramDto();
    when(programReferenceDataService.findAll()).thenReturn(Collections.singletonList(programDto));
    when(programRealProgramRepository.findAll()).thenReturn(
        Collections.singletonList(
            mockRealProgram(UUID.randomUUID(), "PT", programDto.getCode(),
                "PTV", true)));
    FacilityDto originFacility = new FacilityDto();
    originFacility.setId(UUID.randomUUID());
    originFacility.setName("facility");
    originFacility.setCode("facility2");
    originFacility.setActive(true);
    originFacility.setDescription("description");
    originFacility.setSupportedPrograms(Collections.emptyList());
    FacilityTypeDto typeDto = new FacilityTypeDto();
    typeDto.setCode("type2");
    originFacility.setType(typeDto);
    OpenLmisGeographicZoneDto zoneDto = new OpenLmisGeographicZoneDto();
    zoneDto.setCode("nationalGeographicZone");
    originFacility.setGeographicZone(zoneDto);
    when(facilityService.findOne(originFacility.getId())).thenReturn(originFacility);
    when(facilityService.findAll()).thenReturn(Collections.singletonList(originFacility));
    when(geographicZoneService.searchAllGeographicZones()).thenReturn(getGeographicZones());
    FacilityTypeDto typeDto1 = mockFacilityTypeDto(typeDto.getCode(), "typeName", true);
    when(facilityTypeService.searchAllFacilityTypes())
        .thenReturn(Collections.singletonList(typeDto1));
    FcFacilityDto facilityDto = getFcFacilityDto("facility2", "type2");

    // when
    fcFacilityService.processFacility(Collections.singletonList(facilityDto));

    // then
    verify(facilityService).saveFacility(facilityCaptor.capture());
    assertEquals("description 1",
        facilityCaptor.getValue().getGeographicZone().getName());
    assertEquals("facility2", facilityCaptor.getValue().getCode());
    assertEquals("type2", facilityCaptor.getValue().getType().getCode());
    assertEquals("TARV", facilityCaptor.getValue()
        .getSupportedPrograms().get(0).getName());
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

  private ProgramDto getProgramDto() {
    ProgramDto programDto = new ProgramDto();
    programDto.setId(programId);
    programDto.setCode("T");
    programDto.setName("TARV");
    programDto.setActive(true);
    return programDto;
  }

  private ProgramRealProgram mockRealProgram(UUID id, String code, String programCode,
      String name, boolean active) {
    ProgramRealProgram program = new ProgramRealProgram();
    program.setId(id);
    program.setRealProgramCode(code);
    program.setRealProgramName(name);
    program.setProgramCode(programCode);
    program.setActive(active);
    return program;
  }

  private List<OpenLmisGeographicZoneDto> getGeographicZones() {
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
    OpenLmisGeographicZoneDto national1 = OpenLmisGeographicZoneDto.builder()
        .code("nationalGeographicZone")
        .name("description 1")
        .level(level1)
        .parent(null)
        .build();
    OpenLmisGeographicZoneDto province1 = OpenLmisGeographicZoneDto.builder()
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
