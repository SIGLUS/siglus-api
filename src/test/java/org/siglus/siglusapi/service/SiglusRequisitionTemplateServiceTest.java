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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.javers.common.collections.Sets;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.requisition.dto.RequisitionTemplateDto;
import org.siglus.common.domain.RequisitionTemplateAssociateProgram;
import org.siglus.common.domain.RequisitionTemplateExtension;
import org.siglus.common.dto.RequisitionTemplateExtensionDto;
import org.siglus.common.repository.RequisitionTemplateAssociateProgramRepository;
import org.siglus.common.repository.RequisitionTemplateExtensionRepository;
import org.siglus.siglusapi.domain.AvailableUsageColumn;
import org.siglus.siglusapi.domain.AvailableUsageColumnSection;
import org.siglus.siglusapi.domain.UsageCategory;
import org.siglus.siglusapi.domain.UsageTemplateColumn;
import org.siglus.siglusapi.domain.UsageTemplateColumnSection;
import org.siglus.siglusapi.dto.AvailableUsageColumnDto;
import org.siglus.siglusapi.dto.SiglusRequisitionTemplateDto;
import org.siglus.siglusapi.dto.UsageTemplateColumnDto;
import org.siglus.siglusapi.dto.UsageTemplateSectionDto;
import org.siglus.siglusapi.repository.AvailableUsageColumnRepository;
import org.siglus.siglusapi.repository.AvailableUsageColumnSectionRepository;
import org.siglus.siglusapi.repository.UsageTemplateColumnSectionRepository;
import org.siglus.siglusapi.service.client.RequisitionTemplateRequisitionService;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("PMD.TooManyMethods")
public class SiglusRequisitionTemplateServiceTest {

  private static final String kitSectionName = "kit section Name";

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

  @Mock
  private UsageTemplateColumnSectionRepository columnSectionRepository;

  @Mock
  private AvailableUsageColumnRepository availableUsageColumnRepository;

  @Mock
  private AvailableUsageColumnSectionRepository availableUsageColumnSectionRepository;

  @InjectMocks
  private SiglusRequisitionTemplateService siglusRequisitionTemplateService;

  private UUID tempalteId;

  private UUID tempalteExtensionId;

  private UUID programId1 = UUID.randomUUID();

  private UUID programId2 = UUID.randomUUID();

  @Before
  public void prepare() {
    tempalteId = UUID.randomUUID();
    tempalteExtensionId = UUID.randomUUID();
    programId1 = UUID.randomUUID();
    programId2 = UUID.randomUUID();

    when(availableUsageColumnRepository.findAll()).thenReturn(getAvailableUsageColumns());
    when(availableUsageColumnSectionRepository.findAll()).thenReturn(getMockAvailableSection());
    when(columnSectionRepository.findByRequisitionTemplateId(any())).thenReturn(Arrays.asList());
  }

  private List<AvailableUsageColumnSection> getMockAvailableSection() {
    AvailableUsageColumnSection section = new AvailableUsageColumnSection();
    section.setName("sectionName");
    section.setId(UUID.randomUUID());
    section.setColumns(getAvailableUsageColumns());
    section.setDisplayOrder(1);
    return Arrays.asList(section);
  }

  private List<AvailableUsageColumn> getAvailableUsageColumns() {
    AvailableUsageColumn availableUsageColumn = new AvailableUsageColumn();
    availableUsageColumn.setId(UUID.randomUUID());
    availableUsageColumn.setName("available");
    availableUsageColumn.setSources("USER_INPUT|STOCK_CARDS");
    availableUsageColumn.setDisplayOrder(1);
    return Arrays.asList(availableUsageColumn);
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
    requestDto.setId(tempalteId);
    requestDto.setExtension(extensionDto);
    when(requisitionTemplateExtensionRepository.save(any(RequisitionTemplateExtension.class)))
        .thenReturn(extension);

    SiglusRequisitionTemplateDto result = siglusRequisitionTemplateService
        .updateTemplate(updatedDto, requestDto);

    assertEquals(extensionDto, result.getExtension());
  }

  @Test
  public void shouldCreateExtensionIfExtensionIsNotEmptyWhenCreateTemplate() {
    RequisitionTemplateDto updatedDto = new RequisitionTemplateDto();
    updatedDto.setId(tempalteId);
    SiglusRequisitionTemplateDto requestDto = new SiglusRequisitionTemplateDto();
    RequisitionTemplateExtension extension = prepareExtension();
    RequisitionTemplateExtensionDto extensionDto = prepareExtensionDto();
    requestDto.setId(tempalteId);
    requestDto.setExtension(extensionDto);
    when(requisitionTemplateExtensionRepository.save(any(RequisitionTemplateExtension.class)))
        .thenReturn(extension);

    SiglusRequisitionTemplateDto result = siglusRequisitionTemplateService
        .createTemplateExtension(updatedDto, requestDto);

    assertEquals(extensionDto, result.getExtension());
  }

  @Test
  public void shouldGetExtensionIfExtensionIsNotEmptyWhenGetTemplate() {
    UUID templateId = UUID.randomUUID();
    SiglusRequisitionTemplateDto templateDto = new SiglusRequisitionTemplateDto();
    templateDto.setId(tempalteId);
    when(requisitionTemplateRequisitionService.findTemplate(templateId)).thenReturn(templateDto);

    RequisitionTemplateExtension extension = prepareExtension();
    when(requisitionTemplateExtensionRepository.findByRequisitionTemplateId(templateId))
        .thenReturn(extension);

    RequisitionTemplateExtensionDto extensionDto = prepareExtensionDto();
    templateDto.setId(tempalteId);
    templateDto.setExtension(extensionDto);

    SiglusRequisitionTemplateDto result = siglusRequisitionTemplateService
        .getTemplate(templateId);

    assertEquals(extensionDto, result.getExtension());
  }

  @Test
  public void shouldGetUsageTemplateIfExtensionIsNotEmptyWhenGetTemplate() {
    UUID templateId = UUID.randomUUID();
    SiglusRequisitionTemplateDto templateDto = new SiglusRequisitionTemplateDto();
    templateDto.setId(tempalteId);
    when(requisitionTemplateRequisitionService.findTemplate(templateId)).thenReturn(templateDto);

    RequisitionTemplateExtension extension = prepareExtension();
    when(requisitionTemplateExtensionRepository.findByRequisitionTemplateId(templateId))
        .thenReturn(extension);

    RequisitionTemplateExtensionDto extensionDto = prepareExtensionDto();
    templateDto.setId(tempalteId);
    templateDto.setExtension(extensionDto);

    when(columnSectionRepository.findByRequisitionTemplateId(templateId))
        .thenReturn(getMockColumnSection());

    SiglusRequisitionTemplateDto result = siglusRequisitionTemplateService
        .getTemplate(templateId);

    assertEquals(kitSectionName, result.getKitUsage().get(0).getName());
  }

  @Test
  public void shouldUpdateNewUsageExtensionIfTemplateIdModifiedWhenUpdateTemplate() {
    UUID updatedTemplateId = UUID.randomUUID();
    RequisitionTemplateDto updatedDto = new RequisitionTemplateDto();
    updatedDto.setId(updatedTemplateId);

    SiglusRequisitionTemplateDto requestDto = new SiglusRequisitionTemplateDto();
    requestDto.setId(UUID.randomUUID());
    UsageTemplateSectionDto mockKitSectionDto = getMockKitTemplateSectionDto();
    requestDto.setKitUsage(Arrays.asList(mockKitSectionDto));

    RequisitionTemplateExtension extension = getRequisitionTemplateExtension(requestDto);
    when(requisitionTemplateExtensionRepository.save(any(RequisitionTemplateExtension.class)))
        .thenReturn(extension);

    List<AvailableUsageColumnSection> mockKitAvailableSections =
        Arrays.asList(getMockKitAvailableSection());
    when(availableUsageColumnSectionRepository.findAll()).thenReturn(mockKitAvailableSections);

    List<AvailableUsageColumn> mockKitAvailableUsageColumns =
        Arrays.asList(getMockKitAvailableUsageColumn());
    when(availableUsageColumnRepository.findAll()).thenReturn(mockKitAvailableUsageColumns);

    UsageTemplateColumnSection section = UsageTemplateColumnSection
        .from(mockKitSectionDto, UsageCategory.KITUSAGE,
            updatedDto.getId(), mockKitAvailableSections, mockKitAvailableUsageColumns);
    when(columnSectionRepository.save(Arrays.asList(any(UsageTemplateColumnSection.class))))
        .thenReturn(Arrays.asList(section));
    SiglusRequisitionTemplateDto newTemplateDto =
        siglusRequisitionTemplateService.updateTemplate(updatedDto, requestDto);
    verify(columnSectionRepository, never())
        .delete(Arrays.asList(any(UsageTemplateColumnSection.class)));
    assertEquals("request colum name", newTemplateDto.getKitUsage().get(0)
        .getColumns().get(0).getName());
  }


  @Test
  public void shouldUpdateUsageExtensionIfTemplateIdNotModifiedWhenUpdateTemplate() {
    UUID updatedTemplateId = UUID.randomUUID();
    RequisitionTemplateDto updatedDto = new RequisitionTemplateDto();
    updatedDto.setId(updatedTemplateId);

    SiglusRequisitionTemplateDto requestDto = new SiglusRequisitionTemplateDto();
    requestDto.setId(updatedTemplateId);
    UsageTemplateSectionDto mockKitSectionDto = getMockKitTemplateSectionDto();
    requestDto.setKitUsage(Arrays.asList(mockKitSectionDto));

    RequisitionTemplateExtension extension = getRequisitionTemplateExtension(requestDto);
    when(requisitionTemplateExtensionRepository.save(any(RequisitionTemplateExtension.class)))
        .thenReturn(extension);

    List<AvailableUsageColumnSection> mockKitAvailableSections =
        Arrays.asList(getMockKitAvailableSection());
    when(availableUsageColumnSectionRepository.findAll()).thenReturn(mockKitAvailableSections);

    List<AvailableUsageColumn> mockKitAvailableUsageColumns =
        Arrays.asList(getMockKitAvailableUsageColumn());
    when(availableUsageColumnRepository.findAll()).thenReturn(mockKitAvailableUsageColumns);

    UsageTemplateColumnSection section = UsageTemplateColumnSection
        .from(mockKitSectionDto, UsageCategory.KITUSAGE,
            updatedDto.getId(), mockKitAvailableSections, mockKitAvailableUsageColumns);
    when(columnSectionRepository.findByRequisitionTemplateId(requestDto.getId()))
        .thenReturn(Arrays.asList(section));
    when(columnSectionRepository.save(Arrays.asList(any(UsageTemplateColumnSection.class))))
        .thenReturn(Arrays.asList(section));
    SiglusRequisitionTemplateDto newTemplateDto =
        siglusRequisitionTemplateService.updateTemplate(updatedDto, requestDto);
    verify(columnSectionRepository).delete(Arrays.asList(any(UsageTemplateColumnSection.class)));
    assertEquals("request colum name", newTemplateDto.getKitUsage().get(0)
        .getColumns().get(0).getName());
  }

  @Test
  public void shouldNotDeleteAndCreateAssociateProgramsIfNotChangedWhenUpdateTemplate() {
    RequisitionTemplateDto updatedDto = new RequisitionTemplateDto();
    updatedDto.setId(tempalteId);
    SiglusRequisitionTemplateDto requestDto = new SiglusRequisitionTemplateDto();
    Set<UUID> uuids = prepareAssociatedProgramIds();
    requestDto.setId(tempalteId);
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
    requestDto.setId(tempalteId);
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

  private UsageTemplateSectionDto getMockKitTemplateSectionDto() {
    UsageTemplateSectionDto templateSectionDto = new UsageTemplateSectionDto();
    templateSectionDto.setName("request section name");
    templateSectionDto.setDisplayOrder(0);
    UsageTemplateColumnDto columnDto = new UsageTemplateColumnDto();
    columnDto.setName("request colum name");
    AvailableUsageColumnDto availableUsageColumnDto = new AvailableUsageColumnDto();
    availableUsageColumnDto.setSources(new ArrayList<>());
    columnDto.setColumnDefinition(availableUsageColumnDto);
    columnDto.setDisplayOrder(0);
    templateSectionDto.setColumns(Arrays.asList(columnDto));
    return templateSectionDto;
  }

  private AvailableUsageColumnSection getMockKitAvailableSection() {
    AvailableUsageColumnSection availableSection = new AvailableUsageColumnSection();
    availableSection.setName(kitSectionName);
    availableSection.setId(UUID.randomUUID());
    availableSection.setCategory(UsageCategory.KITUSAGE);
    availableSection.setDisplayOrder(1);
    return availableSection;
  }

  private AvailableUsageColumn getMockKitAvailableUsageColumn() {
    AvailableUsageColumn availableUsageColumn = new AvailableUsageColumn();
    availableUsageColumn.setId(UUID.randomUUID());
    availableUsageColumn.setName("kit available");
    availableUsageColumn.setSources("USER_INPUT|STOCK_CARDS");
    availableUsageColumn.setDisplayOrder(1);
    return availableUsageColumn;
  }

  private List<UsageTemplateColumnSection> getMockColumnSection() {
    UsageTemplateColumnSection templateColumnSection = new UsageTemplateColumnSection();
    templateColumnSection.setCategory(UsageCategory.KITUSAGE);
    templateColumnSection.setName(kitSectionName);

    AvailableUsageColumnSection availableSection = new AvailableUsageColumnSection();
    availableSection.setName(kitSectionName);
    availableSection.setId(UUID.randomUUID());
    availableSection.setColumns(getAvailableUsageColumns());
    availableSection.setDisplayOrder(1);
    templateColumnSection.setSection(availableSection);

    UsageTemplateColumn column = new UsageTemplateColumn();
    column.setId(UUID.randomUUID());
    column.setName("kitColumn");
    column.setSource("USER_INPUT");
    column.setDisplayOrder(1);
    column.setAvailableSources("USER_INPUT");
    templateColumnSection.setColumns(Arrays.asList(column));
    return Arrays.asList(templateColumnSection);
  }

  private RequisitionTemplateExtension getRequisitionTemplateExtension(
      SiglusRequisitionTemplateDto requestDto) {
    RequisitionTemplateExtension extension = prepareExtension();
    RequisitionTemplateExtensionDto extensionDto = prepareExtensionDto();
    requestDto.setExtension(extensionDto);
    return extension;
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
            .enableQuicklyFill(false)
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