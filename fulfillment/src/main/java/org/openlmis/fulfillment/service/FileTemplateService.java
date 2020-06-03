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

package org.openlmis.fulfillment.service;

import org.openlmis.fulfillment.domain.FileTemplate;
import org.openlmis.fulfillment.domain.TemplateType;
import org.openlmis.fulfillment.repository.FileTemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FileTemplateService {

  @Autowired
  private FileTemplateRepository fileTemplateRepository;

  /**
   * Get order file FileTemplate.
   *
   * @return FileTemplate.
   */
  public FileTemplate getOrderFileTemplate() {
    return fileTemplateRepository.findFirstByTemplateType(TemplateType.ORDER);
  }

  /**
   * Get File Template Object by template type.
   *
   * @return FileTemplate.
   */
  public FileTemplate getFileTemplate(TemplateType templateType) {
    return fileTemplateRepository.findFirstByTemplateType(templateType);
  }



}
