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
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.siglus.siglusapi.constant.ProgramConstants.MALARIA_PROGRAM_CODE;
import static org.siglus.siglusapi.constant.ProgramConstants.MTB_PROGRAM_CODE;
import static org.siglus.siglusapi.constant.ProgramConstants.RAPIDTEST_PROGRAM_CODE;
import static org.siglus.siglusapi.constant.ProgramConstants.TARV_PROGRAM_CODE;
import static org.siglus.siglusapi.constant.ProgramConstants.VIA_PROGRAM_CODE;

import java.time.LocalDate;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.referencedata.domain.ProcessingPeriod;
import org.openlmis.requisition.dto.ObjectReferenceDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.siglus.siglusapi.domain.RequisitionExtension;
import org.siglus.siglusapi.dto.FacilityDto;
import org.siglus.siglusapi.dto.SiglusRequisitionDto;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.repository.ProcessingPeriodRepository;
import org.siglus.siglusapi.repository.RequisitionExtensionRepository;
import org.siglus.siglusapi.service.client.SiglusFacilityReferenceDataService;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;

@RunWith(MockitoJUnitRunner.class)
public class SiglusRequisitionExtensionServiceTest {

  private static final String FACILITY_CODE = "01031708";

  @Captor
  private ArgumentCaptor<RequisitionExtension> requisitionExtensionArgumentCaptor;

  @InjectMocks
  private SiglusRequisitionExtensionService siglusRequisitionExtensionService;

  @Mock
  private SiglusFacilityReferenceDataService siglusFacilityReferenceDataService;

  @Mock
  private SiglusGeneratedNumberService siglusGeneratedNumberService;

  @Mock
  private RequisitionExtensionRepository requisitionExtensionRepository;

  @Mock
  private ProcessingPeriodRepository processingPeriodRepository;

  @Mock
  private SiglusAuthenticationHelper authenticationHelper;

  @Mock
  private SiglusProgramService siglusProgramService;

  private final UUID requisitionId = UUID.randomUUID();
  private final UUID facilityId = UUID.randomUUID();
  private final UUID programId = UUID.randomUUID();
  private final int year = 2022;
  private final boolean emergency = false;
  private final ProcessingPeriod period = new ProcessingPeriod();
  private final SiglusRequisitionDto siglusRequisitionDto = new SiglusRequisitionDto();

  @Before
  public void prepare() {
    period.setEndDate(LocalDate.of(2022, 10, 22));
    siglusRequisitionDto.setId(requisitionId);
    siglusRequisitionDto.setEmergency(false);
    siglusRequisitionDto.setFacility(new ObjectReferenceDto(facilityId));
    siglusRequisitionDto.setProgram(new ObjectReferenceDto(programId));
    when(processingPeriodRepository.findOneById(siglusRequisitionDto.getProcessingPeriodId())).thenReturn(period);
    when(authenticationHelper.getCurrentUser()).thenReturn(any(UserDto.class));
  }

  @Test
  public void shouldSaveRequisitionExtensionWhenCreateVia() {
    // given
    FacilityDto facilityDto = FacilityDto.builder().code(FACILITY_CODE).build();
    when(siglusFacilityReferenceDataService.findOne(facilityId)).thenReturn(facilityDto);
    when(siglusGeneratedNumberService.getGeneratedNumber(facilityId, programId, year, emergency)).thenReturn(3);
    ProgramDto programDto = new ProgramDto();
    programDto.setCode(VIA_PROGRAM_CODE);
    when(siglusProgramService.getProgram(programId)).thenReturn(programDto);

    // when
    siglusRequisitionExtensionService.createRequisitionExtension(siglusRequisitionDto);

    // then
    verify(requisitionExtensionRepository).save(requisitionExtensionArgumentCaptor.capture());
    RequisitionExtension requisitionExtension = requisitionExtensionArgumentCaptor.getValue();
    assertEquals(requisitionId, requisitionExtension.getRequisitionId());
    assertEquals("RNO.01031708.2210.", requisitionExtension.getRequisitionNumberPrefix());
    assertEquals(3, requisitionExtension.getRequisitionNumber().intValue());
  }

  @Test
  public void shouldSaveRequisitionExtensionWhenCreateEmergencyVia() {
    // given
    FacilityDto facilityDto = FacilityDto.builder().code(FACILITY_CODE).build();
    when(siglusFacilityReferenceDataService.findOne(facilityId)).thenReturn(facilityDto);
    when(siglusGeneratedNumberService.getGeneratedNumber(facilityId, programId, year, true)).thenReturn(3);
    siglusRequisitionDto.setEmergency(true);
    ProgramDto programDto = new ProgramDto();
    programDto.setCode(VIA_PROGRAM_CODE);
    when(siglusProgramService.getProgram(programId)).thenReturn(programDto);

    // when
    siglusRequisitionExtensionService.createRequisitionExtension(siglusRequisitionDto);

    // then
    verify(requisitionExtensionRepository).save(requisitionExtensionArgumentCaptor.capture());
    RequisitionExtension requisitionExtension = requisitionExtensionArgumentCaptor.getValue();
    assertEquals(requisitionId, requisitionExtension.getRequisitionId());
    assertEquals("REM.01031708.2210.", requisitionExtension.getRequisitionNumberPrefix());
    assertEquals(3, requisitionExtension.getRequisitionNumber().intValue());
  }

  @Test
  public void shouldSaveRequisitionExtensionWhenCreateMmia() {
    // given
    FacilityDto facilityDto = FacilityDto.builder().code(FACILITY_CODE).build();
    when(siglusFacilityReferenceDataService.findOne(facilityId)).thenReturn(facilityDto);
    when(siglusGeneratedNumberService.getGeneratedNumber(facilityId, programId, year, emergency)).thenReturn(3);
    ProgramDto programDto = new ProgramDto();
    programDto.setCode(TARV_PROGRAM_CODE);
    when(siglusProgramService.getProgram(programId)).thenReturn(programDto);

    // when
    siglusRequisitionExtensionService.createRequisitionExtension(siglusRequisitionDto);

    // then
    verify(requisitionExtensionRepository).save(requisitionExtensionArgumentCaptor.capture());
    RequisitionExtension requisitionExtension = requisitionExtensionArgumentCaptor.getValue();
    assertEquals(requisitionId, requisitionExtension.getRequisitionId());
    assertEquals("MIA.01031708.2210.", requisitionExtension.getRequisitionNumberPrefix());
    assertEquals(3, requisitionExtension.getRequisitionNumber().intValue());
  }

  @Test
  public void shouldSaveRequisitionExtensionWhenCreateRapidTest() {
    // given
    FacilityDto facilityDto = FacilityDto.builder().code(FACILITY_CODE).build();
    when(siglusFacilityReferenceDataService.findOne(facilityId)).thenReturn(facilityDto);
    when(siglusGeneratedNumberService.getGeneratedNumber(facilityId, programId, year, emergency)).thenReturn(3);
    ProgramDto programDto = new ProgramDto();
    programDto.setCode(RAPIDTEST_PROGRAM_CODE);
    when(siglusProgramService.getProgram(programId)).thenReturn(programDto);

    // when
    siglusRequisitionExtensionService.createRequisitionExtension(siglusRequisitionDto);

    // then
    verify(requisitionExtensionRepository).save(requisitionExtensionArgumentCaptor.capture());
    RequisitionExtension requisitionExtension = requisitionExtensionArgumentCaptor.getValue();
    assertEquals(requisitionId, requisitionExtension.getRequisitionId());
    assertEquals("MIT.01031708.2210.", requisitionExtension.getRequisitionNumberPrefix());
    assertEquals(3, requisitionExtension.getRequisitionNumber().intValue());
  }

  @Test
  public void shouldSaveRequisitionExtensionWhenCreateMtb() {
    // given
    FacilityDto facilityDto = FacilityDto.builder().code(FACILITY_CODE).build();
    when(siglusFacilityReferenceDataService.findOne(facilityId)).thenReturn(facilityDto);
    when(siglusGeneratedNumberService.getGeneratedNumber(facilityId, programId, year, emergency)).thenReturn(3);
    ProgramDto programDto = new ProgramDto();
    programDto.setCode(MTB_PROGRAM_CODE);
    when(siglusProgramService.getProgram(programId)).thenReturn(programDto);

    // when
    siglusRequisitionExtensionService.createRequisitionExtension(siglusRequisitionDto);

    // then
    verify(requisitionExtensionRepository).save(requisitionExtensionArgumentCaptor.capture());
    RequisitionExtension requisitionExtension = requisitionExtensionArgumentCaptor.getValue();
    assertEquals(requisitionId, requisitionExtension.getRequisitionId());
    assertEquals("MTB.01031708.2210.", requisitionExtension.getRequisitionNumberPrefix());
    assertEquals(3, requisitionExtension.getRequisitionNumber().intValue());
  }

  @Test
  public void shouldSaveRequisitionExtensionWhenCreateMalaria() {
    // given
    FacilityDto facilityDto = FacilityDto.builder().code(FACILITY_CODE).build();
    when(siglusFacilityReferenceDataService.findOne(facilityId)).thenReturn(facilityDto);
    when(siglusGeneratedNumberService.getGeneratedNumber(facilityId, programId, year, emergency)).thenReturn(3);
    ProgramDto programDto = new ProgramDto();
    programDto.setCode(MALARIA_PROGRAM_CODE);
    when(siglusProgramService.getProgram(programId)).thenReturn(programDto);

    // when
    siglusRequisitionExtensionService.createRequisitionExtension(siglusRequisitionDto);

    // then
    verify(requisitionExtensionRepository).save(requisitionExtensionArgumentCaptor.capture());
    RequisitionExtension requisitionExtension = requisitionExtensionArgumentCaptor.getValue();
    assertEquals(requisitionId, requisitionExtension.getRequisitionId());
    assertEquals("ALS.01031708.2210.", requisitionExtension.getRequisitionNumberPrefix());
    assertEquals(3, requisitionExtension.getRequisitionNumber().intValue());
  }

  @Test
  public void shouldReturnFormatRequisitionNumber() {
    // given
    RequisitionExtension requisitionExtension = RequisitionExtension.builder()
        .requisitionNumberPrefix("RNO.01031708.22-10.")
        .requisitionNumber(4)
        .build();
    when(requisitionExtensionRepository.findByRequisitionId(requisitionId)).thenReturn(requisitionExtension);

    // when
    String formatRequisitionNumber = siglusRequisitionExtensionService.formatRequisitionNumber(requisitionId);

    // then
    assertEquals("RNO.01031708.22-10.04", formatRequisitionNumber);
  }

  @Test
  public void shouldReturnNullIfRequisitionExtensionNotExist() {
    // given
    when(requisitionExtensionRepository.findByRequisitionId(requisitionId))
        .thenReturn(null);

    // when
    String formatRequisitionNumber = siglusRequisitionExtensionService
        .formatRequisitionNumber(requisitionId);

    // then
    assertNull(formatRequisitionNumber);
  }

  @Test
  public void shouldDeleteRequisitionExtension() {
    // given
    RequisitionExtension requisitionExtension = new RequisitionExtension();
    when(requisitionExtensionRepository.findByRequisitionId(requisitionId))
        .thenReturn(requisitionExtension);

    // when
    siglusRequisitionExtensionService.deleteRequisitionExtension(requisitionId);

    // then
    verify(requisitionExtensionRepository).delete(requisitionExtension);
  }

  @Test
  public void shouldNotDeleteRequisitionExtensionIfRequisitionExtensionNotExist() {
    // given
    when(requisitionExtensionRepository.findByRequisitionId(requisitionId))
        .thenReturn(null);

    // when
    siglusRequisitionExtensionService.deleteRequisitionExtension(requisitionId);

    // then
    verify(requisitionExtensionRepository, times(0)).delete(any(RequisitionExtension.class));
  }
}
