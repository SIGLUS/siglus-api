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

package org.openlmis.requisition.service;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_REPORTING_FILE_EMPTY;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_REPORTING_FILE_INCORRECT_TYPE;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_REPORTING_FILE_INVALID;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_REPORTING_FILE_MISSING;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_REPORTING_PARAMETER_MISSING;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_REPORTING_TEMPLATE_EXIST;
import static org.openlmis.requisition.service.JasperTemplateService.REPORT_TYPE_PROPERTY;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import net.sf.jasperreports.engine.JRExpression;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JRPropertiesMap;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.openlmis.requisition.domain.JasperTemplate;
import org.openlmis.requisition.domain.JasperTemplateParameter;
import org.openlmis.requisition.domain.JasperTemplateParameterDependency;
import org.openlmis.requisition.exception.ReportingException;
import org.openlmis.requisition.repository.JasperTemplateRepository;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(BlockJUnit4ClassRunner.class)
@PrepareForTest({JasperTemplateService.class, JasperCompileManager.class})
@SuppressWarnings("PMD.TooManyMethods")
public class JasperTemplateServiceTest {

  @Mock
  private JasperTemplateRepository jasperTemplateRepository;

  @InjectMocks
  private JasperTemplateService jasperTemplateService;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private static final String NAME_OF_FILE = "report.jrxml";
  private static final String DISPLAY_NAME = "displayName";
  private static final String PARAM_DISPLAY_NAME = "Param Display Name";
  private static final String REQUIRED = "required";
  private static final String PARAM1 = "param1";
  private static final String PARAM2 = "param2";
  private static final String PARAM3 = "param3";
  private static final String PARAM4 = "param4";
  
  private HttpServletRequest request;
  private JasperTemplate template;
  
  @Before
  public void setUp() {
    request = mock(HttpServletRequest.class);
    template = mock(JasperTemplate.class);
  }
  
  @Test
  public void shouldThrowErrorIfFileNotOfTypeJasperXml() throws Exception {
    expectedException.expect(ReportingException.class);
    expectedException.expectMessage(ERROR_REPORTING_FILE_INCORRECT_TYPE);

    MockMultipartFile file = new MockMultipartFile("report.pdf", new byte[1]);
    jasperTemplateService.validateFileAndInsertTemplate(new JasperTemplate(), file);
  }

  @Test
  public void shouldThrowErrorIfFileEmpty() throws Exception {
    expectedException.expect(ReportingException.class);
    expectedException.expectMessage(ERROR_REPORTING_FILE_EMPTY);
    MockMultipartFile file = new MockMultipartFile(
        NAME_OF_FILE, NAME_OF_FILE, "", new byte[0]);

    jasperTemplateService.validateFileAndInsertTemplate(new JasperTemplate(), file);
  }

  @Test
  public void shouldThrowErrorIfFileNotPresent() throws Exception {
    expectedException.expect(ReportingException.class);
    expectedException.expectMessage(ERROR_REPORTING_FILE_MISSING);

    jasperTemplateService.validateFileAndInsertTemplate(new JasperTemplate(), null);
  }

  @Test
  public void shouldThrowErrorIfFileIsInvalid() throws Exception {
    expectedException.expect(ReportingException.class);
    expectedException.expectMessage(ERROR_REPORTING_FILE_INVALID);

    jasperTemplateService.validateFileAndInsertTemplate(new JasperTemplate(),
        new MockMultipartFile(NAME_OF_FILE, NAME_OF_FILE, "", new byte[1]));
  }

  @Test
  public void shouldThrowErrorIfTemplateNameAlreadyExists() throws Exception {
    JasperTemplate jasperTemplate = new JasperTemplate();
    jasperTemplate.setName("Name");
    expectedException.expect(ReportingException.class);
    expectedException.expectMessage(ERROR_REPORTING_TEMPLATE_EXIST);
    when(jasperTemplateRepository.findByName(Matchers.anyObject())).thenReturn(jasperTemplate);

    jasperTemplateService.validateFileAndInsertTemplate(jasperTemplate, null);
  }

  @Test
  public void shouldThrowErrorIfDisplayNameOfParameterIsMissing() throws Exception {
    expectedException.expect(ReportingException.class);
    expectedException.expect(hasProperty("params", arrayContaining("displayName")));
    expectedException.expectMessage(ERROR_REPORTING_PARAMETER_MISSING);

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
    when(param1.isForPrompting()).thenReturn(true);
    when(param2.isForPrompting()).thenReturn(true);

    String[] propertyNames = {"name1"};
    when(propertiesMap.getPropertyNames()).thenReturn(propertyNames);
    when(propertiesMap.getProperty(DISPLAY_NAME)).thenReturn(null);
    JasperTemplate jasperTemplate = new JasperTemplate();

    jasperTemplateService.validateFileAndInsertTemplate(jasperTemplate, file);

    verify(jasperTemplateService, never()).saveWithParameters(jasperTemplate);
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
    when(report.getProperty(REPORT_TYPE_PROPERTY)).thenReturn("test type");
    when(JasperCompileManager.compileReport(inputStream)).thenReturn(report);
    when(propertiesMap.getPropertyNames()).thenReturn(propertyNames);
    when(propertiesMap.getProperty(DISPLAY_NAME)).thenReturn(PARAM_DISPLAY_NAME);
    when(propertiesMap.getProperty(REQUIRED)).thenReturn("true");
    when(propertiesMap.getProperty("options")).thenReturn("option 1,opt\\,ion 2");
    when(propertiesMap.getProperty("dependencies")).thenReturn("dep1:plc1,dep\\,2:plc2");

    when(param1.getPropertiesMap()).thenReturn(propertiesMap);
    when(param1.getValueClassName()).thenReturn("java.lang.String");
    when(param1.getName()).thenReturn("name");
    when(param1.isForPrompting()).thenReturn(true);
    when(param1.getDescription()).thenReturn("desc");
    when(param1.getDefaultValueExpression()).thenReturn(jrExpression);
    when(jrExpression.getText()).thenReturn("text");

    when(param2.getPropertiesMap()).thenReturn(propertiesMap);
    when(param2.getValueClassName()).thenReturn("java.lang.Integer");
    when(param2.getName()).thenReturn("name");
    when(param2.isForPrompting()).thenReturn(true);
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
    JasperTemplate jasperTemplate = new JasperTemplate();

    jasperTemplateService.validateFileAndInsertTemplate(jasperTemplate, file);

    verify(jasperTemplateRepository).save(jasperTemplate);

    assertEquals("test type", jasperTemplate.getType());
    assertThat(jasperTemplate.getTemplateParameters().get(0).getDisplayName(),
        is(PARAM_DISPLAY_NAME));
    assertThat(jasperTemplate.getTemplateParameters().get(0).getDescription(), is("desc"));
    assertThat(jasperTemplate.getTemplateParameters().get(0).getName(), is("name"));
    assertThat(jasperTemplate.getTemplateParameters().get(0).getRequired(), is(true));
    assertThat(jasperTemplate.getTemplateParameters().get(0).getOptions(),
        contains("option 1", "opt,ion 2"));
    assertThat(jasperTemplate.getTemplateParameters().get(0)
            .getDependencies()
            .stream()
            .map(JasperTemplateParameterDependency::getDependency).collect(Collectors.toList()),
        contains("dep1", "dep,2"));
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
    when(param1.getValueClassName()).thenReturn("java.lang.String");
    when(param1.isForPrompting()).thenReturn(true);
    when(param1.getDefaultValueExpression()).thenReturn(jrExpression);
    when(jrExpression.getText()).thenReturn("text");

    when(param2.getPropertiesMap()).thenReturn(propertiesMap);
    when(param2.getValueClassName()).thenReturn("java.lang.Integer");
    when(param2.isForPrompting()).thenReturn(true);
    when(param2.getDefaultValueExpression()).thenReturn(null);

    ByteArrayOutputStream byteOutputStream = mock(ByteArrayOutputStream.class);
    whenNew(ByteArrayOutputStream.class).withAnyArguments().thenReturn(byteOutputStream);
    ObjectOutputStream objectOutputStream = spy(new ObjectOutputStream(byteOutputStream));
    whenNew(ObjectOutputStream.class).withArguments(byteOutputStream)
        .thenReturn(objectOutputStream);
    doNothing().when(objectOutputStream).writeObject(report);
    byte[] byteData = new byte[1];
    when(byteOutputStream.toByteArray()).thenReturn(byteData);
    JasperTemplate jasperTemplate = new JasperTemplate();

    jasperTemplateService.validateFileAndInsertTemplate(jasperTemplate, file);

    verify(jasperTemplateRepository).save(jasperTemplate);
    assertThat(jasperTemplate.getTemplateParameters().get(0).getDisplayName(),
        is(PARAM_DISPLAY_NAME));
  }
  
  @Test
  public void mapRequestParametersToTemplateShouldReturnEmptyMapIfNoParameters() {
    when(request.getParameterMap()).thenReturn(Collections.emptyMap());
    when(template.getTemplateParameters()).thenReturn(null);
    
    Map<String, Object> resultMap = jasperTemplateService.mapRequestParametersToTemplate(request, 
        template);
    
    assertThat(resultMap.size(), is(0));
  }

  @Test
  public void mapRequestParametersToTemplateShouldReturnEmptyMapIfNoTemplateParameters() {
    when(request.getParameterMap()).thenReturn(Collections.singletonMap("key1",
        new String[]{"value1"}));
    when(template.getTemplateParameters()).thenReturn(null);

    Map<String, Object> resultMap = jasperTemplateService.mapRequestParametersToTemplate(request,
        template);

    assertThat(resultMap.size(), is(0));
  }

  @Test
  public void mapRequestParametersToTemplateShouldReturnEmptyMapIfNoRequestParameters() {
    JasperTemplateParameter templateParameter = new JasperTemplateParameter();
    templateParameter.setTemplate(template);
    templateParameter.setName(PARAM1);

    when(request.getParameterMap()).thenReturn(Collections.emptyMap());
    when(template.getTemplateParameters()).thenReturn(Collections.singletonList(templateParameter));

    Map<String, Object> resultMap = jasperTemplateService.mapRequestParametersToTemplate(request,
        template);

    assertThat(resultMap.size(), is(0));
  }

  @Test
  public void mapRequestParametersToTemplateShouldReturnMatchingParameters() {
    JasperTemplateParameter param1 = new JasperTemplateParameter();
    param1.setTemplate(template);
    param1.setName(PARAM1);

    JasperTemplateParameter param2 = new JasperTemplateParameter();
    param2.setTemplate(template);
    param2.setName(PARAM2);
    
    Map<String, String[]> requestParameterMap = new HashMap<>();
    requestParameterMap.put(PARAM1, new String[]{"value1"});
    requestParameterMap.put(PARAM3, new String[]{"value3"});

    List<JasperTemplateParameter> templateParameterList = Arrays.asList(param1, param2);

    when(request.getParameterMap()).thenReturn(requestParameterMap);
    when(template.getTemplateParameters()).thenReturn(templateParameterList);

    Map<String, Object> resultMap = jasperTemplateService.mapRequestParametersToTemplate(request,
        template);

    assertThat(resultMap.size(), is(1));
    assertTrue(resultMap.containsKey(PARAM1));
    assertEquals("value1", resultMap.get(PARAM1));
  }

  @Test
  public void mapRequestParametersToTemplateShouldNotReturnBlankNullOrUndefinedStringValues() {
    JasperTemplateParameter param1 = new JasperTemplateParameter();
    param1.setTemplate(template);
    param1.setName(PARAM1);

    JasperTemplateParameter param2 = new JasperTemplateParameter();
    param2.setTemplate(template);
    param2.setName(PARAM2);

    JasperTemplateParameter param3 = new JasperTemplateParameter();
    param3.setTemplate(template);
    param3.setName(PARAM3);

    JasperTemplateParameter param4 = new JasperTemplateParameter();
    param4.setTemplate(template);
    param4.setName(PARAM4);

    Map<String, String[]> requestParameterMap = new HashMap<>();
    requestParameterMap.put(PARAM1, new String[]{""});
    requestParameterMap.put(PARAM2, new String[]{" "});
    requestParameterMap.put(PARAM3, new String[]{"null"});
    requestParameterMap.put(PARAM4, new String[]{"undefined"});

    List<JasperTemplateParameter> templateParameterList =
        Arrays.asList(param1, param2, param3, param4);

    when(request.getParameterMap()).thenReturn(requestParameterMap);
    when(template.getTemplateParameters()).thenReturn(templateParameterList);

    Map<String, Object> resultMap = jasperTemplateService.mapRequestParametersToTemplate(request,
        template);

    assertThat(resultMap.size(), is(0));
  }
}
