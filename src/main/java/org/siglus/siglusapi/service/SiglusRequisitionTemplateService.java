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

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.openlmis.requisition.dto.RequisitionTemplateDto;
import org.siglus.common.domain.RequisitionTemplateAssociateProgram;
import org.siglus.common.domain.RequisitionTemplateExtension;
import org.siglus.common.dto.RequisitionTemplateExtensionDto;
import org.siglus.common.repository.RequisitionTemplateAssociateProgramRepository;
import org.siglus.common.repository.RequisitionTemplateExtensionRepository;
import org.siglus.siglusapi.domain.AvailableUsageColumn;
import org.siglus.siglusapi.domain.AvailableUsageColumnSection;
import org.siglus.siglusapi.domain.UsageCategory;
import org.siglus.siglusapi.domain.UsageTemplateColumnSection;
import org.siglus.siglusapi.dto.SiglusRequisitionTemplateDto;
import org.siglus.siglusapi.dto.UsageTemplateSectionDto;
import org.siglus.siglusapi.repository.AvailableUsageColumnRepository;
import org.siglus.siglusapi.repository.AvailableUsageColumnSectionRepository;
import org.siglus.siglusapi.repository.UsageTemplateColumnSectionRepository;
import org.siglus.siglusapi.service.client.RequisitionTemplateRequisitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class SiglusRequisitionTemplateService {

  @Autowired
  private RequisitionTemplateRequisitionService requisitionTemplateRequisitionService;

  @Autowired
  private RequisitionTemplateExtensionRepository requisitionTemplateExtensionRepository;

  @Autowired
  private RequisitionTemplateAssociateProgramRepository associateProgramExtensionRepository;

  @Autowired
  AvailableUsageColumnSectionRepository availableUsageColumnSectionRepository;

  @Autowired
  AvailableUsageColumnRepository availableUsageColumnRepository;

  @Autowired
  UsageTemplateColumnSectionRepository columnSectionRepository;


  public SiglusRequisitionTemplateDto getTemplate(UUID id) {
    SiglusRequisitionTemplateDto templateDto = SiglusRequisitionTemplateDto.from(
        getTemplateByOpenLmis(id));
    log.info("find requisition template extension: {}", id);
    RequisitionTemplateExtension extension = requisitionTemplateExtensionRepository
        .findByRequisitionTemplateId(id);
    templateDto.setExtension(RequisitionTemplateExtensionDto.from(extension));
    templateDto.setAssociateProgramsIds(getAssociateProgram(id));
    log.info("find requisition template usage column Section: {}", id);
    List<UsageTemplateColumnSection> usageTemplateColumns = columnSectionRepository
        .findByRequisitionTemplateId(id);
    return setUsageTemplateDto(templateDto, usageTemplateColumns);

  }

  @Transactional
  public SiglusRequisitionTemplateDto createTemplateExtension(RequisitionTemplateDto updatedDto,
      SiglusRequisitionTemplateDto requestDto) {
    SiglusRequisitionTemplateDto templateExtension = updateTemplateExtension(updatedDto,
        requestDto);
    templateExtension = updateTemplateAsscociatedProgram(templateExtension, requestDto);
    List<UsageTemplateColumnSection> usageTemplateColumns = createUsageTemplateColumn(updatedDto);
    return setUsageTemplateDto(templateExtension, usageTemplateColumns);
  }

  @Transactional
  public SiglusRequisitionTemplateDto updateTemplate(RequisitionTemplateDto updatedDto,
      SiglusRequisitionTemplateDto requestDto) {
    SiglusRequisitionTemplateDto templateExtension = updateTemplateExtension(updatedDto,
        requestDto);
    templateExtension = updateTemplateAsscociatedProgram(templateExtension, requestDto);
    return updateUsageTemplateDto(templateExtension, requestDto);
  }

  private SiglusRequisitionTemplateDto updateTemplateExtension(
      RequisitionTemplateDto updatedDto, SiglusRequisitionTemplateDto requestDto) {
    SiglusRequisitionTemplateDto newDto = SiglusRequisitionTemplateDto.from(updatedDto);
    RequisitionTemplateExtensionDto extension = requestDto.getExtension();
    if (extension == null) {
      return newDto;
    }
    log.info("create or update requisition template extension: {}", extension);
    RequisitionTemplateExtension templateExtension = saveTemplateExtension(
        updatedDto.getId(), extension);
    newDto.setExtension(RequisitionTemplateExtensionDto.from(templateExtension));
    return newDto;
  }

  private SiglusRequisitionTemplateDto updateTemplateAsscociatedProgram(
      RequisitionTemplateDto updatedDto, SiglusRequisitionTemplateDto requestDto) {
    SiglusRequisitionTemplateDto newDto = SiglusRequisitionTemplateDto.from(updatedDto);
    Set<UUID> uuids = requestDto.getAssociateProgramsIds();
    log.info("save requisition template asscociated programs: {}", uuids);
    List<RequisitionTemplateAssociateProgram> associatePrograms =
        associateProgramExtensionRepository.findByRequisitionTemplateId(updatedDto.getId());
    Set<UUID> associateProgramIds = associatePrograms.stream()
        .map(RequisitionTemplateAssociateProgram::getAssociatedProgramId)
        .collect(Collectors.toSet());
    if (!associateProgramIds.equals(uuids)) {
      log.info("delete old requisition template asscociated programss: {}", associatePrograms);
      associateProgramExtensionRepository.delete(associatePrograms);
      log.info("create new requisition template asscociated programss: {}", uuids);
      associateProgramExtensionRepository.save(
          RequisitionTemplateAssociateProgram.from(updatedDto.getId(), uuids));
    }
    newDto.setAssociateProgramsIds(uuids);
    return newDto;
  }

  private Set<UUID> getAssociateProgram(UUID templateId) {
    List<RequisitionTemplateAssociateProgram> associatePrograms =
        associateProgramExtensionRepository.findByRequisitionTemplateId(templateId);
    return associatePrograms.stream()
        .map(RequisitionTemplateAssociateProgram::getAssociatedProgramId)
        .collect(Collectors.toSet());
  }

  private RequisitionTemplateDto getTemplateByOpenLmis(UUID id) {
    // call origin OpenLMIS API
    return requisitionTemplateRequisitionService.findTemplate(id);
  }

  private RequisitionTemplateExtension saveTemplateExtension(UUID templateId,
      RequisitionTemplateExtensionDto extensionDto) {
    if (extensionDto == null) {
      return null;
    }
    // archive old requisition template and create new requisition template
    if (!templateId.equals(extensionDto.getRequisitionTemplateId())) {
      extensionDto.setId(null);
    }

    log.info("save requisition template extsion column section: {}", extensionDto);
    return requisitionTemplateExtensionRepository.save(
        RequisitionTemplateExtension.from(templateId, extensionDto));
  }

  private List<UsageTemplateColumnSection> createUsageTemplateColumn(RequisitionTemplateDto
      updatedDto) {
    List<AvailableUsageColumnSection> usageSections = availableUsageColumnSectionRepository
        .findAll();
    List<UsageTemplateColumnSection> updatedTemplateColumns = new ArrayList<>();
    for (AvailableUsageColumnSection section : usageSections) {
      UsageTemplateColumnSection columnSection = UsageTemplateColumnSection
          .from(section, updatedDto.getId());
      columnSection.setRequisitionTemplateId(updatedDto.getId());
      updatedTemplateColumns.add(columnSection);
    }
    log.info("save requisition template usage column section: {}", updatedTemplateColumns);
    return columnSectionRepository.save(updatedTemplateColumns);
  }

  public Map<UsageCategory, List<UsageTemplateSectionDto>> getUsageTempateDto(
      List<UsageTemplateColumnSection> templateColumnSections) {
    Map<UsageCategory, List<UsageTemplateColumnSection>> usageCategoryMap = getUsageCategoryListMap(
        templateColumnSections);
    EnumMap<UsageCategory, List<UsageTemplateSectionDto>> usageCategoryListMap = new EnumMap<>(
        UsageCategory.class);
    for (Map.Entry<UsageCategory, List<UsageTemplateColumnSection>>
        categoryMapEntry : usageCategoryMap.entrySet()) {
      List<UsageTemplateSectionDto> dtos = categoryMapEntry.getValue().stream()
          .map(UsageTemplateSectionDto::from)
          .collect(Collectors.toList());
      usageCategoryListMap.put(categoryMapEntry.getKey(), dtos);
    }
    return usageCategoryListMap;
  }

  public Map<UsageCategory, List<UsageTemplateColumnSection>> getUsageCategoryListMap(
      List<UsageTemplateColumnSection> templateColumnSections) {
    return templateColumnSections.stream().collect(
        Collectors.groupingBy(UsageTemplateColumnSection::getCategory));
  }

  public List<UsageTemplateSectionDto> getCategoryDto(
      Map<UsageCategory, List<UsageTemplateSectionDto>> categoryListMap, UsageCategory category) {
    if (!categoryListMap.containsKey(category)) {
      return new ArrayList<>();
    }
    return categoryListMap.get(category);
  }

  private SiglusRequisitionTemplateDto updateUsageTemplateDto(
      SiglusRequisitionTemplateDto updatedDto, SiglusRequisitionTemplateDto requestDto) {
    EnumMap<UsageCategory, List<UsageTemplateSectionDto>> allUsageTemplateCategoryDto =
        getUsageCategoryListEnumMap(requestDto);

    List<AvailableUsageColumn> availableUsageColumns = availableUsageColumnRepository.findAll();
    List<AvailableUsageColumnSection> availableUsageColumnSection =
        availableUsageColumnSectionRepository.findAll();
    List<UsageTemplateColumnSection> updatedColumnSections = new ArrayList<>();
    Map<UsageCategory, List<UsageTemplateColumnSection>> usageCategoryMap =
        getUsageCategoryListMap(
            columnSectionRepository.findByRequisitionTemplateId(requestDto.getId()));
    for (Map.Entry<UsageCategory, List<UsageTemplateSectionDto>> categoryListEntry :
        allUsageTemplateCategoryDto.entrySet()) {
      UsageCategory category = categoryListEntry.getKey();
      List<UsageTemplateSectionDto> categoryDto = categoryListEntry.getValue();
      if (categoryDto != null && !categoryDto.isEmpty()) {
        List<UsageTemplateColumnSection> columnSections = categoryDto.stream()
            .map(sectionDto -> UsageTemplateColumnSection.from(sectionDto, category,
                updatedDto.getId(), availableUsageColumnSection, availableUsageColumns))
            .collect(Collectors.toList());
        if (!requestDto.getId().equals(updatedDto.getId())) {
          columnSections = columnSections.stream()
              .map(UsageTemplateColumnSection::getNewTemplateSection)
              .collect(Collectors.toList());
        } else if (usageCategoryMap.get(category) != null) {
          List<UsageTemplateColumnSection> sections = usageCategoryMap.get(category);
          log.info("delete requisition template usage column section: {}", sections);
          columnSectionRepository.delete(sections);
        }
        log.info("save requisition template usage column section: {}", columnSections);
        updatedColumnSections.addAll(columnSectionRepository.save(columnSections));
      }
    }

    return setUsageTemplateDto(updatedDto, updatedColumnSections);
  }

  private EnumMap<UsageCategory, List<UsageTemplateSectionDto>> getUsageCategoryListEnumMap(
      SiglusRequisitionTemplateDto requestDto) {
    EnumMap<UsageCategory, List<UsageTemplateSectionDto>> allUsageTemplateCategoryDto =
        new EnumMap<>(UsageCategory.class);
    allUsageTemplateCategoryDto.put(UsageCategory.KITUSAGE, requestDto.getKitUsage());
    allUsageTemplateCategoryDto.put(UsageCategory.PATIENT, requestDto.getPatient());
    allUsageTemplateCategoryDto.put(UsageCategory.REGIMEN, requestDto.getRegimen());
    allUsageTemplateCategoryDto.put(UsageCategory.CONSULTATIONNUMBER,
        requestDto.getConsultationNumber());
    allUsageTemplateCategoryDto.put(UsageCategory.RAPIDTESTCONSUMPTION,
        requestDto.getTestConsumption());
    allUsageTemplateCategoryDto.put(UsageCategory.USAGEINFORMATION,
        requestDto.getUsageInformation());
    return allUsageTemplateCategoryDto;
  }


  private SiglusRequisitionTemplateDto setUsageTemplateDto(SiglusRequisitionTemplateDto templateDto,
      List<UsageTemplateColumnSection> columnSections) {
    Map<UsageCategory, List<UsageTemplateSectionDto>> categoryListMap =
        getUsageTempateDto(columnSections);
    templateDto.setKitUsage(getCategoryDto(categoryListMap, UsageCategory.KITUSAGE));
    templateDto.setPatient(getCategoryDto(categoryListMap, UsageCategory.PATIENT));
    templateDto.setRegimen(getCategoryDto(categoryListMap, UsageCategory.REGIMEN));
    templateDto
        .setConsultationNumber(getCategoryDto(categoryListMap, UsageCategory.CONSULTATIONNUMBER));
    templateDto.setTestConsumption(
        getCategoryDto(categoryListMap, UsageCategory.RAPIDTESTCONSUMPTION));
    templateDto
        .setUsageInformation(getCategoryDto(categoryListMap, UsageCategory.USAGEINFORMATION));
    return templateDto;
  }

}
