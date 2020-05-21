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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.junit.Assert.assertEquals;

import java.util.Map;
import lombok.Getter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.requisition.utils.Message;
import org.springframework.validation.BindingResult;
import org.springframework.validation.DirectFieldBindingResult;

@RunWith(MockitoJUnitRunner.class)
public class BaseControllerTest {

  private static final String FIELD = "field";
  private static final String FIELD2 = "field2";
  private static final String ERROR_MESSAGE_KEY = "requisition.some.error";

  private MockController controller;
  private BindingResult bindingResult;
  private MockClass mockObject;

  @Before
  public void setUp() {
    controller = new MockController();
    mockObject = new MockClass();
    bindingResult = new DirectFieldBindingResult(mockObject, "mockObject");
  }

  @Test
  public void shouldExtractMessageKeyFromFieldError() {
    rejectValue(FIELD, ERROR_MESSAGE_KEY);
    Map<String, Message> result = controller.getErrorsMock(bindingResult);

    assertEquals(1, result.size());
    assertEquals(ERROR_MESSAGE_KEY, result.get(FIELD).getKey());
  }

  @Test
  public void shouldExtractMessageKeyFromFieldErrors() {
    rejectValue(FIELD, ERROR_MESSAGE_KEY);
    rejectValue(FIELD2, ERROR_MESSAGE_KEY + 2);
    Map<String, Message> result = controller.getErrorsMock(bindingResult);

    assertEquals(2, result.size());
    assertEquals(ERROR_MESSAGE_KEY, result.get(FIELD).getKey());
    assertEquals(ERROR_MESSAGE_KEY + 2, result.get(FIELD2).getKey());
  }

  @Test
  public void shouldExtractMessageKeyAndParamsFromFieldError() {
    rejectValue(FIELD, ERROR_MESSAGE_KEY, "param1", "param2");
    Map<String, Message> result = controller.getErrorsMock(bindingResult);

    assertEquals(1, result.size());
    assertEquals(ERROR_MESSAGE_KEY, result.get(FIELD).getKey());
    assertThat(result.get(FIELD).getParams(), arrayContaining("param1", "param2"));
  }

  private void rejectValue(String fieldName, String key, String... parameters) {
    bindingResult.rejectValue(fieldName, new Message(key, parameters).toString());
  }

  private class MockController extends BaseController {

    private Map<String, Message> getErrorsMock(BindingResult bindingResult) {
      return getErrors(bindingResult);
    }
  }

  @Getter
  private class MockClass {
    private String field;
    private String field2;
  }
}
