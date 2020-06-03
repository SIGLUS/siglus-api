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

import static java.io.File.createTempFile;
import static org.apache.commons.io.FileUtils.writeByteArrayToFile;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.openlmis.fulfillment.i18n.MessageKeys.ERROR_IO;
import static org.openlmis.fulfillment.i18n.MessageKeys.ERROR_REPORTING_CREATION;
import static org.openlmis.fulfillment.i18n.MessageKeys.REPORTING_EXTRA_PROPERTIES;
import static org.openlmis.fulfillment.i18n.MessageKeys.REPORTING_FILE_EMPTY;
import static org.openlmis.fulfillment.i18n.MessageKeys.REPORTING_FILE_INCORRECT_TYPE;
import static org.openlmis.fulfillment.i18n.MessageKeys.REPORTING_FILE_INVALID;
import static org.openlmis.fulfillment.i18n.MessageKeys.REPORTING_FILE_MISSING;
import static org.openlmis.fulfillment.i18n.MessageKeys.REPORTING_PARAMETER_INCORRECT_TYPE;
import static org.openlmis.fulfillment.i18n.MessageKeys.REPORTING_PARAMETER_MISSING;
import static org.openlmis.fulfillment.i18n.MessageKeys.REPORTING_TEMPLATE_EXISTS;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;
import org.apache.log4j.Logger;
import org.openlmis.fulfillment.domain.Template;
import org.openlmis.fulfillment.domain.TemplateParameter;
import org.openlmis.fulfillment.repository.TemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class TemplateService {

  private static final Logger LOGGER = Logger.getLogger(TemplateService.class);

  @Autowired
  private TemplateRepository templateRepository;

  public Template getByName(String name) {
    return templateRepository.findByName(name);
  }

  /**
   * Validate ".jrmxl" file and insert this template to database.
   */
  public void validateFileAndInsertTemplate(Template template, MultipartFile file) {
    if (templateRepository.findByName(template.getName()) != null) {
      throw new ReportingException(REPORTING_TEMPLATE_EXISTS);
    }
    validateFileAndCreateParameters(template, file);
    saveWithParameters(template);
  }

  /**
   * Validate ".jrmxl" file and insert if template not exist.
   * If this name of template already exist, remove older template and insert new.
   */
  public void validateFileAndSaveTemplate(Template template, MultipartFile file) {
    Template templateTmp = templateRepository.findByName(template.getName());
    if (templateTmp != null) {
      templateRepository.removeAndFlush(templateTmp);
    }

    validateFileAndCreateParameters(template, file);
    saveWithParameters(template);
  }

  /**
   * Insert template and template parameters to database.
   */
  public void saveWithParameters(Template template) {
    templateRepository.save(template);
  }

  /**
   * Convert template from ".jasper" format in database to ".jrxml"(extension) format.
   */
  public File convertJasperToXml(Template template) {
    try (InputStream inputStream = new ByteArrayInputStream(template.getData());
         ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      JasperCompileManager.writeReportToXmlStream(inputStream, outputStream);
      File xmlReport = createTempFile(template.getName(), ".jrxml");
      writeByteArrayToFile(xmlReport, outputStream.toByteArray());
      return xmlReport;
    } catch (JRException | IOException ex) {
      throw new ReportingException(ex, ERROR_REPORTING_CREATION);
    }
  }

  private void setTemplateParameters(Template template, JRParameter[] jrParameters) {
    ArrayList<TemplateParameter> parameters = new ArrayList<>();

    for (JRParameter jrParameter : jrParameters) {
      if (!jrParameter.isSystemDefined()) {
        parameters.add(createParameter(jrParameter));
      }
    }

    template.setTemplateParameters(parameters);
  }

  /**
   * Create additional report parameters.
   * Save additional report parameters as TemplateParameter list.
   * Save report file as ".jasper" in byte array in Template class.
   * If report is not valid throw exception.
   *
   * @param template The template to insert parameters to
   * @param inputStream input stream of the file
   */
  public void createTemplateParameters(Template template, InputStream inputStream) {
    try {
      JasperReport report = JasperCompileManager.compileReport(inputStream);
      JRParameter[] jrParameters = report.getParameters();

      if (jrParameters != null && jrParameters.length > 0) {
        setTemplateParameters(template, jrParameters);
      }

      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      ObjectOutputStream out = new ObjectOutputStream(bos);
      out.writeObject(report);
      template.setData(bos.toByteArray());
    } catch (JRException ex) {
      throw new ReportingException(ex, REPORTING_FILE_INVALID);
    } catch (IOException ex) {
      throw new ReportingException(ex, ERROR_IO, ex.getMessage());
    }
  }

  /**
   * Validate ".jrxml" report file. If it is valid, create template parameters.
   */
  private void validateFileAndCreateParameters(Template template, MultipartFile file) {

    if (file == null) {
      throw new ReportingException(REPORTING_FILE_MISSING);
    }
    if (!file.getOriginalFilename().endsWith(".jrxml")) {
      throw new ReportingException(REPORTING_FILE_INCORRECT_TYPE);
    }
    if (file.isEmpty()) {
      throw new ReportingException(REPORTING_FILE_EMPTY);
    }

    try {
      createTemplateParameters(template, file.getInputStream());
    } catch (IOException ex) {
      throw new ReportingException(ex, ERROR_IO, ex.getMessage());
    }
  }

  /**
   * Create new report parameter of report which is not defined in Jasper system.
   */
  private TemplateParameter createParameter(JRParameter jrParameter) {
    String[] propertyNames = jrParameter.getPropertiesMap().getPropertyNames();
    //Check # of properties and that required ones are given.
    if (propertyNames.length > 2) {
      throw new ReportingException(REPORTING_EXTRA_PROPERTIES);
    }
    String displayName = jrParameter.getPropertiesMap().getProperty("displayName");
    if (isBlank(displayName)) {
      throw new ReportingException(
          REPORTING_PARAMETER_MISSING, "displayName");
    }
    //Look for sql for select and that data type is supported string.
    String dataType = jrParameter.getValueClassName();
    String selectSql = jrParameter.getPropertiesMap().getProperty("selectSql");
    //Sql selects need String data type.
    if (isNotBlank(selectSql) && !"java.lang.String".equals(dataType)) {
      throw new ReportingException(
          REPORTING_PARAMETER_INCORRECT_TYPE, "sql", "string");
    }
    //Set parameters.
    TemplateParameter templateParameter = new TemplateParameter();
    templateParameter.setName(jrParameter.getName());
    templateParameter.setDisplayName(displayName);
    templateParameter.setDescription(jrParameter.getDescription());
    templateParameter.setDataType(dataType);
    if (isNotBlank(selectSql)) {
      LOGGER.debug("SQL from report parameter: " + selectSql);
      templateParameter.setSelectSql(selectSql);
    }
    if (jrParameter.getDefaultValueExpression() != null) {
      templateParameter.setDefaultValue(jrParameter.getDefaultValueExpression()
          .getText().replace("\"", "").replace("\'", ""));
    }
    return templateParameter;
  }
}
