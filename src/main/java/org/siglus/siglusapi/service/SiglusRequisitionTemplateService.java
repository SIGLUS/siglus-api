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

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.openlmis.requisition.dto.RequisitionTemplateDto;
import org.siglus.siglusapi.domain.RequisitionTemplateAssociateProgram;
import org.siglus.siglusapi.domain.RequisitionTemplateExtension;
import org.siglus.siglusapi.dto.RequisitionTemplateExtensionDto;
import org.siglus.siglusapi.dto.SiglusRequisitionTemplateDto;
import org.siglus.siglusapi.repository.RequisitionTemplateAssociateProgramRepository;
import org.siglus.siglusapi.repository.RequisitionTemplateExtensionRepository;
import org.siglus.siglusapi.service.client.RequisitionTemplateRequisitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SiglusRequisitionTemplateService {

  @Autowired
  private RequisitionTemplateRequisitionService requisitionTemplateRequisitionService;

  @Autowired
  private RequisitionTemplateExtensionRepository requisitionTemplateExtensionRepository;

  @Autowired
  private RequisitionTemplateAssociateProgramRepository associateProgramExtensionRepository;

  public SiglusRequisitionTemplateDto getTemplate(UUID id) {
    SiglusRequisitionTemplateDto templateDto = SiglusRequisitionTemplateDto.from(
        getTemplateByOpenLmis(id));
    RequisitionTemplateExtension extension = requisitionTemplateExtensionRepository
        .findByRequisitionTemplateId(id);
    templateDto.setExtension(RequisitionTemplateExtensionDto.from(extension));
    templateDto.setAssociateProgramsIds(getAssociateProgram(id));
    return templateDto;
  }

  public SiglusRequisitionTemplateDto updateTemplate(RequisitionTemplateDto updatedDto,
      SiglusRequisitionTemplateDto requestDto) {
    SiglusRequisitionTemplateDto newDto = updateTemplateExtension(updatedDto, requestDto);
    return updateTemplateAsscociatedProgram(newDto, requestDto);
  }

  public SiglusRequisitionTemplateDto updateTemplateExtension(
      RequisitionTemplateDto updatedDto, SiglusRequisitionTemplateDto requestDto) {
    SiglusRequisitionTemplateDto newDto = SiglusRequisitionTemplateDto.from(updatedDto);
    RequisitionTemplateExtensionDto extension = requestDto.getExtension();
    if (extension == null) {
      return newDto;
    }
    log.info("create or update requisition template extension: {}",  extension);
    RequisitionTemplateExtension templateExtension = saveTemplateExtension(
        updatedDto.getId(), extension);
    newDto.setExtension(RequisitionTemplateExtensionDto.from(templateExtension));
    return newDto;
  }

  public SiglusRequisitionTemplateDto updateTemplateAsscociatedProgram(
      RequisitionTemplateDto updatedDto, SiglusRequisitionTemplateDto requestDto) {
    SiglusRequisitionTemplateDto newDto = SiglusRequisitionTemplateDto.from(updatedDto);
    Set<UUID> uuids = requestDto.getAssociateProgramsIds();
    if (uuids.isEmpty()) {
      return newDto;
    }
    log.info("save requisition template asscociated programs: {}",  uuids);
    List<RequisitionTemplateAssociateProgram> associatePrograms =
        associateProgramExtensionRepository.findByRequisitionTemplateId(updatedDto.getId());
    Set<UUID> associateProgramIds = associatePrograms.stream()
        .map(RequisitionTemplateAssociateProgram::getAssociatedProgramId)
        .collect(Collectors.toSet());
    if (!associateProgramIds.equals(uuids)) {
      log.info("delete old requisition template asscociated programss: {}",  associatePrograms);
      associateProgramExtensionRepository.delete(associatePrograms);
      log.info("create new requisition template asscociated programss: {}",  uuids);
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
    return requisitionTemplateExtensionRepository.save(
        RequisitionTemplateExtension.from(templateId, extensionDto));
  }

}
