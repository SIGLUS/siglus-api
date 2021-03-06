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

package org.siglus.siglusapi.web;

import java.util.UUID;
import org.openlmis.requisition.dto.RequisitionTemplateDto;
import org.openlmis.requisition.web.RequisitionTemplateController;
import org.siglus.siglusapi.dto.SiglusRequisitionTemplateDto;
import org.siglus.siglusapi.service.SiglusRequisitionTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/siglusapi/requisitionTemplates")
public class SiglusRequisitionTemplateController {

  @Autowired
  private SiglusRequisitionTemplateService siglusRequisitionTemplateService;

  @Autowired
  private RequisitionTemplateController requisitionTemplateController;

  @GetMapping("/{id}")
  public SiglusRequisitionTemplateDto searchRequisitionTemplate(@PathVariable("id") UUID id) {
    return siglusRequisitionTemplateService.getTemplate(id);
  }

  @PutMapping("/{id}")
  @Transactional
  public SiglusRequisitionTemplateDto updateRequisitionTemplate(
      @PathVariable("id") UUID requisitionTemplateId,
      @RequestBody SiglusRequisitionTemplateDto requisitionTemplateDto,
      BindingResult bindingResult) {
    // call modified OpenLMIS API
    RequisitionTemplateDto updatedDto = requisitionTemplateController
        .updateRequisitionTemplate(requisitionTemplateId, requisitionTemplateDto, bindingResult);
    return siglusRequisitionTemplateService.updateTemplate(updatedDto, requisitionTemplateDto);
  }

  @PostMapping
  @Transactional
  public SiglusRequisitionTemplateDto createRequisitionTemplate(
      @RequestBody SiglusRequisitionTemplateDto requisitionTemplateDto,
      BindingResult bindingResult) {
    // call modified OpenLMIS API
    RequisitionTemplateDto updatedDto = requisitionTemplateController
        .createRequisitionTemplate(requisitionTemplateDto, bindingResult);
    return siglusRequisitionTemplateService.createTemplateExtension(updatedDto,
        requisitionTemplateDto);
  }
}
