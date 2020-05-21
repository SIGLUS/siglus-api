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

package org.openlmis.requisition.domain.requisition;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.openlmis.requisition.domain.requisition.RequisitionLineItem.ADDITIONAL_QUANTITY_REQUIRED;
import static org.openlmis.requisition.domain.requisition.RequisitionLineItem.TOTAL_CONSUMED_QUANTITY;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.openlmis.requisition.domain.RequisitionTemplate;
import org.openlmis.requisition.domain.RequisitionTemplateDataBuilder;
import org.openlmis.requisition.domain.SourceType;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.requisition.dto.OrderableDto;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.dto.ProgramOrderableDto;
import org.openlmis.requisition.dto.RequisitionLineItemDto;
import org.openlmis.requisition.dto.stockmanagement.StockAdjustmentDto;
import org.openlmis.requisition.dto.stockmanagement.StockCardRangeSummaryDto;
import org.openlmis.requisition.exception.ValidationMessageException;
import org.openlmis.requisition.i18n.MessageKeys;
import org.openlmis.requisition.testutils.ApprovedProductDtoDataBuilder;
import org.openlmis.requisition.testutils.OrderableDtoDataBuilder;
import org.openlmis.requisition.testutils.ProcessingPeriodDtoDataBuilder;
import org.openlmis.requisition.testutils.StockCardRangeSummaryDtoDataBuilder;

@SuppressWarnings("PMD.TooManyMethods")
public class RequisitionLineItemTest {

  private static final String COLUMN_IDENTIFIER = "I";
  private static final String CONSUMED_TAG = "consumed";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private Requisition initiatedRequisition;

  private UUID programId = UUID.randomUUID();
  private UUID orderableId = UUID.randomUUID();
  private UUID approvedProductId = UUID.randomUUID();
  private Money pricePerPack = Money.of(CurrencyUnit.USD, 5.79);
  private double maxPeriodsOfStock = 7.25;

  @Before
  public void setUp() {
    initiatedRequisition = mockReq(RequisitionStatus.INITIATED);
  }

  @Test
  public void shouldCreateNewRequisitionLineItem() {
    RequisitionLineItem item =
        new RequisitionLineItem(initiatedRequisition,
            createDefaultApprovedProduct(pricePerPack));

    checkResultsOfConstruction(item);
  }

  @Test
  public void shouldResetData() {
    RequisitionLineItem item = createDefaultRequisitionLineItem(
        createDefaultApprovedProduct(null), Collections.emptyList()
    );

    item.resetData();

    assertNull(item.getTotalReceivedQuantity());
    assertNull(item.getTotalLossesAndAdjustments());
    assertNull(item.getStockOnHand());
    assertNull(item.getRequestedQuantityExplanation());
    assertNull(item.getRemarks());
    assertNull(item.getApprovedQuantity());
    assertNull(item.getRequestedQuantity());
    assertNull(item.getTotalConsumedQuantity());
    assertNull(item.getTotal());
    assertNull(item.getRequestedQuantityExplanation());
    assertNull(item.getTotalStockoutDays());
    assertNull(item.getPacksToShip());
    assertNull(item.getTotalCost());
    assertNull(item.getNumberOfNewPatientsAdded());
    assertNull(item.getAdjustedConsumption());
    assertNull(item.getAverageConsumption());
    assertNull(item.getMaximumStockQuantity());
    assertNull(item.getCalculatedOrderQuantity());
    assertEquals(item.getStockAdjustments().size(), 0);
    assertEquals(item.getPreviousAdjustedConsumptions().size(), 0);
    assertNull(item.getCalculatedOrderQuantityIsa());
  }

  @Test
  public void shouldUpdatePacksToShip() {
    // given
    UUID productId = UUID.randomUUID();

    OrderableDto product = new OrderableDtoDataBuilder()
        .withId(productId)
        .withNetContent(1)
        .buildAsDto();

    RequisitionLineItem item = new RequisitionLineItem();
    item.setRequisition(initiatedRequisition);
    item.setRequestedQuantity(5);
    item.setApprovedQuantity(5);
    item.setCalculatedOrderQuantity(5);

    // when
    item.updatePacksToShip(product);

    // then
    assertEquals(5L, item.getPacksToShip().longValue());
  }

  @Test
  public void shouldOnlyUpdateApprovedFieldsWhenRequisitionStatusIsAuthorized() {
    Requisition requisition = mockReq(RequisitionStatus.AUTHORIZED);
    assertOnlyApprovalFieldsEditable(requisition);
  }

  @Test
  public void shouldOnlyUpdateApprovedFieldsWhenRequisitionStatusIsInApproval() {
    Requisition requisition = mockReq(RequisitionStatus.IN_APPROVAL);
    assertOnlyApprovalFieldsEditable(requisition);
  }

  @Test
  public void shouldUpdateSubmissionFields() {
    RequisitionLineItem item = new RequisitionLineItem();
    item.setRequisition(initiatedRequisition);
    item.setStockOnHand(1);
    item.setTotalConsumedQuantity(2);
    item.setBeginningBalance(3);
    item.setTotalReceivedQuantity(4);
    item.setRequestedQuantity(5);
    item.setTotalStockoutDays(6);
    item.setTotal(7);
    item.setNumberOfNewPatientsAdded(1);

    RequisitionLineItem updateItem = new RequisitionLineItem();
    updateItem.setStockOnHand(11);
    updateItem.setTotalConsumedQuantity(22);
    updateItem.setBeginningBalance(33);
    updateItem.setTotalReceivedQuantity(44);
    updateItem.setRequestedQuantity(55);
    updateItem.setTotalStockoutDays(66);
    updateItem.setTotal(77);
    updateItem.setNumberOfNewPatientsAdded(2);

    item.updateFrom(updateItem);

    assertThat(item.getStockOnHand(), is(11));
    assertThat(item.getTotalConsumedQuantity(), is(22));
    assertThat(item.getBeginningBalance(), is(33));
    assertThat(item.getTotalReceivedQuantity(), is(44));
    assertThat(item.getRequestedQuantity(), is(55));
    assertThat(item.getTotalStockoutDays(), is(66));
    assertThat(item.getTotal(), is(77));
    assertThat(item.getNumberOfNewPatientsAdded(), is(2));
  }

  @Test
  public void shouldSetSkippedAsFalseByDefault() {
    RequisitionLineItem item = new RequisitionLineItem();
    item.setRequisition(initiatedRequisition);

    RequisitionLineItem updateItem = new RequisitionLineItem();
    updateItem.setSkipped(null);

    item.updateFrom(updateItem);

    assertFalse(item.getSkipped());
  }

  @Test
  public void shouldClearStockAdjustmentsIfNullWhileUpdating() {
    StockAdjustment adjustment = new StockAdjustmentDataBuilder().build();

    RequisitionLineItem item = new RequisitionLineItem();
    item.setRequisition(initiatedRequisition);
    item.setStockAdjustments(null);

    RequisitionLineItem updateItem = new RequisitionLineItem();
    updateItem.setStockAdjustments(singletonList(adjustment));

    item.updateFrom(updateItem);

    assertThat(item.getStockAdjustments(), hasItems(adjustment));

    List<StockAdjustment> adjustments = new ArrayList<>();
    adjustments.add(new StockAdjustmentDataBuilder().build());
    item.setStockAdjustments(adjustments);
    item.updateFrom(updateItem);

    assertThat(item.getStockAdjustments(), hasItems(adjustment));
  }

  @Test
  public void shouldClearStockAdjustmentsWhileUpdating() {
    RequisitionLineItem item = new RequisitionLineItem();
    item.setRequisition(initiatedRequisition);
    List<StockAdjustment> adjustments = new ArrayList<>();
    adjustments.add(new StockAdjustmentDataBuilder().build());
    item.setStockAdjustments(adjustments);

    RequisitionLineItem updateItem = new RequisitionLineItem();
    updateItem.setStockAdjustments(null);

    item.updateFrom(updateItem);

    assertTrue(isEmpty(item.getStockAdjustments()));
  }

  @Test
  public void shouldReturnApprovedQuantityWhenItIsNotNull() {
    // given
    RequisitionLineItem item = new RequisitionLineItem();
    item.setRequisition(new Requisition());
    item.getRequisition().setStatus(RequisitionStatus.AUTHORIZED);
    item.setApprovedQuantity(4);

    // when
    int quantity = item.getOrderQuantity();

    // then
    assertEquals(4, quantity);
  }

  @Test
  public void shouldReturnZeroIfRequestedQuantityIsNotNullAndApprovedQuantityIsNull() {
    RequisitionLineItem item = new RequisitionLineItem();
    item.setRequisition(new Requisition());
    item.getRequisition().setStatus(RequisitionStatus.AUTHORIZED);
    item.setRequestedQuantity(5);
    item.setApprovedQuantity(null);

    // when
    int quantity = item.getOrderQuantity();

    // then
    assertEquals(0, quantity);
  }

  @Test
  public void shouldReturnZeroWhenApprovedQuantityAndRequestedQuantityIsNull() {
    // given
    Requisition requisition = new Requisition();
    requisition.setTemplate(new RequisitionTemplateDataBuilder().build());

    RequisitionLineItem item = new RequisitionLineItem();
    item.setRequisition(requisition);
    item.getRequisition().setStatus(RequisitionStatus.AUTHORIZED);

    item.setApprovedQuantity(null);
    item.setRequestedQuantity(null);

    // when
    int quantity = item.getOrderQuantity();

    // then
    assertEquals(0, quantity);
  }

  @Test
  public void shouldReturnCalculatedOrderQuantityIsaWhenRequisitionIsStockBasedAndItIsNotNull() {
    // given
    Requisition requisition = new Requisition();
    requisition.setTemplate(new RequisitionTemplateDataBuilder()
        .withPopulateStockOnHandFromStockCards()
        .build());

    RequisitionLineItem item = new RequisitionLineItem();
    item.setRequisition(requisition);
    item.getRequisition().setStatus(RequisitionStatus.SUBMITTED);

    item.setApprovedQuantity(null);
    item.setRequestedQuantity(null);
    item.setCalculatedOrderQuantityIsa(100);

    // when
    int quantity = item.getOrderQuantity();

    // then
    assertEquals(100, quantity);
  }

  @Test
  public void shouldReturnCalculatedOrderQuantityWhenRequisitionIsNotStockBasedAndItIsNotNull() {
    // given
    Requisition requisition = new Requisition();
    requisition.setTemplate(new RequisitionTemplateDataBuilder()
        .build());

    RequisitionLineItem item = new RequisitionLineItem();
    item.setRequisition(requisition);
    item.getRequisition().setStatus(RequisitionStatus.SUBMITTED);

    item.setApprovedQuantity(null);
    item.setRequestedQuantity(null);
    item.setCalculatedOrderQuantity(50);

    // when
    int quantity = item.getOrderQuantity();

    // then
    assertEquals(50, quantity);
  }

  @Test
  public void shouldNotReturnApprovedQuantityIfRequisitionIsSubmitted() {
    // given
    RequisitionLineItem item = new RequisitionLineItem();
    item.setRequisition(new Requisition());
    item.getRequisition().setStatus(RequisitionStatus.SUBMITTED);

    item.setApprovedQuantity(10);
    item.setRequestedQuantity(5);

    // when
    int quantity = item.getOrderQuantity();

    // then
    assertEquals(5, quantity);
  }

  @Test
  public void shouldReturnZeroIfNotPopulatedFromStockCardsAndCalculatedIsNull() {
    Requisition requisition = new Requisition();
    requisition.setTemplate(new RequisitionTemplateDataBuilder()
        .build());

    RequisitionLineItem item = new RequisitionLineItem();
    item.setRequisition(requisition);
    item.getRequisition().setStatus(RequisitionStatus.SUBMITTED);

    item.setApprovedQuantity(null);
    item.setRequestedQuantity(null);
    item.setCalculatedOrderQuantity(null);

    assertEquals(0, item.getOrderQuantity());
  }

  @Test
  public void shouldReturnZeroIfPopulatedFromStockCardsAndCalculatedIsaIsNull() {
    Requisition requisition = new Requisition();
    requisition.setTemplate(new RequisitionTemplateDataBuilder()
        .withPopulateStockOnHandFromStockCards()
        .build());

    RequisitionLineItem item = new RequisitionLineItem();
    item.setRequisition(requisition);
    item.getRequisition().setStatus(RequisitionStatus.SUBMITTED);

    item.setApprovedQuantity(null);
    item.setRequestedQuantity(null);
    item.setCalculatedOrderQuantityIsa(null);

    assertEquals(0, item.getOrderQuantity());
  }

  @Test
  public void shouldBuildFromConstructorAndExport() {
    ProgramDto program = new ProgramDto();
    program.setId(programId);

    List<StockAdjustment> stockAdjustments = new ArrayList<>();
    stockAdjustments.add(new StockAdjustmentDataBuilder().build());
    stockAdjustments.add(new StockAdjustmentDataBuilder().build());
    stockAdjustments.add(new StockAdjustmentDataBuilder().build());
    stockAdjustments.add(new StockAdjustmentDataBuilder().build());

    ApprovedProductDto ftap = createDefaultApprovedProduct(pricePerPack);
    RequisitionLineItem requisitionLineItem = createDefaultRequisitionLineItem(
        ftap, stockAdjustments
    );
    ProgramOrderableDto programOrderable = ftap.getOrderable().getProgramOrderable(programId);
    OrderableDto orderableDto = generateOrderableDto(programOrderable);
    ApprovedProductDto approvedProductDto = generateApprovedProductDto(orderableDto);

    RequisitionLineItemDto dto = new RequisitionLineItemDto();
    requisitionLineItem.export(dto, orderableDto, approvedProductDto);

    assertNotNull(dto);
    assertThat(dto.getOrderableIdentity().getId(), is(requisitionLineItem.getOrderable().getId()));
    assertThat(dto.getId(), is(requisitionLineItem.getId()));
    assertThat(dto.getBeginningBalance(), is(requisitionLineItem.getBeginningBalance()));
    assertThat(dto.getTotalReceivedQuantity(), is(requisitionLineItem.getTotalReceivedQuantity()));
    assertThat(dto.getTotalLossesAndAdjustments(),
        is(requisitionLineItem.getTotalLossesAndAdjustments()));
    assertThat(dto.getStockOnHand(), is(requisitionLineItem.getStockOnHand()));
    assertThat(dto.getRequestedQuantity(), is(requisitionLineItem.getRequestedQuantity()));
    assertThat(dto.getTotalConsumedQuantity(), is(requisitionLineItem.getTotalConsumedQuantity()));
    assertThat(dto.getTotal(), is(requisitionLineItem.getTotal()));
    assertThat(dto.getApprovedQuantity(), is(requisitionLineItem.getApprovedQuantity()));
    assertThat(dto.getTotalStockoutDays(), is(requisitionLineItem.getTotalStockoutDays()));
    assertThat(dto.getRemarks(), is(requisitionLineItem.getRemarks()));
    assertThat(dto.getRequestedQuantityExplanation(),
        is(requisitionLineItem.getRequestedQuantityExplanation()));
    assertThat(dto.getNumberOfNewPatientsAdded(),
        is(requisitionLineItem.getNumberOfNewPatientsAdded()));
    assertThat(dto.getCalculatedOrderQuantityIsa(),
        is(requisitionLineItem.getCalculatedOrderQuantityIsa()));

    StockAdjustment.Importer[] items = stockAdjustments
        .stream()
        .map(item -> {
          StockAdjustmentDto container = new StockAdjustmentDto();
          item.export(container);

          return (StockAdjustment.Importer) container;
        }).toArray(StockAdjustment.Importer[]::new);

    assertThat(dto.getStockAdjustments(), hasSize(items.length));
    assertThat(dto.getStockAdjustments(), hasItems(items));
  }

  @Test
  public void shouldReturnNumberOfNewPatientsAddedWhenItIsNotNull() {
    RequisitionLineItem item = new RequisitionLineItem();
    item.setNumberOfNewPatientsAdded(10);

    assertEquals(10, item.getNumberOfNewPatientsAdded().intValue());
  }

  @Test
  public void shouldReturnFalseWhenSkippedIsNotSet() {
    RequisitionLineItemDto requisitionLineItemDto = new RequisitionLineItemDataBuilder()
        .buildAsDto();
    RequisitionLineItem requisitionLineItem =
        RequisitionLineItem.newRequisitionLineItem(requisitionLineItemDto);
    assertEquals(false, requisitionLineItem.getSkipped());
  }

  @Test
  public void shouldNotSetPreviousAdjustedConsumption() {
    RequisitionLineItemDto requisitionLineItemDto = new RequisitionLineItemDataBuilder()
        .buildAsDto();
    requisitionLineItemDto.setPreviousAdjustedConsumptions(singletonList(1));
    RequisitionLineItem requisitionLineItem =
        RequisitionLineItem.newRequisitionLineItem(requisitionLineItemDto);
    assertEquals(0, requisitionLineItem.getPreviousAdjustedConsumptions().size());
  }

  @Test
  public void shouldSetStockAdjustments() {
    RequisitionLineItemDto requisitionLineItemDto = new RequisitionLineItemDataBuilder()
        .buildAsDto();
    requisitionLineItemDto.setStockAdjustments(singletonList(new StockAdjustmentDto()));
    RequisitionLineItem requisitionLineItem =
        RequisitionLineItem.newRequisitionLineItem(requisitionLineItemDto);
    assertEquals(0, requisitionLineItem.getPreviousAdjustedConsumptions().size());
  }

  @Test
  public void shouldSetAverageConsumption() {
    RequisitionLineItem requisitionLineItem = new RequisitionLineItemDataBuilder()
        .withPreviousAdjustedConsumptions(Lists.newArrayList(1, 2, 3))
        .withAdjustedConsumption(4)
        .build();

    requisitionLineItem.calculateAndSetAverageConsumption();

    assertEquals(3L, requisitionLineItem.getAverageConsumption().longValue());
  }

  @Test
  public void shouldReturnFalseIfSkippedIsNull() {
    RequisitionLineItem requisitionLineItem = new RequisitionLineItemDataBuilder()
        .withSkippedFlag(null)
        .build();

    assertFalse(requisitionLineItem.isLineSkipped());
  }

  @Test
  public void shouldReturnSkippedValueIfSkippedIsNotNull() {
    RequisitionLineItem requisitionLineItem = new RequisitionLineItemDataBuilder()
        .withSkippedFlag(true)
        .build();
    assertTrue(requisitionLineItem.isLineSkipped());
    requisitionLineItem.setSkipped(false);
    assertFalse(requisitionLineItem.isLineSkipped());
  }

  @Test
  public void shouldReturnTrueIfStockOnHandIsNullForTotalConsumedQuantity() {
    RequisitionLineItem item = new RequisitionLineItemDataBuilder().withStockOnHand(null).build();
    assertTrue(item.allRequiredCalcFieldsNotFilled(RequisitionLineItem.TOTAL_CONSUMED_QUANTITY));
  }

  @Test
  public void shouldReturnFalseIfStockOnHandIsNotNullForTotalConsumedQuantity() {
    RequisitionLineItem item = new RequisitionLineItemDataBuilder().withStockOnHand(0).build();
    assertFalse(item.allRequiredCalcFieldsNotFilled(RequisitionLineItem.TOTAL_CONSUMED_QUANTITY));
  }

  @Test
  public void shouldReturnTrueIfTotalConsumedQuantityIsNullForStockOnHand() {
    RequisitionLineItem item = new RequisitionLineItemDataBuilder()
        .withTotalConsumedQuantity(null).build();
    assertTrue(item.allRequiredCalcFieldsNotFilled(RequisitionLineItem.STOCK_ON_HAND));
  }

  @Test
  public void shouldReturnFalseIfTotalConsumedQuantityIsNotNullForStockOnHand() {
    RequisitionLineItem item = new RequisitionLineItemDataBuilder()
        .withTotalConsumedQuantity(0).build();
    assertFalse(item.allRequiredCalcFieldsNotFilled(RequisitionLineItem.STOCK_ON_HAND));
  }

  @Test
  public void shouldReturnFalseForOtherColumns() {
    RequisitionLineItem item = new RequisitionLineItemDataBuilder().build();
    assertFalse(item.allRequiredCalcFieldsNotFilled(RequisitionLineItem.ADJUSTED_CONSUMPTION));
    assertFalse(item.allRequiredCalcFieldsNotFilled(RequisitionLineItem.APPROVED_QUANTITY));
    assertFalse(item.allRequiredCalcFieldsNotFilled(RequisitionLineItem.REMARKS_COLUMN));
    assertFalse(item.allRequiredCalcFieldsNotFilled(""));
  }

  @Test
  public void shouldThrowExceptionIfNoProductWasFound() {
    expectedException.expect(ValidationMessageException.class);
    expectedException.expectMessage(MessageKeys.CAN_NOT_FIND_PROGRAM_DETAILS_FROM_ORDERABLE);

    UUID programId = UUID.randomUUID();
    new RequisitionLineItem(
        new RequisitionDataBuilder().withProgramId(programId).build(),
        new ApprovedProductDtoDataBuilder().buildAsDto());
  }

  @Test
  public void shouldCalculateStockBasedAverageConsumptionWithAdditionalQuantities() {
    UUID orderableId = UUID.randomUUID();
    RequisitionLineItem item = new RequisitionLineItemDataBuilder()
        .withOrderable(orderableId, 1L)
        .withAdditionalQuantityRequired(30)
        .build();
    List<Requisition> previousRequisitions = singletonList(
        new RequisitionDataBuilder()
            .withLineItems(singletonList(
                new RequisitionLineItemDataBuilder()
                    .withAdditionalQuantityRequired(10)
                    .withOrderable(orderableId, 1L)
                    .build()), false)
            .build());
    RequisitionTemplate template = new RequisitionTemplateDataBuilder()
        .withAdditionalQuantityRequiredColumnDisplayed()
        .withStockBasedColumn(TOTAL_CONSUMED_QUANTITY, COLUMN_IDENTIFIER, CONSUMED_TAG)
        .withNumberOfPeriodsToAverage(2)
        .build();
    StockCardRangeSummaryDto summary = new StockCardRangeSummaryDtoDataBuilder()
        .withOrderableId(orderableId)
        .withStockOutDays(2)
        .withTags(ImmutableMap.of(CONSUMED_TAG, -100))
        .buildAsDto();
    List<ProcessingPeriodDto> previousPeriods = asList(
        new ProcessingPeriodDtoDataBuilder().withDurationInMonths(3).buildAsDto(),
        new ProcessingPeriodDtoDataBuilder().withDurationInMonths(3).buildAsDto());

    item.calculateAndSetStockBasedAverageConsumption(
        summary, template, previousPeriods, previousRequisitions);

    assertEquals(new Integer(71), item.getAverageConsumption());
  }

  @Test
  public void shouldCalculateStockBasedAverageConsumptionWithoutAdditionalQuantities() {
    UUID orderableId = UUID.randomUUID();
    RequisitionLineItem item = new RequisitionLineItemDataBuilder()
        .withOrderable(orderableId, 1L)
        .withAdditionalQuantityRequired(30)
        .build();
    List<Requisition> previousRequisitions = singletonList(
        new RequisitionDataBuilder()
            .withLineItems(singletonList(
                new RequisitionLineItemDataBuilder()
                    .withAdditionalQuantityRequired(10)
                    .withOrderable(orderableId, 1L)
                    .build()), false)
            .build());
    RequisitionTemplate template = new RequisitionTemplateDataBuilder()
        .withColumn(ADDITIONAL_QUANTITY_REQUIRED, COLUMN_IDENTIFIER, SourceType.USER_INPUT,
            singleton(SourceType.USER_INPUT), false)
        .withStockBasedColumn(TOTAL_CONSUMED_QUANTITY, COLUMN_IDENTIFIER, CONSUMED_TAG)
        .withNumberOfPeriodsToAverage(2)
        .build();
    StockCardRangeSummaryDto summary = new StockCardRangeSummaryDtoDataBuilder()
        .withOrderableId(orderableId)
        .withStockOutDays(2)
        .withTags(ImmutableMap.of(CONSUMED_TAG, -100))
        .buildAsDto();
    List<ProcessingPeriodDto> previousPeriods = asList(
        new ProcessingPeriodDtoDataBuilder().withDurationInMonths(3).buildAsDto(),
        new ProcessingPeriodDtoDataBuilder().withDurationInMonths(3).buildAsDto());

    item.calculateAndSetStockBasedAverageConsumption(
        summary, template, previousPeriods, previousRequisitions);

    assertEquals(new Integer(51), item.getAverageConsumption());
  }

  private void checkResultsOfConstruction(RequisitionLineItem item) {
    assertEquals(initiatedRequisition, item.getRequisition());
    assertEquals(orderableId, item.getOrderable().getId());
  }

  private void assertOnlyApprovalFieldsEditable(Requisition requisition) {
    RequisitionLineItem requisitionLineItem = new RequisitionLineItemDataBuilder()
        .withOrderable(UUID.randomUUID(), 1L)
        .withRequisition(requisition)
        .withApprovedQuantity(1)
        .withRemarks("Remarks")
        .withStockOnHand(5)
        .build();

    RequisitionLineItem updatedItem = new RequisitionLineItem();
    updatedItem.setRequisition(requisition);
    updatedItem.updateFrom(requisitionLineItem);

    assertEquals(1, updatedItem.getApprovedQuantity().intValue());
    assertEquals("Remarks", updatedItem.getRemarks());
    assertNull(updatedItem.getStockOnHand());
  }

  private RequisitionLineItem createDefaultRequisitionLineItem(
      ApprovedProductDto ftap, List<StockAdjustment> stockAdjustments) {
    return new RequisitionLineItemDataBuilder()
        .withRequisition(initiatedRequisition)
        .withApprovedProduct(ftap)
        .withIdealStockAmount(30)
        .withBeginningBalance(3)
        .withTotalReceivedQuantity(4)
        .withTotalLossesAndAdjustments(0)
        .withStockAdjustments(stockAdjustments)
        .withStockOnHand(1)
        .withRequestedQuantity(5)
        .withTotalConsumedQuantity(2)
        .withTotal(7)
        .withApprovedQuantity(5)
        .withTotalStockoutDays(6)
        .withRemarks("remarks")
        .withRequestedQuantityExplanation("explanation")
        .withNumberOfNewPatientsAdded(10)
        .build();
  }

  private ApprovedProductDto createDefaultApprovedProduct(Money pricePerPack) {
    return new ApprovedProductDtoDataBuilder()
        .withOrderable(
            new OrderableDtoDataBuilder()
                .withId(orderableId)
                .withProgramOrderable(programId, pricePerPack)
                .buildAsDto())
        .withMaxPeriodsOfStock(maxPeriodsOfStock)
        .buildAsDto();
  }

  private OrderableDto generateOrderableDto(ProgramOrderableDto programOrderableDto) {
    programOrderableDto.setProgramId(programId);
    Set<ProgramOrderableDto> products = Sets.newHashSet(programOrderableDto);

    return new OrderableDtoDataBuilder()
        .withId(orderableId)
        .withVersionNumber(1L)
        .withPrograms(products)
        .buildAsDto();
  }

  private ApprovedProductDto generateApprovedProductDto(OrderableDto orderableDto) {
    ApprovedProductDto approvedProductDto = new ApprovedProductDtoDataBuilder()
        .withId(approvedProductId)
        .withOrderable(orderableDto)
        .buildAsDto();
    return approvedProductDto;
  }

  private Requisition mockReq(RequisitionStatus status) {
    Requisition requisition = mock(Requisition.class);
    when(requisition.getStatus()).thenReturn(status);
    when(requisition.isApprovable()).thenReturn(status.duringApproval());
    when(requisition.getProgramId()).thenReturn(programId);
    return requisition;
  }
}
