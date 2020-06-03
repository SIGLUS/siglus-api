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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.openlmis.fulfillment.i18n.MessageKeys.REPORTING_EXTRA_PROPERTIES;
import static org.openlmis.fulfillment.i18n.MessageKeys.REPORTING_FILE_EMPTY;
import static org.openlmis.fulfillment.i18n.MessageKeys.REPORTING_FILE_INCORRECT_TYPE;
import static org.openlmis.fulfillment.i18n.MessageKeys.REPORTING_FILE_INVALID;
import static org.openlmis.fulfillment.i18n.MessageKeys.REPORTING_FILE_MISSING;
import static org.openlmis.fulfillment.i18n.MessageKeys.REPORTING_PARAMETER_MISSING;
import static org.openlmis.fulfillment.i18n.MessageKeys.REPORTING_TEMPLATE_EXISTS;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import net.sf.jasperreports.engine.JRExpression;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JRPropertiesMap;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.openlmis.fulfillment.domain.Template;
import org.openlmis.fulfillment.repository.TemplateRepository;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(BlockJUnit4ClassRunner.class)
@PrepareForTest({TemplateService.class, JasperCompileManager.class})
public class TemplateServiceTest {

  @Mock
  private TemplateRepository templateRepository;

  @InjectMocks
  private TemplateService templateService;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private static final String NAME_OF_FILE = "report.jrxml";
  private static final String DISPLAY_NAME = "displayName";
  private static final String PARAM_DISPLAY_NAME = "Param Display Name";

  @Test
  public void shouldThrowErrorIfFileNotOfTypeJasperXml() throws Exception {
    expectedException.expect(ReportingException.class);
    expectedException.expectMessage(REPORTING_FILE_INCORRECT_TYPE);

    MockMultipartFile file = new MockMultipartFile("report.pdf", new byte[1]);
    templateService.validateFileAndInsertTemplate(new Template(), file);
  }

  @Test
  public void shouldThrowErrorIfFileEmpty() throws Exception {
    expectedException.expect(ReportingException.class);
    expectedException.expectMessage(REPORTING_FILE_EMPTY);
    MockMultipartFile file = new MockMultipartFile(
        NAME_OF_FILE, NAME_OF_FILE, "", new byte[0]);

    templateService.validateFileAndInsertTemplate(new Template(), file);
  }

  @Test
  public void shouldThrowErrorIfFileNotPresent() throws Exception {
    expectedException.expect(ReportingException.class);
    expectedException.expectMessage(REPORTING_FILE_MISSING);

    templateService.validateFileAndInsertTemplate(new Template(), null);
  }

  @Test
  public void shouldThrowErrorIfFileIsInvalid() throws Exception {
    expectedException.expect(ReportingException.class);
    expectedException.expectMessage(REPORTING_FILE_INVALID);

    templateService.validateFileAndInsertTemplate(new Template(),
        new MockMultipartFile(NAME_OF_FILE, NAME_OF_FILE, "", new byte[1]));
  }

  @Test
  public void shouldThrowErrorIfTemplateNameAlreadyExists() throws Exception {
    Template template = new Template();
    template.setName("Name");
    expectedException.expect(ReportingException.class);
    expectedException.expectMessage(REPORTING_TEMPLATE_EXISTS);
    when(templateRepository.findByName(Matchers.anyObject())).thenReturn(template);

    templateService.validateFileAndInsertTemplate(template, null);
  }

  @Test
  public void shouldThrowErrorIfDisplayNameOfParameterIsMissing() throws Exception {
    expectedException.expect(ReportingException.class);
    expectedException.expect(hasProperty("params", arrayContaining("displayName")));
    expectedException.expectMessage(REPORTING_PARAMETER_MISSING);

    MultipartFile file = mock(MultipartFile.class);
    when(file.getOriginalFilename()).thenReturn(NAME_OF_FILE);

    mockStatic(JasperCompileManager.class);
    JasperReport report = mock(JasperReport.class);
    InputStream inputStream = mock(InputStream.class);
    when(file.getInputStream()).thenReturn(inputStream);

    JRParameter param1 = mock(JRParameter.class);
    JRParameter param2 = mock(JRParameter.class);
    JRPropertiesMap propertiesMap = mock(JRPropertiesMap.class);

    when(report.getParameters()).thenReturn(new JRParameter[]{param1, param2});
    when(JasperCompileManager.compileReport(inputStream)).thenReturn(report);
    when(param1.getPropertiesMap()).thenReturn(propertiesMap);
    String[] propertyNames = {"name1"};
    when(propertiesMap.getPropertyNames()).thenReturn(propertyNames);
    when(propertiesMap.getProperty(DISPLAY_NAME)).thenReturn(null);
    Template template = new Template();

    templateService.validateFileAndInsertTemplate(template, file);

    verify(templateService, never()).saveWithParameters(template);
  }

  @Test
  public void shouldThrowErrorIfThereAreExtraParameterProperties() throws Exception {
    expectedException.expect(ReportingException.class);
    expectedException.expectMessage(REPORTING_EXTRA_PROPERTIES);
    MultipartFile file = mock(MultipartFile.class);
    when(file.getOriginalFilename()).thenReturn(NAME_OF_FILE);

    mockStatic(JasperCompileManager.class);
    JasperReport report = mock(JasperReport.class);
    InputStream inputStream = mock(InputStream.class);
    when(file.getInputStream()).thenReturn(inputStream);

    JRParameter param1 = mock(JRParameter.class);
    JRPropertiesMap propertiesMap = mock(JRPropertiesMap.class);

    when(report.getParameters()).thenReturn(new JRParameter[]{param1});
    when(JasperCompileManager.compileReport(inputStream)).thenReturn(report);
    when(param1.getPropertiesMap()).thenReturn(propertiesMap);
    String[] propertyNames = {"name1", "name2", "name3"};
    when(propertiesMap.getPropertyNames()).thenReturn(propertyNames);
    Template template = new Template();

    templateService.validateFileAndInsertTemplate(template, file);

    verify(templateService, never()).saveWithParameters(template);
  }

  @Test
  public void shouldValidateFileAndSetData() throws Exception {
    MultipartFile file = mock(MultipartFile.class);
    when(file.getOriginalFilename()).thenReturn(NAME_OF_FILE);

    mockStatic(JasperCompileManager.class);
    JasperReport report = mock(JasperReport.class);
    InputStream inputStream = mock(InputStream.class);
    when(file.getInputStream()).thenReturn(inputStream);

    JRParameter param1 = mock(JRParameter.class);
    JRParameter param2 = mock(JRParameter.class);
    JRPropertiesMap propertiesMap = mock(JRPropertiesMap.class);
    JRExpression jrExpression = mock(JRExpression.class);

    String[] propertyNames = {DISPLAY_NAME};
    when(report.getParameters()).thenReturn(new JRParameter[]{param1, param2});
    when(JasperCompileManager.compileReport(inputStream)).thenReturn(report);
    when(propertiesMap.getPropertyNames()).thenReturn(propertyNames);
    when(propertiesMap.getProperty(DISPLAY_NAME)).thenReturn(PARAM_DISPLAY_NAME);

    when(param1.getPropertiesMap()).thenReturn(propertiesMap);
    when(param1.getValueClassName()).thenReturn("String");
    when(param1.getName()).thenReturn("name");
    when(param1.getDescription()).thenReturn("desc");
    when(param1.getDefaultValueExpression()).thenReturn(jrExpression);
    when(jrExpression.getText()).thenReturn("text");

    when(param2.getPropertiesMap()).thenReturn(propertiesMap);
    when(param2.getValueClassName()).thenReturn("Integer");
    when(param2.getName()).thenReturn("name");
    when(param2.getDescription()).thenReturn("desc");
    when(param2.getDefaultValueExpression()).thenReturn(jrExpression);

    ByteArrayOutputStream byteOutputStream = mock(ByteArrayOutputStream.class);
    whenNew(ByteArrayOutputStream.class).withAnyArguments().thenReturn(byteOutputStream);
    ObjectOutputStream objectOutputStream = spy(new ObjectOutputStream(byteOutputStream));
    whenNew(ObjectOutputStream.class).withArguments(byteOutputStream)
        .thenReturn(objectOutputStream);
    doNothing().when(objectOutputStream).writeObject(report);
    byte[] byteData = new byte[1];
    when(byteOutputStream.toByteArray()).thenReturn(byteData);
    Template template = new Template();

    templateService.validateFileAndInsertTemplate(template, file);

    verify(templateRepository).save(template);

    assertThat(template.getTemplateParameters().get(0).getDisplayName(),
        is(PARAM_DISPLAY_NAME));
    assertThat(template.getTemplateParameters().get(0).getDescription(), is("desc"));
    assertThat(template.getTemplateParameters().get(0).getName(), is("name"));
  }

  @Test
  public void shouldValidateFileAndSetDataIfDefaultValueExpressionIsNull() throws Exception {
    MultipartFile file = mock(MultipartFile.class);
    when(file.getOriginalFilename()).thenReturn(NAME_OF_FILE);

    mockStatic(JasperCompileManager.class);
    JasperReport report = mock(JasperReport.class);
    InputStream inputStream = mock(InputStream.class);
    when(file.getInputStream()).thenReturn(inputStream);

    JRParameter param1 = mock(JRParameter.class);
    JRParameter param2 = mock(JRParameter.class);
    JRPropertiesMap propertiesMap = mock(JRPropertiesMap.class);
    JRExpression jrExpression = mock(JRExpression.class);
    String[] propertyNames = {DISPLAY_NAME};

    when(report.getParameters()).thenReturn(new JRParameter[]{param1, param2});
    when(JasperCompileManager.compileReport(inputStream)).thenReturn(report);
    when(propertiesMap.getPropertyNames()).thenReturn(propertyNames);
    when(propertiesMap.getProperty(DISPLAY_NAME)).thenReturn(PARAM_DISPLAY_NAME);

    when(param1.getPropertiesMap()).thenReturn(propertiesMap);
    when(param1.getValueClassName()).thenReturn("String");
    when(param1.getDefaultValueExpression()).thenReturn(jrExpression);
    when(jrExpression.getText()).thenReturn("text");

    when(param2.getPropertiesMap()).thenReturn(propertiesMap);
    when(param2.getValueClassName()).thenReturn("Integer");
    when(param2.getDefaultValueExpression()).thenReturn(null);

    ByteArrayOutputStream byteOutputStream = mock(ByteArrayOutputStream.class);
    whenNew(ByteArrayOutputStream.class).withAnyArguments().thenReturn(byteOutputStream);
    ObjectOutputStream objectOutputStream = spy(new ObjectOutputStream(byteOutputStream));
    whenNew(ObjectOutputStream.class).withArguments(byteOutputStream)
        .thenReturn(objectOutputStream);
    doNothing().when(objectOutputStream).writeObject(report);
    byte[] byteData = new byte[1];
    when(byteOutputStream.toByteArray()).thenReturn(byteData);
    Template template = new Template();

    templateService.validateFileAndInsertTemplate(template, file);

    verify(templateRepository).save(template);
    assertThat(template.getTemplateParameters().get(0).getDisplayName(),
        is(PARAM_DISPLAY_NAME));
  }
}
