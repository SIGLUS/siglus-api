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
import static org.mockito.Matchers.any;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

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
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorFactory;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.executable.ExecutableValidator;
import lombok.SneakyThrows;
import org.hibernate.validator.internal.util.privilegedactions.NewInstance;
import org.hibernate.validator.messageinterpolation.ResourceBundleMessageInterpolator;
import org.hibernate.validator.resourceloading.PlatformResourceBundleLocator;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.common.dto.referencedata.OrderableDto;
import org.siglus.siglusapi.dto.android.request.StockCardCreateRequest;
import org.siglus.siglusapi.dto.android.validators.stockcard.KitProductEmptyLotsValidator;
import org.siglus.siglusapi.dto.android.validators.stockcard.NonKitProductNotEmptyLotsValidator;
import org.siglus.siglusapi.service.SiglusOrderableService;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.TooManyMethods"})
public class SiglusMeControllerValidationTest {

  private static final String MAY_NOT_BE_EMPTY = "may not be empty";
  private static final String MAY_NOT_BE_NULL = "may not be null";
  private static final String MUST_BE_POSITIVE = "must be greater than or equal to 0";

  @InjectMocks
  private SiglusMeController controller;

  @Mock
  private SiglusOrderableService orderableService;

  private final ObjectMapper mapper = new ObjectMapper();

  private JavaType stockCardCreateRequestListType;

  private ExecutableValidator forExecutables;

  private Method createStockCards;

  @Before
  public void setup() throws NoSuchMethodException {
    Locale.setDefault(Locale.ENGLISH);
    mapper.registerModule(new JavaTimeModule());
    ResourceBundleMessageInterpolator messageInterpolator =
        new ResourceBundleMessageInterpolator(new PlatformResourceBundleLocator("messages"));
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    stockCardCreateRequestListType = mapper.getTypeFactory()
        .constructCollectionType(List.class, StockCardCreateRequest.class);
    forExecutables = Validation.byDefaultProvider().configure()
        .constraintValidatorFactory(new InnerConstraintValidatorFactory())
        .messageInterpolator(messageInterpolator)
        .buildValidatorFactory().getValidator().forExecutables();
    createStockCards = SiglusMeController.class.getDeclaredMethod("createStockCards", List.class);
    orderableService = mock(SiglusOrderableService.class);
    OrderableDto notKitProduct = mock(OrderableDto.class);
    OrderableDto kitProduct = mock(OrderableDto.class);
    when(kitProduct.getIsKit()).thenReturn(true);
    when(orderableService.getOrderableByCode(any())).then(invocation -> {
      String productCode = invocation.getArgumentAt(0, String.class);
      if ("08K".equals(productCode)) {
        return kitProduct;
      }
      return notKitProduct;
    });
  }

  @Test
  public void shouldReturnViolationWhenValidateCreateStockCardsGivenEmptyList() throws IOException {
    // given
    String json = "[]";
    Object param = mapper.readValue(json, stockCardCreateRequestListType);

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
    Object param = mapper.readValue(json, stockCardCreateRequestListType);

    // when
    Map<String, String> violations = forExecutables
        .validateParameters(controller, createStockCards, new Object[]{param}).stream()
        .collect(toMap(v -> v.getPropertyPath().toString(), ConstraintViolation::getMessage));

    // then
    assertEquals(6, violations.size());
    // could be empty when product is kit
    // assertEquals(MAY_NOT_BE_EMPTY, violations.get("createStockCards.arg0[0].lotEvents"));
    assertEquals(MAY_NOT_BE_NULL, violations.get("createStockCards.arg0[0].occurredDate"));
    assertEquals(MAY_NOT_BE_NULL, violations.get("createStockCards.arg0[0].createdAt"));
    assertEquals(MAY_NOT_BE_EMPTY, violations.get("createStockCards.arg0[0].productCode"));
    assertEquals(MAY_NOT_BE_NULL, violations.get("createStockCards.arg0[0].quantity"));
    assertEquals(MAY_NOT_BE_NULL, violations.get("createStockCards.arg0[0].stockOnHand"));
    assertEquals(MAY_NOT_BE_EMPTY, violations.get("createStockCards.arg0[0].type"));
  }

  @Test
  public void shouldReturnViolationWhenValidateCreateStockCardsGivenEmptyLotList()
      throws IOException {
    // given
    Object param = parseParam("emptyLotList.json");

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
    Object param = parseParam("negativeNumber.json");

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
  public void shouldReturnViolationWhenValidateCreateStockCardsGivenInconsistentProductSoh()
      throws IOException {
    // given
    Object param = parseParam("inconsistentProductSoh.json");

    // when
    Map<String, String> violations = forExecutables
        .validateParameters(controller, createStockCards, new Object[]{param}).stream()
        .collect(toMap(v -> v.getPropertyPath().toString(), ConstraintViolation::getMessage));

    // then
    assertEquals(2, violations.size());
    assertEquals("The adjustment with soh[10] is inconsistent with quantity[20].",
        violations.get("createStockCards.arg0[0]"));
  }

  @Test
  public void shouldReturnViolationWhenValidateCreateStockCardsGivenInconsistentLotSoh()
      throws IOException {
    // given
    Object param = parseParam("inconsistentLotSoh.json");

    // when
    Map<String, String> violations = forExecutables
        .validateParameters(controller, createStockCards, new Object[]{param}).stream()
        .collect(toMap(v -> v.getPropertyPath().toString(), ConstraintViolation::getMessage));

    // then
    assertEquals(1, violations.size());
    assertEquals("The adjustment with soh[5] is inconsistent with quantity[10].",
        violations.get("createStockCards.arg0[0].lotEvents[0]"));
  }

  @Test
  public void shouldReturnViolationWhenValidateCreateStockCardsGivenInconsistentLotQuantitySum()
      throws IOException {
    // given
    Object param = parseParam("inconsistentLotQuantitySum.json");

    // when
    Map<String, String> violations = forExecutables
        .validateParameters(controller, createStockCards, new Object[]{param}).stream()
        .collect(toMap(v -> v.getPropertyPath().toString(), ConstraintViolation::getMessage));

    // then
    assertEquals(1, violations.size());
    assertEquals("The stock card for 08S01Z on 2021-06-17 is inconsistent with its lot events.",
        violations.get("createStockCards.arg0[0]"));
  }

  @Test
  public void shouldReturnViolationWhenValidateCreateStockCardsGivenInconsistentLotSohSum()
      throws IOException {
    // given
    Object param = parseParam("inconsistentLotSohSum.json");

    // when
    Map<String, String> violations = forExecutables
        .validateParameters(controller, createStockCards, new Object[]{param}).stream()
        .collect(toMap(v -> v.getPropertyPath().toString(), ConstraintViolation::getMessage));

    // then
    assertEquals(2, violations.size());
    assertEquals("The stock card for 08S01Z on 2021-06-17 is inconsistent with its lot events.",
        violations.get("createStockCards.arg0[0]"));
  }

  @Ignore
  @Test
  public void shouldReturnViolationWhenValidateCreateStockCardsGivenInconsistentProductsByGap()
      throws IOException {
    // given
    Object param = parseParam("inconsistentProductsByGap.json");

    // when
    Map<String, String> violations = forExecutables
        .validateParameters(controller, createStockCards, new Object[]{param}).stream()
        .collect(toMap(v -> v.getPropertyPath().toString(), ConstraintViolation::getMessage));

    // then
    assertEquals(1, violations.size());
    assertEquals("The product 08S01Z is not consistent by gap.",
        violations.get("createStockCards.arg0"));
  }

  @Ignore
  @Test
  public void shouldReturnViolationWhenValidateCreateStockCardsGivenInconsistentProductsByEach()
      throws IOException {
    // given
    Object param = parseParam("inconsistentProductsByEach.json");

    // when
    Map<String, String> violations = forExecutables
        .validateParameters(controller, createStockCards, new Object[]{param}).stream()
        .collect(toMap(v -> v.getPropertyPath().toString(), ConstraintViolation::getMessage));

    // then
    assertEquals(1, violations.size());
    assertEquals("The product 08S01Z is not consistent on 2021-06-16.",
        violations.get("createStockCards.arg0"));
  }

  @Ignore
  @Test
  public void shouldReturnViolationWhenValidateCreateStockCardsGivenInconsistentLotsByGap()
      throws IOException {
    // given
    Object param = parseParam("inconsistentLotsByGap.json");

    // when
    Map<String, String> violations = forExecutables
        .validateParameters(controller, createStockCards, new Object[]{param}).stream()
        .collect(toMap(v -> v.getPropertyPath().toString(), ConstraintViolation::getMessage));

    // then
    assertEquals(1, violations.size());
    assertEquals("The lot SEM-LOTE-02A01-062021 of the product 08S01Z is not consistent by gap.",
        violations.get("createStockCards.arg0"));
  }

  @Ignore
  @Test
  public void shouldReturnViolationWhenValidateCreateStockCardsGivenInconsistentLotsByEach()
      throws IOException {
    // given
    Object param = parseParam("inconsistentLotsByEach.json");

    // when
    Map<String, String> violations = forExecutables
        .validateParameters(controller, createStockCards, new Object[]{param}).stream()
        .collect(toMap(v -> v.getPropertyPath().toString(), ConstraintViolation::getMessage));

    // then
    assertEquals(1, violations.size());
    assertEquals("The lot SEM-LOTE-02A01-062021 of the product 08S01Z is not consistent on 2021-06-16.",
        violations.get("createStockCards.arg0"));
  }

  @Ignore
  @Test
  public void shouldReturnViolationWhenValidateCreateStockCardsGivenInconsistentLotsOverProduct()
      throws IOException {
    // given
    Object param = parseParam("inconsistentLotsOverProduct.json");

    // when
    Map<String, String> violations = forExecutables
        .validateParameters(controller, createStockCards, new Object[]{param}).stream()
        .collect(toMap(v -> v.getPropertyPath().toString(), ConstraintViolation::getMessage));

    // then
    assertEquals(1, violations.size());
    assertEquals("The product 08S01Z is not consistent since it has less SOH than the sum its lots' on 2021-06-16.",
        violations.get("createStockCards.arg0"));
  }

  @Test
  public void shouldReturnViolationWhenValidateCreateStockCardsGivenKitProductWithLots()
      throws IOException {
    // given
    Object param = parseParam("kitProductWithLots.json");

    // when
    Map<String, String> violations = forExecutables
        .validateParameters(controller, createStockCards, new Object[]{param}).stream()
        .collect(toMap(v -> v.getPropertyPath().toString(), ConstraintViolation::getMessage));

    // then
    System.out.println(violations);
    assertEquals(1, violations.size());
    assertEquals("The product 08K should not contain lot events since it's a kit product.",
        violations.get("createStockCards.arg0[0]"));
  }

  @Test
  public void shouldReturnViolationWhenValidateCreateStockCardsGivenNonKitProductWithEmptyLots()
      throws IOException {
    // given
    Object param = parseParam("nonKitProductWithEmptyLots.json");

    // when
    Map<String, String> violations = forExecutables
        .validateParameters(controller, createStockCards, new Object[]{param}).stream()
        .collect(toMap(v -> v.getPropertyPath().toString(), ConstraintViolation::getMessage));

    // then
    assertEquals(1, violations.size());
    assertEquals("The product 08S01Z should contain at least one lot event since it's not a kit product.",
        violations.get("createStockCards.arg0[0]"));
  }

  @Test
  public void shouldReturnViolationWhenValidateCreateStockCardsGivenNonKitProductWithoutLots()
      throws IOException {
    // given
    Object param = parseParam("nonKitProductWithoutLots.json");

    // when
    Map<String, String> violations = forExecutables
        .validateParameters(controller, createStockCards, new Object[]{param}).stream()
        .collect(toMap(v -> v.getPropertyPath().toString(), ConstraintViolation::getMessage));

    // then
    assertEquals(1, violations.size());
    assertEquals("The product 08S01Z should contain at least one lot event since it's not a kit product.",
        violations.get("createStockCards.arg0[0]"));
  }

  private Object parseParam(String fileName) throws IOException {
    String json = readFromFile(fileName);
    return mapper.readValue(json, stockCardCreateRequestListType);
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

  private class InnerConstraintValidatorFactory implements ConstraintValidatorFactory {

    @Override
    @SuppressWarnings("unchecked")
    public <T extends ConstraintValidator<?, ?>> T getInstance(Class<T> key) {
      if (key == KitProductEmptyLotsValidator.class) {
        return (T) new KitProductEmptyLotsValidator(orderableService);
      }
      if (key == NonKitProductNotEmptyLotsValidator.class) {
        return (T) new NonKitProductNotEmptyLotsValidator(orderableService);
      }
      return NewInstance.action(key, "ConstraintValidator").run();
    }

    @Override
    public void releaseInstance(ConstraintValidator<?, ?> instance) {
      // nothing to do
    }

  }

}
