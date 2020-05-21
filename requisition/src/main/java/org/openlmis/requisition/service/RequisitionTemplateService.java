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

package org.openlmis.requisition.service;

import static org.openlmis.requisition.i18n.MessageKeys.ERROR_REQUISITION_TEMPLATE_NOT_DEFINED;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_REQUISITION_TEMPLATE_NOT_FOUND;

import java.util.UUID;
import org.openlmis.requisition.domain.RequisitionTemplate;
import org.openlmis.requisition.exception.ContentNotFoundMessageException;
import org.openlmis.requisition.exception.ValidationMessageException;
import org.openlmis.requisition.repository.RequisitionTemplateRepository;
import org.openlmis.requisition.utils.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RequisitionTemplateService {

  @Autowired
  private RequisitionTemplateRepository requisitionTemplateRepository;

  /**
   * Find template for the given program and facility type.
   */
  public RequisitionTemplate findTemplate(UUID programId, UUID facilityTypeId, boolean reportOnly) {
    RequisitionTemplate template = requisitionTemplateRepository
        .findTemplate(programId, facilityTypeId);

    if (null == template) {
      throw new ContentNotFoundMessageException(new Message(ERROR_REQUISITION_TEMPLATE_NOT_FOUND));
    }

    if (!template.hasColumnsDefined()) {
      throw new ValidationMessageException(new Message(ERROR_REQUISITION_TEMPLATE_NOT_DEFINED));
    }
    
    if (reportOnly) {
      RequisitionTemplate templateCopy = new RequisitionTemplate(template);
      templateCopy.hideOrderRelatedColumns();
      return templateCopy;
    } else {
      return template;
    }
  }

}
