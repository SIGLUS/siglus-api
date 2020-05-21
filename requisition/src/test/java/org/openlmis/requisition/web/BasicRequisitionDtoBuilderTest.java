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

package org.openlmis.requisition.web;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anySetOf;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionDataBuilder;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.dto.BasicRequisitionDto;
import org.openlmis.requisition.dto.FacilityDto;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.service.PeriodService;
import org.openlmis.requisition.service.referencedata.FacilityReferenceDataService;
import org.openlmis.requisition.service.referencedata.PeriodReferenceDataService;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.openlmis.requisition.testutils.DtoGenerator;

@RunWith(MockitoJUnitRunner.class)
public class BasicRequisitionDtoBuilderTest {

  @Mock
  private FacilityReferenceDataService facilityReferenceDataService;

  @Mock
  private PeriodService periodService;

  @Mock
  private ProgramReferenceDataService programReferenceDataService;

  @Mock
  private PeriodReferenceDataService periodReferenceDataService;

  private FacilityDto facility = DtoGenerator.of(FacilityDto.class);
  private ProcessingPeriodDto processingPeriod = DtoGenerator.of(ProcessingPeriodDto.class);
  private ProgramDto program = DtoGenerator.of(ProgramDto.class);

  @InjectMocks
  private BasicRequisitionDtoBuilder basicRequisitionDtoBuilder = new BasicRequisitionDtoBuilder();

  private Requisition requisition;

  private UUID requisitionUuid = UUID.randomUUID();
  private UUID supervisoryNodeUuid = UUID.randomUUID();

  @Before
  public void setUp() {
    requisition = new RequisitionDataBuilder()
        .withId(requisitionUuid)
        .withSupervisoryNodeId(supervisoryNodeUuid)
        .withModifiedDate(ZonedDateTime.now())
        .withFacilityId(facility.getId())
        .withProgramId(program.getId())
        .withProcessingPeriodId(processingPeriod.getId())
        .withStatus(RequisitionStatus.INITIATED)
        .withEmergency(false)
        .build();
  }

  @Test
  public void shouldBuildDtoFromRequisition() {
    when(facilityReferenceDataService.findOne(facility.getId())).thenReturn(facility);
    when(programReferenceDataService.findOne(program.getId())).thenReturn(program);
    when(periodService.getPeriod(processingPeriod.getId())).thenReturn(processingPeriod);

    BasicRequisitionDto basicRequisitionDto = basicRequisitionDtoBuilder.build(requisition);

    assertDto(basicRequisitionDto);
  }

  @Test
  public void shouldBuildDtoFromRequisitionWhenReferenceDataInstancesDoNotExist() {
    when(facilityReferenceDataService.findOne(facility.getId())).thenReturn(null);
    when(programReferenceDataService.findOne(program.getId())).thenReturn(null);
    when(periodService.getPeriod(processingPeriod.getId())).thenReturn(null);

    BasicRequisitionDto basicRequisitionDto = basicRequisitionDtoBuilder.build(requisition);

    assertNotNull(basicRequisitionDto);
    assertNull(basicRequisitionDto.getFacility());
    assertNull(basicRequisitionDto.getProgram());
    assertNull(basicRequisitionDto.getProcessingPeriod());
  }

  @Test
  public void shouldCallReferenceDataIfPassedValuesAreNull() {
    when(facilityReferenceDataService.findOne(facility.getId())).thenReturn(facility);
    when(programReferenceDataService.findOne(program.getId())).thenReturn(program);
    when(periodService.getPeriod(processingPeriod.getId())).thenReturn(processingPeriod);

    BasicRequisitionDto basicRequisitionDto = basicRequisitionDtoBuilder
        .build(requisition, null, null);

    assertDto(basicRequisitionDto);

    verify(facilityReferenceDataService).findOne(facility.getId());
    verify(programReferenceDataService).findOne(program.getId());
    verify(periodService).getPeriod(processingPeriod.getId());
  }

  @Test
  public void shouldNotCallReferenceDataIfPassedValuesAreNotNull() {
    when(periodService.getPeriod(processingPeriod.getId())).thenReturn(processingPeriod);

    BasicRequisitionDto basicRequisitionDto = basicRequisitionDtoBuilder
        .build(requisition, facility, program);

    assertDto(basicRequisitionDto);

    verify(facilityReferenceDataService, never()).findOne(any(UUID.class));
    verify(programReferenceDataService, never()).findOne(any(UUID.class));
    verify(periodService).getPeriod(processingPeriod.getId());
  }

  @Test
  public void shouldBuildListOfDtos() {
    when(facilityReferenceDataService.search(anySetOf(UUID.class)))
        .thenReturn(Lists.newArrayList(facility));
    when(programReferenceDataService.search(anySetOf(UUID.class)))
        .thenReturn(Lists.newArrayList(program));
    when(periodReferenceDataService.search(anySetOf(UUID.class)))
        .thenReturn(Lists.newArrayList(processingPeriod));

    List<BasicRequisitionDto> list = basicRequisitionDtoBuilder
        .build(Lists.newArrayList(requisition));

    assertThat(list, hasSize(1));
    assertDto(list.get(0));


    verify(facilityReferenceDataService, never()).findOne(any(UUID.class));
    verify(programReferenceDataService, never()).findOne(any(UUID.class));
    verify(periodReferenceDataService, never()).findOne(any(UUID.class));
    verify(periodService, never()).getPeriod(any(UUID.class));
  }

  private void assertDto(BasicRequisitionDto basicRequisitionDto) {
    assertNotNull(basicRequisitionDto);
    assertEquals(requisition.getId(), basicRequisitionDto.getId());
    assertEquals(requisition.getEmergency(), basicRequisitionDto.getEmergency());
    assertEquals(facility, basicRequisitionDto.getFacility());
    assertEquals(program, basicRequisitionDto.getProgram());
    assertEquals(processingPeriod, basicRequisitionDto.getProcessingPeriod());
    assertEquals(requisition.getModifiedDate(), basicRequisitionDto.getModifiedDate());
  }
}
