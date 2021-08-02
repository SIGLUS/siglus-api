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

package org.siglus.siglusapi.errorhandling;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Locale;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.siglus.common.constant.DateFormatConstants;
import org.siglus.siglusapi.i18n.ExposedMessageSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;
import org.zalando.problem.ProblemModule;

@SuppressWarnings({"PMD.AvoidDuplicateLiterals"})
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {
    ExceptionHandlerExceptionResolver.class, ObjectMapper.class, GlobalErrorHandling.class
})
public class GlobalErrorHandlingMvcTest {

  static final String ERROR_MESSAGE = "error-message";

  @MockBean
  private ExposedMessageSource messageSource;

  @Autowired
  private ExceptionHandlerExceptionResolver exceptionResolver;

  @Autowired
  private ObjectMapper mapper;

  private MockMvc mockMvc;

  @Before
  public void setUp() {
    // this part has been done in ErrorHandingConfig
    mapper.registerModule(new ProblemModule().withStackTraces(false));
    // otherwise the bean can't be write into the response
    exceptionResolver.getMessageConverters().add(new MappingJackson2HttpMessageConverter(mapper));
    mockMvc = MockMvcBuilders.standaloneSetup(new GlobalErrorHandlingTestController())
        .setControllerAdvice(exceptionResolver)
        .build();

    when(messageSource.getMessage(any(), any(), eq(Locale.ENGLISH))).then(i -> i.getArgumentAt(0, String.class));
    when(messageSource.getMessage(any(), any(), eq(DateFormatConstants.PORTUGAL)))
        .then(i -> i.getArgumentAt(0, String.class) + " in Portuguese");
  }

  @Test
  public void shouldReturnBadRequestWhenHandleErrorGivenValidationMessageException() throws Exception {
    // given
    RequestBuilder request = get("/test/validation-message").contentType(MediaType.APPLICATION_JSON);

    // then
    ResultActions response = mockMvc.perform(request).andDo(print());

    // when
    response.andExpect(status().isBadRequest())
        .andExpect(jsonPath("messageKey").value("key"))
        .andExpect(jsonPath("message").value("key"))
        .andExpect(jsonPath("messageInEnglish").value("key"))
        .andExpect(jsonPath("messageInPortuguese").value("key in Portuguese"));
  }

  @Test
  public void shouldReturnBadRequestWhenHandleErrorGivenDataIntegrityException() throws Exception {
    // given
    RequestBuilder request = get("/test/data-integrity").contentType(MediaType.APPLICATION_JSON);

    // then
    ResultActions response = mockMvc.perform(request).andDo(print());

    // when
    response.andExpect(status().isBadRequest())
        .andExpect(jsonPath("messageKey").value("siglusapi.error.widget.code.duplicated"))
        .andExpect(jsonPath("message").value("siglusapi.error.widget.code.duplicated"))
        .andExpect(jsonPath("messageInEnglish").value("siglusapi.error.widget.code.duplicated"))
        .andExpect(jsonPath("messageInPortuguese").value("siglusapi.error.widget.code.duplicated in Portuguese"));
  }

  @Test
  public void shouldReturnBadRequestWhenHandleErrorGivenDataIntegrityExceptionWithNonExistedKeyCause()
      throws Exception {
    // given
    RequestBuilder request = get("/test/data-integrity-with-non-existed-key").contentType(MediaType.APPLICATION_JSON);

    // then
    ResultActions response = mockMvc.perform(request).andDo(print());

    // when
    response.andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("messageKey").value("nested exception is org.hibernate.exception.ConstraintViolationException"))
        .andExpect(
            jsonPath("message").value("nested exception is org.hibernate.exception.ConstraintViolationException"))
        .andExpect(jsonPath("messageInEnglish")
            .value("nested exception is org.hibernate.exception.ConstraintViolationException"))
        .andExpect(jsonPath("messageInPortuguese")
            .value("nested exception is org.hibernate.exception.ConstraintViolationException in Portuguese"));
  }

  @Test
  public void shouldReturnBadRequestWhenHandleErrorGivenDataIntegrityExceptionWithoutCause() throws Exception {
    // given
    RequestBuilder request = get("/test/data-integrity-without-cause").contentType(MediaType.APPLICATION_JSON);

    // then
    ResultActions response = mockMvc.perform(request).andDo(print());

    // when
    response.andExpect(status().isBadRequest())
        .andExpect(jsonPath("messageKey").value("error-message"))
        .andExpect(jsonPath("message").value("error-message"))
        .andExpect(jsonPath("messageInEnglish").value("error-message"))
        .andExpect(jsonPath("messageInPortuguese").value("error-message in Portuguese"));
  }

  @Test
  public void shouldReturnBadRequestWhenHandleErrorGivenConstraintViolationException() throws Exception {
    // given
    RequestBuilder request = get("/test/constraint-violation").contentType(MediaType.APPLICATION_JSON);

    // then
    ResultActions response = mockMvc.perform(request).andDo(print());

    // when
    response.andExpect(status().isBadRequest())
        .andExpect(jsonPath("messageKey").value("siglusapi.error.validationFail"))
        .andExpect(jsonPath("message").value("siglusapi.error.validationFail"))
        .andExpect(jsonPath("messageInEnglish").value("siglusapi.error.validationFail"))
        .andExpect(jsonPath("messageInPortuguese").value("siglusapi.error.validationFail in Portuguese"))
        .andExpect(jsonPath("fields[0].propertyPath").value("propertyPath"))
        .andExpect(jsonPath("fields[0].messageInEnglish").value("text"))
        .andExpect(jsonPath("fields[0].messageInPortuguese").value("text"));
  }

}
