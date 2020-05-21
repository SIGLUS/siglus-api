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
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.openlmis.requisition.CurrencyConfig.currencyCode;
import static org.openlmis.requisition.domain.requisition.Requisition.STOCK_ON_HAND;
import static org.openlmis.requisition.domain.requisition.Requisition.TOTAL_CONSUMED_QUANTITY;
import static org.openlmis.requisition.domain.requisition.RequisitionLineItem.CALCULATED_ORDER_QUANTITY_ISA;
import static org.openlmis.requisition.domain.requisition.RequisitionLineItem.SKIPPED_COLUMN;
import static org.openlmis.requisition.domain.requisition.RequisitionLineItem.TOTAL_LOSSES_AND_ADJUSTMENTS;
import static org.openlmis.requisition.domain.requisition.RequisitionLineItem.TOTAL_STOCKOUT_DAYS;
import static org.openlmis.requisition.dto.OrderableDto.COMMODITY_TYPE_IDENTIFIER;
import static org.openlmis.requisition.dto.ProofOfDeliveryStatus.CONFIRMED;
import static org.openlmis.requisition.dto.ProofOfDeliveryStatus.INITIATED;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_FIELD_MUST_HAVE_VALUES;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_PROGRAM_DOES_NOT_ALLOW_SKIP;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_SKIP_FAILED_EMERGENCY;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_SKIP_FAILED_WRONG_STATUS;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_VALIDATION_NON_NEGATIVE_NUMBER;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_VALIDATION_NON_POSITIVE_NUMBER;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.hamcrest.Matcher;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.openlmis.requisition.domain.RequisitionTemplate;
import org.openlmis.requisition.domain.RequisitionTemplateColumn;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.requisition.dto.ObjectReferenceDto;
import org.openlmis.requisition.dto.OrderableDto;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.openlmis.requisition.dto.ProofOfDeliveryDto;
import org.openlmis.requisition.dto.ProofOfDeliveryLineItemDto;
import org.openlmis.requisition.dto.SupervisoryNodeDto;
import org.openlmis.requisition.dto.SupplyLineDto;
import org.openlmis.requisition.dto.VersionIdentityDto;
import org.openlmis.requisition.dto.stockmanagement.StockCardRangeSummaryDto;
import org.openlmis.requisition.exception.ValidationMessageException;
import org.openlmis.requisition.testutils.ApprovedProductDtoDataBuilder;
import org.openlmis.requisition.testutils.DtoGenerator;
import org.openlmis.requisition.testutils.OrderableDtoDataBuilder;
import org.openlmis.requisition.testutils.ProcessingPeriodDtoDataBuilder;
import org.openlmis.requisition.testutils.StockCardRangeSummaryDtoDataBuilder;
import org.openlmis.requisition.testutils.SupplyLineDtoDataBuilder;
import org.openlmis.requisition.utils.Message;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@PrepareForTest({LineItemFieldsCalculator.class})
@RunWith(PowerMockRunner.class)
@SuppressWarnings("PMD.TooManyMethods")
public class RequisitionTest {

  private static final CurrencyUnit CURRENCY_UNIT = CurrencyUnit.USD;
  private static final int ADJUSTED_CONSUMPTION = 1;
  private static final int AVERAGE_CONSUMPTION = 1;
  private static final Money TOTAL_COST = Money.of(CURRENCY_UNIT, 5);
  private static final int MONTHS_IN_PERIOD = 1;
  private static final int CALCULATED_ORDER_QUANTITY = 5;
  private static final int REQUESTED_QUANTITY = 5;
  private static final int STOCK_ON_HAND_VALUE = 10;

  private static final String TOTAL_RECEIVED_QUANTITY = "totalReceivedQuantity";
  private static final String CONSUMED_TAG = "consumed";
  private static final String RECEIVED_TAG = "received";
  private static final String ADJUSTMENT_TAG = "adjustment";

  private Requisition requisition;
  private RequisitionLineItem requisitionLineItem;
  private StockCardRangeSummaryDto stockCardRangeSummaryDto;

  private ApprovedProductDto product;
  private OrderableDto orderable;
  private Map<VersionIdentityDto, OrderableDto> orderables;
  private Map<VersionIdentityDto, ApprovedProductDto> approvedProducts;

  private ProcessingPeriodDto period;

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Mock
  private RequisitionTemplate template;

  @Mock
  private RequisitionTemplateColumn totalReceivedQuantity;

  @Mock
  private RequisitionTemplateColumn totalConsumedQuantity;

  @Mock
  private RequisitionTemplateColumn totalLossesAndAdjustments;

  @Mock
  private RequisitionTemplateColumn totalStockoutDays;

  @Before
  public void setUp() {
    requisitionLineItem = new RequisitionLineItemDataBuilder()
        .withId(UUID.randomUUID())
        .withRequestedQuantity(REQUESTED_QUANTITY)
        .withStockOnHand(20)
        .withOrderable(UUID.randomUUID(), 1L)
        .withCalculatedOrderQuantity(CALCULATED_ORDER_QUANTITY)
        .withRequisition(requisition)
        .build();

    requisition = new RequisitionDataBuilder()
        .addLineItem(requisitionLineItem, false)
        .withNumberOfMonthsInPeriod(MONTHS_IN_PERIOD)
        .withDatePhysicalStockCountCompleted((DatePhysicalStockCountCompleted) null)
        .buildInitiatedRegularRequisition();

    orderable = new OrderableDtoDataBuilder()
        .withId(requisitionLineItem.getOrderable().getId())
        .withVersionNumber(requisitionLineItem.getOrderable().getVersionNumber())
        .withProgramOrderable(requisition.getProgramId(), true)
        .buildAsDto();
    orderables = Maps.newHashMap(ImmutableMap.of(orderable.getIdentity(), orderable));

    Map<String, Integer> tags = new HashMap<>();
    tags.put(RECEIVED_TAG, 100);
    tags.put(CONSUMED_TAG, -200);
    tags.put(ADJUSTMENT_TAG, -50);
    stockCardRangeSummaryDto = new StockCardRangeSummaryDtoDataBuilder()
        .withOrderableId(requisitionLineItem.getOrderable().getId())
        .withStockOutDays(3)
        .withTags(tags)
        .buildAsDto();

    product = new ApprovedProductDtoDataBuilder()
        .withOrderable(orderable)
        .buildAsDto();

    approvedProducts = Maps.newHashMap(ImmutableMap.of(product.getIdentity(), product));

    period = new ProcessingPeriodDtoDataBuilder().buildAsDto();
  }

  @Test
  public void shouldChangeStatusToRejectedAfterReject() {
    // given
    requisition.setTemplate(template);
    requisition.setStatus(RequisitionStatus.AUTHORIZED);

    // when
    requisition.reject(orderables, UUID.randomUUID());

    // then
    assertEquals(requisition.getStatus(), RequisitionStatus.REJECTED);
  }

  @Test
  public void shouldSubmitRequisitionIfItsStatusIsInitiated() {
    requisition.setTemplate(mock(RequisitionTemplate.class));
    requisition.setStatus(RequisitionStatus.INITIATED);
    requisition.submit(orderables, UUID.randomUUID(), false);

    assertEquals(requisition.getStatus(), RequisitionStatus.SUBMITTED);
  }

  @Test
  public void shouldSubmitRequisitionIfItsStatusIsRejected() {
    requisition.setTemplate(mock(RequisitionTemplate.class));
    requisition.setStatus(RequisitionStatus.REJECTED);
    requisition.submit(orderables, UUID.randomUUID(), false);

    assertEquals(requisition.getStatus(), RequisitionStatus.SUBMITTED);
  }

  @Test
  public void shouldSubmitRequisitionAndMarkAsAuthorizedWhenAuthorizeIsSkipped() {
    requisition.setTemplate(mock(RequisitionTemplate.class));
    requisition.setStatus(RequisitionStatus.INITIATED);
    requisition.submit(orderables, UUID.randomUUID(), true);

    assertEquals(requisition.getStatus(), RequisitionStatus.AUTHORIZED);
  }

  @Test
  public void shouldNotSubmitRegularRequisitionIfRegularFieldsNotFilled() {
    prepareRequisitionToHaveRequiredFieldStockOnHandNotFilled();
    assertThatThrownBy(() -> requisition.submit(orderables, UUID.randomUUID(), false))
        .isInstanceOf(ValidationMessageException.class)
        .hasMessage(getRequiredFieldErrorMessage(STOCK_ON_HAND, TOTAL_CONSUMED_QUANTITY));

    prepareRequisitionToHaveRequiredFieldTotalConsumedQuantityNotFilled();
    assertThatThrownBy(() -> requisition.submit(orderables, UUID.randomUUID(), false))
        .isInstanceOf(ValidationMessageException.class)
        .hasMessage(getRequiredFieldErrorMessage(TOTAL_CONSUMED_QUANTITY, STOCK_ON_HAND));

    assertEquals(requisition.getStatus(), RequisitionStatus.INITIATED);
  }

  @Test
  public void shouldSubmitEmergencyRequisitionIfRegularFieldsNotFilled() {
    prepareRequisitionToHaveRequiredFieldStockOnHandNotFilled();
    requisition.setEmergency(true);

    requisition.submit(orderables, UUID.randomUUID(), false);

    assertEquals(requisition.getStatus(), RequisitionStatus.SUBMITTED);
  }

  @Test
  public void shouldAuthorizeRequisitionIfItsStatusIsSubmitted() {
    requisition.setTemplate(mock(RequisitionTemplate.class));
    requisition.setStatus(RequisitionStatus.SUBMITTED);
    requisition.authorize(orderables, UUID.randomUUID());

    assertEquals(requisition.getStatus(), RequisitionStatus.AUTHORIZED);
  }

  @Test
  public void shouldInApprovalRequisitionIfItsStatusIsAuthorizedAndParentNodeExists() {
    requisition.setTemplate(mock(RequisitionTemplate.class));
    requisition.setStatus(RequisitionStatus.AUTHORIZED);
    SupervisoryNodeDto parentNode = mockSupervisoryParentNode(UUID.randomUUID());

    requisition.approve(parentNode.getId(), orderables, Collections.emptyList(),
        UUID.randomUUID());

    assertEquals(requisition.getStatus(), RequisitionStatus.IN_APPROVAL);
  }

  @Test
  public void shouldInApprovalRequisitionIfItsStatusIsInApprovalAndParentNodeExists() {
    requisition.setTemplate(mock(RequisitionTemplate.class));
    requisition.setStatus(RequisitionStatus.IN_APPROVAL);
    SupervisoryNodeDto parentNode = mockSupervisoryParentNode(UUID.randomUUID());

    requisition.approve(parentNode.getId(), orderables, Collections.emptyList(),
        UUID.randomUUID());

    assertEquals(requisition.getStatus(), RequisitionStatus.IN_APPROVAL);
  }

  @Test
  public void shouldApproveRequisitionIfItsStatusIsAuthorizedAndParentNodeDoesNotExist() {
    requisition.setTemplate(mock(RequisitionTemplate.class));
    requisition.setStatus(RequisitionStatus.AUTHORIZED);
    SupervisoryNodeDto parentNode = mockSupervisoryParentNode(null);
    SupplyLineDto supplyLine = new SupplyLineDtoDataBuilder().buildAsDto();

    requisition.approve(parentNode.getId(), orderables,
        Collections.singletonList(supplyLine), UUID.randomUUID());

    assertEquals(requisition.getStatus(), RequisitionStatus.APPROVED);
  }

  @Test
  public void shouldApproveRequisitionIfItsStatusIsInApprovalAndParentNodeNotExists() {
    requisition.setTemplate(mock(RequisitionTemplate.class));
    requisition.setStatus(RequisitionStatus.IN_APPROVAL);
    SupervisoryNodeDto parentNode = mockSupervisoryParentNode(null);
    SupplyLineDto supplyLine = new SupplyLineDtoDataBuilder().buildAsDto();

    requisition.approve(parentNode.getId(), orderables,
        Collections.singletonList(supplyLine), UUID.randomUUID());

    assertEquals(requisition.getStatus(), RequisitionStatus.APPROVED);
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowExceptionWhenAuthorizingRequisitionWithNotSubmittedStatus() {
    requisition.setTemplate(mock(RequisitionTemplate.class));
    requisition.authorize(orderables, UUID.randomUUID());
  }

  @Test
  public void shouldCalculateStockOnHandForRequisitionLineItemsWhenAuthorizing() {
    PowerMockito.spy(LineItemFieldsCalculator.class);
    RequisitionLineItem requisitionLineItem = mock(RequisitionLineItem.class);
    when(requisitionLineItem.getOrderable())
        .thenReturn(new VersionEntityReference(orderable.getId(), orderable.getVersionNumber()));

    requisition.setRequisitionLineItems(new ArrayList<>(
        Collections.singletonList(requisitionLineItem)));

    RequisitionTemplate requisitionTemplate = mock(RequisitionTemplate.class);
    when(requisitionTemplate.isColumnDisplayed("stockOnHand")).thenReturn(true);
    when(requisitionTemplate.isColumnCalculated("stockOnHand")).thenReturn(true);
    when(LineItemFieldsCalculator.calculateTotal(requisitionLineItem))
        .thenReturn(1);

    requisition.setTemplate(requisitionTemplate);
    requisition.setStatus(RequisitionStatus.SUBMITTED);

    requisition.authorize(orderables, UUID.randomUUID());
    requisition.updateFrom(new Requisition(), orderables, approvedProducts,true);

    assertEquals(requisition.getStatus(), RequisitionStatus.AUTHORIZED);
    verifyStatic(times(1));
  }

  @Test
  public void shouldDefaultApprovedQuantityOnAuthorizeWhenCalcOrderQuantityColumnDisplayed() {
    requisition.setTemplate(template);
    requisition.setStatus(RequisitionStatus.SUBMITTED);

    when(template.isColumnDisplayed(RequisitionLineItem.CALCULATED_ORDER_QUANTITY))
        .thenReturn(true);

    requisitionLineItem.setRequestedQuantity(null);

    requisition.authorize(orderables, null);

    assertEquals(Integer.valueOf(CALCULATED_ORDER_QUANTITY),
        requisitionLineItem.getApprovedQuantity());
  }

  @Test
  public void shouldDefaultApprovedQuantityOnAuthorizeWhenCalcOrderQuantityIsaColumnDisplayed() {
    requisition.setTemplate(template);
    requisition.setStatus(RequisitionStatus.SUBMITTED);

    when(template.isColumnDisplayed(RequisitionLineItem.CALCULATED_ORDER_QUANTITY))
        .thenReturn(false);
    when(template.isColumnInTemplate(RequisitionLineItem.CALCULATED_ORDER_QUANTITY_ISA))
        .thenReturn(true);
    when(template.isColumnDisplayed(RequisitionLineItem.CALCULATED_ORDER_QUANTITY_ISA))
        .thenReturn(true);

    requisitionLineItem.setRequestedQuantity(null);
    requisitionLineItem.setCalculatedOrderQuantityIsa(100);

    requisition.authorize(orderables, null);

    assertEquals(Integer.valueOf(100), requisitionLineItem.getApprovedQuantity());
  }

  @Test
  public void shouldDefaultApprovedQuantityOnSubmitWhenAuthorizationStepSkipped() {
    requisition.setTemplate(template);
    requisition.setStatus(RequisitionStatus.INITIATED);

    when(template.isColumnDisplayed(RequisitionLineItem.CALCULATED_ORDER_QUANTITY))
        .thenReturn(true);

    requisitionLineItem.setRequestedQuantity(null);

    requisition.submit(orderables, null, true);

    assertEquals(Integer.valueOf(CALCULATED_ORDER_QUANTITY),
        requisitionLineItem.getApprovedQuantity());
  }

  @Test
  public void shouldPopulateWithReqQuantityWhenIsNotNullAndCalcOrderQuantityColumnDisplayed() {
    requisition.setTemplate(template);
    requisition.setStatus(RequisitionStatus.SUBMITTED);

    when(template.isColumnDisplayed(RequisitionLineItem.CALCULATED_ORDER_QUANTITY))
        .thenReturn(true);

    requisition.authorize(orderables, null);

    assertEquals(Integer.valueOf(REQUESTED_QUANTITY), requisitionLineItem.getApprovedQuantity());
  }

  @Test
  public void shouldPopulateWithRequestedQuantityWhenCalculatedOrderQuantityIsNotDisplayed() {
    requisition.setTemplate(template);
    requisition.setStatus(RequisitionStatus.SUBMITTED);

    when(template.isColumnDisplayed(RequisitionLineItem.CALCULATED_ORDER_QUANTITY))
        .thenReturn(false);

    requisitionLineItem.setRequestedQuantity(REQUESTED_QUANTITY);

    requisition.authorize(orderables, null);

    assertEquals(Integer.valueOf(REQUESTED_QUANTITY), requisitionLineItem.getApprovedQuantity());
  }

  @Test
  public void shouldPopulateWithRequestedQuantityWhenCalculatedOrderQuantityIsaIsNotDisplayed() {
    requisition.setTemplate(template);
    requisition.setStatus(RequisitionStatus.SUBMITTED);

    when(template.isColumnDisplayed(RequisitionLineItem.CALCULATED_ORDER_QUANTITY))
        .thenReturn(false);
    when(template.isColumnInTemplate(RequisitionLineItem.CALCULATED_ORDER_QUANTITY_ISA))
        .thenReturn(true);
    when(template.isColumnDisplayed(RequisitionLineItem.CALCULATED_ORDER_QUANTITY_ISA))
        .thenReturn(false);

    requisitionLineItem.setRequestedQuantity(REQUESTED_QUANTITY);

    requisition.authorize(orderables, null);

    assertEquals(Integer.valueOf(REQUESTED_QUANTITY), requisitionLineItem.getApprovedQuantity());
  }

  @Test
  public void shouldPopulateWithRequestedQuantityWhenCalculatedOrderQuantityIsaNotExists() {
    requisition.setTemplate(template);
    requisition.setStatus(RequisitionStatus.SUBMITTED);

    when(template.isColumnDisplayed(RequisitionLineItem.CALCULATED_ORDER_QUANTITY))
        .thenReturn(false);
    when(template.isColumnInTemplate(RequisitionLineItem.CALCULATED_ORDER_QUANTITY_ISA))
        .thenReturn(false);

    requisitionLineItem.setRequestedQuantity(REQUESTED_QUANTITY);

    requisition.authorize(orderables, null);

    assertEquals(Integer.valueOf(REQUESTED_QUANTITY), requisitionLineItem.getApprovedQuantity());
  }

  @Test
  public void shouldPopulateNonFullSupplyLineItems() {
    requisition.setTemplate(template);
    requisition.setStatus(RequisitionStatus.SUBMITTED);

    when(template.isColumnDisplayed(RequisitionLineItem.CALCULATED_ORDER_QUANTITY))
        .thenReturn(false);

    requisitionLineItem.setRequestedQuantity(REQUESTED_QUANTITY);

    requisition.authorize(orderables, null);

    assertEquals(Integer.valueOf(REQUESTED_QUANTITY), requisitionLineItem.getApprovedQuantity());
  }

  @Test
  public void shouldPopulateWithNullRequestedQuantityWhenCalculatedOrderQuantityIsNotDisplayed() {
    requisition.setTemplate(template);
    requisition.setStatus(RequisitionStatus.SUBMITTED);

    when(template.isColumnDisplayed(RequisitionLineItem.CALCULATED_ORDER_QUANTITY))
        .thenReturn(false);

    requisitionLineItem.setRequestedQuantity(null);

    requisition.authorize(orderables, null);

    assertNull(requisitionLineItem.getApprovedQuantity());
  }

  @Test
  public void shouldCalculateTotalValueWhenUpdatingRequisition() {
    RequisitionTemplate requisitionTemplate = mock(RequisitionTemplate.class);
    PowerMockito.spy(LineItemFieldsCalculator.class);
    RequisitionLineItem requisitionLineItem = mock(RequisitionLineItem.class);
    when(requisitionLineItem.getOrderable())
        .thenReturn(new VersionEntityReference(orderable.getId(), orderable.getVersionNumber()));

    when(requisitionTemplate.isColumnDisplayed("total")).thenReturn(true);
    when(LineItemFieldsCalculator.calculateStockOnHand(requisitionLineItem))
        .thenReturn(1);

    requisition.setRequisitionLineItems(new ArrayList<>(
        Collections.singletonList(requisitionLineItem)));

    requisition.setTemplate(requisitionTemplate);
    requisition.updateFrom(new Requisition(), orderables, approvedProducts,true);
    verifyStatic(times(1));
  }

  @Test
  public void shouldAddFullSupplyLinesForEmergencyRequisition() {
    requisition.setRequisitionLineItems(Lists.newArrayList());
    requisition.setEmergency(true);

    RequisitionLineItem fullSupply = new RequisitionLineItemDataBuilder().build();
    OrderableDto fullSupplyOrderable = new OrderableDtoDataBuilder()
        .withId(fullSupply.getOrderable().getId())
        .withVersionNumber(fullSupply.getOrderable().getVersionNumber())
        .withProgramOrderable(requisition.getProgramId(), true)
        .buildAsDto();

    orderables.put(fullSupplyOrderable.getIdentity(), fullSupplyOrderable);

    RequisitionLineItem nonFullSupply = new RequisitionLineItemDataBuilder().build();
    OrderableDto nonFullSupplyOrderable = new OrderableDtoDataBuilder()
        .withId(nonFullSupply.getOrderable().getId())
        .withVersionNumber(nonFullSupply.getOrderable().getVersionNumber())
        .withProgramOrderable(requisition.getProgramId(), false)
        .buildAsDto();

    orderables.put(nonFullSupplyOrderable.getIdentity(), nonFullSupplyOrderable);

    Requisition newRequisition = new Requisition();
    newRequisition.setRequisitionLineItems(Lists.newArrayList(fullSupply, nonFullSupply));

    requisition.setTemplate(mock(RequisitionTemplate.class));
    requisition.updateFrom(newRequisition, orderables, approvedProducts,true);

    assertThat(requisition.getRequisitionLineItems(), hasSize(2));
  }

  @Test
  public void shouldRemoveFullSupplyLinesForEmergencyRequisition() {
    RequisitionLineItem fullSupply = new RequisitionLineItemDataBuilder().build();
    OrderableDto fullSupplyOrderable = new OrderableDtoDataBuilder()
        .withId(fullSupply.getOrderable().getId())
        .withVersionNumber(fullSupply.getOrderable().getVersionNumber())
        .withProgramOrderable(requisition.getProgramId(), true)
        .buildAsDto();

    orderables.put(fullSupplyOrderable.getIdentity(), fullSupplyOrderable);

    requisition.setRequisitionLineItems(Lists.newArrayList(fullSupply));
    requisition.setEmergency(true);

    RequisitionLineItem nonFullSupply = new RequisitionLineItemDataBuilder().build();
    OrderableDto nonFullSupplyOrderable = new OrderableDtoDataBuilder()
        .withId(nonFullSupply.getOrderable().getId())
        .withVersionNumber(nonFullSupply.getOrderable().getVersionNumber())
        .withProgramOrderable(requisition.getProgramId(), false)
        .buildAsDto();

    orderables.put(nonFullSupplyOrderable.getIdentity(), nonFullSupplyOrderable);

    Requisition newRequisition = new Requisition();
    newRequisition.setRequisitionLineItems(Lists.newArrayList(nonFullSupply));

    requisition.setTemplate(mock(RequisitionTemplate.class));
    requisition.updateFrom(newRequisition, orderables, approvedProducts, true);

    assertThat(requisition.getRequisitionLineItems(), hasSize(1));
  }

  @Test
  public void shouldFindRequisitionLineItemByProductId() {
    RequisitionLineItem found = requisition.findLineByProduct(orderable.getId(), 1L);

    assertNotNull(found);
    assertEquals(requisitionLineItem, found);
  }

  @Test
  public void shouldSetRequisitionFieldForLineItemsAfterUpdate() {
    // given
    Requisition newRequisition = new Requisition();

    // existing line
    RequisitionLineItem firstRequisitionLineItem = new RequisitionLineItemDataBuilder().build();
    firstRequisitionLineItem.setId(requisitionLineItem.getId());
    firstRequisitionLineItem.setRequestedQuantity(10);
    firstRequisitionLineItem.setStockOnHand(20);
    firstRequisitionLineItem.setRequisition(newRequisition);

    // new line
    RequisitionLineItem secondRequisitionLineItem = new RequisitionLineItemDataBuilder().build();
    secondRequisitionLineItem.setRequestedQuantity(10);
    secondRequisitionLineItem.setStockOnHand(20);
    secondRequisitionLineItem.setRequisition(newRequisition);

    newRequisition.setId(UUID.randomUUID());
    newRequisition.setStatus(RequisitionStatus.INITIATED);
    newRequisition.setRequisitionLineItems(
        Lists.newArrayList(firstRequisitionLineItem, secondRequisitionLineItem)
    );

    newRequisition
        .getRequisitionLineItems()
        .stream()
        .map(line -> new OrderableDtoDataBuilder()
            .withId(line.getOrderable().getId())
            .withVersionNumber(line.getOrderable().getVersionNumber())
            .withProgramOrderable(requisition.getProgramId(), true)
            .buildAsDto())
        .forEach(orderable -> orderables.put(orderable.getIdentity(), orderable));

    // when
    requisition.setTemplate(template);
    requisition.setId(UUID.randomUUID());
    requisition.updateFrom(newRequisition, orderables, approvedProducts, true);

    // then
    requisition
        .getRequisitionLineItems()
        .forEach(line -> assertThat(line.getRequisition().getId(), is(requisition.getId())));
  }

  @Test
  public void shouldSetNullForCalculatedValuesIfColumnIsHidden() {
    requisitionLineItem.setStockOnHand(10);
    requisitionLineItem.setTotalConsumedQuantity(15);

    RequisitionTemplate requisitionTemplate = mock(RequisitionTemplate.class);

    when(requisitionTemplate.isColumnDisplayed("stockOnHand")).thenReturn(false);
    when(requisitionTemplate.isColumnDisplayed(TOTAL_CONSUMED_QUANTITY)).thenReturn(false);

    requisition.setTemplate(requisitionTemplate);
    requisition.updateFrom(new Requisition(), orderables, approvedProducts, true);

    assertThat(requisitionLineItem.getStockOnHand(), is(nullValue()));
    assertThat(requisitionLineItem.getTotalConsumedQuantity(), is(nullValue()));
  }

  @Test
  public void shouldSetDatePhysicalStockCountCompletedWhenUpdateRequisition() {
    Requisition requisition = updateWithDatePhysicalCountCompleted(true);
    assertEquals(
        requisition.getDatePhysicalStockCountCompleted(),
        this.requisition.getDatePhysicalStockCountCompleted());
  }

  @Test
  public void shouldNotSetDatePhysicalStockCountCompletedWhenItIsDisabled() {
    updateWithDatePhysicalCountCompleted(false);
    assertNull(this.requisition.getDatePhysicalStockCountCompleted());
  }

  @Test
  public void shouldReturnZeroIfNoTotalCosts() {
    // given;
    requisition.setRequisitionLineItems(Collections.emptyList());

    // when
    Money result = requisition.getTotalCost();
    BigDecimal amount = result.getAmount();

    // then
    assertEquals(0, 0, amount.doubleValue());
  }

  @Test
  public void shouldGetTotalCost() {
    // given;
    setUpGetTotalCost(BigDecimal.ONE, BigDecimal.ONE);

    // when
    Money result = requisition.getTotalCost();
    BigDecimal amount = result.getAmount();

    // then
    assertEquals(2, 0, amount.doubleValue());
  }

  @Test
  public void shouldGetFullSupplyTotalCost() {
    // given;
    setUpGetTotalCost(BigDecimal.ONE, BigDecimal.ZERO);

    // when
    Money result = requisition.getFullSupplyTotalCost(orderables);
    BigDecimal amount = result.getAmount();

    // then
    assertEquals(1, 0, amount.doubleValue());
  }

  @Test
  public void shouldGetNonFullSupplyTotalCost() {
    // given;
    setUpGetTotalCost(BigDecimal.ZERO, BigDecimal.ONE);

    // when
    Money result = requisition.getNonFullSupplyTotalCost(orderables);
    BigDecimal amount = result.getAmount();

    // then
    assertEquals(1, 0, amount.doubleValue());
  }

  @Test
  public void shouldInitiateRequisitionLineItemFieldsIfPreviousRequisitionProvided() {
    // given
    Requisition previousRequisition = mock(Requisition.class);
    List<RequisitionLineItem> items = new ArrayList<>();
    items.add(mockReqLine(previousRequisition, orderable.getId(), STOCK_ON_HAND_VALUE));

    when(previousRequisition.getRequisitionLineItems()).thenReturn(items);

    when(template.isColumnDisplayed(RequisitionLineItem.BEGINNING_BALANCE)).thenReturn(true);

    // when
    Requisition req = new RequisitionDataBuilder()
        .withProgramId(requisition.getProgramId())
        .build();
    req.initiate(template, singletonList(product),
        Collections.singletonList(previousRequisition), 0, null, emptyMap(), UUID.randomUUID(),
        new StockData(), singletonList(stockCardRangeSummaryDto),
        singletonList(stockCardRangeSummaryDto), singletonList(period));

    // then
    List<RequisitionLineItem> lineItems = req.getRequisitionLineItems();

    assertEquals(1, lineItems.size());
    assertThat(req.findLineByProduct(orderable.getId(), 1L).getBeginningBalance(),
        is(STOCK_ON_HAND_VALUE));
    assertThat(req.findLineByProduct(orderable.getId(), 1L).getTotalReceivedQuantity(),
        is(nullValue()));
  }

  @Test
  public void shouldInitiateRequisitionLineItemFieldWithOrderableId() {
    // when
    Requisition req = new RequisitionDataBuilder()
        .withProgramId(requisition.getProgramId())
        .build();

    req.initiate(template, Collections.singleton(product), Collections.emptyList(), 0, null,
        emptyMap(), UUID.randomUUID(), new StockData(), singletonList(stockCardRangeSummaryDto),
        singletonList(stockCardRangeSummaryDto), singletonList(period));

    // then
    List<RequisitionLineItem> lineItems = req.getRequisitionLineItems();

    assertThat(lineItems.get(0).getOrderable().getId(), is(orderable.getId()));
  }

  @Test
  public void shouldInsertValueFromProofOfDeliveryToTotalReceivedQuantity() {
    // given
    Requisition previousRequisition = mock(Requisition.class);
    List<RequisitionLineItem> items = new ArrayList<>();
    items.add(mockReqLine(previousRequisition, orderable.getId(), STOCK_ON_HAND_VALUE));

    when(previousRequisition.getRequisitionLineItems()).thenReturn(items);

    when(template.isColumnDisplayed(RequisitionLineItem.BEGINNING_BALANCE)).thenReturn(true);

    ProofOfDeliveryDto pod = DtoGenerator.of(ProofOfDeliveryDto.class);
    pod.setStatus(CONFIRMED);
    pod.setLineItems(DtoGenerator.of(ProofOfDeliveryLineItemDto.class, 2));
    pod.getLineItems().get(0).setOrderable(new ObjectReferenceDto(orderable.getId()));

    // when
    Requisition req = new RequisitionDataBuilder()
        .withProgramId(requisition.getProgramId())
        .build();
    req.initiate(template, singletonList(product),
        Collections.singletonList(previousRequisition), 0, pod, emptyMap(),
        UUID.randomUUID(), new StockData(), singletonList(stockCardRangeSummaryDto),
        singletonList(stockCardRangeSummaryDto), singletonList(period));

    // then
    List<RequisitionLineItem> lineItems = req.getRequisitionLineItems();

    assertEquals(1, lineItems.size());
    assertThat(req.findLineByProduct(orderable.getId(), 1L).getBeginningBalance(),
        is(STOCK_ON_HAND_VALUE));
    assertThat(
        req.findLineByProduct(orderable.getId(), 1L).getTotalReceivedQuantity(),
        is(pod.getLineItems().get(0).getQuantityAccepted()));
  }

  @Test
  public void shouldNotInsertValueFromProofOfDeliveryIfNotSubmitted() {
    // given
    Requisition previousRequisition = mock(Requisition.class);
    List<RequisitionLineItem> items = new ArrayList<>();
    items.add(mockReqLine(previousRequisition, orderable.getId(), STOCK_ON_HAND_VALUE));

    when(previousRequisition.getRequisitionLineItems()).thenReturn(items);

    when(template.isColumnDisplayed(RequisitionLineItem.BEGINNING_BALANCE)).thenReturn(true);

    ProofOfDeliveryDto pod = DtoGenerator.of(ProofOfDeliveryDto.class);
    pod.setStatus(INITIATED);

    // when
    Requisition req = new RequisitionDataBuilder()
        .withProgramId(requisition.getProgramId())
        .build();

    req.initiate(template, singletonList(product),
        Collections.singletonList(previousRequisition), 0, pod, emptyMap(), UUID.randomUUID(),
        new StockData(), singletonList(stockCardRangeSummaryDto),
        singletonList(stockCardRangeSummaryDto), singletonList(period));

    // then
    List<RequisitionLineItem> lineItems = req.getRequisitionLineItems();

    assertEquals(1, lineItems.size());
    assertThat(req.findLineByProduct(orderable.getId(), 1L).getBeginningBalance(),
        is(STOCK_ON_HAND_VALUE));
    assertThat(req.findLineByProduct(orderable.getId(), 1L).getTotalReceivedQuantity(),
        is(nullValue()));
  }

  @Test
  public void shouldReturnNonSkippedRequisitionLineItems() {
    RequisitionLineItem notSkipped = getRequisitionLineItem(false);
    RequisitionLineItem skipped = getRequisitionLineItem(true);

    Requisition requisition = getRequisition(notSkipped, skipped);
    List<RequisitionLineItem> nonSkippedRequisitionLineItems =
        requisition.getNonSkippedRequisitionLineItems();
    RequisitionLineItem requisitionLineItem =
        nonSkippedRequisitionLineItems.get(0);

    assertEquals(1, nonSkippedRequisitionLineItems.size());
    assertEquals(notSkipped.getId(), requisitionLineItem.getId());
  }

  @Test
  public void shouldReturnNonSkippedFullSupplyRequisitionLineItems() {
    RequisitionLineItem notSkipped = getRequisitionLineItem(false);
    RequisitionLineItem skipped = getRequisitionLineItem(true);
    RequisitionLineItem nullSkippedValueLine = getRequisitionLineItem(null);

    Requisition requisition = getRequisition(notSkipped, skipped);
    requisition.getRequisitionLineItems().add(nullSkippedValueLine);

    List<RequisitionLineItem> nonSkippedRequisitionLineItems = requisition
            .getNonSkippedFullSupplyRequisitionLineItems(orderables);
    RequisitionLineItem requisitionLineItem = nonSkippedRequisitionLineItems.get(0);

    assertEquals(2, nonSkippedRequisitionLineItems.size());
    assertEquals(notSkipped.getId(), requisitionLineItem.getId());
  }

  @Test
  public void shouldGetNonSkippedNonFullSupplyRequisitionLineItems() {
    // given
    RequisitionLineItem notSkipped = getRequisitionLineItem(false, false);
    RequisitionLineItem skipped = getRequisitionLineItem(true, false);
    Requisition requisition = getRequisition(notSkipped, skipped);

    // when
    List<RequisitionLineItem> result = requisition
        .getNonSkippedNonFullSupplyRequisitionLineItems(orderables);

    // then
    assertEquals(1, result.size());
    assertEquals(notSkipped.getId(), result.get(0).getId());
  }

  @Test
  public void shouldReturnSkippedRequisitionLineItems() {
    RequisitionLineItem notSkipped = getRequisitionLineItem(false);
    RequisitionLineItem skipped = getRequisitionLineItem(true);

    Requisition requisition = getRequisition(notSkipped, skipped);
    List<RequisitionLineItem> skippedRequisitionLineItems =
        requisition.getSkippedRequisitionLineItems();
    RequisitionLineItem requisitionLineItem =
        skippedRequisitionLineItems.get(0);

    assertEquals(1, skippedRequisitionLineItems.size());
    assertEquals(skipped.getId(), requisitionLineItem.getId());
  }

  @Test
  public void shouldUpdatePacksToShipOnSubmit() {
    // given
    long packsToShip = 5L;
    orderable.setNetContent(1);

    setUpTestUpdatePacksToShip(orderable, packsToShip);

    // when
    requisition.submit(orderables, UUID.randomUUID(), false);

    // then
    assertEquals(packsToShip, requisitionLineItem.getPacksToShip().longValue());
  }

  @Test
  public void shouldUpdatePacksToShipOnAuthorize() {
    // given
    long packsToShip = 5L;
    orderable.setNetContent(1);

    setUpTestUpdatePacksToShip(orderable, packsToShip);
    requisition.setStatus(RequisitionStatus.SUBMITTED);

    // when
    requisition.authorize(orderables, UUID.randomUUID());

    // then
    assertEquals(packsToShip, requisitionLineItem.getPacksToShip().longValue());
  }

  @Test
  public void shouldSetStatusAsReleasedWithoutOrderIfPeriodIsReportOnly() {
    // given
    requisition.setStatus(RequisitionStatus.AUTHORIZED);
    requisition.setReportOnly(true);
    setUpValidRequisitionTemplate();

    // when
    requisition.approve(null, orderables, Collections.emptyList(),
        UUID.randomUUID());

    // then
    assertThat(requisition.getStatus(), is(RequisitionStatus.RELEASED_WITHOUT_ORDER));
  }

  @Test
  public void shouldUpdatePacksToShipOnApprove() {
    // given
    long packsToShip = 5L;
    orderable.setNetContent(1);

    setUpTestUpdatePacksToShip(orderable, packsToShip);
    requisitionLineItem.setApprovedQuantity((int) packsToShip);
    requisition.setStatus(RequisitionStatus.AUTHORIZED);

    // when
    requisition.approve(null, orderables, Collections.emptyList(), UUID.randomUUID());

    // then
    assertEquals(packsToShip, requisitionLineItem.getPacksToShip().longValue());
  }

  @Test
  public void shouldCalculateAdjustedConsumptionTotalCostAndAverageConsumptionWhenSubmit() {
    // given
    prepareForTestAdjustedConcumptionTotalCostAndAverageConsumption();

    //when
    requisition.submit(orderables, UUID.randomUUID(), false);

    //then
    assertEquals(ADJUSTED_CONSUMPTION, requisitionLineItem.getAdjustedConsumption().longValue());
    assertEquals(AVERAGE_CONSUMPTION, requisitionLineItem.getAverageConsumption().longValue());
    assertEquals(TOTAL_COST, requisitionLineItem.getTotalCost());
  }

  @Test
  public void shouldCalculateAdjustedConsumptionTotalCostAndAverageConsumptionWhenReject() {
    // given
    prepareForTestAdjustedConcumptionTotalCostAndAverageConsumption();
    requisition.setStatus(RequisitionStatus.AUTHORIZED);

    //when
    requisition.reject(orderables, UUID.randomUUID());

    //then
    assertEquals(ADJUSTED_CONSUMPTION, requisitionLineItem.getAdjustedConsumption().longValue());
    assertEquals(AVERAGE_CONSUMPTION, requisitionLineItem.getAverageConsumption().longValue());
    assertEquals(TOTAL_COST, requisitionLineItem.getTotalCost());
  }

  @Test
  public void shouldCalculateAverageConsumptionWhenSubmitWithOnePreviousRequisition() {
    // given
    List<Integer> adjustedConsumptions = new ArrayList<>();
    adjustedConsumptions.add(5);
    prepareForTestAverageConsumption(adjustedConsumptions);
    when(LineItemFieldsCalculator
        .calculateAverageConsumption(Arrays.asList(5, ADJUSTED_CONSUMPTION)))
        .thenReturn(AVERAGE_CONSUMPTION);

    //when
    requisition.submit(orderables, UUID.randomUUID(), false);

    //then
    assertEquals(ADJUSTED_CONSUMPTION, requisitionLineItem.getAdjustedConsumption().longValue());
    assertEquals(AVERAGE_CONSUMPTION, requisitionLineItem.getAverageConsumption().longValue());
  }

  @Test
  public void shouldCalculateAverageConsumptionWhenSubmitWithManyPreviousRequisitions() {
    // given
    List<Integer> adjustedConsumptions = new ArrayList<>();
    adjustedConsumptions.add(5);
    adjustedConsumptions.add(5);
    adjustedConsumptions.add(5);
    prepareForTestAverageConsumption(adjustedConsumptions);
    when(LineItemFieldsCalculator
        .calculateAverageConsumption(Arrays.asList(5, 5, 5, ADJUSTED_CONSUMPTION)))
        .thenReturn(AVERAGE_CONSUMPTION);

    //when
    requisition.submit(orderables, UUID.randomUUID(), false);

    //then
    assertEquals(ADJUSTED_CONSUMPTION, requisitionLineItem.getAdjustedConsumption().longValue());
    assertEquals(AVERAGE_CONSUMPTION, requisitionLineItem.getAverageConsumption().longValue());
  }

  @Test
  public void shouldCalculateAdjustedConsumptionTotalCostAndAverageConsumptionWhenAuthorize() {
    // given
    prepareForTestAdjustedConcumptionTotalCostAndAverageConsumption();
    requisition.setStatus(RequisitionStatus.SUBMITTED);

    //when
    requisition.authorize(orderables, UUID.randomUUID());

    //then
    assertEquals(ADJUSTED_CONSUMPTION, requisitionLineItem.getAdjustedConsumption().longValue());
    assertEquals(AVERAGE_CONSUMPTION, requisitionLineItem.getAverageConsumption().longValue());
    assertEquals(TOTAL_COST, requisitionLineItem.getTotalCost());
  }

  @Test
  public void shouldCalculateAdjustedConsumptionTotalCostAndAverageConsumptionWhenApprove() {
    // given
    prepareForTestAdjustedConcumptionTotalCostAndAverageConsumption();
    requisition.setStatus(RequisitionStatus.APPROVED);

    //when
    requisition.approve(null, orderables, Collections.emptyList(),
        UUID.randomUUID());

    //then
    assertEquals(ADJUSTED_CONSUMPTION, requisitionLineItem.getAdjustedConsumption().longValue());
    assertEquals(AVERAGE_CONSUMPTION, requisitionLineItem.getAverageConsumption().longValue());
    assertEquals(TOTAL_COST, requisitionLineItem.getTotalCost());
  }

  @Test
  public void shouldSetPreviousAdjustedConsumptionsWhenOnePreviousRequisition() {
    RequisitionLineItem previousRequisitionLineItem = new RequisitionLineItem(requisitionLineItem);
    previousRequisitionLineItem.setAdjustedConsumption(5);

    Requisition previousRequisition = new Requisition();
    previousRequisition.setRequisitionLineItems(
        Collections.singletonList(previousRequisitionLineItem));

    requisition.setPreviousRequisitions(Collections.singletonList(previousRequisition));
    requisition.setPreviousAdjustedConsumptions(1);

    assertEquals(Collections.singletonList(5),
        requisitionLineItem.getPreviousAdjustedConsumptions());
  }

  @Test
  public void shouldSetPreviousAdjustedConsumptionsFromManyPreviousRequisitions() {
    RequisitionLineItem previousRequisitionLineItem = new RequisitionLineItem(requisitionLineItem);
    previousRequisitionLineItem.setAdjustedConsumption(5);

    Requisition previousRequisition = new Requisition();
    previousRequisition
        .setRequisitionLineItems(Arrays.asList(previousRequisitionLineItem,
            previousRequisitionLineItem, previousRequisitionLineItem));

    requisition.setPreviousRequisitions(Collections.singletonList(previousRequisition));
    requisition.setPreviousAdjustedConsumptions(1);

    assertEquals(Arrays.asList(5, 5, 5), requisitionLineItem.getPreviousAdjustedConsumptions());
  }

  @Test
  public void shouldNotAddPreviousAdjustedConsumptionIfLineSkipped() {
    final RequisitionLineItem requisitionLineItem = new RequisitionLineItem();

    RequisitionLineItem previousRequisitionLineItem = new RequisitionLineItem();
    previousRequisitionLineItem.setAdjustedConsumption(5);
    previousRequisitionLineItem.setSkipped(true);
    Requisition previousRequisition = new Requisition();
    previousRequisition.setRequisitionLineItems(
        Collections.singletonList(previousRequisitionLineItem));

    Requisition requisition = new Requisition();
    requisition.setRequisitionLineItems(Lists.newArrayList(requisitionLineItem));

    requisition.setPreviousRequisitions(Collections.singletonList(previousRequisition));
    requisition.setPreviousAdjustedConsumptions(1);

    assertEquals(Collections.emptyList(),
        requisitionLineItem.getPreviousAdjustedConsumptions());
  }

  @Test
  public void shouldNotAddPreviousAdjustedConsumptionIfItIsNull() {
    //given
    RequisitionLineItem previousRequisitionLineItem = new RequisitionLineItem(requisitionLineItem);
    previousRequisitionLineItem.setAdjustedConsumption(null);

    Requisition previousRequisition = new Requisition();
    previousRequisition.setRequisitionLineItems(
        Collections.singletonList(previousRequisitionLineItem));

    requisition.setPreviousRequisitions(Collections.singletonList(previousRequisition));
    final int numberOfMonthsToAverage = 1;

    //when
    requisition.setPreviousAdjustedConsumptions(numberOfMonthsToAverage);

    //then
    assertEquals(Collections.emptyList(),
        requisitionLineItem.getPreviousAdjustedConsumptions());
  }

  @Test
  public void shouldNotUpdatePreviousAdjustedConsumptions() {
    requisitionLineItem.setPreviousAdjustedConsumptions(Lists.newArrayList(1));

    RequisitionLineItem newLineItem = new RequisitionLineItem();
    newLineItem.setPreviousAdjustedConsumptions(Collections.singletonList(2));
    newLineItem.setId(requisitionLineItem.getId());

    Requisition newRequisition = new Requisition();
    newRequisition.setRequisitionLineItems(Lists.newArrayList(newLineItem));

    RequisitionTemplate requisitionTemplate = mock(RequisitionTemplate.class);
    requisition.setTemplate(requisitionTemplate);
    requisition.updateFrom(newRequisition, orderables, approvedProducts, true);

    assertEquals(Integer.valueOf(1), requisitionLineItem.getPreviousAdjustedConsumptions().get(0));
  }

  @Test
  public void shouldRecordStatusChangeOnInitiate() {
    UUID initiatorId = UUID.randomUUID();
    Requisition requisition = createRequisitionWithStatusOf(RequisitionStatus.INITIATED);

    requisition.initiate(template, Collections.emptyList(), Collections.emptyList(), 0, null,
        emptyMap(), initiatorId, new StockData(), singletonList(stockCardRangeSummaryDto),
        singletonList(stockCardRangeSummaryDto), singletonList(period));

    assertStatusChangeExistsAndAuthorIdMatches(requisition, RequisitionStatus.INITIATED,
        initiatorId);
  }

  @Test
  public void shouldRecordStatusChangeOnSubmit() {
    UUID submitterId = UUID.randomUUID();
    Requisition requisition = createRequisitionWithStatusOf(RequisitionStatus.INITIATED);
    requisition.setTemplate(template);

    requisition.submit(orderables, submitterId, false);

    assertStatusChangeExistsAndAuthorIdMatches(requisition, RequisitionStatus.SUBMITTED,
        submitterId);
  }

  @Test
  public void shouldRecordStatusChangeOnSubmitWithSkippedAuthorization() {
    UUID submitterId = UUID.randomUUID();
    Requisition requisition = createRequisitionWithStatusOf(RequisitionStatus.INITIATED);
    requisition.setTemplate(template);
    requisition.setRequisitionLineItems(Collections.emptyList());

    requisition.submit(orderables, submitterId, true);

    assertStatusChangeExistsAndAuthorIdMatches(requisition, RequisitionStatus.SUBMITTED,
        submitterId);
    assertStatusChangeExistsAndAuthorIdMatches(requisition, RequisitionStatus.AUTHORIZED,
        submitterId);
  }

  @Test
  public void shouldRecordStatusChangeOnAuthorize() {
    UUID authorizerId = UUID.randomUUID();
    Requisition requisition = createRequisitionWithStatusOf(RequisitionStatus.SUBMITTED);
    requisition.setTemplate(template);
    requisition.setRequisitionLineItems(Collections.emptyList());

    requisition.authorize(orderables, authorizerId);

    assertStatusChangeExistsAndAuthorIdMatches(requisition, RequisitionStatus.AUTHORIZED,
        authorizerId);
  }

  @Test
  public void shouldRecordStatusChangeOnApprove() {
    UUID approverId = UUID.randomUUID();
    Requisition requisition = createRequisitionWithStatusOf(RequisitionStatus.AUTHORIZED);
    requisition.setTemplate(template);
    requisition.setRequisitionLineItems(Collections.emptyList());

    requisition.approve(null, orderables, Collections.emptyList(), approverId);

    assertStatusChangeExistsAndAuthorIdMatches(requisition, RequisitionStatus.APPROVED,
        approverId);
  }

  @Test
  public void shouldRecordStatusChangeOnReject() {
    UUID rejectorId = UUID.randomUUID();
    Requisition requisition = createRequisitionWithStatusOf(RequisitionStatus.AUTHORIZED);
    requisition.setTemplate(template);

    requisition.reject(orderables, rejectorId);

    assertStatusChangeExistsAndAuthorIdMatches(requisition, RequisitionStatus.REJECTED,
        rejectorId);
  }

  @Test
  public void shouldRecordStatusChangeOnRelease() {
    UUID releaserId = UUID.randomUUID();
    Requisition requisition = createRequisitionWithStatusOf(RequisitionStatus.APPROVED);

    requisition.release(releaserId);

    assertStatusChangeExistsAndAuthorIdMatches(requisition, RequisitionStatus.RELEASED,
        releaserId);
  }

  @Test
  public void shouldRecordStatusChangeOnReleaseWithoutOrder() {
    UUID releaserId = UUID.randomUUID();
    Requisition requisition = createRequisitionWithStatusOf(RequisitionStatus.APPROVED);

    requisition.releaseWithoutOrder(releaserId);

    assertStatusChangeExistsAndAuthorIdMatches(requisition,
        RequisitionStatus.RELEASED_WITHOUT_ORDER, releaserId);
  }


  @Test
  public void shouldProperlyRecognizeDeletableRequisition() {
    Requisition requisition = createRequisitionWithStatusOf(RequisitionStatus.INITIATED);
    assertTrue(requisition.isDeletable());

    requisition.setStatus(RequisitionStatus.REJECTED);
    assertTrue(requisition.isDeletable());

    requisition.setStatus(RequisitionStatus.SKIPPED);
    assertTrue(requisition.isDeletable());

    requisition.setStatus(RequisitionStatus.SUBMITTED);
    assertTrue(requisition.isDeletable());

    requisition.setStatus(RequisitionStatus.AUTHORIZED);
    assertFalse(requisition.isDeletable());

    requisition.setStatus(RequisitionStatus.IN_APPROVAL);
    assertFalse(requisition.isDeletable());

    requisition.setStatus(RequisitionStatus.APPROVED);
    assertFalse(requisition.isDeletable());

    requisition.setStatus(RequisitionStatus.RELEASED);
    assertFalse(requisition.isDeletable());

    requisition.setStatus(RequisitionStatus.RELEASED_WITHOUT_ORDER);
    assertFalse(requisition.isDeletable());
  }

  @Test
  public void shouldFindLatestStatusChange() {
    // given
    List<StatusChange> statusChanges = Arrays.asList(
        StatusChange.newStatusChange(requisition, UUID.randomUUID()),
        StatusChange.newStatusChange(requisition, UUID.randomUUID()),
        StatusChange.newStatusChange(requisition, UUID.randomUUID())
    );
    ZonedDateTime expectedDate = ZonedDateTime.now();
    ZonedDateTime dt = expectedDate;
    for (StatusChange statusChange : statusChanges) {
      statusChange.setCreatedDate(dt);
      dt = dt.minusDays(1);
    }
    requisition.setStatusChanges(statusChanges);

    // when
    StatusChange latestStatusChange = requisition.getLatestStatusChange();

    // then
    assertEquals(expectedDate, latestStatusChange.getCreatedDate());
  }

  @Test
  public void shouldSetIdealStockAmountForLineItems() {
    // given
    final UUID commodityTypeId = UUID.randomUUID();

    product
        .getOrderable()
        .setIdentifiers(ImmutableMap.of(COMMODITY_TYPE_IDENTIFIER, commodityTypeId.toString()));

    Map<UUID, Integer> idealStockAmounts = Maps.newHashMap();
    idealStockAmounts.put(commodityTypeId, 1000);

    // when
    Requisition req = createRequisitionWithStatusOf(RequisitionStatus.INITIATED);
    req.initiate(template, singletonList(product), emptyList(), 0, null, idealStockAmounts,
        UUID.randomUUID(), new StockData(), singletonList(stockCardRangeSummaryDto),
        singletonList(stockCardRangeSummaryDto), singletonList(period));

    // then
    List<RequisitionLineItem> lineItems = req.getRequisitionLineItems();

    assertEquals(1, lineItems.size());
    assertThat(req.findLineByProduct(orderable.getId(), 1L).getIdealStockAmount(), is(1000));
  }

  @Test
  public void shouldSetValuesFromStockCardRangeSummaryIfRequisitionIsStockBased() {
    Map<UUID, Integer> orderableSoh = Maps.newHashMap();
    orderableSoh.put(orderable.getId(), 1000);

    stockCardRangeSummaryDto.getOrderable().setId(orderable.getId());

    RequisitionTemplate requisitionTemplate = mockStockBasedRequisitionTemplate();

    Requisition req = createRequisitionWithStatusOf(RequisitionStatus.INITIATED);
    req.setNumberOfMonthsInPeriod(1);

    req.initiate(requisitionTemplate, singletonList(product), emptyList(), 0, null, emptyMap(),
        UUID.randomUUID(), new StockData(orderableSoh, emptyMap()),
        singletonList(stockCardRangeSummaryDto), singletonList(stockCardRangeSummaryDto),
        singletonList(period));

    List<RequisitionLineItem> lineItems = req.getRequisitionLineItems();

    assertEquals(1, lineItems.size());
    assertThat(req.findLineByProduct(orderable.getId(), 1L).getTotalReceivedQuantity(), is(100));
    assertThat(req.findLineByProduct(orderable.getId(), 1L).getTotalConsumedQuantity(), is(200));
    assertThat(req.findLineByProduct(orderable.getId(), 1L).getTotalLossesAndAdjustments(),
        is(-50));
    assertThat(req.findLineByProduct(orderable.getId(), 1L).getTotalStockoutDays(), is(3));
  }

  @Test
  public void shouldNotExceedNumberOfDaysInPeriod() {
    Map<UUID, Integer> orderableSoh = Maps.newHashMap();
    orderableSoh.put(orderable.getId(), 1000);

    stockCardRangeSummaryDto.getOrderable().setId(orderable.getId());
    stockCardRangeSummaryDto.setStockOutDays(31);

    RequisitionTemplate requisitionTemplate = mockStockBasedRequisitionTemplate();

    Requisition req = createRequisitionWithStatusOf(RequisitionStatus.INITIATED);
    req.setNumberOfMonthsInPeriod(1);

    req.initiate(requisitionTemplate, singletonList(product), emptyList(), 0, null, emptyMap(),
        UUID.randomUUID(), new StockData(orderableSoh, emptyMap()),
        singletonList(stockCardRangeSummaryDto), singletonList(stockCardRangeSummaryDto),
        singletonList(period));

    List<RequisitionLineItem> lineItems = req.getRequisitionLineItems();

    assertEquals(1, lineItems.size());
    assertThat(req.findLineByProduct(orderable.getId(), 1L).getTotalStockoutDays(), is(30));
  }

  @Test
  public void shouldRejectIfRequisitionIsStockBasedAndTotalConsumedValueIsPositive() {
    Map<UUID, Integer> orderableSoh = Maps.newHashMap();
    orderableSoh.put(orderable.getId(), 1000);

    stockCardRangeSummaryDto.getOrderable().setId(orderable.getId());
    stockCardRangeSummaryDto.getTags().put(CONSUMED_TAG, 1000);

    RequisitionTemplate requisitionTemplate = mockStockBasedRequisitionTemplate();

    Requisition req = createRequisitionWithStatusOf(RequisitionStatus.INITIATED);
    req.setNumberOfMonthsInPeriod(1);

    assertThatThrownBy(() -> req.initiate(requisitionTemplate,
        singletonList(product), emptyList(),
        0, null, emptyMap(), UUID.randomUUID(), new StockData(orderableSoh, emptyMap()),
        singletonList(stockCardRangeSummaryDto), singletonList(stockCardRangeSummaryDto),
        singletonList(period)))
        .isInstanceOf(ValidationMessageException.class)
        .hasMessage(getNonNegativeNumberErrorMessage(
            TOTAL_CONSUMED_QUANTITY, orderable.getId().toString()));
  }

  @Test
  public void shouldRejectIfRequisitionIsStockBasedAndTotalReceivedValueIsNegative() {
    Map<UUID, Integer> orderableSoh = Maps.newHashMap();
    orderableSoh.put(orderable.getId(), 1000);

    stockCardRangeSummaryDto.getOrderable().setId(orderable.getId());
    stockCardRangeSummaryDto.getTags().put(RECEIVED_TAG, -1000);

    RequisitionTemplate requisitionTemplate = mockStockBasedRequisitionTemplate();

    Requisition req = createRequisitionWithStatusOf(RequisitionStatus.INITIATED);
    req.setNumberOfMonthsInPeriod(1);

    assertThatThrownBy(() -> req.initiate(requisitionTemplate,
        singletonList(product), emptyList(),
        0, null, emptyMap(), UUID.randomUUID(), new StockData(orderableSoh, emptyMap()),
        singletonList(stockCardRangeSummaryDto), singletonList(stockCardRangeSummaryDto),
        singletonList(period)))
        .isInstanceOf(ValidationMessageException.class)
        .hasMessage(getNonPositiveNumberErrorMessage(
            TOTAL_RECEIVED_QUANTITY, orderable.getId().toString()));
  }

  @Test
  public void shouldSetStockOnHandForLineItems() {
    // given
    Map<UUID, Integer> orderableSoh = Maps.newHashMap();
    orderableSoh.put(orderable.getId(), 1000);

    // when
    Requisition req = createRequisitionWithStatusOf(RequisitionStatus.INITIATED);
    template = mockStockBasedRequisitionTemplate();
    when(template.isPopulateStockOnHandFromStockCards()).thenReturn(true);

    req.initiate(template, singletonList(product), emptyList(), 0, null, emptyMap(),
        UUID.randomUUID(), new StockData(orderableSoh, emptyMap()),
        singletonList(stockCardRangeSummaryDto), singletonList(stockCardRangeSummaryDto),
        singletonList(period));

    // then
    List<RequisitionLineItem> lineItems = req.getRequisitionLineItems();

    assertEquals(1, lineItems.size());
    assertThat(req.findLineByProduct(orderable.getId(), 1L).getStockOnHand(), is(1000));
  }

  @Test
  public void shouldSetBeginningBalanceForLineItems() {
    // given
    Map<UUID, Integer> beginningBalances = Maps.newHashMap();
    beginningBalances.put(orderable.getId(), 1000);

    // when
    Requisition req = createRequisitionWithStatusOf(RequisitionStatus.INITIATED);
    template = mockStockBasedRequisitionTemplate();
    when(template.isPopulateStockOnHandFromStockCards()).thenReturn(true);
    req.initiate(template, singletonList(product), emptyList(), 0, null, emptyMap(),
        UUID.randomUUID(), new StockData(emptyMap(), beginningBalances),
        singletonList(stockCardRangeSummaryDto), singletonList(stockCardRangeSummaryDto),
        singletonList(period));

    // then
    List<RequisitionLineItem> lineItems = req.getRequisitionLineItems();

    assertEquals(1, lineItems.size());
    assertThat(req.findLineByProduct(orderable.getId(), 1L).getBeginningBalance(), is(1000));
  }

  @Test
  public void shouldCopySkippedValueFromPreviousRequisition() {
    // given
    Requisition previousReq = mock(Requisition.class);
    RequisitionLineItem lineItem1 = mockReqLine(previousReq, orderable.getId(), 0);
    lineItem1.setSkipped(true);

    when(previousReq.getRequisitionLineItems()).thenReturn(asList(lineItem1));
    PowerMockito.mockStatic(LineItemFieldsCalculator.class);
    PowerMockito.spy(LineItemFieldsCalculator.class);

    PowerMockito.when(LineItemFieldsCalculator
        .canSkipLineItem(
            org.mockito.Matchers.any(RequisitionLineItem.class),
            org.mockito.Matchers.any(RequisitionLineItem.class)))
        .thenReturn(true);
    when(template.isColumnInTemplateAndDisplayed(SKIPPED_COLUMN)).thenReturn(true);
    when(template.isColumnFromPreviousRequisition(SKIPPED_COLUMN)).thenReturn(true);
    when(template.isPopulateStockOnHandFromStockCards()).thenReturn(false);

    Requisition req = createRequisitionWithStatusOf(RequisitionStatus.INITIATED);
    req.setEmergency(false);
    req.initiate(template, asList(product), asList(previousReq), 0, null,
        emptyMap(), UUID.randomUUID(), null, singletonList(stockCardRangeSummaryDto),
        singletonList(stockCardRangeSummaryDto), singletonList(period));

    assertThat(req.findLineByProduct(orderable.getId(), 1L).getSkipped(), is(true));
  }

  @Test
  public void shouldNotCopySkippedValueWhenSourceIsNotFromPreviousRequisition() {
    Requisition previousReq = mock(Requisition.class);
    RequisitionLineItem lineItem1 = mockReqLine(previousReq, orderable.getId(), 0);
    lineItem1.setSkipped(true);

    when(previousReq.getRequisitionLineItems()).thenReturn(asList(lineItem1));
    when(template.isColumnInTemplateAndDisplayed(SKIPPED_COLUMN)).thenReturn(true);
    when(template.isColumnFromPreviousRequisition(SKIPPED_COLUMN)).thenReturn(false);
    when(template.isPopulateStockOnHandFromStockCards()).thenReturn(false);

    Requisition req = createRequisitionWithStatusOf(RequisitionStatus.INITIATED);
    req.setEmergency(false);
    req.initiate(template, asList(product), asList(previousReq), 0, null,
        emptyMap(), UUID.randomUUID(), null, singletonList(stockCardRangeSummaryDto),
        singletonList(stockCardRangeSummaryDto), singletonList(period));

    assertThat(req.findLineByProduct(orderable.getId(), 1L).getSkipped(), is(false));
  }

  @Test
  public void shouldNotCopySkippedValueWhenSourceIsFromPreviousRequisitionButNoPreviousFound() {
    when(template.isColumnInTemplateAndDisplayed(SKIPPED_COLUMN)).thenReturn(true);
    when(template.isColumnFromPreviousRequisition(SKIPPED_COLUMN)).thenReturn(true);
    when(template.isPopulateStockOnHandFromStockCards()).thenReturn(false);
    Requisition req = createRequisitionWithStatusOf(RequisitionStatus.INITIATED);
    req.setEmergency(false);

    req.initiate(template, asList(product), emptyList(), 0, null, emptyMap(), UUID.randomUUID(),
        null, singletonList(stockCardRangeSummaryDto), singletonList(stockCardRangeSummaryDto),
        singletonList(period));

    assertThat(req.findLineByProduct(orderable.getId(), 1L).getSkipped(), is(false));
  }

  @Test
  public void shouldNotCopySkippedValueWhenEmergencyRequisitionSourceIsFromPreviousRequisition() {
    // given
    when(template.isColumnInTemplateAndDisplayed(SKIPPED_COLUMN)).thenReturn(true);
    when(template.isColumnFromPreviousRequisition(SKIPPED_COLUMN)).thenReturn(true);
    when(template.isPopulateStockOnHandFromStockCards()).thenReturn(false);

    Requisition req = createRequisitionWithStatusOf(RequisitionStatus.INITIATED);
    req.setEmergency(true);
    req.initiate(template, singletonList(product), emptyList(), 0, null, emptyMap(),
        UUID.randomUUID(),
        null, singletonList(stockCardRangeSummaryDto), singletonList(stockCardRangeSummaryDto),
        singletonList(period));

    assertThat(req.getRequisitionLineItems().size(), is(0));
  }

  @Test
  public void shouldNotSetLineItemsForEmergencyRequisition() {
    // when
    Requisition req = createRequisitionWithStatusOf(RequisitionStatus.INITIATED);
    req.setEmergency(true);

    req.initiate(template, singletonList(product), emptyList(), 0, null, emptyMap(),
        UUID.randomUUID(), new StockData(), singletonList(stockCardRangeSummaryDto),
        singletonList(stockCardRangeSummaryDto), singletonList(period));

    // then
    List<RequisitionLineItem> lineItems = req.getRequisitionLineItems();
    assertThat(lineItems, hasSize(0));
    assertThat(req.findLineByProduct(orderable.getId(), 1L), is(nullValue()));
  }

  @Test
  public void shouldSkipRequisition() {
    // given
    RequisitionLineItem lineItem = mock(RequisitionLineItem.class);

    Requisition requisition = createRequisitionWithStatusOf(RequisitionStatus.INITIATED);
    requisition.setTemplate(template);
    requisition.setRequisitionLineItems(Collections.singletonList(lineItem));

    //when
    requisition.skip(true, UUID.randomUUID());

    // then
    Matcher<RequisitionStatus> statusMatcher = is(RequisitionStatus.SKIPPED);
    assertThat(requisition.getStatus(), statusMatcher);
    assertThat(requisition.getLatestStatusChange(), hasProperty("status", statusMatcher));

    verify(lineItem).skipLineItem(template);
  }

  @Test
  public void shouldNotSkipRequisitionIfRequisitionHasIncorrectStatus() {
    exception.expect(ValidationMessageException.class);
    exception.expectMessage(containsString(ERROR_SKIP_FAILED_WRONG_STATUS));

    Requisition requisition = createRequisitionWithStatusOf(RequisitionStatus.SUBMITTED);
    requisition.skip(true, UUID.randomUUID());
  }

  @Test
  public void shouldNotSkipRequisitionIfProgramNotSupportSkip() {
    exception.expect(ValidationMessageException.class);
    exception.expectMessage(containsString(ERROR_PROGRAM_DOES_NOT_ALLOW_SKIP));

    Requisition requisition = createRequisitionWithStatusOf(RequisitionStatus.SUBMITTED);
    requisition.skip(false, UUID.randomUUID());
  }

  @Test
  public void shouldNotSkipRequisitionIfRequisitionIsEmergency() {
    exception.expect(ValidationMessageException.class);
    exception.expectMessage(containsString(ERROR_SKIP_FAILED_EMERGENCY));

    Requisition requisition = createRequisitionWithStatusOf(RequisitionStatus.INITIATED);
    requisition.setEmergency(true);

    requisition.skip(true, UUID.randomUUID());
  }

  @Test
  public void shouldSetApprovedQuantityAccordingToIsaWhenSubmittingStockBasedRequisition() {
    RequisitionTemplate template = mockStockBasedRequisitionTemplate();

    when(template.isColumnInTemplate(CALCULATED_ORDER_QUANTITY_ISA)).thenReturn(true);
    when(template.isColumnDisplayed(CALCULATED_ORDER_QUANTITY_ISA)).thenReturn(true);

    requisition.setTemplate(template);
    requisitionLineItem.setRequestedQuantity(null);
    requisitionLineItem.setCalculatedOrderQuantityIsa(689);

    //when
    requisition.submit(orderables, UUID.randomUUID(), true);

    //then
    assertEquals(
        requisitionLineItem.getCalculatedOrderQuantityIsa(),
        requisitionLineItem.getApprovedQuantity()
    );
  }

  @Test
  public void shouldSetApprovedQuantityAccordingToIsaWhenAuthorizingStockBasedRequisition() {
    RequisitionTemplate template = mockStockBasedRequisitionTemplate();

    when(template.isColumnInTemplate(CALCULATED_ORDER_QUANTITY_ISA)).thenReturn(true);
    when(template.isColumnDisplayed(CALCULATED_ORDER_QUANTITY_ISA)).thenReturn(true);

    requisition.setTemplate(template);
    requisition.setStatus(RequisitionStatus.SUBMITTED);
    requisitionLineItem.setRequestedQuantity(null);
    requisitionLineItem.setCalculatedOrderQuantityIsa(689);

    //when
    requisition.authorize(orderables, UUID.randomUUID());

    //then
    assertEquals(
        requisitionLineItem.getCalculatedOrderQuantityIsa(),
        requisitionLineItem.getApprovedQuantity()
    );
  }

  private Requisition updateWithDatePhysicalCountCompleted(boolean updateStockDate) {
    RequisitionTemplate requisitionTemplate = mock(RequisitionTemplate.class);
    this.requisition.setTemplate(requisitionTemplate);

    Requisition requisition = new Requisition();
    requisition.setDatePhysicalStockCountCompleted(
        new DatePhysicalStockCountCompleted(LocalDate.now()));
    this.requisition.updateFrom(requisition, orderables, approvedProducts, updateStockDate);

    return requisition;
  }

  private Requisition createRequisitionWithStatusOf(RequisitionStatus status) {
    return new Requisition(UUID.randomUUID(), requisition.getProgramId(), UUID
        .randomUUID(), status, false);
  }

  private void assertStatusChangeExistsAndAuthorIdMatches(Requisition requisition,
      RequisitionStatus status, UUID authorId) {
    Optional<StatusChange> change = requisition.getStatusChanges().stream()
        .filter(statusChange -> statusChange.getStatus() == status)
        .findFirst();
    assertTrue(change.isPresent());
    assertEquals(authorId, change.get().getAuthorId());
  }

  private void setUpGetTotalCost(BigDecimal fullSupplyCost, BigDecimal nonFullSupplyCost) {
    CurrencyUnit currency = CurrencyUnit.of(currencyCode);

    RequisitionLineItem fullSupplyItem = getRequisitionLineItem(false, true);
    fullSupplyItem.setTotalCost(Money.of(currency, fullSupplyCost));

    RequisitionLineItem nonFullSupplyItem = getRequisitionLineItem(false, false);
    nonFullSupplyItem.setTotalCost(Money.of(currency, nonFullSupplyCost));

    requisition.getRequisitionLineItems().clear();
    requisition.getRequisitionLineItems().add(fullSupplyItem);
    requisition.getRequisitionLineItems().add(nonFullSupplyItem);
  }

  private void setUpTestUpdatePacksToShip(OrderableDto productMock, long packsToShip) {
    requisitionLineItem.setPacksToShip(packsToShip);
    productMock.setId(requisitionLineItem.getOrderable().getId());
    productMock
        .getMeta()
        .setVersionNumber(requisitionLineItem.getOrderable().getVersionNumber());
    setUpValidRequisitionTemplate();
  }

  private void prepareForTestAdjustedConcumptionTotalCostAndAverageConsumption() {
    PowerMockito.spy(LineItemFieldsCalculator.class);
    when(LineItemFieldsCalculator
        .calculateAdjustedConsumption(requisitionLineItem, MONTHS_IN_PERIOD, true)
    ).thenReturn(ADJUSTED_CONSUMPTION);
    when(LineItemFieldsCalculator
        .calculateTotalCost(requisitionLineItem,
            orderable.getProgramOrderable(requisitionLineItem.getRequisition().getProgramId()),
            CURRENCY_UNIT))
        .thenReturn(TOTAL_COST);
    when(LineItemFieldsCalculator
        .calculateAverageConsumption(Collections.singletonList(ADJUSTED_CONSUMPTION)))
        .thenReturn(AVERAGE_CONSUMPTION);

    when(template.isColumnInTemplateAndDisplayed(any())).thenReturn(true);
    requisition.setTemplate(template);
  }

  private void prepareForTestAverageConsumption(List<Integer> adjustedConsumptions) {
    requisitionLineItem.setPreviousAdjustedConsumptions(adjustedConsumptions);

    PowerMockito.spy(LineItemFieldsCalculator.class);
    when(LineItemFieldsCalculator
        .calculateAdjustedConsumption(requisitionLineItem, MONTHS_IN_PERIOD, false)
    ).thenReturn(ADJUSTED_CONSUMPTION);

    setUpValidRequisitionTemplate();
  }

  private void setUpValidRequisitionTemplate() {
    when(template.isColumnInTemplateAndDisplayed(RequisitionLineItem.ADJUSTED_CONSUMPTION))
        .thenReturn(true);
    when(template.isColumnInTemplateAndDisplayed(RequisitionLineItem.AVERAGE_CONSUMPTION))
        .thenReturn(true);
    requisition.setTemplate(template);
  }

  private RequisitionLineItem mockReqLine(Requisition requisition, UUID productId,
      int stockOnHand) {
    RequisitionLineItem item = mock(RequisitionLineItem.class);
    when(item.getOrderable())
        .thenReturn(new VersionEntityReference(orderable.getId(), orderable.getVersionNumber()));
    when(item.getStockOnHand()).thenReturn(stockOnHand);

    when(requisition.findLineByProduct(productId, 1L)).thenReturn(item);
    return item;
  }

  private SupervisoryNodeDto mockSupervisoryParentNode(UUID parentId) {
    SupervisoryNodeDto parentNode = new SupervisoryNodeDto();
    parentNode.setId(parentId);
    return parentNode;
  }

  private RequisitionLineItem getRequisitionLineItem(Boolean skipped) {
    RequisitionLineItem lineItem = new RequisitionLineItemDataBuilder()
        .withRequisition(requisition)
        .withSkippedFlag(skipped)
        .build();

    OrderableDto orderable = new OrderableDtoDataBuilder()
        .withId(lineItem.getOrderable().getId())
        .withVersionNumber(lineItem.getOrderable().getVersionNumber())
        .withProgramOrderable(requisition.getProgramId(), true)
        .buildAsDto();

    orderables.put(orderable.getIdentity(), orderable);

    return lineItem;
  }

  private RequisitionLineItem getRequisitionLineItem(boolean skipped, boolean fullSupply) {
    RequisitionLineItem item = getRequisitionLineItem(skipped);
    orderables.get(new VersionIdentityDto(item.getOrderable()))
        .getProgramOrderable(requisition.getProgramId())
        .setFullSupply(fullSupply);

    return item;
  }

  private Requisition getRequisition(RequisitionLineItem notSkipped, RequisitionLineItem skipped) {
    return new RequisitionDataBuilder()
        .withProgramId(requisition.getProgramId())
        .addLineItem(notSkipped, false)
        .addLineItem(skipped, false)
        .build();
  }

  private void prepareRequisitionToHaveRequiredFieldStockOnHandNotFilled() {
    mockTemplateWithCalculatedColumn(TOTAL_CONSUMED_QUANTITY);
    requisition.getRequisitionLineItems().get(0).setStockOnHand(null);
    requisition.getRequisitionLineItems().get(0).setTotalConsumedQuantity(10);
  }

  private void prepareRequisitionToHaveRequiredFieldTotalConsumedQuantityNotFilled() {
    mockTemplateWithCalculatedColumn(STOCK_ON_HAND);
    requisition.getRequisitionLineItems().get(0).setTotalConsumedQuantity(null);
    requisition.getRequisitionLineItems().get(0).setStockOnHand(10);
  }

  private String getRequiredFieldErrorMessage(String requiredField, String calculatedColumn) {
    return new Message(ERROR_FIELD_MUST_HAVE_VALUES, requisition.getId(),
        requiredField, calculatedColumn).toString();
  }

  private String getNonNegativeNumberErrorMessage(String column, String orderableId) {
    return new Message(ERROR_VALIDATION_NON_NEGATIVE_NUMBER, column, orderableId).toString();
  }

  private String getNonPositiveNumberErrorMessage(String column, String orderableId) {
    return new Message(ERROR_VALIDATION_NON_POSITIVE_NUMBER, column, orderableId).toString();
  }

  private void mockTemplateWithCalculatedColumn(String column) {
    RequisitionTemplate template = mock(RequisitionTemplate.class);
    when(template.isColumnCalculated(column)).thenReturn(true);
    requisition.setTemplate(template);
  }

  private RequisitionTemplate mockStockBasedRequisitionTemplate() {
    RequisitionTemplate requisitionTemplate = mock(RequisitionTemplate.class);

    when(requisitionTemplate.isColumnInTemplateAndDisplayed(TOTAL_CONSUMED_QUANTITY))
        .thenReturn(true);
    when(requisitionTemplate.isPopulateStockOnHandFromStockCards()).thenReturn(true);

    when(requisitionTemplate.isColumnStockBased(TOTAL_CONSUMED_QUANTITY)).thenReturn(true);
    when(requisitionTemplate.isColumnStockBased(TOTAL_RECEIVED_QUANTITY)).thenReturn(true);
    when(requisitionTemplate.isColumnStockBased(TOTAL_LOSSES_AND_ADJUSTMENTS)).thenReturn(true);
    when(requisitionTemplate.isColumnStockBased(TOTAL_STOCKOUT_DAYS)).thenReturn(true);

    when(requisitionTemplate.findColumn(TOTAL_CONSUMED_QUANTITY)).thenReturn(totalConsumedQuantity);
    when(requisitionTemplate.findColumn(TOTAL_RECEIVED_QUANTITY)).thenReturn(totalReceivedQuantity);
    when(requisitionTemplate.findColumn(TOTAL_LOSSES_AND_ADJUSTMENTS))
        .thenReturn(totalLossesAndAdjustments);
    when(requisitionTemplate.findColumn(TOTAL_STOCKOUT_DAYS)).thenReturn(totalStockoutDays);

    when(totalConsumedQuantity.getTag()).thenReturn(CONSUMED_TAG);
    when(totalReceivedQuantity.getTag()).thenReturn(RECEIVED_TAG);
    when(totalLossesAndAdjustments.getTag()).thenReturn(ADJUSTMENT_TAG);

    return requisitionTemplate;
  }
}
