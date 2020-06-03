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

package org.openlmis.fulfillment.web;

import static org.openlmis.fulfillment.i18n.MessageKeys.ERROR_ORDER_FILE_TEMPLATE_CREATION;

import javax.validation.Valid;
import org.openlmis.fulfillment.domain.FileTemplate;
import org.openlmis.fulfillment.domain.TemplateType;
import org.openlmis.fulfillment.repository.FileTemplateRepository;
import org.openlmis.fulfillment.service.FileTemplateService;
import org.openlmis.fulfillment.service.PermissionService;
import org.openlmis.fulfillment.web.util.FileTemplateDto;
import org.openlmis.fulfillment.web.validator.FileTemplateValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@Transactional
public class FileTemplateController extends BaseController {

  private static final Logger LOGGER = LoggerFactory.getLogger(FileTemplateController.class);

  @Autowired
  private FileTemplateValidator validator;
  @Autowired
  private FileTemplateRepository fileTemplateRepository;
  @Autowired
  private FileTemplateService fileTemplateService;
  @Autowired
  private PermissionService permissionService;

  @InitBinder
  protected void initBinder(WebDataBinder binder) {
    binder.setValidator(this.validator);
  }

  /**
   * Allows updating order file templates.
   *
   * @param fileTemplateDto An order file template bound to the request body
   * @return ResponseEntity containing saved fileTemplate
   */
  @RequestMapping(value = "/fileTemplates", method = RequestMethod.PUT)
  @ResponseBody
  public FileTemplateDto saveCsvFileTemplate(
          @RequestBody @Valid FileTemplateDto fileTemplateDto, BindingResult bindingResult) {
    LOGGER.debug("Checking right to update order file template");
    permissionService.canManageSystemSettings();

    if (bindingResult.hasErrors()) {
      throw new ValidationException(bindingResult.getAllErrors().get(0).getDefaultMessage());
    }

    FileTemplate template = fileTemplateService.getFileTemplate(fileTemplateDto
        .getTemplateType());
    if (!template.getId().equals(fileTemplateDto.getId())) {
      throw new ValidationException(ERROR_ORDER_FILE_TEMPLATE_CREATION);
    }

    LOGGER.debug("Saving CSV File Template");
    template.importDto(fileTemplateDto);
    template = fileTemplateRepository.save(template);

    LOGGER.debug("Saved CSV File Template with id: " + template.getId());
    return FileTemplateDto.newInstance(template);
  }

  /**
   * Get fileTemplate.
   *
   * @return FileTemplate.
   */
  @RequestMapping(value = "/fileTemplates", method = RequestMethod.GET)
  public ResponseEntity<FileTemplateDto> getCsvFileTemplate(
      @RequestParam(name = "templateType", required = false, defaultValue = "ORDER")
          TemplateType templateType) {

    LOGGER.debug("Checking right to view order file template");
    permissionService.canManageSystemSettings();

    FileTemplate fileTemplate = fileTemplateService.getFileTemplate(templateType);
    if (fileTemplate == null) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    } else {
      return new ResponseEntity<>(FileTemplateDto.newInstance(fileTemplate),
          HttpStatus.OK);
    }
  }
}
