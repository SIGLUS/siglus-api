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

package org.openlmis.requisition.utils;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.openlmis.requisition.domain.requisition.RequisitionLineItem.STOCK_ON_HAND;
import static org.openlmis.requisition.domain.requisition.RequisitionLineItem.TOTAL_CONSUMED_QUANTITY;
import static org.openlmis.requisition.domain.requisition.RequisitionLineItem.TOTAL_LOSSES_AND_ADJUSTMENTS;
import static org.openlmis.requisition.domain.requisition.RequisitionLineItem.TOTAL_RECEIVED_QUANTITY;

import com.google.common.collect.Maps;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.requisition.domain.RequisitionTemplate;
import org.openlmis.requisition.domain.RequisitionTemplateColumn;
import org.openlmis.requisition.domain.RequisitionTemplateColumnDataBuilder;
import org.openlmis.requisition.domain.RequisitionTemplateDataBuilder;
import org.openlmis.requisition.domain.requisition.DatePhysicalStockCountCompleted;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionDataBuilder;
import org.openlmis.requisition.domain.requisition.RequisitionLineItem;
import org.openlmis.requisition.domain.requisition.RequisitionLineItemDataBuilder;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.domain.requisition.StatusChange;
import org.openlmis.requisition.domain.requisition.StockAdjustment;
import org.openlmis.requisition.domain.requisition.StockAdjustmentDataBuilder;
import org.openlmis.requisition.domain.requisition.StockAdjustmentReason;
import org.openlmis.requisition.dto.OrderableDto;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.openlmis.requisition.dto.VersionIdentityDto;
import org.openlmis.requisition.dto.stockmanagement.StockCardDto;
import org.openlmis.requisition.dto.stockmanagement.StockEventAdjustmentDto;
import org.openlmis.requisition.dto.stockmanagement.StockEventDto;
import org.openlmis.requisition.dto.stockmanagement.StockEventLineItemDto;
import org.openlmis.requisition.service.referencedata.PeriodReferenceDataService;
import org.openlmis.requisition.service.stockmanagement.StockCardStockManagementService;
import org.openlmis.requisition.settings.service.ConfigurationSettingService;
import org.openlmis.requisition.testutils.OrderableDtoDataBuilder;
import org.openlmis.requisition.testutils.ProcessingPeriodDtoDataBuilder;
import org.openlmis.requisition.testutils.StatusChangeDataBuilder;
import org.openlmis.requisition.testutils.StockAdjustmentReasonDataBuilder;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("PMD.TooManyMethods")
public class StockEventBuilderTest {

  private static final DatePhysicalStockCountCompleted DATE_PHYSICAL_STOCK_COUNT_COMPLETED =
      new DatePhysicalStockCountCompleted(LocalDate.now().minusDays(3));
  private static final LocalDate PERIOD_END_DATE = LocalDate.now().minusDays(1);
  private static final ZoneId ZONE_ID = ZoneId.systemDefault();

  private Requisition requisition;

  private RequisitionTemplateColumn totalReceivedQuantityColumn;

  private RequisitionTemplateColumn totalConsumedQuantityColumn;

  private RequisitionTemplateColumn totalLossesAndAdjustmentsColumn;

  private RequisitionTemplateColumn stockOnHandColumn;

  private RequisitionLineItem lineItemOneDto;

  private RequisitionLineItem lineItemTwoDto;

  private List<StockAdjustmentReason> reasons;

  private StockAdjustmentReason receiptsReason;

  private StockAdjustmentReason consumedReason;

  private StockAdjustmentReason beginningBalanceExcess;

  private StockAdjustmentReason beginningBalanceInsufficiency;

  private ProcessingPeriodDto period;

  private UUID userId = UUID.randomUUID();

  private List<StockCardDto> stockCards;

  private Map<VersionIdentityDto, OrderableDto> orderables = Maps.newHashMap();

  @Mock
  private DateHelper dateHelper;

  @Mock
  private PeriodReferenceDataService periodReferenceDataService;

  @Mock
  private StockCardStockManagementService stockCardStockManagementService;

  @Mock
  private ConfigurationSettingService configurationSettingService;

  @InjectMocks
  private StockEventBuilder stockEventBuilder;

  @Before
  public void setUp() {
    period = preparePeriod();
    stockCards = new ArrayList<>();
    requisition = prepareRequisitionDto(period.getId());

    when(configurationSettingService.getReasonIdForReceipts())
        .thenReturn(receiptsReason.getReasonId());
    when(configurationSettingService.getReasonIdForConsumed())
        .thenReturn(consumedReason.getReasonId());
    when(configurationSettingService.getReasonIdForBeginningBalanceExcess())
        .thenReturn(beginningBalanceExcess.getReasonId());
    when(configurationSettingService.getReasonIdForBeginningBalanceInsufficiency())
        .thenReturn(beginningBalanceInsufficiency.getReasonId());

    when(dateHelper.getZone()).thenReturn(ZONE_ID);
    when(periodReferenceDataService.findOne(period.getId())).thenReturn(period);
    when(stockCardStockManagementService.getStockCards(
        requisition.getFacilityId(),
        requisition.getProgramId())
    ).thenReturn(stockCards);
  }

  @Test
  public void itShouldIncludeReceiptsIfTotalReceivedQuantityIsDisplayed() throws Exception {
    StockEventDto result = getStockEventDto();

    assertThat(result.getLineItems().size()).isGreaterThan(0);
    result.getLineItems()
        .forEach(lineItem -> assertThat(containsReason(lineItem, receiptsReason)).isTrue());
  }

  @Test
  public void itShouldNotIncludeReceiptsIfTotalReceivedQuantityDoesNotExist() throws Exception {
    Map<String, RequisitionTemplateColumn> columns = new HashMap<>(
        requisition.getTemplate().viewColumns()
    );
    columns.remove(TOTAL_RECEIVED_QUANTITY);

    requisition.setTemplate(new RequisitionTemplate(columns));

    StockEventDto result = getStockEventDto();

    assertThat(result.getLineItems().size()).isGreaterThan(0);
    result.getLineItems()
        .forEach(lineItem -> assertThat(containsReason(lineItem, receiptsReason)).isFalse());
  }

  @Test
  public void itShouldNotIncludeReceiptsIfTotalReceivedQuantityIsNotDisplayed() throws Exception {
    totalReceivedQuantityColumn.setIsDisplayed(false);

    StockEventDto result = getStockEventDto();

    assertThat(result.getLineItems().size()).isGreaterThan(0);
    result.getLineItems()
        .forEach(lineItem -> assertThat(containsReason(lineItem, receiptsReason)).isFalse());
  }

  @Test
  public void itShouldNotIncludeReceiptsIfTheReasonDoesNotExist() throws Exception {
    reasons.remove(receiptsReason);

    StockEventDto result = getStockEventDto();

    assertThat(result.getLineItems().size()).isGreaterThan(0);
    result.getLineItems()
        .forEach(lineItem -> assertThat(containsReason(lineItem, receiptsReason)).isFalse());
  }

  @Test
  public void itShouldIncludeConsumedIfTotalConsumedQuantityIsDisplayed() throws Exception {
    StockEventDto result = getStockEventDto();

    assertThat(result.getLineItems().size()).isGreaterThan(0);
    result.getLineItems()
        .forEach(lineItem -> assertThat(containsReason(lineItem, consumedReason)).isTrue());
  }

  @Test
  public void itShouldNotIncludeConsumedIfTotalConsumedQuantityIsNotDisplayed() throws Exception {
    totalConsumedQuantityColumn.setIsDisplayed(false);

    StockEventDto result = getStockEventDto();

    assertThat(result.getLineItems().size()).isGreaterThan(0);
    result.getLineItems()
        .forEach(lineItem -> assertThat(containsReason(lineItem, consumedReason)).isFalse());
  }

  @Test
  public void itShouldNotIncludeConsumedIfTotalConsumedQuantityDoesNotExist() throws Exception {
    Map<String, RequisitionTemplateColumn> columns = new HashMap<>(
        requisition.getTemplate().viewColumns()
    );
    columns.remove(TOTAL_CONSUMED_QUANTITY);

    requisition.setTemplate(new RequisitionTemplate(columns));

    StockEventDto result = getStockEventDto();

    assertThat(result.getLineItems().size()).isGreaterThan(0);
    result.getLineItems()
        .forEach(lineItem -> assertThat(containsReason(lineItem, consumedReason)).isFalse());
  }

  @Test
  public void itShouldNotIncludeConsumedIfTheReasonDoesNotExist() throws Exception {
    reasons.remove(consumedReason);

    StockEventDto result = getStockEventDto();

    assertThat(result.getLineItems().size()).isGreaterThan(0);
    result.getLineItems()
        .forEach(lineItem -> assertThat(containsReason(lineItem, consumedReason)).isFalse());
  }

  @Test
  public void itShouldNotIncludeAdditionalAdjustmentsIfTotalLossesAndAdjustmentsAreNotDisplayed()
      throws Exception {
    StockEventDto result = getStockEventDto();

    assertThat(result.getLineItems().get(0).getStockAdjustments().size()).isEqualTo(4);
    assertThat(result.getLineItems().get(0).getStockAdjustments().get(0))
        .isEqualToComparingFieldByFieldRecursively(new StockEventAdjustmentDto(
            reasons.get(0).getReasonId(), 24));
    assertThat(result.getLineItems().get(0).getStockAdjustments().get(1))
        .isEqualToComparingFieldByFieldRecursively(new StockEventAdjustmentDto(
            reasons.get(2).getReasonId(), 25));

    assertThat(result.getLineItems().get(1).getStockAdjustments().size()).isEqualTo(4);
    assertThat(result.getLineItems().get(1).getStockAdjustments().get(0))
        .isEqualToComparingFieldByFieldRecursively(new StockEventAdjustmentDto(
            reasons.get(1).getReasonId(), 37));
    assertThat(result.getLineItems().get(1).getStockAdjustments().get(1))
        .isEqualToComparingFieldByFieldRecursively(new StockEventAdjustmentDto(
            reasons.get(3).getReasonId(), 38));
  }

  @Test
  public void itShouldIncludeAdditionalAdjustmentsIfTotalLossesAndAdjustmentsAreDisplayed()
      throws Exception {
    totalLossesAndAdjustmentsColumn.setIsDisplayed(false);

    StockEventDto result = getStockEventDto();

    assertThat(result.getLineItems().get(0).getStockAdjustments().size()).isEqualTo(2);
    assertThat(result.getLineItems().get(0).getStockAdjustments().get(0))
        .isEqualToComparingFieldByFieldRecursively(new StockEventAdjustmentDto(
            reasons.get(4).getReasonId(), 23));
    assertThat(result.getLineItems().get(0).getStockAdjustments().get(1))
        .isEqualToComparingFieldByFieldRecursively(new StockEventAdjustmentDto(
            reasons.get(5).getReasonId(), 22));

    assertThat(result.getLineItems().get(1).getStockAdjustments().size()).isEqualTo(2);
    assertThat(result.getLineItems().get(1).getStockAdjustments().get(0))
        .isEqualToComparingFieldByFieldRecursively(new StockEventAdjustmentDto(
            reasons.get(4).getReasonId(), 36));
    assertThat(result.getLineItems().get(1).getStockAdjustments().get(1))
        .isEqualToComparingFieldByFieldRecursively(new StockEventAdjustmentDto(
            reasons.get(5).getReasonId(), 35));
  }

  @Test
  public void itShouldMapStockOnHandIfItIsDisplayed() throws Exception {
    StockEventDto result = getStockEventDto();

    assertThat(result.getLineItems().size()).isEqualTo(2);
    assertThat(result.getLineItems().get(0).getQuantity())
        .isEqualTo(lineItemOneDto.getStockOnHand());
    assertThat(result.getLineItems().get(1).getQuantity())
        .isEqualTo(lineItemTwoDto.getStockOnHand());
  }

  @Test
  public void itShouldMapStockOnHandEvenIfItIsNotDisplayed() throws Exception {
    stockOnHandColumn.setIsDisplayed(false);

    StockEventDto result = getStockEventDto();

    assertThat(result.getLineItems().size()).isEqualTo(2);
    assertThat(result.getLineItems().get(0).getQuantity())
        .isEqualTo(lineItemOneDto.getStockOnHand());
    assertThat(result.getLineItems().get(1).getQuantity())
        .isEqualTo(lineItemTwoDto.getStockOnHand());
  }

  @Test
  public void itShouldUseDatePhysicalStockCountCompletedIfItIsGiven() throws Exception {
    StockEventDto result = getStockEventDto();

    assertThat(result.getLineItems().size()).isEqualTo(2);
    result.getLineItems().forEach(lineItem -> assertThat(lineItem.getOccurredDate())
        .isEqualByComparingTo(DATE_PHYSICAL_STOCK_COUNT_COMPLETED.getLocalDate()));
  }

  @Test
  public void itShouldUseSubmittedDateOnEmergencyRequisition() throws Exception {
    requisition.setDatePhysicalStockCountCompleted(null);
    requisition.setEmergency(true);

    ZonedDateTime dateTime = ZonedDateTime.now();
    StatusChange statusChange = new StatusChangeDataBuilder()
        .withCreatedDate(dateTime)
        .withStatus(RequisitionStatus.SUBMITTED)
        .build();
    requisition.setStatusChanges(Arrays.asList(statusChange));

    StockEventDto result = getStockEventDto();

    assertThat(result.getLineItems().size()).isEqualTo(2);
    result.getLineItems().forEach(lineItem -> assertThat(lineItem.getOccurredDate())
        .isEqualByComparingTo(dateTime.toLocalDate()));
  }

  @Test
  public void itShouldUsePeriodEndDateIfDatePhysicalStockCountCompletedIsNull() throws Exception {
    requisition.setDatePhysicalStockCountCompleted(null);

    StockEventDto result = getStockEventDto();

    assertThat(result.getLineItems().size()).isEqualTo(2);
    result.getLineItems().forEach(lineItem -> assertThat(lineItem.getOccurredDate())
        .isEqualByComparingTo(PERIOD_END_DATE));

  }

  @Test
  public void itShouldIncludeOrderableIds() throws Exception {
    StockEventDto result = getStockEventDto();

    assertThat(result.getLineItems().get(0).getOrderableId())
        .isEqualTo(lineItemOneDto.getOrderable().getId());
    assertThat(result.getLineItems().get(1).getOrderableId())
        .isEqualTo(lineItemTwoDto.getOrderable().getId());
  }

  @Test
  public void itShouldIgnoreSkippedLineItems() throws Exception {
    lineItemOneDto.setSkipped(true);

    StockEventDto result = getStockEventDto();

    assertThat(result.getLineItems().size()).isEqualTo(1);
    assertThat(result.getLineItems().get(0).getQuantity())
        .isNotEqualTo(lineItemOneDto.getStockOnHand());
  }

  @Test
  public void itShouldIgnoreNonFullSupplyLineItems() throws Exception {
    orderables
        .get(new VersionIdentityDto(lineItemOneDto.getOrderable()))
        .getProgramOrderable(requisition.getProgramId())
        .setFullSupply(false);

    StockEventDto result = getStockEventDto();

    assertThat(result.getLineItems().size()).isEqualTo(1);
    assertThat(result.getLineItems().get(0).getQuantity())
        .isNotEqualTo(lineItemOneDto.getStockOnHand());
  }

  @Test
  public void itShouldIncludeUserId() throws Exception {
    StockEventDto result = getStockEventDto();

    assertThat(result.getUserId())
        .isEqualTo(userId);
  }

  @Test
  public void itShouldIncludeBeginningBalanceExcessIfBeginningBalanceIsBiggerThanStockOnHand() {
    lineItemOneDto.setBeginningBalance(20);

    StockEventDto result = getStockEventDto();

    assertThat(result.getLineItems().get(0).getStockAdjustments().size()).isEqualTo(5);
    assertThat(result.getLineItems().get(0).getStockAdjustments().get(4))
        .isEqualToComparingFieldByFieldRecursively(new StockEventAdjustmentDto(
            beginningBalanceInsufficiency.getReasonId(), 10));
  }

  @Test
  public void itShouldNotIncludeBeginningBalanceExcessIfStockCardIsNull() {
    lineItemOneDto.setBeginningBalance(20);
    stockCards.clear();

    StockEventDto result = getStockEventDto();

    assertThat(result.getLineItems().get(0).getStockAdjustments().size()).isEqualTo(4);
  }

  @Test
  public void
      itShouldIncludeBeginningBalanceInsufficiencyIfBeginningBalanceIsLowerThanStockOnHand() {
    lineItemTwoDto.setBeginningBalance(33);

    StockEventDto result = getStockEventDto();

    assertThat(result.getLineItems().get(1).getStockAdjustments().size()).isEqualTo(5);
    assertThat(result.getLineItems().get(1).getStockAdjustments().get(4))
        .isEqualToComparingFieldByFieldRecursively(new StockEventAdjustmentDto(
            beginningBalanceExcess.getReasonId(), 3));
  }

  @Test
  public void itShouldNotIncludeBeginningBalanceInsufficiencyIfStockCardIsNull() {
    lineItemOneDto.setBeginningBalance(20);
    stockCards.clear();

    StockEventDto result = getStockEventDto();

    assertThat(result.getLineItems().get(0).getStockAdjustments().size()).isEqualTo(4);
  }

  private RequisitionLineItem prepareLineItemOneDto() {
    lineItemOneDto = new RequisitionLineItemDataBuilder()
        .withSkippedFlag(false)
        .withBeginningBalance(30)
        .withStockOnHand(21)
        .withTotalReceivedQuantity(22)
        .withTotalConsumedQuantity(23)
        .withStockAdjustments(Arrays.asList(
            prepareStockAdjustment(reasons.get(0), 24),
            prepareStockAdjustment(reasons.get(2), 25)
        ))
        .withOrderable(UUID.randomUUID(), 1L)
        .build();
    stockCards.add(StockCardDto.builder()
        .orderable(new OrderableDtoDataBuilder()
            .withId(lineItemOneDto.getOrderable().getId())
            .buildAsDto())
        .stockOnHand(30)
        .build());


    return lineItemOneDto;
  }

  private RequisitionLineItem prepareLineItemTwoDto() {
    lineItemTwoDto = new RequisitionLineItemDataBuilder()
      .withSkippedFlag(false)
      .withBeginningBalance(30)
      .withStockOnHand(34)
      .withTotalReceivedQuantity(35)
      .withTotalConsumedQuantity(36)
      .withStockAdjustments(Arrays.asList(
        prepareStockAdjustment(reasons.get(1), 37),
        prepareStockAdjustment(reasons.get(3), 38)
      ))
      .withOrderable(UUID.randomUUID(), 1L)
      .build();

    stockCards.add(StockCardDto.builder()
        .orderable(new OrderableDtoDataBuilder()
            .withId(lineItemTwoDto.getOrderable().getId())
            .buildAsDto())
        .stockOnHand(30)
        .build());

    return lineItemTwoDto;
  }

  private Requisition prepareRequisitionDto(UUID periodId) {
    Requisition requisition = new RequisitionDataBuilder()
        .withDatePhysicalStockCountCompleted(DATE_PHYSICAL_STOCK_COUNT_COMPLETED)
        .withTemplate(prepareTemplate())
        .withFacilityId(UUID.randomUUID())
        .withProgramId(UUID.randomUUID())
        .withStockAdjustmentReasons(prepareReasons())
        .withProcessingPeriodId(periodId)
        .withRequisitionLineItems(Arrays.asList(
            prepareLineItemOneDto(),
            prepareLineItemTwoDto()
        ))
        .withEmergency(false)
        .build();

    orderables = requisition
        .getRequisitionLineItems()
        .stream()
        .map(line -> new OrderableDtoDataBuilder()
            .withId(line.getOrderable().getId())
            .withVersionNumber(line.getOrderable().getVersionNumber())
            .withProgramOrderable(requisition.getProgramId(), true)
            .buildAsDto())
        .collect(Collectors.toMap(OrderableDto::getIdentity, Function.identity()));

    return requisition;
  }

  private RequisitionTemplate prepareTemplate() {
    return new RequisitionTemplateDataBuilder().withColumns(prepareColumnsMap()).build();
  }

  private Map<String, RequisitionTemplateColumn> prepareColumnsMap() {
    totalReceivedQuantityColumn = prepareColumn(TOTAL_RECEIVED_QUANTITY);
    totalConsumedQuantityColumn = prepareColumn(TOTAL_CONSUMED_QUANTITY);
    totalLossesAndAdjustmentsColumn = prepareColumn(TOTAL_LOSSES_AND_ADJUSTMENTS);
    stockOnHandColumn = prepareColumn(STOCK_ON_HAND);

    Map<String, RequisitionTemplateColumn> columnsMap = new HashMap<>();
    columnsMap.put(TOTAL_CONSUMED_QUANTITY, totalConsumedQuantityColumn);
    columnsMap.put(TOTAL_RECEIVED_QUANTITY, totalReceivedQuantityColumn);
    columnsMap.put(TOTAL_LOSSES_AND_ADJUSTMENTS, totalLossesAndAdjustmentsColumn);
    columnsMap.put(STOCK_ON_HAND, stockOnHandColumn);

    return columnsMap;
  }

  private RequisitionTemplateColumn prepareColumn(String columnName) {
    return new RequisitionTemplateColumnDataBuilder()
        .withName(columnName)
        .build();
  }

  private List<StockAdjustmentReason> prepareReasons() {
    consumedReason = prepareReason("Consumed");
    receiptsReason = prepareReason("Receipts");
    beginningBalanceExcess = prepareReason("Beginning Balance Excess");
    beginningBalanceInsufficiency = prepareReason("Beginning Balance Insufficiency");

    reasons = new ArrayList<>();

    reasons.add(prepareReason("Reason One"));
    reasons.add(prepareReason("Reason Two"));
    reasons.add(prepareReason("Reason Three"));
    reasons.add(prepareReason("Reason Four"));
    reasons.add(consumedReason);
    reasons.add(receiptsReason);
    reasons.add(beginningBalanceExcess);
    reasons.add(beginningBalanceInsufficiency);

    return reasons;
  }

  private boolean containsReason(StockEventLineItemDto lineItem,
                                 StockAdjustmentReason reason) {
    return lineItem
        .getStockAdjustments()
        .stream()
        .anyMatch(stockAdjustment -> stockAdjustment.getReasonId().equals(reason.getReasonId()));
  }

  private ProcessingPeriodDto preparePeriod() {
    return new ProcessingPeriodDtoDataBuilder()
        .withEndDate(PERIOD_END_DATE)
        .buildAsDto();
  }

  private StockAdjustmentReason prepareReason(String reasonName) {
    StockAdjustmentReason reason = new StockAdjustmentReasonDataBuilder()
        .withName(reasonName)
        .build();

    return reason;
  }

  private StockAdjustment prepareStockAdjustment(StockAdjustmentReason reason, Integer quantity) {
    StockAdjustment stockAdjustment = new StockAdjustmentDataBuilder()
        .withQuantity(quantity)
        .withReasonId(reason.getReasonId())
        .build();

    return stockAdjustment;
  }

  private StockEventDto getStockEventDto() {
    return stockEventBuilder.fromRequisition(requisition, userId, orderables);
  }
}
