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

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.openlmis.requisition.dto.RequisitionTemplateDto;
import org.siglus.siglusapi.domain.RequisitionTemplateExtension;
import org.siglus.siglusapi.dto.RequisitionTemplateExtensionDto;
import org.siglus.siglusapi.dto.SiglusRequisitionTemplateDto;
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

  public SiglusRequisitionTemplateDto getTemplate(UUID id) {
    SiglusRequisitionTemplateDto template = getTemplateByOpenLmis(id);
    RequisitionTemplateExtension extension = requisitionTemplateExtensionRepository
        .findByRequisitionTemplateId(id);
    template.setExtension(RequisitionTemplateExtensionDto.from(extension));
    return template;
  }

  public RequisitionTemplateDto updateTemplateExtension(SiglusRequisitionTemplateDto updatedDto,
      RequisitionTemplateExtensionDto extensionDto) {
    log.info("create or update requisition template extension: {}",  extensionDto);
    RequisitionTemplateExtension templateExtension = saveTemplateExtension(updatedDto.getId(),
        extensionDto);
    updatedDto.setExtension(RequisitionTemplateExtensionDto.from(templateExtension));
    return updatedDto;
  }

  private SiglusRequisitionTemplateDto getTemplateByOpenLmis(UUID id) {
    return (SiglusRequisitionTemplateDto) requisitionTemplateRequisitionService.findTemplate(id);
  }

  private RequisitionTemplateExtension saveTemplateExtension(UUID id,
      RequisitionTemplateExtensionDto extensionDto) {
    if (extensionDto == null) {
      return null;
    }
    RequisitionTemplateExtension tobeSave = RequisitionTemplateExtension.from(id, extensionDto);
    return requisitionTemplateExtensionRepository.save(tobeSave);
  }

}
