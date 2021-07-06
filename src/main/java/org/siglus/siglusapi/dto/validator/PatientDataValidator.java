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

package org.siglus.siglusapi.dto.validator;

import java.util.Collection;
import lombok.extern.slf4j.Slf4j;
import org.siglus.common.domain.RequisitionTemplateExtension;
import org.siglus.common.repository.RequisitionTemplateExtensionRepository;
import org.siglus.siglusapi.domain.UsageCategory;
import org.siglus.siglusapi.dto.PatientColumnDto;
import org.siglus.siglusapi.dto.PatientGroupDto;
import org.siglus.siglusapi.dto.SiglusRequisitionDto;
import org.siglus.siglusapi.dto.constraint.PatientDataConstraint;
import org.siglus.siglusapi.repository.UsageTemplateColumnSectionRepository;
import org.siglus.siglusapi.service.client.SiglusRequisitionRequisitionService;
import org.springframework.stereotype.Component;

/**
 * Validate patientLineItems in {@linkplain SiglusRequisitionDto#getPatientLineItems()
 * SiglusRequisitionDto}.
 * <p>
 * Check following:
 * <ul>
 *   <li>No missing or extra groups and columns</li>
 *   <li>Total column equals the sum of the rest column values
 *   if the source of the total column is calculated</li>
 *   <li>No value overflow</li>
 * </ul>
 * </p>
 */
@Component
@Slf4j
public class PatientDataValidator extends
    UsageLineItemValidator<PatientDataConstraint, PatientGroupDto, PatientColumnDto> {

  public PatientDataValidator(
      RequisitionTemplateExtensionRepository templateExtensionRepo,
      SiglusRequisitionRequisitionService requisitionService,
      UsageTemplateColumnSectionRepository columnSectionRepo) {
    super(templateExtensionRepo, requisitionService, columnSectionRepo, UsageCategory.PATIENT,
        "patientLineItems");
  }

  @Override
  protected boolean checkEnable(RequisitionTemplateExtension templateExtension) {
    return templateExtension.getEnablePatientLineItem();
  }

  @Override
  protected Collection<PatientGroupDto> getUploadedGroups(SiglusRequisitionDto uploadedValue) {
    return uploadedValue.getPatientLineItems();
  }

  @Override
  protected Integer mapColumnToInt(PatientColumnDto column) {
    return column.getValue();
  }

}