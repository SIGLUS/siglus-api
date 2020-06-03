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
import static net.sf.jasperreports.engine.export.JRHtmlExporterParameter.IS_USING_IMAGES_TO_ALIGN;
import static org.apache.commons.io.FileUtils.writeByteArrayToFile;
import static org.openlmis.fulfillment.i18n.MessageKeys.CLASS_NOT_FOUND;
import static org.openlmis.fulfillment.i18n.MessageKeys.ERROR_IO;
import static org.openlmis.fulfillment.i18n.MessageKeys.ERROR_JASPER_FILE_CREATION;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.domain.Template;
import org.openlmis.fulfillment.util.AuthenticationHelper;
import org.openlmis.fulfillment.web.util.OrderReportDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.jasperreports.JasperReportsMultiFormatView;

@Service
public class JasperReportsViewService {

  @Autowired
  private DataSource replicationDataSource;

  @Autowired
  private ExporterBuilder exporter;

  @Autowired
  private AuthenticationHelper authenticationHelper;

  /**
   * Create Jasper Report View.
   * Create Jasper Report (".jasper" file) from bytes from Template entity.
   * Set 'Jasper' exporter parameters, data source, web application context, url to file.
   * @param template template that will be used to create a view
   * @param request it is used to take web application context
   * @return created jasper view.
   */
  public JasperReportsMultiFormatView getJasperReportsView(Template template,
                                                           HttpServletRequest request) {
    JasperReportsMultiFormatView jasperView = new JasperReportsMultiFormatView();
    setExportParams(jasperView);
    jasperView.setJdbcDataSource(replicationDataSource);
    jasperView.setUrl(getReportUrlForReportData(template));
    if (getApplicationContext(request) != null) {
      jasperView.setApplicationContext(getApplicationContext(request));
    }
    return jasperView;
  }

  /**
   * Get customized Jasper Report View for Order Report.
   *
   * @param jasperView generic jasper report view
   * @param parameters template parameters populated with values from the request
   * @param order the reporting order
   * @return customized jasper view.
   */
  public ModelAndView getOrderJasperReportView(JasperReportsMultiFormatView jasperView,
                                               Map<String, Object> parameters, Order order) {
    OrderReportDto orderDto = OrderReportDto.newInstance(order, exporter);
    parameters.put("datasource", new JRBeanCollectionDataSource(orderDto.getOrderLineItems()));
    parameters.put("order", orderDto);

    String userName = "";
    if (authenticationHelper != null) {
      userName = authenticationHelper.getCurrentUser().printName();
    }
    parameters.put("loggedInUser", userName);

    return new ModelAndView(jasperView, parameters);
  }

  /**
   * Set export parameters in jasper view.
   */
  private void setExportParams(JasperReportsMultiFormatView jasperView) {
    Map<JRExporterParameter, Object> reportFormatMap = new HashMap<>();
    reportFormatMap.put(IS_USING_IMAGES_TO_ALIGN, false);
    jasperView.setExporterParameters(reportFormatMap);
  }

  /**
   * Get application context from servlet.
   */
  public WebApplicationContext getApplicationContext(HttpServletRequest servletRequest) {
    ServletContext servletContext = servletRequest.getSession().getServletContext();
    return WebApplicationContextUtils.getWebApplicationContext(servletContext);
  }

  /**
   * Create ".jasper" file with byte array from Template.
   *
   * @return Url to ".jasper" file.
   */
  private String getReportUrlForReportData(Template template) {
    File tmpFile;

    try {
      tmpFile = createTempFile(template.getName() + "_temp", ".jasper");
    } catch (IOException exp) {
      throw new JasperReportViewException(
          exp, ERROR_JASPER_FILE_CREATION
      );
    }

    try (ObjectInputStream inputStream =
             new ObjectInputStream(new ByteArrayInputStream(template.getData()))) {
      JasperReport jasperReport = (JasperReport) inputStream.readObject();

      try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
           ObjectOutputStream out = new ObjectOutputStream(bos)) {

        out.writeObject(jasperReport);
        writeByteArrayToFile(tmpFile, bos.toByteArray());

        return tmpFile.toURI().toURL().toString();
      }
    } catch (IOException exp) {
      throw new JasperReportViewException(exp, ERROR_IO, exp.getMessage());
    } catch (ClassNotFoundException exp) {
      throw new JasperReportViewException(exp, CLASS_NOT_FOUND, JasperReport.class.getName());
    }
  }
}
