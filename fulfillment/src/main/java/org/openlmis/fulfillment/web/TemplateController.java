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

import java.util.UUID;
import org.apache.log4j.Logger;
import org.openlmis.fulfillment.domain.Template;
import org.openlmis.fulfillment.repository.TemplateRepository;
import org.openlmis.fulfillment.service.TemplateService;
import org.openlmis.fulfillment.web.util.TemplateDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartFile;

@Controller
@Transactional
@RequestMapping("/api/reports/templates/fulfillment")
public class TemplateController extends BaseController {

  private static final Logger LOGGER = Logger.getLogger(TemplateController.class);

  private static final String CONSISTENCY_REPORT = "Consistency Report";

  @Autowired
  private TemplateService templateService;

  @Autowired
  private TemplateRepository templateRepository;

  /**
   * Adding report templates with ".jrxml" format to database.
   *
   * @param file        File in ".jrxml" format to upload
   * @param name        Name of file in database
   * @param description Description of the file
   */
  @RequestMapping(method = RequestMethod.POST)
  @ResponseStatus(HttpStatus.OK)
  public void createJasperReportTemplate(@RequestPart("file") MultipartFile file,
                                         String name, String description) {
    Template template = new Template(name, null, null, CONSISTENCY_REPORT, description);
    templateService.validateFileAndInsertTemplate(template, file);
  }

  /**
   * Get all templates.
   *
   * @return Templates.
   */
  @RequestMapping(method = RequestMethod.GET)
  @ResponseBody
  public ResponseEntity<Iterable<TemplateDto>> getAllTemplates() {
    Iterable<TemplateDto> templates = TemplateDto.newInstance(templateRepository.findAll());
    return new ResponseEntity<>(templates, HttpStatus.OK);
  }

  /**
   * Allows updating templates.
   *
   * @param templateDto   A template bound to the request body
   * @param templateId UUID of template which we want to update
   * @return ResponseEntity containing the updated template
   */
  @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
  public ResponseEntity<TemplateDto> updateTemplate(@RequestBody TemplateDto templateDto,
                                          @PathVariable("id") UUID templateId) {
    Template template = Template.newInstance(templateDto);
    Template templateToUpdate = templateRepository.findOne(templateId);
    if (templateToUpdate == null) {
      templateToUpdate = new Template();
      LOGGER.info("Creating new template");
    } else {
      LOGGER.debug("Updating template with id: " + templateId);
    }

    templateToUpdate.updateFrom(template);
    templateToUpdate = templateRepository.save(templateToUpdate);

    LOGGER.debug("Saved template with id: " + templateToUpdate.getId());
    return new ResponseEntity<>(TemplateDto.newInstance(templateToUpdate), HttpStatus.OK);
  }

  /**
   * Get chosen template.
   *
   * @param templateId UUID of template which we want to get
   * @return Template.
   */
  @RequestMapping(value = "/{id}", method = RequestMethod.GET)
  public ResponseEntity<TemplateDto> getTemplate(@PathVariable("id") UUID templateId) {
    Template template = templateRepository.findOne(templateId);
    if (template == null) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    } else {
      return new ResponseEntity<>(TemplateDto.newInstance(template), HttpStatus.OK);
    }
  }

  /**
   * Allows deleting template.
   *
   * @param templateId UUID of template which we want to delete
   * @return ResponseEntity containing the HTTP Status
   */
  @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
  public ResponseEntity<TemplateDto> deleteTemplate(@PathVariable("id")
                                              UUID templateId) {
    Template template = templateRepository.findOne(templateId);
    if (template == null) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    } else {
      templateRepository.delete(template);
      return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
  }
}
