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

package org.siglus.siglusapi.web.android;

import static java.util.stream.Collectors.toMap;
import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.executable.ExecutableValidator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.dto.android.request.StockCardCreateRequest;

@RunWith(MockitoJUnitRunner.class)
public class SiglusMeControllerValidationTest {

  private static final String MAY_NOT_BE_EMPTY = "may not be empty";
  private static final String MAY_NOT_BE_NULL = "may not be null";
  private static final String MUST_BE_POSITIVE = "must be greater than or equal to 0";

  @InjectMocks
  private SiglusMeController controller;

  private final ObjectMapper mapper = new ObjectMapper();

  private JavaType stockCardCreateRequestList;

  private ExecutableValidator forExecutables;

  private Method createStockCards;

  @Before
  public void setup() throws NoSuchMethodException {
    Locale.setDefault(Locale.ENGLISH);
    mapper.registerModule(new JavaTimeModule());
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    stockCardCreateRequestList = mapper.getTypeFactory()
        .constructCollectionType(List.class, StockCardCreateRequest.class);
    forExecutables = Validation.buildDefaultValidatorFactory().getValidator().forExecutables();
    createStockCards = SiglusMeController.class.getDeclaredMethod("createStockCards", List.class);
  }

  @Test
  public void shouldReturnViolationWhenValidateCreateStockCardsGivenEmptyList() throws IOException {
    // given
    String json = "[]";
    Object param = mapper.readValue(json, stockCardCreateRequestList);

    // when
    Map<String, String> violations = forExecutables
        .validateParameters(controller, createStockCards, new Object[]{param}).stream()
        .collect(toMap(v -> v.getPropertyPath().toString(), ConstraintViolation::getMessage));

    // then
    assertEquals(1, violations.size());
    assertEquals(MAY_NOT_BE_EMPTY, violations.get("createStockCards.arg0"));
  }

  @Test
  public void shouldReturnViolationWhenValidateCreateStockCardsGivenEmptyRequest()
      throws IOException {
    // given
    String json = "[{}]";
    Object param = mapper.readValue(json, stockCardCreateRequestList);

    // when
    Map<String, String> violations = forExecutables
        .validateParameters(controller, createStockCards, new Object[]{param}).stream()
        .collect(toMap(v -> v.getPropertyPath().toString(), ConstraintViolation::getMessage));

    // then
    assertEquals(7, violations.size());
    assertEquals(MAY_NOT_BE_EMPTY, violations.get("createStockCards.arg0[0].lotEvents"));
    assertEquals(MAY_NOT_BE_NULL, violations.get("createStockCards.arg0[0].occurred"));
    assertEquals(MAY_NOT_BE_NULL, violations.get("createStockCards.arg0[0].processedDate"));
    assertEquals(MAY_NOT_BE_EMPTY, violations.get("createStockCards.arg0[0].productCode"));
    assertEquals(MAY_NOT_BE_NULL, violations.get("createStockCards.arg0[0].quantity"));
    assertEquals(MAY_NOT_BE_NULL, violations.get("createStockCards.arg0[0].stockOnHand"));
    assertEquals(MAY_NOT_BE_EMPTY, violations.get("createStockCards.arg0[0].type"));
  }

  @Test
  public void shouldReturnViolationWhenValidateCreateStockCardsGivenEmptyLotList()
      throws IOException {
    // given
    String json = "[{\n"
        + "\t\"SOH\": \"3\",\n"
        + "\t\"quantity\": 10,\n"
        + "\t\"occurred\": \"2021-06-17\",\n"
        + "\t\"documentationNo\": \"doc-001\",\n"
        + "\t\"productCode\": \"08S01Z\",\n"
        + "\t\"type\": \"ADJUSTMENT\",\n"
        + "\t\"processeddate\": \"2021-06-17T14:20:56.000Z\",\n"
        + "\t\"signature\": \"zhangsan\",\n"
        + "\t\"lotEventList\": [{}]\n"
        + "}]";
    Object param = mapper.readValue(json, stockCardCreateRequestList);

    // when
    Map<String, String> violations = forExecutables
        .validateParameters(controller, createStockCards, new Object[]{param}).stream()
        .collect(toMap(v -> v.getPropertyPath().toString(), ConstraintViolation::getMessage));

    // then
    System.out.println(violations);
    assertEquals(4, violations.size());
    assertEquals(MAY_NOT_BE_NULL,
        violations.get("createStockCards.arg0[0].lotEvents[0].stockOnHand"));
    assertEquals(MAY_NOT_BE_EMPTY,
        violations.get("createStockCards.arg0[0].lotEvents[0].lotNumber"));
    assertEquals(MAY_NOT_BE_NULL,
        violations.get("createStockCards.arg0[0].lotEvents[0].expirationDate"));
    assertEquals(MAY_NOT_BE_NULL,
        violations.get("createStockCards.arg0[0].lotEvents[0].quantity"));
  }

  @Test
  public void shouldReturnViolationWhenValidateCreateStockCardsGivenNegativeNumber()
      throws IOException {
    // given
    String json = "[{\n"
        + "\t\"SOH\": \"-23\",\n"
        + "\t\"quantity\": -10,\n"
        + "\t\"occurred\": \"2021-06-17\",\n"
        + "\t\"documentationNo\": \"doc-001\",\n"
        + "\t\"productCode\": \"08S01Z\",\n"
        + "\t\"type\": \"ADJUSTMENT\",\n"
        + "\t\"processeddate\": \"2021-06-17T14:20:56.000Z\",\n"
        + "\t\"signature\": \"zhangsan\",\n"
        + "\t\"lotEventList\": [{\n"
        + "\t\t\"SOH\": \"-5\",\n"
        + "\t\t\"expirationDate\": \"2021-06-29\",\n"
        + "\t\t\"lotNumber\": \"SEM-LOTE-02A01-062021\",\n"
        + "\t\t\"quantity\": -2,\n"
        + "\t\t\"reasonName\": \"DISTRICT_DDM\",\n"
        + "\t\t\"documentationNo\": \"doc-001\"\n"
        + "\t}]\n"
        + "}]";
    Object param = mapper.readValue(json, stockCardCreateRequestList);

    // when
    Map<String, String> violations = forExecutables
        .validateParameters(controller, createStockCards, new Object[]{param}).stream()
        .collect(toMap(v -> v.getPropertyPath().toString(), ConstraintViolation::getMessage));

    // then
    assertEquals(4, violations.size());
    assertEquals(MUST_BE_POSITIVE,
        violations.get("createStockCards.arg0[0].quantity"));
    assertEquals(MUST_BE_POSITIVE,
        violations.get("createStockCards.arg0[0].stockOnHand"));
    assertEquals(MUST_BE_POSITIVE,
        violations.get("createStockCards.arg0[0].lotEvents[0].quantity"));
    assertEquals(MUST_BE_POSITIVE,
        violations.get("createStockCards.arg0[0].lotEvents[0].stockOnHand"));
  }

}
