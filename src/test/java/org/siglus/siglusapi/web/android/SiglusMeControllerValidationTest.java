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

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toMap;
import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.executable.ExecutableValidator;
import lombok.SneakyThrows;
import org.hibernate.validator.messageinterpolation.ResourceBundleMessageInterpolator;
import org.hibernate.validator.resourceloading.PlatformResourceBundleLocator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.dto.android.request.StockCardCreateRequest;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
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
    ResourceBundleMessageInterpolator messageInterpolator =
        new ResourceBundleMessageInterpolator(new PlatformResourceBundleLocator("messages"));
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    stockCardCreateRequestList = mapper.getTypeFactory()
        .constructCollectionType(List.class, StockCardCreateRequest.class);
    forExecutables = Validation.byDefaultProvider().configure()
        .messageInterpolator(messageInterpolator)
        .buildValidatorFactory().getValidator().forExecutables();
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
    String json = readFromFile("emptyLotList.json");
    Object param = mapper.readValue(json, stockCardCreateRequestList);

    // when
    Map<String, String> violations = forExecutables
        .validateParameters(controller, createStockCards, new Object[]{param}).stream()
        .collect(toMap(v -> v.getPropertyPath().toString(), ConstraintViolation::getMessage));

    // then
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
    String json = readFromFile("negativeNumber.json");
    Object param = mapper.readValue(json, stockCardCreateRequestList);

    // when
    Map<String, String> violations = forExecutables
        .validateParameters(controller, createStockCards, new Object[]{param}).stream()
        .collect(toMap(v -> v.getPropertyPath().toString(), ConstraintViolation::getMessage));

    // then
    assertEquals(2, violations.size());
    assertEquals(MUST_BE_POSITIVE,
        violations.get("createStockCards.arg0[0].stockOnHand"));
    assertEquals(MUST_BE_POSITIVE,
        violations.get("createStockCards.arg0[0].lotEvents[0].stockOnHand"));
  }

  @Test
  public void shouldReturnViolationWhenValidateCreateStockCardsGivenInconsistentInitSoh()
      throws IOException {
    // given
    String json = readFromFile("inconsistentInitSoh.json");
    Object param = mapper.readValue(json, stockCardCreateRequestList);

    // when
    Map<String, String> violations = forExecutables
        .validateParameters(controller, createStockCards, new Object[]{param}).stream()
        .collect(toMap(v -> v.getPropertyPath().toString(), ConstraintViolation::getMessage));

    // then
    assertEquals(1, violations.size());
    assertEquals("The records of the product 08S01Z are not consistent.",
        violations.get("createStockCards.arg0"));
  }

  @Test
  public void shouldReturnViolationWhenValidateCreateStockCardsGivenInconsistentGap()
      throws IOException {
    // given
    String json = readFromFile("inconsistentGap.json");
    Object param = mapper.readValue(json, stockCardCreateRequestList);

    // when
    Map<String, String> violations = forExecutables
        .validateParameters(controller, createStockCards, new Object[]{param}).stream()
        .collect(toMap(v -> v.getPropertyPath().toString(), ConstraintViolation::getMessage));

    // then
    assertEquals(1, violations.size());
    assertEquals("The records of the product 08S01Z are not consistent.",
        violations.get("createStockCards.arg0"));
  }

  @Test
  public void shouldReturnViolationWhenValidateCreateStockCardsGivenInconsistentStockCard()
      throws IOException {
    // given
    String json = readFromFile("inconsistentInitStockCard.json");
    Object param = mapper.readValue(json, stockCardCreateRequestList);

    // when
    Map<String, String> violations = forExecutables
        .validateParameters(controller, createStockCards, new Object[]{param}).stream()
        .collect(toMap(v -> v.getPropertyPath().toString(), ConstraintViolation::getMessage));

    // then
    assertEquals(1, violations.size());
    assertEquals("The records of the product 08S01Z are not consistent.",
        violations.get("createStockCards.arg0"));
  }

  @SneakyThrows
  private String readFromFile(String fileName) {
    String name = this.getClass().getName();
    String folder = name.replace("org.siglus.siglusapi.", "").replaceAll("\\.", "/");
    ClassLoader classLoader = this.getClass().getClassLoader();
    List<String> allLines = Optional.ofNullable(classLoader.getResource(folder + "/" + fileName))
        .map(this::toUri)
        .map(Paths::get)
        .map(this::readAllLines)
        .orElse(emptyList());
    return String.join("\n", allLines);
  }

  @SneakyThrows
  private URI toUri(URL url) {
    return url.toURI();
  }

  @SneakyThrows
  private List<String> readAllLines(Path path) {
    return Files.readAllLines(path);
  }

}
