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
import org.siglus.siglusapi.dto.SiglusRequisitionTemplateDto;
import org.siglus.siglusapi.service.SiglusRequisitionTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/siglusintegration/requisitionTemplates")
public class SiglusRequisitionTemplateController {

  //  private static final Logger LOGGER = LoggerFactory.getLogger(
  //      SiglusRequisitionTemplateController.class);
  //  private static final String MESSAGE_SEPARATOR = ":";
  //  private static final String PARAMETER_SEPARATOR = ",";

  @Autowired
  private SiglusRequisitionTemplateService siglusRequisitionTemplateService;

  @GetMapping("/{id}")
  public SiglusRequisitionTemplateDto searchRequisitionTemplate(@PathVariable("id") UUID id) {
    return siglusRequisitionTemplateService.getTemplate(id);
  }

  //  @PutMapping("/{id}")
  //  public RequisitionTemplateDto updateRequisitionTemplate(
  //      @PathVariable("id") UUID requisitionTemplateId,
  //      @RequestBody RequisitionTemplateDto requisitionTemplateDto,
  //      BindingResult bindingResult) {
  //    permissionService.canManageRequisitionTemplate().throwExceptionIfHasErrors();
  //
  //    validator.validate(requisitionTemplateDto, bindingResult);
  //
  //    if (bindingResult.hasErrors()) {
  //      throw new BindingResultException(getErrors(bindingResult));
  //    }
  //
  //    RequisitionTemplate template = RequisitionTemplate.newInstance(requisitionTemplateDto,
  //        findColumnNamesWithTagRequired());
  //    RequisitionTemplate toUpdate = requisitionTemplateRepository.findOne(requisitionTemplateId);
  //    RequisitionTemplate toSave;
  //
  //    if (toUpdate == null) {
  //      LOGGER.info("Creating new requisition template");
  //      toSave = template;
  //    } else if (!requisitionRepository.findByTemplateId(toUpdate.getId()).isEmpty()) {
  //      LOGGER.info("Archiving requisition template {}", toUpdate.getId());
  //      toUpdate.archive();
  //      requisitionTemplateRepository.saveAndFlush(toUpdate);
  //
  //      LOGGER.info("Creating new requisition template");
  //      toSave = template;
  //      toSave.setId(null);
  //    } else {
  //      LOGGER.debug("Updating requisition template {}", requisitionTemplateId);
  //      toSave = toUpdate;
  //      toSave.updateFrom(template);
  //    }
  //
  //    toSave = requisitionTemplateRepository.save(toSave);
  //
  //    LOGGER.debug("Saved requisitionTemplate with id: {}", toSave.getId());
  //
  //    // [SIGLUS change start]
  //    // [change reason]: need enhance requisition template to support 7 modules.
  //    RequisitionTemplateDto updatedDto = dtoBuilder.newInstance(toSave);
  //    LOGGER.debug("create or update requisition template extension: {}",
  //        requisitionTemplateDto.getExtension());
  //    return siglusRequisitionTemplateService.updateTemplateExtension(updatedDto,
  //        requisitionTemplateDto.getExtension());
  //    // [SIGLUS change end]
  //  }
  //
  //  @PostMapping
  //  public RequisitionTemplateDto createRequisitionTemplate(
  //      @RequestBody RequisitionTemplateDto requisitionTemplateDto, BindingResult bindingResult) {
  //    permissionService.canManageRequisitionTemplate().throwExceptionIfHasErrors();
  //
  //    validator.validate(requisitionTemplateDto, bindingResult);
  //
  //    RequisitionTemplate requisitionTemplate =
  //    RequisitionTemplate.newInstance(requisitionTemplateDto, findColumnNamesWithTagRequired());
  //
  //    if (bindingResult.hasErrors()) {
  //      throw new BindingResultException(getErrors(bindingResult));
  //    }
  //
  //    LOGGER.debug("Creating new requisitionTemplate");
  //    requisitionTemplate.setId(null);
  //    RequisitionTemplate newRequisitionTemplate =
  //        requisitionTemplateRepository.save(requisitionTemplate);
  //    return dtoBuilder.newInstance(newRequisitionTemplate);
  //  }
  //
  //  private List<String> findColumnNamesWithTagRequired() {
  //    List<AvailableRequisitionColumn> availableRequisitionColumns =
  //        availableRequisitionColumnRepository.findBySupportsTag(true);
  //
  //    return availableRequisitionColumns.stream()
  //        .map(AvailableRequisitionColumn::getName)
  //        .collect(Collectors.toList());
  //  }
  //
  //  private Map<String, Message> getErrors(BindingResult bindingResult) {
  //    Map<String, Message> errors = new HashMap<>();
  //
  //    for (FieldError error : bindingResult.getFieldErrors()) {
  //      String[] parts = error.getCode().split(MESSAGE_SEPARATOR);
  //      String messageKey = parts[0];
  //      String[] parameters = parts[1].split(PARAMETER_SEPARATOR);
  //      errors.put(error.getField(), new Message(messageKey.trim(),
  //          Arrays.stream(parameters).map(String::trim).toArray()));
  //    }
  //
  //    return errors;
  //  }
}
