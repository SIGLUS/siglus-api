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

package org.siglus.siglusapi.dto.validation.validator;

import java.util.Collection;
import lombok.extern.slf4j.Slf4j;
import org.siglus.common.domain.RequisitionTemplateExtension;
import org.siglus.common.repository.RequisitionTemplateExtensionRepository;
import org.siglus.siglusapi.domain.UsageCategory;
import org.siglus.siglusapi.dto.ConsultationNumberColumnDto;
import org.siglus.siglusapi.dto.ConsultationNumberGroupDto;
import org.siglus.siglusapi.dto.SiglusRequisitionDto;
import org.siglus.siglusapi.dto.validation.constraint.ConsultationNumberConstraint;
import org.siglus.siglusapi.repository.UsageTemplateColumnSectionRepository;
import org.siglus.siglusapi.service.client.SiglusRequisitionRequisitionService;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ConsultationNumberValidator extends UsageLineItemValidator
    <ConsultationNumberConstraint, ConsultationNumberGroupDto, ConsultationNumberColumnDto> {

  public ConsultationNumberValidator(
      RequisitionTemplateExtensionRepository templateExtensionRepo,
      SiglusRequisitionRequisitionService requisitionService,
      UsageTemplateColumnSectionRepository columnSectionRepo) {
    super(templateExtensionRepo, requisitionService, columnSectionRepo,
        UsageCategory.CONSULTATIONNUMBER, "consultationNumberLineItem");
  }


  @Override
  protected boolean checkEnable(RequisitionTemplateExtension templateExtension) {
    return templateExtension.getEnableConsultationNumber();
  }

  @Override
  protected Collection<ConsultationNumberGroupDto> getUploadedGroups(
      SiglusRequisitionDto uploadedValue) {
    return uploadedValue.getConsultationNumberLineItems();
  }

  @Override
  protected int mapColumnToInt(ConsultationNumberColumnDto column) {
    return column.getValue();
  }
}