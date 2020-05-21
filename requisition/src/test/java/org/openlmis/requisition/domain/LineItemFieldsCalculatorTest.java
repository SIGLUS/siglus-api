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

package org.openlmis.requisition.domain;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.openlmis.requisition.domain.SourceType.CALCULATED;
import static org.openlmis.requisition.domain.requisition.LineItemFieldsCalculator.calculateCalculatedOrderQuantityIsa;
import static org.openlmis.requisition.domain.requisition.LineItemFieldsCalculator.calculateStockBasedAverageConsumption;
import static org.openlmis.requisition.domain.requisition.LineItemFieldsCalculator.calculateStockBasedTotalConsumedQuantity;
import static org.openlmis.requisition.domain.requisition.LineItemFieldsCalculator.calculateStockBasedTotalLossesAndAdjustments;
import static org.openlmis.requisition.domain.requisition.LineItemFieldsCalculator.calculateStockBasedTotalReceivedQuantity;
import static org.openlmis.requisition.domain.requisition.LineItemFieldsCalculator.calculateStockBasedTotalStockoutDays;
import static org.openlmis.requisition.domain.requisition.LineItemFieldsCalculator.canSkipLineItem;
import static org.openlmis.requisition.domain.requisition.RequisitionLineItem.MAXIMUM_STOCK_QUANTITY;
import static org.openlmis.requisition.domain.requisition.RequisitionLineItem.TOTAL_CONSUMED_QUANTITY;
import static org.openlmis.requisition.domain.requisition.RequisitionLineItem.TOTAL_LOSSES_AND_ADJUSTMENTS;
import static org.openlmis.requisition.domain.requisition.RequisitionLineItem.TOTAL_RECEIVED_QUANTITY;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.openlmis.requisition.domain.requisition.LineItemFieldsCalculator;
import org.openlmis.requisition.domain.requisition.RequisitionLineItem;
import org.openlmis.requisition.domain.requisition.RequisitionLineItemDataBuilder;
import org.openlmis.requisition.domain.requisition.StockAdjustment;
import org.openlmis.requisition.domain.requisition.StockAdjustmentReason;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.openlmis.requisition.dto.ProgramOrderableDto;
import org.openlmis.requisition.dto.stockmanagement.StockCardRangeSummaryDto;
import org.openlmis.requisition.exception.ValidationMessageException;
import org.openlmis.requisition.i18n.MessageKeys;
import org.openlmis.requisition.testutils.ProcessingPeriodDtoDataBuilder;
import org.openlmis.requisition.testutils.ProgramOrderableDtoDataBuilder;
import org.openlmis.requisition.testutils.StockCardRangeSummaryDtoDataBuilder;

@SuppressWarnings("PMD.TooManyMethods")
public class LineItemFieldsCalculatorTest {

  private static final String CONSUMED_TAG = "consumed";
  private static final String RECEIVED_TAG = "received";
  private static final String ADJUSTMENT_TAG = "adjustment";
  private static final String OTHER_TAG = "other";
  private static final String COLUMN_IDENTIFIER = "I";
  private static final Double MAX_PERIODS_OF_STOCK_7_25 = 7.25;
  private static final Double MAX_PERIODS_OF_STOCK_7 = 7.0;

  private static final int STOCK_ON_HAND = 3789;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void shouldCalculateTotalLossesAndAdjustments() {
    // given
    UUID id1 = UUID.randomUUID();
    UUID id2 = UUID.randomUUID();
    UUID id3 = UUID.randomUUID();
    UUID id4 = UUID.randomUUID();

    StockAdjustmentReason reason1 = mock(StockAdjustmentReason.class);
    StockAdjustmentReason reason2 = mock(StockAdjustmentReason.class);
    StockAdjustmentReason reason3 = mock(StockAdjustmentReason.class);
    StockAdjustmentReason reason4 = mock(StockAdjustmentReason.class);

    StockAdjustment adjustment1 = mock(StockAdjustment.class);
    StockAdjustment adjustment2 = mock(StockAdjustment.class);
    StockAdjustment adjustment3 = mock(StockAdjustment.class);
    StockAdjustment adjustment4 = mock(StockAdjustment.class);

    // when
    doReturn(true).when(reason1).isCreditReasonType();
    doReturn(id1).when(reason1).getReasonId();
    doReturn(true).when(reason2).isCreditReasonType();
    doReturn(id2).when(reason2).getReasonId();
    doReturn(false).when(reason3).isCreditReasonType();
    doReturn(id3).when(reason3).getReasonId();
    doReturn(false).when(reason4).isCreditReasonType();
    doReturn(id4).when(reason4).getReasonId();

    doReturn(5).when(adjustment1).getQuantity();
    doReturn(id1).when(adjustment1).getReasonId();
    doReturn(10).when(adjustment2).getQuantity();
    doReturn(id2).when(adjustment2).getReasonId();
    doReturn(2).when(adjustment3).getQuantity();
    doReturn(id3).when(adjustment3).getReasonId();
    doReturn(7).when(adjustment4).getQuantity();
    doReturn(id4).when(adjustment4).getReasonId();

    RequisitionLineItem requisitionLineItem = new RequisitionLineItemDataBuilder()
        .withStockAdjustments(Lists.newArrayList(adjustment1, adjustment2,
            adjustment3, adjustment4))
        .build();
    requisitionLineItem.setTotalLossesAndAdjustments(
        LineItemFieldsCalculator.calculateTotalLossesAndAdjustments(
            requisitionLineItem, Lists.newArrayList(reason1, reason2, reason3, reason4)));

    // then
    assertThat(requisitionLineItem.getTotalLossesAndAdjustments(), is(6));
  }

  @Test
  public void shouldCalculateBeginningBalanceBasedOnPrevious() {
    RequisitionLineItem previous = new RequisitionLineItem();
    previous.setStockOnHand(STOCK_ON_HAND);

    assertThat(LineItemFieldsCalculator.calculateBeginningBalance(previous), is(STOCK_ON_HAND));
  }

  @Test
  public void shouldSetZeroToBeginningBalanceIfPreviousNotExist() {
    assertThat(LineItemFieldsCalculator.calculateBeginningBalance(null), is(0));
  }

  @Test
  public void shouldSetZeroToBeginningBalanceIfPreviousNotHasData() {
    assertThat(LineItemFieldsCalculator.calculateBeginningBalance(new RequisitionLineItem()),
        is(0));
  }

  @Test
  public void shouldCalculateStockOnHand() {
    RequisitionLineItem requisitionLineItem = new RequisitionLineItemDataBuilder()
        .withTotalLossesAndAdjustments(-100)
        .withTotalConsumedQuantity(200)
        .withTotalReceivedQuantity(500)
        .withBeginningBalance(1000)
        .build();

    assertEquals(1200, LineItemFieldsCalculator.calculateStockOnHand(requisitionLineItem));
  }

  @Test
  public void shouldCalculateStockOnHandIfNull() {
    RequisitionLineItem requisitionLineItem = new RequisitionLineItemDataBuilder()
        .withTotalLossesAndAdjustments(null)
        .withTotalConsumedQuantity(200)
        .withTotalReceivedQuantity(500)
        .withBeginningBalance(1000)
        .build();

    assertEquals(1300, LineItemFieldsCalculator.calculateStockOnHand(requisitionLineItem));
  }

  @Test
  public void shouldCalculateTotal() {
    RequisitionLineItem requisitionLineItem = new RequisitionLineItemDataBuilder()
        .withTotalReceivedQuantity(500)
        .withBeginningBalance(1000)
        .build();

    assertEquals(1500, LineItemFieldsCalculator.calculateTotal(requisitionLineItem));
  }

  @Test
  public void shouldCalculateTotalConsumedQuantity() {
    RequisitionLineItem requisitionLineItem = new RequisitionLineItemDataBuilder()
        .withTotalLossesAndAdjustments(-800)
        .withStockOnHand(300)
        .withTotalReceivedQuantity(500)
        .withBeginningBalance(1000)
        .build();

    assertEquals(400, LineItemFieldsCalculator.calculateTotalConsumedQuantity(requisitionLineItem));
  }

  @Test
  public void shouldCalculateTotalCost() {
    RequisitionLineItem requisitionLineItem = new RequisitionLineItemDataBuilder()
        .withPacksToShip(40L)
        .build();

    ProgramOrderableDto programOrderable = new ProgramOrderableDtoDataBuilder()
        .withProgramId(requisitionLineItem.getRequisition().getProgramId())
        .withPricePerPack(Money.of(CurrencyUnit.USD, 3.25))
        .buildAsDto();

    Money totalCost = LineItemFieldsCalculator
            .calculateTotalCost(requisitionLineItem, programOrderable, CurrencyUnit.USD);

    assertEquals(Money.of(CurrencyUnit.USD, 130), totalCost);
  }

  @Test
  public void shouldCalculateTotalCostAsZeroIfValuesAreMissing() {
    RequisitionLineItem requisitionLineItem = new RequisitionLineItemDataBuilder()
        .withPacksToShip(null)
        .build();

    ProgramOrderableDto programOrderable = new ProgramOrderableDtoDataBuilder()
        .withProgramId(requisitionLineItem.getRequisition().getProgramId())
        .withPricePerPack(Money.of(CurrencyUnit.USD, 3.25))
        .buildAsDto();

    Money totalCost = LineItemFieldsCalculator
        .calculateTotalCost(requisitionLineItem, programOrderable, CurrencyUnit.USD);
    assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY), totalCost.getAmount());

    programOrderable.setPricePerPack(null);
    totalCost = LineItemFieldsCalculator
        .calculateTotalCost(requisitionLineItem, programOrderable, CurrencyUnit.USD);
    assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY), totalCost.getAmount());

    requisitionLineItem.setPacksToShip(20L);
    totalCost = LineItemFieldsCalculator
        .calculateTotalCost(requisitionLineItem, programOrderable, CurrencyUnit.USD);
    assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY), totalCost.getAmount());
  }


  @Test
  public void shouldCalculateAdjustedConsumption() {
    RequisitionLineItem requisitionLineItem = new RequisitionLineItemDataBuilder()
        .withTotalStockoutDays(5)
        .withTotalConsumedQuantity(20)
        .build();

    assertEquals(22, LineItemFieldsCalculator.calculateAdjustedConsumption(requisitionLineItem,
        3, false));
  }

  @Test
  public void shouldCalculateAdjustedConsumptionWithAdditionalQuantityRequested() {
    RequisitionLineItem requisitionLineItem = new RequisitionLineItemDataBuilder()
        .withTotalStockoutDays(5)
        .withAdditionalQuantityRequired(20)
        .withTotalConsumedQuantity(20)
        .build();

    assertEquals(42, LineItemFieldsCalculator.calculateAdjustedConsumption(requisitionLineItem,
        3, true));
  }

  @Test
  public void shouldCalcAdjustedConsumptionWithoutAdditionalQuantityRequested() {
    RequisitionLineItem requisitionLineItem = new RequisitionLineItemDataBuilder()
        .withTotalStockoutDays(5)
        .withAdditionalQuantityRequired(10)
        .withTotalConsumedQuantity(20)
        .build();

    assertEquals(22, LineItemFieldsCalculator.calculateAdjustedConsumption(requisitionLineItem,
        3, false));
  }

  @Test
  public void shouldCalculateAdjustedConsumptionWhenNonStockoutDaysIsZero() {
    RequisitionLineItem requisitionLineItem = new RequisitionLineItemDataBuilder()
        .withTotalStockoutDays(90)
        .withTotalConsumedQuantity(20)
        .build();

    assertEquals(20, LineItemFieldsCalculator.calculateAdjustedConsumption(requisitionLineItem,
        3, false));
  }

  @Test
  public void shouldCalculateAverageConsumption() throws Exception {
    int averageConsumption =
        LineItemFieldsCalculator.calculateAverageConsumption(asList(5, 10, 15));

    assertEquals(10, averageConsumption);
  }

  @Test
  public void shouldReturnAdjustedConsumptionWhenNoPreviousPeriods() {
    int averageConsumption = LineItemFieldsCalculator
        .calculateAverageConsumption(singletonList(5));

    assertEquals(5, averageConsumption);
  }

  @Test
  public void shouldCalculateAverageConsumptionWhenOnePreviousPeriod() {
    int averageConsumption =
        LineItemFieldsCalculator.calculateAverageConsumption(asList(5, 10));

    assertEquals(8, averageConsumption);
  }

  @Test
  public void shouldCalculateMaximumStockQuantityForDefaultOption() {
    RequisitionTemplate template = new RequisitionTemplateDataBuilder()
        .withRequiredColumns()
        .withColumn(MAXIMUM_STOCK_QUANTITY, null, CALCULATED,
            new AvailableRequisitionColumnOption(null, "default", "Default"),
            Sets.newHashSet(CALCULATED), null, true)
        .build();

    RequisitionLineItem item = new RequisitionLineItemDataBuilder()
        .withAverageConsumption(2)
        .build();

    assertThat(
        LineItemFieldsCalculator.calculateMaximumStockQuantity(
            item, template, MAX_PERIODS_OF_STOCK_7_25),
        is(equalTo(15))
    );
  }

  @Test
  public void shouldCalculateMaximumStockQuantityWhenOptionIsNotSelected() {
    RequisitionTemplate template = new RequisitionTemplateDataBuilder()
        .withRequiredColumns()
        .withColumn(MAXIMUM_STOCK_QUANTITY, null, CALCULATED, Sets.newHashSet(CALCULATED))
        .build();

    RequisitionLineItem item = new RequisitionLineItemDataBuilder()
        .withAverageConsumption(2)
        .build();

    assertThat(
        LineItemFieldsCalculator.calculateMaximumStockQuantity(
            item, template, MAX_PERIODS_OF_STOCK_7_25),
        is(equalTo(15))
    );
  }

  @Test
  public void shouldCalculateCalculatedOrderQuantity() {

    RequisitionLineItem item = new RequisitionLineItemDataBuilder()
        .withStockOnHand(5)
        .withMaximumStockQuantity(10)
        .build();

    assertThat(
        LineItemFieldsCalculator.calculateCalculatedOrderQuantity(
            item, new RequisitionTemplate(), null),
        is(equalTo(5))
    );

  }

  @Test
  public void shouldCalculateCalculatedOrderQuantityIfMaximumStockQuantityIsNotSet() {
    RequisitionTemplateColumn column = new RequisitionTemplateColumnDataBuilder()
        .withOption(new AvailableRequisitionColumnOption(null, "default", "Default"))
        .build();

    final RequisitionTemplate template = new RequisitionTemplate(
        ImmutableMap.of(MAXIMUM_STOCK_QUANTITY, column)
    );

    RequisitionLineItem item = new RequisitionLineItemDataBuilder()
        .withEmptyNumericFields()
        .withAverageConsumption(2)
        .withStockOnHand(4)
        .build();

    assertThat(
        LineItemFieldsCalculator.calculateCalculatedOrderQuantity(
            item, template, MAX_PERIODS_OF_STOCK_7),
        is(equalTo(10))
    );

  }

  @Test
  public void shouldCalculateCalculatedOrderQuantityIfStockOnHandIsNotSet() {

    RequisitionLineItem item = new RequisitionLineItemDataBuilder()
        .withStockOnHand(null)
        .withBeginningBalance(5)
        .withTotalReceivedQuantity(0)
        .withTotalLossesAndAdjustments(0)
        .withTotalConsumedQuantity(0)
        .withMaximumStockQuantity(10)
        .build();

    assertThat(
        LineItemFieldsCalculator.calculateCalculatedOrderQuantity(
            item, new RequisitionTemplate(), null),
        is(equalTo(5))
    );

  }

  @Test
  public void shouldReturnNullForCalculatedOrderQuantityIsaIfIsaIsNull() {
    RequisitionLineItem line = new RequisitionLineItemDataBuilder()
        .withIdealStockAmount(null)
        .build();

    assertThat(calculateCalculatedOrderQuantityIsa(line), is(nullValue()));
  }

  @Test
  public void shouldCalculateCalculatedOrderQuantityIsa() {
    RequisitionLineItem line = new RequisitionLineItemDataBuilder()
        .withIdealStockAmount(1000)
        .withStockOnHand(100)
        .build();

    assertThat(calculateCalculatedOrderQuantityIsa(line), is(900));
  }

  @Test
  public void shouldNotAllowSkippingIfNonZeroOnStockColumns() {
    RequisitionLineItem line = new RequisitionLineItemDataBuilder().withStockOnHand(100).build();

    assertThat(canSkipLineItem(line, line), is(false));
  }

  @Test
  public void shouldAllowSkippingIfPreviousRequisitionWasSkipped() {
    RequisitionLineItem previous =
        new RequisitionLineItemDataBuilder().withSkippedFlag(true).build();
    RequisitionLineItem current = new RequisitionLineItem();

    assertThat(canSkipLineItem(current, previous), is(true));
  }

  @Test
  public void shouldNotAllowSkippingIfPreviousRequisitionWasNotSkipped() {
    RequisitionLineItem previous =
        new RequisitionLineItemDataBuilder().withSkippedFlag(false).build();
    RequisitionLineItem current = new RequisitionLineItem();

    assertThat(canSkipLineItem(current, previous), is(false));
  }

  @Test
  public void shouldCalculateTotalConsumedQuantityFromStockCards() {
    UUID orderableId = UUID.randomUUID();
    StockCardRangeSummaryDto stockCardRangeSummaryDto = new StockCardRangeSummaryDtoDataBuilder()
        .withTags(ImmutableMap.of(CONSUMED_TAG, -2, ADJUSTMENT_TAG, 10, OTHER_TAG, 5))
        .withOrderableId(orderableId).buildAsDto();

    RequisitionTemplate template = new RequisitionTemplateDataBuilder()
        .withStockBasedColumn(TOTAL_CONSUMED_QUANTITY, COLUMN_IDENTIFIER, CONSUMED_TAG)
        .build();

    assertEquals(new Integer(2), calculateStockBasedTotalConsumedQuantity(
        template, stockCardRangeSummaryDto, orderableId));
  }

  @Test
  public void shouldReturnZeroConsumedIfThereIsNoSummaryForOrderable() {
    UUID orderableId = UUID.randomUUID();

    RequisitionTemplate template = new RequisitionTemplateDataBuilder()
        .withStockBasedColumn(TOTAL_CONSUMED_QUANTITY, COLUMN_IDENTIFIER, CONSUMED_TAG)
        .build();

    assertEquals(new Integer(0), calculateStockBasedTotalConsumedQuantity(
        template, null, orderableId));
  }

  @Test
  public void shouldThrowExceptionIfConsumedQuantityIsAboveZero() {
    expectedException.expect(ValidationMessageException.class);
    expectedException.expectMessage(MessageKeys.ERROR_VALIDATION_NON_NEGATIVE_NUMBER);

    UUID orderableId = UUID.randomUUID();
    StockCardRangeSummaryDto stockCardRangeSummaryDto = new StockCardRangeSummaryDtoDataBuilder()
        .withTags(ImmutableMap.of(CONSUMED_TAG, 12, ADJUSTMENT_TAG, 10, OTHER_TAG, 5))
        .withOrderableId(orderableId).buildAsDto();

    RequisitionTemplate template = new RequisitionTemplateDataBuilder()
        .withStockBasedColumn(TOTAL_CONSUMED_QUANTITY, COLUMN_IDENTIFIER, CONSUMED_TAG)
        .build();

    calculateStockBasedTotalConsumedQuantity(template, stockCardRangeSummaryDto, orderableId);
  }

  @Test
  public void shouldCalculateTotalReceivedQuantityFromStockCards() {
    UUID orderableId = UUID.randomUUID();
    StockCardRangeSummaryDto stockCardRangeSummaryDto = new StockCardRangeSummaryDtoDataBuilder()
        .withTags(ImmutableMap.of(RECEIVED_TAG, 2, ADJUSTMENT_TAG, 10, OTHER_TAG, 5))
        .withOrderableId(orderableId).buildAsDto();

    RequisitionTemplate template = new RequisitionTemplateDataBuilder()
        .withStockBasedColumn(TOTAL_RECEIVED_QUANTITY, COLUMN_IDENTIFIER, RECEIVED_TAG)
        .build();

    assertEquals(new Integer(2), calculateStockBasedTotalReceivedQuantity(
        template, stockCardRangeSummaryDto, orderableId));
  }

  @Test
  public void shouldReturnZeroReceivedIfThereIsNoSummaryForOrderable() {
    UUID orderableId = UUID.randomUUID();

    RequisitionTemplate template = new RequisitionTemplateDataBuilder()
        .withStockBasedColumn(TOTAL_RECEIVED_QUANTITY, COLUMN_IDENTIFIER, RECEIVED_TAG)
        .build();

    assertEquals(new Integer(0), calculateStockBasedTotalReceivedQuantity(
        template, null, orderableId));
  }

  @Test
  public void shouldThrowExceptionIfReceivedQuantityIsBelowZero() {
    expectedException.expect(ValidationMessageException.class);
    expectedException.expectMessage(MessageKeys.ERROR_VALIDATION_NON_POSITIVE_NUMBER);

    UUID orderableId = UUID.randomUUID();
    StockCardRangeSummaryDto stockCardRangeSummaryDto = new StockCardRangeSummaryDtoDataBuilder()
        .withTags(ImmutableMap.of(RECEIVED_TAG, -12, ADJUSTMENT_TAG, 10, OTHER_TAG, 5))
        .withOrderableId(orderableId).buildAsDto();

    RequisitionTemplate template = new RequisitionTemplateDataBuilder().withStockBasedColumn(
        TOTAL_RECEIVED_QUANTITY, COLUMN_IDENTIFIER, RECEIVED_TAG)
        .build();

    calculateStockBasedTotalReceivedQuantity(template, stockCardRangeSummaryDto, orderableId);
  }

  @Test
  public void shouldGetStockoutDays() {
    StockCardRangeSummaryDto stockCardRangeSummaryDto = new StockCardRangeSummaryDtoDataBuilder()
        .withStockOutDays(1)
        .buildAsDto();

    assertEquals(new Integer(1),
        calculateStockBasedTotalStockoutDays(stockCardRangeSummaryDto, 3));
  }

  @Test
  public void stockoutDaysShouldNotExceedDaysInPeriod() {
    Integer numberOfMonthsInPeriod = 3;
    StockCardRangeSummaryDto stockCardRangeSummaryDto = new StockCardRangeSummaryDtoDataBuilder()
        .withStockOutDays(100)
        .buildAsDto();

    assertEquals(new Integer(30 * numberOfMonthsInPeriod), calculateStockBasedTotalStockoutDays(
        stockCardRangeSummaryDto, numberOfMonthsInPeriod));
  }

  @Test
  public void shouldReturnZeroStockoutDaysIfThereIsNoSummaryForOrderable() {
    assertEquals(new Integer(0), calculateStockBasedTotalStockoutDays(null, 3));
  }

  @Test
  public void shouldCalculateTotalLossesAndAdjustmentsQuantityFromStockCards() {
    StockCardRangeSummaryDto stockCardRangeSummaryDto = new StockCardRangeSummaryDtoDataBuilder()
        .withTags(ImmutableMap.of(RECEIVED_TAG, 2, ADJUSTMENT_TAG, 10, OTHER_TAG, 5))
        .buildAsDto();

    RequisitionTemplate template = new RequisitionTemplateDataBuilder()
        .withStockBasedColumn(TOTAL_LOSSES_AND_ADJUSTMENTS, COLUMN_IDENTIFIER, ADJUSTMENT_TAG)
        .build();

    assertEquals(new Integer(10), calculateStockBasedTotalLossesAndAdjustments(
        template, stockCardRangeSummaryDto));
  }

  @Test
  public void shouldReturnZeroAdjustmentsIfThereIsNoSummaryForOrderable() {
    RequisitionTemplate template = new RequisitionTemplateDataBuilder()
        .withStockBasedColumn(TOTAL_LOSSES_AND_ADJUSTMENTS, COLUMN_IDENTIFIER, ADJUSTMENT_TAG)
        .build();

    assertEquals(new Integer(0), calculateStockBasedTotalLossesAndAdjustments(
        template, null));
  }

  @Test
  public void shouldReturnZeroAsAvgIfThereIsNoSummaryForOrderable() {
    UUID orderableId = UUID.randomUUID();
    RequisitionTemplate template = new RequisitionTemplateDataBuilder()
        .withStockBasedColumn(TOTAL_CONSUMED_QUANTITY, COLUMN_IDENTIFIER, CONSUMED_TAG)
        .build();
    List<ProcessingPeriodDto> periods = asList(
        new ProcessingPeriodDtoDataBuilder().buildAsDto(),
        new ProcessingPeriodDtoDataBuilder().buildAsDto());

    assertEquals(new Integer(0), calculateStockBasedAverageConsumption(
        null, orderableId, template, periods, null));
  }

  @Test
  public void shouldCalculateAverageConsumptionFromStockCards() {
    UUID orderableId = UUID.randomUUID();
    RequisitionTemplate template = new RequisitionTemplateDataBuilder()
        .withStockBasedColumn(TOTAL_CONSUMED_QUANTITY, COLUMN_IDENTIFIER, CONSUMED_TAG)
        .build();
    List<ProcessingPeriodDto> periods = asList(
        new ProcessingPeriodDtoDataBuilder().buildAsDto(),
        new ProcessingPeriodDtoDataBuilder().buildAsDto());
    StockCardRangeSummaryDto stockCardRangeSummaryDto = new StockCardRangeSummaryDtoDataBuilder()
        .withTags(ImmutableMap.of(CONSUMED_TAG, -10, ADJUSTMENT_TAG, 10, OTHER_TAG, 5))
        .withOrderableId(orderableId).buildAsDto();

    assertEquals(new Integer(5), calculateStockBasedAverageConsumption(
        stockCardRangeSummaryDto, orderableId, template, periods, null));
  }

  @Test
  public void shouldCalculateAverageConsumptionForLongPeriodsFromStockCards() {
    UUID orderableId = UUID.randomUUID();
    RequisitionTemplate template = new RequisitionTemplateDataBuilder()
        .withStockBasedColumn(TOTAL_CONSUMED_QUANTITY, COLUMN_IDENTIFIER, CONSUMED_TAG)
        .build();
    List<ProcessingPeriodDto> periods = asList(
        new ProcessingPeriodDtoDataBuilder()
            .withDurationInMonths(2)
            .buildAsDto(),
        new ProcessingPeriodDtoDataBuilder()
            .withDurationInMonths(3)
            .buildAsDto(),
        new ProcessingPeriodDtoDataBuilder()
            .withDurationInMonths(4)
            .buildAsDto());
    StockCardRangeSummaryDto stockCardRangeSummaryDto = new StockCardRangeSummaryDtoDataBuilder()
        .withTags(ImmutableMap.of(CONSUMED_TAG, -80, ADJUSTMENT_TAG, 10, OTHER_TAG, 5))
        .withOrderableId(orderableId).buildAsDto();

    assertEquals(new Integer(27), calculateStockBasedAverageConsumption(
        stockCardRangeSummaryDto, orderableId, template, periods, null));
  }

  @Test
  public void shouldCalculateAverageConsumptionWithAdditionalQuantityFromStockCards() {
    UUID orderableId = UUID.randomUUID();
    RequisitionTemplate template = new RequisitionTemplateDataBuilder()
        .withStockBasedColumn(TOTAL_CONSUMED_QUANTITY, COLUMN_IDENTIFIER, CONSUMED_TAG)
        .build();
    List<ProcessingPeriodDto> periods = asList(
        new ProcessingPeriodDtoDataBuilder().buildAsDto(),
        new ProcessingPeriodDtoDataBuilder().buildAsDto());
    StockCardRangeSummaryDto stockCardRangeSummaryDto = new StockCardRangeSummaryDtoDataBuilder()
        .withTags(ImmutableMap.of(CONSUMED_TAG, -10, ADJUSTMENT_TAG, 10, OTHER_TAG, 5))
        .withOrderableId(orderableId).buildAsDto();

    assertEquals(new Integer(10), calculateStockBasedAverageConsumption(
        stockCardRangeSummaryDto, orderableId, template, periods, 10));
  }
}
