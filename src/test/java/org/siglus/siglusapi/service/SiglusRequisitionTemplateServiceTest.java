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
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.javers.common.collections.Sets;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openlmis.requisition.dto.RequisitionTemplateDto;
import org.siglus.common.domain.RequisitionTemplateAssociateProgram;
import org.siglus.common.domain.RequisitionTemplateExtension;
import org.siglus.common.dto.RequisitionTemplateExtensionDto;
import org.siglus.common.repository.RequisitionTemplateAssociateProgramRepository;
import org.siglus.common.repository.RequisitionTemplateExtensionRepository;
import org.siglus.siglusapi.dto.SiglusRequisitionTemplateDto;
import org.siglus.siglusapi.service.client.RequisitionTemplateRequisitionService;

@SuppressWarnings("PMD.TooManyMethods")
public class SiglusRequisitionTemplateServiceTest {

  @Mock
  private RequisitionTemplateRequisitionService requisitionTemplateRequisitionService;

  @Mock
  private RequisitionTemplateExtensionRepository requisitionTemplateExtensionRepository;

  @Mock
  private RequisitionTemplateAssociateProgramRepository associateProgramExtensionRepository;

  @Mock
  private RequisitionTemplateAssociateProgram associateProgram1;

  @Mock
  private RequisitionTemplateAssociateProgram associateProgram2;

  @InjectMocks
  private SiglusRequisitionTemplateService siglusRequisitionTemplateService;

  private UUID tempalteId;

  private UUID tempalteExtensionId;

  private UUID programId1 = UUID.randomUUID();

  private UUID programId2 = UUID.randomUUID();

  @Before
  public void prepare() {
    MockitoAnnotations.initMocks(this);
    tempalteId = UUID.randomUUID();
    tempalteExtensionId = UUID.randomUUID();
    programId1 = UUID.randomUUID();
    programId2 = UUID.randomUUID();
  }

  @Test
  public void shouldReturnExtensionAndAssociateProgramsWhenGetTemplate() {
    RequisitionTemplateDto requisitionTemplateDto = new RequisitionTemplateDto();
    requisitionTemplateDto.setId(tempalteId);
    SiglusRequisitionTemplateDto siglusRequisitionTemplateDto = new SiglusRequisitionTemplateDto();
    Set<UUID> associateProgramIds = Sets.asSet(associateProgram1.getId(),
        associateProgram2.getId());
    siglusRequisitionTemplateDto.setId(tempalteId);
    siglusRequisitionTemplateDto.setAssociateProgramsIds(associateProgramIds);
    RequisitionTemplateExtension extension = prepareExtension();
    List<RequisitionTemplateAssociateProgram> associatePrograms = Arrays.asList(associateProgram1,
        associateProgram2);
    when(requisitionTemplateRequisitionService.findTemplate(tempalteId)).thenReturn(
        requisitionTemplateDto);
    when(requisitionTemplateExtensionRepository.findByRequisitionTemplateId(tempalteId)).thenReturn(
        extension);
    when(associateProgramExtensionRepository.findByRequisitionTemplateId(tempalteId)).thenReturn(
        associatePrograms);

    SiglusRequisitionTemplateDto result = siglusRequisitionTemplateService.getTemplate(tempalteId);

    assertEquals(prepareExtensionDto(), result.getExtension());
    assertEquals(associateProgramIds, result.getAssociateProgramsIds());
  }

  @Test
  public void shouldNotUpdateIfExtensionAndAssociatedProgramsIsEmptyWhenUpdateTemplate() {
    RequisitionTemplateDto updatedDto = new RequisitionTemplateDto();
    updatedDto.setId(tempalteId);
    SiglusRequisitionTemplateDto requestDto = new SiglusRequisitionTemplateDto();
    requestDto.setId(tempalteId);
    requestDto.setExtension(null);
    requestDto.setAssociateProgramsIds(Collections.emptySet());

    SiglusRequisitionTemplateDto result = siglusRequisitionTemplateService
        .updateTemplate(updatedDto, requestDto);

    assertNull(result.getExtension());
    assertEquals(Collections.emptySet(), result.getAssociateProgramsIds());
  }

  @Test
  public void shouldUpdateExtensionIfExtensionIsNotEmptyWhenUpdateTemplate() {
    RequisitionTemplateDto updatedDto = new RequisitionTemplateDto();
    updatedDto.setId(tempalteId);
    SiglusRequisitionTemplateDto requestDto = new SiglusRequisitionTemplateDto();
    RequisitionTemplateExtension extension = prepareExtension();
    RequisitionTemplateExtensionDto extensionDto = prepareExtensionDto();
    requestDto.setExtension(extensionDto);
    when(requisitionTemplateExtensionRepository.save(any(RequisitionTemplateExtension.class)))
        .thenReturn(extension);

    SiglusRequisitionTemplateDto result = siglusRequisitionTemplateService
        .updateTemplate(updatedDto, requestDto);

    assertEquals(extensionDto, result.getExtension());
  }

  @Test
  public void shouldNotDeleteAndCreateAssociateProgramsIfNotChangedWhenUpdateTemplate() {
    RequisitionTemplateDto updatedDto = new RequisitionTemplateDto();
    updatedDto.setId(tempalteId);
    SiglusRequisitionTemplateDto requestDto = new SiglusRequisitionTemplateDto();
    Set<UUID> uuids = prepareAssociatedProgramIds();
    requestDto.setAssociateProgramsIds(uuids);
    List<RequisitionTemplateAssociateProgram> associatePrograms = prepareAssociatePrograms();
    when(associateProgramExtensionRepository.findByRequisitionTemplateId(tempalteId)).thenReturn(
        associatePrograms);

    SiglusRequisitionTemplateDto result = siglusRequisitionTemplateService
        .updateTemplate(updatedDto, requestDto);

    verify(associateProgramExtensionRepository, never()).delete(associatePrograms);
    verify(associateProgramExtensionRepository, never()).save(
        anyListOf(RequisitionTemplateAssociateProgram.class));
    assertEquals(uuids, result.getAssociateProgramsIds());
  }

  @Test
  public void shouldDeleteAndCreateAssociateProgramsIfChangedWhenUpdateTemplate() {
    RequisitionTemplateDto updatedDto = new RequisitionTemplateDto();
    updatedDto.setId(tempalteId);
    SiglusRequisitionTemplateDto requestDto = new SiglusRequisitionTemplateDto();
    Set<UUID> uuids = prepareUpdatedAssociatedProgramIds();
    requestDto.setAssociateProgramsIds(uuids);
    List<RequisitionTemplateAssociateProgram> associatePrograms = prepareAssociatePrograms();
    when(associateProgramExtensionRepository.findByRequisitionTemplateId(tempalteId)).thenReturn(
        associatePrograms);

    SiglusRequisitionTemplateDto result = siglusRequisitionTemplateService
        .updateTemplate(updatedDto, requestDto);

    verify(associateProgramExtensionRepository).delete(associatePrograms);
    verify(associateProgramExtensionRepository).save(
        anyListOf(RequisitionTemplateAssociateProgram.class));
    assertEquals(uuids, result.getAssociateProgramsIds());
  }

  private RequisitionTemplateExtension prepareExtension() {
    RequisitionTemplateExtension requisitionTemplateExtension =
        RequisitionTemplateExtension.builder()
            .requisitionTemplateId(tempalteId)
            .enableConsultationNumber(true)
            .enableKitUsage(true)
            .enablePatientLineItem(false)
            .enableProduct(false)
            .enableRapidTestConsumption(false)
            .enableRegimen(false)
            .enableUsageInformation(false)
            .build();
    requisitionTemplateExtension.setId(tempalteExtensionId);
    return requisitionTemplateExtension;
  }

  private RequisitionTemplateExtensionDto prepareExtensionDto() {
    RequisitionTemplateExtensionDto requisitionTemplateExtensionDto =
        RequisitionTemplateExtensionDto.builder()
            .requisitionTemplateId(tempalteId)
            .enableConsultationNumber(true)
            .enableKitUsage(true)
            .enablePatientLineItem(false)
            .enableProduct(false)
            .enableRapidTestConsumption(false)
            .enableRegimen(false)
            .enableUsageInformation(false)
            .build();
    requisitionTemplateExtensionDto.setId(tempalteExtensionId);
    return requisitionTemplateExtensionDto;
  }

  private List<RequisitionTemplateAssociateProgram> prepareAssociatePrograms() {
    RequisitionTemplateAssociateProgram associateProgram1 = new RequisitionTemplateAssociateProgram(
        tempalteId, programId1);
    RequisitionTemplateAssociateProgram associateProgram2 = new RequisitionTemplateAssociateProgram(
        tempalteId, programId2);
    return Arrays.asList(associateProgram1, associateProgram2);
  }

  private Set<UUID> prepareAssociatedProgramIds() {
    return Sets.asSet(programId1, programId2);
  }

  private Set<UUID> prepareUpdatedAssociatedProgramIds() {
    return Sets.asSet(programId1, UUID.randomUUID());
  }

}