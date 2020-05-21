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

import static org.openlmis.requisition.CurrencyConfig.currencyCode;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.assertj.core.util.Lists;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.requisition.dto.OrderableDto;
import org.openlmis.requisition.dto.RequisitionLineItemDto;
import org.openlmis.requisition.testutils.ApprovedProductDtoDataBuilder;
import org.openlmis.requisition.testutils.OrderableDtoDataBuilder;
import org.openlmis.requisition.testutils.api.DataBuilder;
import org.openlmis.requisition.testutils.api.DtoDataBuilder;
import org.openlmis.requisition.testutils.api.RepositoryDataBuilder;

@SuppressWarnings("PMD.TooManyMethods")
public class RequisitionLineItemDataBuilder implements DataBuilder<RequisitionLineItem>,
    RepositoryDataBuilder<RequisitionLineItem>, DtoDataBuilder<RequisitionLineItemDto> {
  private UUID id = UUID.randomUUID();
  private VersionEntityReference orderable = new VersionEntityReference(UUID.randomUUID(), 1L);
  private VersionEntityReference facilityTypeApprovedProduct =
      new VersionEntityReference(UUID.randomUUID(), 1L);
  private Requisition requisition = new RequisitionDataBuilder().build();
  private Integer beginningBalance = 100;
  private Integer totalReceivedQuantity = 50;
  private Integer totalLossesAndAdjustments = 0;
  private Integer stockOnHand = 50;
  private Integer requestedQuantity = 100;
  private Integer totalConsumedQuantity = 100;
  private Integer total = 150;
  private String requestedQuantityExplanation = "we need more";
  private String remarks;
  private Integer approvedQuantity;
  private Integer totalStockoutDays = 0;
  private Long packsToShip = 5L;
  private Boolean skipped = false;
  private Money totalCost = asMoney(60);
  private Integer numberOfNewPatientsAdded = 0;
  private Integer adjustedConsumption = 100;
  private List<Integer> previousAdjustedConsumptions = Lists.emptyList();
  private Integer averageConsumption = 100;
  //this needs to be always averageConsumption * MaxPeriodsOfStock
  private Integer maximumStockQuantity = 300;
  //this needs to be always maximumStockQuantity - stockOnHand
  private Integer calculatedOrderQuantity = 250;
  private List<StockAdjustment> stockAdjustments = new ArrayList<>();
  private Integer idealStockAmount = 100;
  //this needs to be always idealStockAmount - stockOnHand
  private Integer calculatedOrderQuantityIsa = 50;
  private Integer additionalQuantityRequired = 0;

  /**
   * Constructs builder for {@link RequisitionLineItem}.
   */
  public RequisitionLineItemDataBuilder() {
  }

  /**
   * Builds {@link RequisitionLineItem} test data instance.
   * @return RequisitionLineItem
   */
  /**
   * Creates new instance of {@link RequisitionLineItem} with passed data.
   */
  @Override
  public RequisitionLineItem build() {
    return buildForInitiatedRegularRequisition();
  }

  @Override
  public RequisitionLineItem buildAsNew() {
    RequisitionLineItem requisitionLineItem = buildForInitiatedRegularRequisition();
    requisitionLineItem.setId(null);
    return requisitionLineItem;
  }

  /**
   * Builds {@link RequisitionLineItemDto} test data instance.
   * @return RequisitionLineItemDto
   */
  @Override
  public RequisitionLineItemDto buildAsDto() {
    RequisitionLineItem requisitionLineItem = build();

    OrderableDto orderableDto = new OrderableDtoDataBuilder()
        .withId(requisitionLineItem.getOrderable().getId())
        .withVersionNumber(requisitionLineItem.getOrderable().getVersionNumber())
        .withProgramOrderable(requisition.getProgramId(), true, Money.of(CurrencyUnit.USD, 0), 1)
        .buildAsDto();

    ApprovedProductDto approvedProductDto = new ApprovedProductDtoDataBuilder()
        .withId(requisitionLineItem.getFacilityTypeApprovedProduct().getId())
        .withVersionNumber(requisitionLineItem.getFacilityTypeApprovedProduct().getVersionNumber())
        .buildAsDto();

    RequisitionLineItemDto requisitionLineItemDto = new RequisitionLineItemDto();
    requisitionLineItemDto.setMaxPeriodsOfStock(new BigDecimal(
        approvedProductDto.getMaxPeriodsOfStock()));
    requisitionLineItem.export(requisitionLineItemDto, orderableDto, approvedProductDto);
    return requisitionLineItemDto;
  }

  /**
   * Creates new instance of {@link RequisitionLineItem} with passed data.
   */
  public RequisitionLineItem buildForInitiatedRegularRequisition() {
    RequisitionLineItem lineItem = new RequisitionLineItem(
        orderable, facilityTypeApprovedProduct, requisition, beginningBalance,
        totalReceivedQuantity, totalLossesAndAdjustments, stockOnHand, requestedQuantity,
        totalConsumedQuantity, total, requestedQuantityExplanation, remarks, approvedQuantity,
        totalStockoutDays, packsToShip, skipped, totalCost, numberOfNewPatientsAdded,
        additionalQuantityRequired, adjustedConsumption, previousAdjustedConsumptions,
        averageConsumption, maximumStockQuantity, calculatedOrderQuantity, stockAdjustments,
        idealStockAmount, calculatedOrderQuantityIsa
    );
    lineItem.setId(id);

    return lineItem;
  }

  public RequisitionLineItemDataBuilder withId(UUID id) {
    this.id = id;
    return this;
  }

  public RequisitionLineItemDataBuilder withSkippedFlag() {
    this.skipped = true;
    return this;
  }

  public RequisitionLineItemDataBuilder withSkippedFlag(Boolean skipped) {
    this.skipped = skipped;
    return this;
  }

  public RequisitionLineItemDataBuilder withIncorrectStockOnHand() {
    stockOnHand = 10;
    return this;
  }

  public RequisitionLineItemDataBuilder withIncorrectMaximumStockQuantity() {
    maximumStockQuantity = 1000;
    return this;
  }

  public RequisitionLineItemDataBuilder withIncorrectCalculatedOrderQuantityIsa() {
    calculatedOrderQuantityIsa = 1;
    return this;
  }

  public RequisitionLineItemDataBuilder withApprovedQuantity() {
    approvedQuantity = 100;
    return this;
  }

  public RequisitionLineItemDataBuilder withApprovedQuantity(Integer approvedQuantity) {
    this.approvedQuantity = approvedQuantity;
    return this;
  }

  public RequisitionLineItemDataBuilder withRemarks() {
    remarks = "OK";
    return this;
  }

  public RequisitionLineItemDataBuilder withRemarks(String remarks) {
    this.remarks = remarks;
    return this;
  }

  public RequisitionLineItemDataBuilder withTotalStockoutDays(Integer totalStockoutDays) {
    this.totalStockoutDays = totalStockoutDays;
    return this;
  }

  public RequisitionLineItemDataBuilder withStockOnHand(Integer stockOnHand) {
    this.stockOnHand = stockOnHand;
    return this;
  }

  public RequisitionLineItemDataBuilder withTotalConsumedQuantity(Integer totalConsumedQuantity) {
    this.totalConsumedQuantity = totalConsumedQuantity;
    return this;
  }

  public RequisitionLineItemDataBuilder withTotalReceivedQuantity(Integer totalReceivedQuantity) {
    this.totalReceivedQuantity = totalReceivedQuantity;
    return this;
  }

  public RequisitionLineItemDataBuilder withBeginningBalance(Integer beginningBalance) {
    this.beginningBalance = beginningBalance;
    return this;
  }

  public RequisitionLineItemDataBuilder withTotal(Integer total) {
    this.total = total;
    return this;
  }

  public RequisitionLineItemDataBuilder withTotalLossesAndAdjustments(
      Integer totalLossesAndAdjustments) {
    this.totalLossesAndAdjustments = totalLossesAndAdjustments;
    return this;
  }

  public RequisitionLineItemDataBuilder withPacksToShip(Long packsToShip) {
    this.packsToShip = packsToShip;
    return this;
  }

  public RequisitionLineItemDataBuilder addStockAdjustment(StockAdjustment adjustment) {
    this.stockAdjustments.add(adjustment);
    return this;
  }

  private Money asMoney(Number value) {
    return Money.of(CurrencyUnit.of(currencyCode), value.doubleValue());
  }

  public RequisitionLineItemDataBuilder withRequisition(Requisition requisition) {
    this.requisition = requisition;
    return this;
  }

  /**
   * Sets approved product.
   */
  public RequisitionLineItemDataBuilder withApprovedProduct(ApprovedProductDto approvedProduct) {
    OrderableDto orderable = approvedProduct.getOrderable();
    this.orderable = new VersionEntityReference(orderable.getId(), orderable.getVersionNumber());

    return this;
  }

  public RequisitionLineItemDataBuilder withIdealStockAmount(Integer idealStockAmount) {
    this.idealStockAmount = idealStockAmount;
    return this;
  }

  public RequisitionLineItemDataBuilder withStockAdjustments(
      List<StockAdjustment> stockAdjustments) {
    this.stockAdjustments = stockAdjustments;
    return this;
  }

  public RequisitionLineItemDataBuilder withRequestedQuantity(Integer requestedQuantity) {
    this.requestedQuantity = requestedQuantity;
    return this;
  }

  public RequisitionLineItemDataBuilder withRequestedQuantityExplanation(
      String requestedQuantityExplanation) {
    this.requestedQuantityExplanation = requestedQuantityExplanation;
    return this;
  }

  public RequisitionLineItemDataBuilder withTotalCost(Money totalCost) {
    this.totalCost = totalCost;
    return this;
  }

  public RequisitionLineItemDataBuilder withNumberOfNewPatientsAdded(
      Integer numberOfNewPatientsAdded) {
    this.numberOfNewPatientsAdded = numberOfNewPatientsAdded;
    return this;
  }

  public RequisitionLineItemDataBuilder withAdditionalQuantityRequired(
      Integer additionalQuantityRequired) {
    this.additionalQuantityRequired = additionalQuantityRequired;
    return this;
  }

  public RequisitionLineItemDataBuilder withOrderable(UUID orderableId, Long versionNumber) {
    this.orderable = new VersionEntityReference(orderableId, versionNumber);
    return this;
  }

  public RequisitionLineItemDataBuilder withFacilityTypeApprovedProduct(UUID approvedProductId,
      Long versionNumber) {
    this.facilityTypeApprovedProduct = new VersionEntityReference(approvedProductId, versionNumber);
    return this;
  }

  public RequisitionLineItemDataBuilder withAverageConsumption(Integer averageConsumption) {
    this.averageConsumption = averageConsumption;
    return this;
  }

  public RequisitionLineItemDataBuilder withCalculatedOrderQuantity(
      Integer calculatedOrderQuantity) {
    this.calculatedOrderQuantity = calculatedOrderQuantity;
    return this;
  }

  public RequisitionLineItemDataBuilder withPreviousAdjustedConsumptions(
      List<Integer> previousAdjustedConsumptions) {
    this.previousAdjustedConsumptions = previousAdjustedConsumptions;
    return this;
  }

  public RequisitionLineItemDataBuilder withAdjustedConsumption(Integer adjustedConsumption) {
    this.adjustedConsumption = adjustedConsumption;
    return this;
  }

  public RequisitionLineItemDataBuilder withMaximumStockQuantity(Integer maximumStockQuantity) {
    this.maximumStockQuantity = maximumStockQuantity;
    return this;
  }

  /**
   * Sets all fields to null.
   */
  public RequisitionLineItemDataBuilder withEmptyNumericFields() {
    beginningBalance = null;
    totalReceivedQuantity = null;
    totalLossesAndAdjustments = null;
    stockOnHand = null;
    requestedQuantity = null;
    totalConsumedQuantity = null;
    total = null;
    approvedQuantity = null;
    totalStockoutDays = null;
    packsToShip = null;
    totalCost = null;
    numberOfNewPatientsAdded = null;
    adjustedConsumption = null;
    averageConsumption = null;
    maximumStockQuantity = null;
    calculatedOrderQuantity = null;
    idealStockAmount = null;
    calculatedOrderQuantityIsa = null;
    additionalQuantityRequired = null;
    return this;
  }
}
