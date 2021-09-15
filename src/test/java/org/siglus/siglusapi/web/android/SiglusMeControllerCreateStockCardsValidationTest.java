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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorFactory;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.executable.ExecutableValidator;
import org.hibernate.validator.internal.util.privilegedactions.NewInstance;
import org.hibernate.validator.messageinterpolation.ResourceBundleMessageInterpolator;
import org.hibernate.validator.resourceloading.PlatformResourceBundleLocator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.siglus.common.dto.referencedata.OrderableDto;
import org.siglus.common.dto.referencedata.UserDto;
import org.siglus.common.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.dto.FacilityDto;
import org.siglus.siglusapi.dto.FacilityTypeDto;
import org.siglus.siglusapi.dto.android.EventTime;
import org.siglus.siglusapi.dto.android.InventoryDetail;
import org.siglus.siglusapi.dto.android.Lot;
import org.siglus.siglusapi.dto.android.LotMovement;
import org.siglus.siglusapi.dto.android.LotStockOnHand;
import org.siglus.siglusapi.dto.android.MovementDetail;
import org.siglus.siglusapi.dto.android.PeriodOfProductMovements;
import org.siglus.siglusapi.dto.android.ProductLotCode;
import org.siglus.siglusapi.dto.android.ProductMovement;
import org.siglus.siglusapi.dto.android.StocksOnHand;
import org.siglus.siglusapi.dto.android.enumeration.MovementType;
import org.siglus.siglusapi.dto.android.group.SelfCheckGroup;
import org.siglus.siglusapi.dto.android.request.StockCardCreateRequest;
import org.siglus.siglusapi.dto.android.sequence.PerformanceSequence;
import org.siglus.siglusapi.dto.android.validator.stockcard.FacilityApprovedProductValidator;
import org.siglus.siglusapi.dto.android.validator.stockcard.KitProductEmptyLotsValidator;
import org.siglus.siglusapi.dto.android.validator.stockcard.LotStockConsistentWithExistedValidator;
import org.siglus.siglusapi.dto.android.validator.stockcard.SupportReasonNameValidator;
import org.siglus.siglusapi.repository.StockManagementRepository;
import org.siglus.siglusapi.service.SiglusOrderableService;
import org.siglus.siglusapi.service.SiglusValidReasonAssignmentService;
import org.siglus.siglusapi.service.SiglusValidSourceDestinationService;
import org.siglus.siglusapi.service.android.StockCardCreateService;
import org.siglus.siglusapi.service.android.StockCardSearchService;
import org.siglus.siglusapi.service.android.context.StockCardCreateContextHolder;
import org.siglus.siglusapi.service.client.SiglusApprovedProductReferenceDataService;
import org.siglus.siglusapi.util.SupportedProgramsHelper;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.TooManyMethods", "unused"})
public class SiglusMeControllerCreateStockCardsValidationTest extends FileBasedTest {

  private static final String MAY_NOT_BE_EMPTY = "may not be empty";
  private static final String MAY_NOT_BE_NULL = "may not be null";
  private static final String MUST_BE_POSITIVE = "must be greater than or equal to 0";
  private final UUID facilityId = UUID.randomUUID();
  private final UUID userId = UUID.randomUUID();
  private final UUID supportProgramId = UUID.randomUUID();
  private final String supportProgramCode = "programCode";
  private final UUID facilityTypeId = UUID.randomUUID();

  @InjectMocks
  private StockCardCreateService stockCardCreateService;

  @InjectMocks
  private StockCardSearchService stockCardSearchService;

  @Mock
  private SiglusOrderableService orderableService;

  @Mock
  private SiglusAuthenticationHelper authHelper;

  @Mock
  private StockManagementRepository stockManagementRepository;

  @Mock
  private SiglusOrderableService siglusOrderableService;

  @Mock
  private SupportedProgramsHelper programsHelper;

  @Mock
  private ProgramReferenceDataService programDataService;

  @Mock
  private SiglusApprovedProductReferenceDataService approvedProductDataService;

  @InjectMocks
  private StockCardCreateContextHolder holder;

  @Mock
  private SiglusValidReasonAssignmentService validReasonAssignmentService;

  @Mock
  private SiglusValidSourceDestinationService siglusValidSourceDestinationService;

  private final ObjectMapper mapper = new ObjectMapper();

  private JavaType stockCardCreateRequestListType;

  private ExecutableValidator forExecutables;

  private Method method;

  @Before
  public void setup() throws NoSuchMethodException {
    UserDto userDto = new UserDto();
    userDto.setHomeFacilityId(facilityId);
    userDto.setId(userId);
    when(authHelper.getCurrentUser()).thenReturn(userDto);
    Locale.setDefault(Locale.ENGLISH);
    mapper.registerModule(new JavaTimeModule());
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    mapper.configure(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS, false);
    stockCardCreateRequestListType = mapper.getTypeFactory()
        .constructCollectionType(List.class, StockCardCreateRequest.class);
    ResourceBundleMessageInterpolator messageInterpolator =
        new ResourceBundleMessageInterpolator(new PlatformResourceBundleLocator("messages/messages"));
    forExecutables = Validation.byDefaultProvider().configure()
        .constraintValidatorFactory(new InnerConstraintValidatorFactory())
        .messageInterpolator(messageInterpolator)
        .buildValidatorFactory().getValidator().forExecutables();
    method = StockCardCreateService.class.getDeclaredMethod("createStockCards", List.class);
    orderableService = mock(SiglusOrderableService.class);
    OrderableDto notKitProduct = mock(OrderableDto.class);
    when(notKitProduct.getPrograms()).thenReturn(emptySet());
    OrderableDto kitProduct = mock(OrderableDto.class);
    when(kitProduct.getPrograms()).thenReturn(emptySet());
    when(kitProduct.getIsKit()).thenReturn(true);
    when(orderableService.getOrderableByCode(any())).then(invocation -> {
      String productCode = invocation.getArgumentAt(0, String.class);
      if ("08K".equals(productCode)) {
        return kitProduct;
      }
      if ("08A".equals(productCode)) {
        return null;
      }
      return notKitProduct;
    });
    LotStockOnHand stock1 = LotStockOnHand.builder().productCode("08U").lotCode("SEM-LOTE-02A01-082021")
        .stockOnHand(0).occurredDate(LocalDate.of(2021, 6, 15)).build();
    LotStockOnHand stock2 = LotStockOnHand.builder().productCode("08U").lotCode("SEM-LOTE-02A01-062021")
        .stockOnHand(100).occurredDate(LocalDate.of(2021, 6, 15)).build();
    StocksOnHand stocksOnHand = mock(StocksOnHand.class);
    when(stocksOnHand.findInventory(any())).thenReturn(null);
    InventoryDetail inventoryDetail = InventoryDetail.of(200,
        EventTime.fromRequest(LocalDate.of(2021, 8, 6), Instant.parse("2021-08-06T08:52:42.063Z")));
    when(stocksOnHand.findInventory(eq(ProductLotCode.of("08O05Y", "SME-LOTE-08O05Y-072021"))))
        .thenReturn(inventoryDetail);
    when(stockManagementRepository.getStockOnHand(any())).thenReturn(stocksOnHand);
    MovementDetail movementDetail = new MovementDetail(-200, MovementType.ISSUE, "PUB_PHARMACY");
    ProductMovement movement0 = ProductMovement.builder()
        .productCode("08O05Y")
        .eventTime(EventTime.fromRequest(LocalDate.of(2021, 8, 6), Instant.parse("2021-08-06T08:48:58.690Z")))
        .movementDetail(movementDetail)
        .requestedQuantity(200)
        .stockQuantity(0)
        .lotMovements(singletonList(newLotMovement(movementDetail, 0)))
        .build();
    movementDetail = new MovementDetail(0, MovementType.PHYSICAL_INVENTORY, null);
    ProductMovement movement1 = ProductMovement.builder()
        .productCode("08O05Y")
        .eventTime(EventTime.fromRequest(LocalDate.of(2021, 8, 6), Instant.parse("2021-08-06T08:49:13.784Z")))
        .movementDetail(movementDetail)
        .lotMovements(emptyList())
        .stockQuantity(0)
        .build();
    movementDetail = new MovementDetail(200, MovementType.RECEIVE, "DISTRICT_DDM");
    ProductMovement movement2 = ProductMovement.builder()
        .productCode("08O05Y")
        .eventTime(EventTime.fromRequest(LocalDate.of(2021, 8, 6), Instant.parse("2021-08-06T08:52:42.063Z")))
        .movementDetail(movementDetail)
        .lotMovements(singletonList(newLotMovement(movementDetail, 200)))
        .stockQuantity(200)
        .build();
    when(stockManagementRepository.getAllProductMovements(any(), any(LocalDate.class)))
        .thenReturn(new PeriodOfProductMovements(asList(movement0, movement1, movement2), stocksOnHand));
    mockFacilitySupportOrdrables();
  }

  private LotMovement newLotMovement(MovementDetail movementDetail, Integer stockQuantity) {
    return LotMovement.builder().lot(Lot.of("SME-LOTE-08O05Y-072021", java.sql.Date.valueOf("2021-07-31")))
        .stockQuantity(stockQuantity).movementDetail(movementDetail).build();
  }

  @Test
  public void shouldReturnViolationWhenValidateCreateStockCardsGivenEmptyList() throws IOException {
    // given
    String json = "[]";
    Object param = mapper.readValue(json, stockCardCreateRequestListType);

    // when
    Map<String, List<String>> violations = executeValidation(param);

    // then
    assertEquals(1, violations.size());
    assertViolation(MAY_NOT_BE_EMPTY, "createStockCards.arg0", violations);
  }

  @Test
  public void shouldReturnViolationWhenValidateCreateStockCardsGivenEmptyRequest()
      throws IOException {
    // given
    String json = "[{}]";
    Object param = mapper.readValue(json, stockCardCreateRequestListType);

    // when
    Map<String, List<String>> violations = executeValidation(param);

    // then
    assertEquals(7, violations.size());
    // could be empty when product is kit
    // assertViolation(MAY_NOT_BE_EMPTY,        "createStockCards.arg0[0].lotEvents", violations);
    assertViolation(MAY_NOT_BE_NULL, "createStockCards.arg0[0].occurredDate", violations);
    assertViolation(MAY_NOT_BE_NULL, "createStockCards.arg0[0].recordedAt", violations);
    assertViolation(MAY_NOT_BE_EMPTY, "createStockCards.arg0[0].productCode", violations);
    assertViolation(MAY_NOT_BE_NULL, "createStockCards.arg0[0].quantity", violations);
    assertViolation(MAY_NOT_BE_NULL, "createStockCards.arg0[0].stockOnHand", violations);
    assertViolation(MAY_NOT_BE_EMPTY, "createStockCards.arg0[0].type", violations);
    assertViolation(MAY_NOT_BE_NULL, "createStockCards.arg0[0].lotEvents", violations);
  }

  @Test
  public void shouldReturnViolationWhenValidateCreateStockCardsGivenEmptyLotList()
      throws IOException {
    // given
    Object param = parseParam("emptyLotList.json");

    // when
    Map<String, List<String>> violations = executeValidation(param);

    // then
    assertEquals(4, violations.size());
    assertViolation(MAY_NOT_BE_NULL, "createStockCards.arg0[0].lotEvents[0].stockOnHand", violations);
    assertViolation(MAY_NOT_BE_EMPTY, "createStockCards.arg0[0].lotEvents[0].lotCode", violations);
    assertViolation(MAY_NOT_BE_NULL, "createStockCards.arg0[0].lotEvents[0].expirationDate", violations);
    assertViolation(MAY_NOT_BE_NULL, "createStockCards.arg0[0].lotEvents[0].quantity", violations);
  }

  @Test
  public void shouldReturnViolationWhenValidateCreateStockCardsGivenNegativeNumber()
      throws IOException {
    // given
    Object param = parseParam("negativeNumber.json");

    // when
    Map<String, List<String>> violations = executeValidation(param);

    // then
    assertEquals(2, violations.size());
    assertViolation(MUST_BE_POSITIVE, "createStockCards.arg0[0].stockOnHand", violations);
    assertViolation(MUST_BE_POSITIVE, "createStockCards.arg0[0].lotEvents[0].stockOnHand", violations);
  }

  @Test
  public void shouldReturnViolationWhenValidateCreateStockCardsGivenInconsistentProductSoh()
      throws IOException {
    // given
    Object param = parseParam("inconsistentProductSoh.json");

    // when
    Map<String, List<String>> violations = executeValidation(param);

    // then
    assertEquals(2, violations.size());
    assertViolation("Adjust quantity of [08S01Z] cannot be larger than SOH.",
        "createStockCards.arg0[0]", violations);
  }

  @Test
  public void shouldReturnViolationWhenValidateCreateStockCardsGivenInconsistentLotQuantitySum()
      throws IOException {
    // given
    Object param = parseParam("inconsistentLotQuantitySum.json");

    // when
    Map<String, List<String>> violations = executeValidation(param);

    // then
    assertEquals(2, violations.size());
    assertViolation(
        "The stock card for 08S01Z on 2021-06-17(at 2021-06-17T14:20:56Z) is inconsistent with its lot events.",
        "createStockCards.arg0[0]", violations);
  }

  @Test
  public void shouldReturnViolationWhenValidateCreateStockCardsGivenInconsistentLotSohSum()
      throws IOException {
    // given
    Object param = parseParam("inconsistentLotSohSum.json");

    // when
    Map<String, List<String>> violations = executeValidation(param);

    // then
    assertEquals(2, violations.size());

    assertViolation(
        "The stock card for 08S01Z on 2021-06-17(at 2021-06-17T14:20:56Z) is inconsistent with its lot events.",
        "createStockCards.arg0[0]", violations);
  }

  @Test
  public void shouldReturnViolationWhenValidateCreateStockCardsGivenInconsistentProductsByGap()
      throws IOException {
    // given
    Object param = parseParam("inconsistentProductsByGap.json");

    // when
    Map<String, List<String>> violations = executeValidation(param);

    // then
    assertEquals(4, violations.size());
    assertViolation("The product 08S01Z is not consistent by gap.",
        "createStockCards.arg0", violations);
  }

  @Test
  public void shouldReturnViolationWhenValidateCreateStockCardsGivenInconsistentProductsByEach()
      throws IOException {
    // given
    Object param = parseParam("inconsistentProductsByEach.json");

    // when
    Map<String, List<String>> violations = executeValidation(param);

    // then
    assertEquals(5, violations.size());
    assertViolation("The product 08S01Z is not consistent on 2021-06-17(at 2021-06-17T13:20:56Z).",
        "createStockCards.arg0", violations);
  }

  @Test
  public void shouldReturnViolationWhenValidateCreateStockCardsGivenInconsistentLotsByGap()
      throws IOException {
    // given
    Object param = parseParam("inconsistentLotsByGap.json");

    // when
    Map<String, List<String>> violations = executeValidation(param);

    // then
    assertEquals(4, violations.size());
    assertViolation("SOH of SEM-LOTE-02A01-062021(08S01Z) is incorrect.",
        "createStockCards.arg0", violations);
  }

  @Test
  public void shouldReturnViolationWhenValidateCreateStockCardsGivenInconsistentLotsByEach()
      throws IOException {
    // given
    Object param = parseParam("inconsistentLotsByEach.json");

    // when
    Map<String, List<String>> violations = executeValidation(param);

    // then
    assertEquals(5, violations.size());

    assertViolation(
        "SOH of SEM-LOTE-02A01-062021(08S01Z) is incorrect.",
        "createStockCards.arg0", violations);
  }

  @Test
  public void shouldReturnViolationWhenValidateCreateStockCardsGivenInconsistentLotsOverProduct()
      throws IOException {
    // given
    Object param = parseParam("inconsistentLotsOverProduct.json");

    // when
    Map<String, List<String>> violations = executeValidation(param);

    // then
    assertEquals(5, violations.size());
    assertViolation("The product 08S01Z is not consistent "
            + "since it has less SOH than the sum its lots' on 2021-06-17(at 2021-06-17T14:20:56Z).",
        "createStockCards.arg0", violations);
  }

  @Test
  public void shouldReturnViolationWhenValidateCreateStockCardsGivenProductNotExisted()
      throws IOException {
    // given
    Object param = parseParam("productNotExisted.json");

    // when
    Map<String, List<String>> violations = executeValidation(param);

    // then
    assertEquals(1, violations.size());
    assertViolation("08A is not approved by the facility " + facilityId + ".",
        "createStockCards.arg0[0]", violations);
  }

  @Test
  public void shouldReturnViolationWhenValidateCreateStockCardsGivenKitProductWithLots()
      throws IOException {
    // given
    Object param = parseParam("kitProductWithLots.json");

    // when
    Map<String, List<String>> violations = executeValidation(param);

    // then
    assertEquals(1, violations.size());
    assertViolation("Kit(08K) should not contain any lot events.",
        "createStockCards.arg0[0]", violations);
  }

  @Test
  public void shouldReturnNoViolationWhenValidateCreateStockCardsGivenValid1()
      throws IOException {
    // given
    Object param = parseParam("advanceValid1.json");

    // when
    Map<String, List<String>> violations = executeValidation(param);

    // then
    assertEquals(0, violations.size());
  }

  @Test
  public void shouldReturnNoViolationWhenValidateCreateStockCardsGivenValid2()
      throws IOException {
    // given
    Object param = parseParam("advanceValid2.json");

    // when
    Map<String, List<String>> violations = executeValidation(param);

    // then
    assertEquals(0, violations.size());
  }

  @Test
  public void shouldReturnNoViolationWhenValidateCreateStockCardsGivenValid3()
      throws IOException {
    // given
    Object param = parseParam("advanceValid3.json");

    // when
    Map<String, List<String>> violations = executeValidation(param);

    // then
    assertEquals(0, violations.size());
  }

  @Test
  public void shouldReturnViolationWhenValidateCreateStockCardsGivenViolation1()
      throws IOException {
    // given
    Object param = parseParam("advanceViolation1.json");

    // when
    Map<String, List<String>> violations = executeValidation(param);

    // then
    assertEquals(1, violations.size());
    assertViolation("In product(08O05Y) the record on 2021-08-06(recorded at 2021-08-06T08:49:03.784Z) "
            + "is earlier than the latest record in DB.",
        "createStockCards.arg0", violations);
  }

  @Test
  public void shouldReturnViolationWhenValidateCreateStockCardsGivenViolation2()
      throws IOException {
    // given
    Object param = parseParam("advanceViolation2.json");

    // when
    Map<String, List<String>> violations = executeValidation(param);

    // then
    assertEquals(1, violations.size());
    assertViolation("In product(08O05Y) the record on 2021-08-05(recorded at 2021-08-05T08:49:13.784Z) "
            + "is earlier than the latest record in DB.",
        "createStockCards.arg0", violations);
  }

  @Test
  public void shouldReturnViolationWhenValidateCreateStockCardsGivenViolation3()
      throws IOException {
    // given
    Object param = parseParam("advanceViolation3.json");

    // when
    Map<String, List<String>> violations = executeValidation(param);

    // then
    assertEquals(1, violations.size());
    assertViolation("In product(08O05Y) the record on 2021-08-05(recorded at 2021-08-05T08:49:13.784Z) "
            + "is earlier than the latest record in DB.",
        "createStockCards.arg0", violations);
  }

  @Test
  public void shouldReturnViolationWhenValidateCreateStockCardsGivenViolation4Product()
      throws IOException {
    // given
    Object param = parseParam("advanceViolation4Product.json");

    // when
    Map<String, List<String>> violations = executeValidation(param);

    // then
    assertEquals(1, violations.size());

    assertViolation(
        "In product(08O05Y) the stockOnHand of the record on 2021-08-06(recorded at 2021-08-06T08:52:42.063Z) "
            + "should be 200 but is 100.",
        "createStockCards.arg0", violations);
  }

  @Test
  public void shouldReturnViolationWhenValidateCreateStockCardsGivenViolation4Lot()
      throws IOException {
    // given
    Object param = parseParam("advanceViolation4Lot.json");

    // when
    Map<String, List<String>> violations = executeValidation(param);

    // then
    assertEquals(1, violations.size());

    assertViolation(
        "In product(08O05Y) the lotCode of the record on 2021-08-06(recorded at 2021-08-06T08:52:42.063Z) "
            + "should be SME-LOTE-08O05Y-072021 but is SEM-LOTE-08O05Y-072021.",
        "createStockCards.arg0", violations);
  }

  @Test
  public void shouldReturnViolationWhenValidateCreateStockCardsGivenViolation5()
      throws IOException {
    // given
    Object param = parseParam("advanceViolation5.json");

    // when
    Map<String, List<String>> violations = executeValidation(param);

    // then
    assertEquals(1, violations.size());

    assertViolation(
        "In product(08O05Y) the stock on hand before the adjustment of the record on "
            + "2021-08-07(recorded at 2021-08-07T08:52:42.063Z) should be 200 but is 400.",
        "createStockCards.arg0", violations);
  }

  @Test
  public void shouldReturnViolationWhenValidateCreateStockCardsGivenViolation6()
      throws IOException {
    // given
    Object param = parseParam("advanceViolation6.json");

    // when
    Map<String, List<String>> violations = executeValidation(param);

    // then
    assertEquals(1, violations.size());
    assertViolation(
        "SOH of SME-LOTE-08O05Y-072021(08O05Y) does not match latest record in DB.",
        "createStockCards.arg0", violations);
  }

  @Test
  public void shouldReturnViolationWhenValidateCreateStockCardsGivenViolation6Bug1()
      throws IOException {
    // given
    Object param = parseParam("advanceViolation6Bug1.json");

    // when
    Map<String, List<String>> violations = executeValidation(param);

    // then
    assertEquals(0, violations.size());
  }

  private void assertViolation(String message, String key, Map<String, List<String>> violations) {
    assertTrue(violations.containsKey(key));
    assertNotNull(violations.get(key));
    assertTrue(violations.get(key).contains(message));
  }

  private Object parseParam(String fileName) throws IOException {
    String json = readFromFile(fileName);
    return mapper.readValue(json, stockCardCreateRequestListType);
  }

  private Map<String, List<String>> executeValidation(Object... params) {
    holder.initContext(mockFacilityDto(), LocalDate.MIN);
    return forExecutables
        .validateParameters(stockCardCreateService, method, params, SelfCheckGroup.class, PerformanceSequence.class)
        .stream()
        .collect(groupingBy(v -> v.getPropertyPath().toString(), mapping(ConstraintViolation::getMessage, toList())));
  }

  private class InnerConstraintValidatorFactory implements ConstraintValidatorFactory {

    @Override
    @SuppressWarnings("unchecked")
    public <T extends ConstraintValidator<?, ?>> T getInstance(Class<T> key) {
      if (key == KitProductEmptyLotsValidator.class) {
        return (T) new KitProductEmptyLotsValidator(orderableService);
      }
      if (key == LotStockConsistentWithExistedValidator.class) {
        return (T) new LotStockConsistentWithExistedValidator(stockCardSearchService);
      }
      if (key == SupportReasonNameValidator.class) {
        return (T) new SupportReasonNameValidator(orderableService);
      }
      if (key == FacilityApprovedProductValidator.class) {
        return (T) new FacilityApprovedProductValidator(authHelper);
      }
      return NewInstance.action(key, "ConstraintValidator").run();
    }

    @Override
    public void releaseInstance(ConstraintValidator<?, ?> instance) {
      // nothing to do
    }

  }

  private void mockFacilitySupportOrdrables() {
    org.openlmis.requisition.dto.OrderableDto orderableDto1 = new org.openlmis.requisition.dto.OrderableDto();
    orderableDto1.setProductCode("08O05Y");
    orderableDto1.setPrograms(emptySet());
    ApprovedProductDto approvedProductDto1 = new ApprovedProductDto();
    approvedProductDto1.setOrderable(orderableDto1);
    List<ApprovedProductDto> facilitySupportOrderables = new ArrayList<>();
    facilitySupportOrderables.add(approvedProductDto1);

    when(programsHelper.findUserSupportedPrograms())
        .thenReturn(Stream.of(supportProgramId).collect(Collectors.toSet()));
    when(programDataService.findOne(supportProgramId)).thenReturn(buildSupportProgramDto());
    when(approvedProductDataService.getApprovedProducts(facilityId, supportProgramId, emptyList()))
        .thenReturn(facilitySupportOrderables);
  }

  private ProgramDto buildSupportProgramDto() {
    ProgramDto programDto = new ProgramDto();
    programDto.setId(supportProgramId);
    programDto.setCode(supportProgramCode);
    return programDto;
  }

  private FacilityDto mockFacilityDto() {
    FacilityTypeDto facilityTypeDto = new FacilityTypeDto();
    facilityTypeDto.setId(facilityTypeId);
    FacilityDto facilityDto = new FacilityDto();
    facilityDto.setId(facilityId);
    facilityDto.setType(facilityTypeDto);
    return facilityDto;
  }
}
