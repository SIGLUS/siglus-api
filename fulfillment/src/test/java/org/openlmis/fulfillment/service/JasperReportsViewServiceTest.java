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

import static net.sf.jasperreports.engine.export.JRHtmlExporterParameter.IS_USING_IMAGES_TO_ALIGN;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JasperReport;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.domain.Template;
import org.openlmis.fulfillment.web.util.OrderReportDto;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.jasperreports.JasperReportsMultiFormatView;

@SuppressWarnings({"PMD.UnusedPrivateField"})
@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(BlockJUnit4ClassRunner.class)
@PrepareForTest({JasperReportsViewService.class})
public class JasperReportsViewServiceTest {

  private static final String FORMAT = "format";
  private static final String PDF = "pdf";

  @Mock
  private DataSource dataSource;

  @Mock
  private ExporterBuilder exporterBuilder;

  @InjectMocks
  private JasperReportsViewService viewFactory;

  private Template template;
  private JasperReportsMultiFormatView jasperReportsView;
  private JasperReport jasperReport;
  private ObjectInputStream objectInputStream;
  private ObjectOutputStream objectOutputStream;
  private ByteArrayOutputStream byteArrayOutputStream;
  private byte[] reportByteData;

  @Before
  public void setUp() throws Exception {
    template = mock(Template.class);
    when(template.getName()).thenReturn("report1.jrxml");
    reportByteData = new byte[1];
    when(template.getData()).thenReturn(reportByteData);
    jasperReport = mock(JasperReport.class);

    objectInputStream = mock(ObjectInputStream.class);
    objectOutputStream = mock(ObjectOutputStream.class);
    byteArrayOutputStream = mock(ByteArrayOutputStream.class);

    ByteArrayInputStream byteArrayInputStream = mock(ByteArrayInputStream.class);
    whenNew(ByteArrayInputStream.class).withArguments(reportByteData)
        .thenReturn(byteArrayInputStream);
    whenNew(ObjectInputStream.class).withArguments(byteArrayInputStream)
        .thenReturn(objectInputStream);
    whenNew(ByteArrayOutputStream.class).withNoArguments().thenReturn(byteArrayOutputStream);
    whenNew(ObjectOutputStream.class).withArguments(byteArrayOutputStream)
        .thenReturn(objectOutputStream);
    jasperReportsView = spy(new JasperReportsMultiFormatView());
  }

  @Test
  public void shouldGetRequestedViewAndSetDataSourceAndWebContextInJasperView()
      throws Exception {
    whenNew(JasperReportsMultiFormatView.class).withNoArguments().thenReturn(jasperReportsView);
    when(objectInputStream.readObject()).thenReturn(jasperReport);
    when(byteArrayOutputStream.toByteArray()).thenReturn(reportByteData);

    ServletContext servletContext = new MockServletContext("");
    HttpServletRequest httpServletRequest = new MockHttpServletRequest(servletContext);
    JasperReportsMultiFormatView reportView = viewFactory.getJasperReportsView(
        template, httpServletRequest);

    assertThat(reportView, is(jasperReportsView));
    verify(jasperReportsView).setJdbcDataSource(dataSource);
    verify(objectOutputStream).writeObject(jasperReport);
  }

  @Test
  public void shouldAddExportParamToGetRidOfImageInHtmlReport() throws Exception {
    whenNew(JasperReportsMultiFormatView.class).withNoArguments().thenReturn(jasperReportsView);
    when(objectInputStream.readObject()).thenReturn(jasperReport);
    when(byteArrayOutputStream.toByteArray()).thenReturn(reportByteData);

    Map<JRExporterParameter, Object> exportParams = new HashMap<>();
    exportParams.put(IS_USING_IMAGES_TO_ALIGN, false);
    ServletContext servletContext = new MockServletContext("");
    HttpServletRequest httpServletRequest = new MockHttpServletRequest(servletContext);
    viewFactory.getJasperReportsView(template, httpServletRequest);

    verify(jasperReportsView).setExporterParameters(exportParams);
  }

  @Test
  public void shouldGetOrderJasperReportView() {
    JasperReportsMultiFormatView view = mock(JasperReportsMultiFormatView.class);
    Order order = mock(Order.class);

    Map<String, Object> params = new HashMap<>();
    params.put(FORMAT, PDF);

    ModelAndView modelAndView = viewFactory.getOrderJasperReportView(view, params, order);
    Map<String, Object> paramsReturned = modelAndView.getModel();

    assertEquals(PDF, paramsReturned.get(FORMAT));
    assertNotNull(paramsReturned.get("datasource"));
    assertEquals(OrderReportDto.class, paramsReturned.get("order").getClass());
  }
}
