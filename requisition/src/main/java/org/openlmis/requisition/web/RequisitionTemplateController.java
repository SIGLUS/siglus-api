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

package org.openlmis.requisition.web;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.openlmis.requisition.domain.AvailableRequisitionColumn;
import org.openlmis.requisition.domain.RequisitionTemplate;
import org.openlmis.requisition.dto.RequisitionTemplateDto;
import org.openlmis.requisition.exception.BindingResultException;
import org.openlmis.requisition.exception.ContentNotFoundMessageException;
import org.openlmis.requisition.exception.ValidationMessageException;
import org.openlmis.requisition.i18n.MessageKeys;
import org.openlmis.requisition.repository.AvailableRequisitionColumnRepository;
import org.openlmis.requisition.repository.RequisitionRepository;
import org.openlmis.requisition.repository.RequisitionTemplateRepository;
import org.openlmis.requisition.service.PermissionService;
import org.openlmis.requisition.utils.Message;
import org.openlmis.requisition.validate.RequisitionTemplateDtoValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
@Transactional
public class RequisitionTemplateController extends BaseController {

  private static final Logger LOGGER = LoggerFactory.getLogger(RequisitionTemplateController.class);

  @Autowired
  private RequisitionRepository requisitionRepository;

  @Autowired
  private RequisitionTemplateRepository requisitionTemplateRepository;

  @Autowired
  private RequisitionTemplateDtoValidator validator;

  @Autowired
  private PermissionService permissionService;

  @Autowired
  private RequisitionTemplateDtoBuilder dtoBuilder;

  @Autowired
  private AvailableRequisitionColumnRepository availableRequisitionColumnRepository;

  /**
   * Allows creating a new Requisition Template.
   * If the id is specified, it will be ignored.
   *
   * @param requisitionTemplateDto A requisitionTemplateDto bound to the request body.
   * @param bindingResult          Object used for validation.
   * @return created requisitionTemplate.
   */
  @RequestMapping(value = "/requisitionTemplates", method = RequestMethod.POST)
  @ResponseStatus(HttpStatus.CREATED)
  @ResponseBody
  public RequisitionTemplateDto createRequisitionTemplate(
      @RequestBody RequisitionTemplateDto requisitionTemplateDto, BindingResult bindingResult) {
    permissionService.canManageRequisitionTemplate().throwExceptionIfHasErrors();

    validator.validate(requisitionTemplateDto, bindingResult);

    RequisitionTemplate requisitionTemplate =
        RequisitionTemplate.newInstance(requisitionTemplateDto, findColumnNamesWithTagRequired());

    if (bindingResult.hasErrors()) {
      throw new BindingResultException(getErrors(bindingResult));
    }

    LOGGER.debug("Creating new requisitionTemplate");
    requisitionTemplate.setId(null);
    RequisitionTemplate newRequisitionTemplate =
        requisitionTemplateRepository.save(requisitionTemplate);
    LOGGER.debug("Created new requisitionTemplate with id: " + requisitionTemplate.getId());
    return dtoBuilder.newInstance(newRequisitionTemplate);
  }

  /**
   * Get all requisitionTemplates.
   *
   * @return RequisitionTemplates.
   */
  @RequestMapping(value = "/requisitionTemplates", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public Iterable<RequisitionTemplateDto> getCurrentRequisitionTemplates() {
    permissionService.canManageRequisitionTemplate().throwExceptionIfHasErrors();
    return dtoBuilder.newInstance(requisitionTemplateRepository.getActiveTemplates());
  }

  /**
   * Allows updating requisitionTemplates.
   *
   * @param requisitionTemplateId  UUID of requisitionTemplate which we want to update
   * @param requisitionTemplateDto A requisitionTemplateDto bound to the request body
   * @param bindingResult          Object used for validation.
   * @return updated requisitionTemplate.
   */
  @RequestMapping(value = "/requisitionTemplates/{id}", method = RequestMethod.PUT)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public RequisitionTemplateDto updateRequisitionTemplate(
      @PathVariable("id") UUID requisitionTemplateId,
      @RequestBody RequisitionTemplateDto requisitionTemplateDto,
      BindingResult bindingResult) {
    permissionService.canManageRequisitionTemplate().throwExceptionIfHasErrors();

    validator.validate(requisitionTemplateDto, bindingResult);

    if (bindingResult.hasErrors()) {
      throw new BindingResultException(getErrors(bindingResult));
    }

    RequisitionTemplate template = RequisitionTemplate.newInstance(requisitionTemplateDto,
        findColumnNamesWithTagRequired());
    RequisitionTemplate toUpdate = requisitionTemplateRepository.findOne(requisitionTemplateId);
    RequisitionTemplate toSave;

    if (toUpdate == null) {
      LOGGER.info("Creating new requisition template");
      toSave = template;
      toSave.setId(null);
    } else if (!requisitionRepository.findByTemplateId(toUpdate.getId()).isEmpty()) {
      LOGGER.info("Archiving requisition template {}", toUpdate.getId());
      toUpdate.archive();
      requisitionTemplateRepository.saveAndFlush(toUpdate);

      LOGGER.info("Creating new requisition template");
      toSave = template;
      toSave.setId(null);
    } else {
      LOGGER.debug("Updating requisition template {}", requisitionTemplateId);
      toSave = toUpdate;
      toSave.updateFrom(template);
    }

    toSave = requisitionTemplateRepository.save(toSave);

    LOGGER.debug("Saved requisitionTemplate with id: " + toSave.getId());

    return dtoBuilder.newInstance(toSave);
  }

  /**
   * Get chosen requisitionTemplate.
   *
   * @param requisitionTemplateId UUID of requisitionTemplate which we want to get
   * @return RequisitionTemplate.
   */
  @RequestMapping(value = "/requisitionTemplates/{id}", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public RequisitionTemplateDto getRequisitionTemplate(
      @PathVariable("id") UUID requisitionTemplateId) {
    permissionService.canManageRequisitionTemplate().throwExceptionIfHasErrors();

    RequisitionTemplate requisitionTemplate =
        requisitionTemplateRepository.findOne(requisitionTemplateId);
    if (requisitionTemplate == null) {
      throw new ContentNotFoundMessageException(new Message(
          MessageKeys.ERROR_REQUISITION_TEMPLATE_NOT_FOUND_FOR_ID, requisitionTemplateId));
    } else {
      return dtoBuilder.newInstance(requisitionTemplate);
    }
  }

  /**
   * Allows deleting requisitionTemplate.
   *
   * @param requisitionTemplateId UUID of requisitionTemplate which we want to delete
   */
  @RequestMapping(value = "/requisitionTemplates/{id}", method = RequestMethod.DELETE)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteRequisitionTemplate(@PathVariable("id") UUID requisitionTemplateId) {
    permissionService.canManageRequisitionTemplate().throwExceptionIfHasErrors();
    RequisitionTemplate template = requisitionTemplateRepository
        .findOne(requisitionTemplateId);

    if (template == null) {
      throw new ContentNotFoundMessageException(new Message(
          MessageKeys.ERROR_REQUISITION_TEMPLATE_NOT_FOUND_FOR_ID, requisitionTemplateId));
    }

    if (!requisitionRepository.findByTemplateId(template.getId()).isEmpty()) {
      throw new ValidationMessageException(new Message(
          MessageKeys.ERROR_REQUISITION_TEMPLATE_IN_USE,
          template.getId()));
    }

    requisitionTemplateRepository.delete(template);
  }

  private List<String> findColumnNamesWithTagRequired() {
    List<AvailableRequisitionColumn> availableRequisitionColumns =
        availableRequisitionColumnRepository.findBySupportsTag(true);

    return availableRequisitionColumns.stream()
        .map(AvailableRequisitionColumn::getName)
        .collect(Collectors.toList());
  }

}
