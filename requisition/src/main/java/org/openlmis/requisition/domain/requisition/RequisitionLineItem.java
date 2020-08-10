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
import static org.openlmis.requisition.domain.requisition.LineItemFieldsCalculator.calculateAdjustedConsumption;
import static org.openlmis.requisition.domain.requisition.LineItemFieldsCalculator.calculateAverageConsumption;
import static org.openlmis.requisition.domain.requisition.LineItemFieldsCalculator.calculateCalculatedOrderQuantity;
import static org.openlmis.requisition.domain.requisition.LineItemFieldsCalculator.calculateCalculatedOrderQuantityIsa;
import static org.openlmis.requisition.domain.requisition.LineItemFieldsCalculator.calculateMaximumStockQuantity;
import static org.openlmis.requisition.domain.requisition.LineItemFieldsCalculator.calculateStockBasedAverageConsumption;
import static org.openlmis.requisition.domain.requisition.LineItemFieldsCalculator.calculateStockBasedTotalConsumedQuantity;
import static org.openlmis.requisition.domain.requisition.LineItemFieldsCalculator.calculateStockBasedTotalLossesAndAdjustments;
import static org.openlmis.requisition.domain.requisition.LineItemFieldsCalculator.calculateStockBasedTotalReceivedQuantity;
import static org.openlmis.requisition.domain.requisition.LineItemFieldsCalculator.calculateStockBasedTotalStockoutDays;
import static org.openlmis.requisition.domain.requisition.LineItemFieldsCalculator.calculateStockOnHand;
import static org.openlmis.requisition.domain.requisition.LineItemFieldsCalculator.calculateTotal;
import static org.openlmis.requisition.domain.requisition.LineItemFieldsCalculator.calculateTotalConsumedQuantity;
import static org.openlmis.requisition.domain.requisition.LineItemFieldsCalculator.calculateTotalLossesAndAdjustments;

import com.google.common.collect.Sets;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Type;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.openlmis.requisition.domain.BaseEntity;
import org.openlmis.requisition.domain.RequisitionTemplate;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.requisition.dto.OrderableDto;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.openlmis.requisition.dto.ProgramOrderableDto;
import org.openlmis.requisition.dto.VersionIdentityDto;
import org.openlmis.requisition.dto.stockmanagement.StockCardRangeSummaryDto;
import org.openlmis.requisition.exception.ValidationMessageException;
import org.openlmis.requisition.i18n.MessageKeys;
import org.openlmis.requisition.utils.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("PMD.TooManyMethods")
@Entity
@Table(name = "requisition_line_items", schema = "requisition")
@AllArgsConstructor
public class RequisitionLineItem extends BaseEntity {
  private static final Logger LOGGER = LoggerFactory.getLogger(RequisitionLineItem.class);

  public static final String REQUESTED_QUANTITY = "requestedQuantity";
  public static final String REQUESTED_QUANTITY_EXPLANATION = "requestedQuantityExplanation";
  public static final String BEGINNING_BALANCE = "beginningBalance";
  public static final String TOTAL_RECEIVED_QUANTITY = "totalReceivedQuantity";
  public static final String STOCK_ON_HAND = "stockOnHand";
  public static final String TOTAL_CONSUMED_QUANTITY = "totalConsumedQuantity";
  public static final String TOTAL_LOSSES_AND_ADJUSTMENTS = "totalLossesAndAdjustments";
  public static final String APPROVED_QUANTITY = "approvedQuantity";
  public static final String REMARKS_COLUMN = "remarks";
  public static final String TOTAL_STOCKOUT_DAYS = "totalStockoutDays";
  public static final String TOTAL_COLUMN = "total";
  public static final BigDecimal PRICE_PER_PACK_IF_NULL = BigDecimal.ZERO;
  public static final String NUMBER_OF_NEW_PATIENTS_ADDED = "numberOfNewPatientsAdded";
  public static final String SKIPPED_COLUMN = "skipped";
  public static final String ADJUSTED_CONSUMPTION = "adjustedConsumption";
  public static final String AVERAGE_CONSUMPTION = "averageConsumption";
  public static final String MAXIMUM_STOCK_QUANTITY = "maximumStockQuantity";
  public static final String CALCULATED_ORDER_QUANTITY = "calculatedOrderQuantity";
  public static final String CALCULATED_ORDER_QUANTITY_ISA = "calculatedOrderQuantityIsa";
  public static final String ADDITIONAL_QUANTITY_REQUIRED = "additionalQuantityRequired";

  @Embedded
  @AttributeOverrides({
      @AttributeOverride(name = "id", column = @Column(name = "orderableId")),
      @AttributeOverride(name = "versionNumber", column = @Column(name = "orderableVersionNumber"))
  })
  // [SIGLUS change start]
  // [change reason]: enable print requisition
  @Setter
  // [SIGLUS change end]
  @Getter
  private VersionEntityReference orderable;

  @Setter
  @Getter
  @Embedded
  @AttributeOverrides({
      @AttributeOverride(name = "id", column = @Column(
          name = "facilityTypeApprovedProductId")),
      @AttributeOverride(name = "versionNumber", column = @Column(
          name = "facilityTypeApprovedProductVersionNumber"))
  })
  private VersionEntityReference facilityTypeApprovedProduct;

  @ManyToOne(cascade = CascadeType.REFRESH)
  @JoinColumn(name = "requisitionId")
  @Getter
  @Setter
  private Requisition requisition;

  @Getter
  @Setter
  private Integer beginningBalance;

  @Getter
  @Setter
  private Integer totalReceivedQuantity;

  @Getter
  @Setter
  private Integer totalLossesAndAdjustments;

  @Getter
  @Setter
  private Integer stockOnHand;

  @Getter
  @Setter
  private Integer requestedQuantity;

  @Getter
  @Setter
  private Integer totalConsumedQuantity;

  @Getter
  @Setter
  private Integer total;

  @Getter
  @Setter
  private String requestedQuantityExplanation;

  @Column(length = 250)
  @Getter
  @Setter
  private String remarks;

  @Getter
  @Setter
  private Integer approvedQuantity;

  @Getter
  @Setter
  private Integer totalStockoutDays;

  @Getter
  @Setter
  private Long packsToShip;

  @Getter
  @Setter
  private Boolean skipped;

  @Getter
  @Setter
  @Type(type = "org.openlmis.requisition.domain.type.CustomSingleColumnMoneyUserType")
  private Money totalCost;

  @Setter
  @Getter
  private Integer numberOfNewPatientsAdded;

  @Setter
  @Getter
  private Integer additionalQuantityRequired;

  @Setter
  @Getter
  private Integer adjustedConsumption;

  @ElementCollection
  @CollectionTable(name = "previous_adjusted_consumptions", schema = "requisition",
      joinColumns = @JoinColumn(name = "requisitionLineItemId"))
  @Column(name = "previousAdjustedConsumption")
  @Setter
  @Getter
  private List<Integer> previousAdjustedConsumptions;

  @Setter
  @Getter
  private Integer averageConsumption;

  @Getter
  @Setter
  private Integer maximumStockQuantity;

  @Getter
  @Setter
  private Integer calculatedOrderQuantity;

  @OneToMany(
      cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.REMOVE},
      fetch = FetchType.LAZY,
      orphanRemoval = true)
  @Getter
  @Setter
  @JoinColumn(name = "requisitionLineItemId")
  @BatchSize(size = STANDARD_BATCH_SIZE)
  private List<StockAdjustment> stockAdjustments;

  @Setter
  @Getter
  private Integer idealStockAmount;

  @Setter
  @Getter
  private Integer calculatedOrderQuantityIsa;

  /**
   * Initiates a requisition line item.
   */
  public RequisitionLineItem() {
    stockAdjustments = new ArrayList<>();
    this.skipped = false;
    previousAdjustedConsumptions = new ArrayList<>();
  }

  /**
   * Initiates a full supply requisition line item with specified requisition and product.
   *
   * @param requisition     requisition to apply
   * @param approvedProduct facilityTypeApprovedProduct to apply
   */
  RequisitionLineItem(Requisition requisition, ApprovedProductDto approvedProduct) {
    this();
    this.requisition = requisition;
    this.facilityTypeApprovedProduct = new VersionEntityReference(approvedProduct.getId(),
        approvedProduct.getVersionNumber());

    OrderableDto orderableDto = approvedProduct.getOrderable();

    // the method will throw exception if program orderable does not exist.
    // orderableDto.getProgramOrderable(requisition.getProgramId());
    // [SIGLUS change end]

    this.orderable = new VersionEntityReference(orderableDto.getId(),
        orderableDto.getVersionNumber());
  }

  /**
   * Copy constructor.
   *
   * @param original an original line item with data that will be placed in a new line item.
   */
  public RequisitionLineItem(RequisitionLineItem original) {
    this(null, null, original.requisition,
        original.beginningBalance, original.totalReceivedQuantity,
        original.totalLossesAndAdjustments, original.stockOnHand, original.requestedQuantity,
        original.totalConsumedQuantity, original.total, original.requestedQuantityExplanation,
        original.remarks, original.approvedQuantity, original.totalStockoutDays,
        original.packsToShip, original.skipped, original.totalCost,
        original.numberOfNewPatientsAdded, original.additionalQuantityRequired,
        original.adjustedConsumption, original.previousAdjustedConsumptions,
        original.averageConsumption, original.maximumStockQuantity,
        original.calculatedOrderQuantity, null,
        original.idealStockAmount, original.calculatedOrderQuantityIsa);
    setId(original.getId());
    this.orderable = new VersionEntityReference(original.orderable);
    this.facilityTypeApprovedProduct = new VersionEntityReference(
        original.facilityTypeApprovedProduct);
    this.stockAdjustments = original
        .stockAdjustments
        .stream()
        .map(item -> new StockAdjustment(item.getReasonId(), item.getQuantity()))
        .collect(Collectors.toList());
  }

  /**
   * Creates new instance of RequisitionLineItem object based on data from
   * {@link Importer}.
   *
   * @param importer instance of {@link Importer}
   * @return new instance of RequisitionLineItem.
   */
  static RequisitionLineItem newRequisitionLineItem(Importer importer) {

    RequisitionLineItem requisitionLineItem = new RequisitionLineItem();
    requisitionLineItem.setId(importer.getId());
    if (importer.getOrderableIdentity() != null) {
      VersionIdentityDto orderable = importer.getOrderableIdentity();
      requisitionLineItem.orderable = new VersionEntityReference(
          orderable.getId(), orderable.getVersionNumber());
    }
    if (importer.getApprovedProductIdentity() != null) {
      VersionIdentityDto approvedProduct = importer.getApprovedProductIdentity();
      requisitionLineItem.facilityTypeApprovedProduct = new VersionEntityReference(
          approvedProduct.getId(), approvedProduct.getVersionNumber());
    }
    requisitionLineItem.setBeginningBalance(importer.getBeginningBalance());
    requisitionLineItem.setTotalReceivedQuantity(importer.getTotalReceivedQuantity());
    requisitionLineItem.setTotalLossesAndAdjustments(importer.getTotalLossesAndAdjustments());
    requisitionLineItem.setStockOnHand(importer.getStockOnHand());
    requisitionLineItem.setRequestedQuantity(importer.getRequestedQuantity());
    requisitionLineItem.setTotalConsumedQuantity(importer.getTotalConsumedQuantity());
    requisitionLineItem.setRequestedQuantityExplanation(importer.getRequestedQuantityExplanation());
    requisitionLineItem.setRemarks(importer.getRemarks());
    requisitionLineItem.setApprovedQuantity(importer.getApprovedQuantity());
    requisitionLineItem.setTotalStockoutDays(importer.getTotalStockoutDays());
    requisitionLineItem.setPacksToShip(importer.getPacksToShip());
    requisitionLineItem.setNumberOfNewPatientsAdded(importer.getNumberOfNewPatientsAdded());
    requisitionLineItem.setTotal(importer.getTotal());
    requisitionLineItem.setTotalCost(importer.getTotalCost());
    requisitionLineItem.setAdjustedConsumption(importer.getAdjustedConsumption());
    requisitionLineItem.setAverageConsumption(importer.getAverageConsumption());
    requisitionLineItem.setMaximumStockQuantity(importer.getMaximumStockQuantity());
    requisitionLineItem.setCalculatedOrderQuantity(importer.getCalculatedOrderQuantity());
    requisitionLineItem.setIdealStockAmount(importer.getIdealStockAmount());
    requisitionLineItem.setCalculatedOrderQuantityIsa(importer.getCalculatedOrderQuantityIsa());
    requisitionLineItem.setAdditionalQuantityRequired(importer.getAdditionalQuantityRequired());

    List<StockAdjustment> stockAdjustments = new ArrayList<>();
    for (StockAdjustment.Importer stockAdjustmentImporter : importer.getStockAdjustments()) {
      stockAdjustments.add(StockAdjustment.newStockAdjustment(stockAdjustmentImporter));
    }

    requisitionLineItem.setStockAdjustments(stockAdjustments);

    return requisitionLineItem;
  }

  /**
   * Copy values of attributes into new or updated RequisitionLineItem.
   *
   * @param requisitionLineItem RequisitionLineItem with new values.
   */
  void updateFrom(RequisitionLineItem requisitionLineItem) {
    if (requisition.isApprovable()) {
      this.approvedQuantity = requisitionLineItem.getApprovedQuantity();
      this.remarks = requisitionLineItem.getRemarks();
      // [SIGLUS change start]
      // [change reason]: should support updating normal data when approve.
    }
    // } else {
    // [SIGLUS change end]
    this.stockOnHand = requisitionLineItem.getStockOnHand();
    this.beginningBalance = requisitionLineItem.getBeginningBalance();
    this.totalReceivedQuantity = requisitionLineItem.getTotalReceivedQuantity();
    this.totalConsumedQuantity = requisitionLineItem.getTotalConsumedQuantity();
    this.requestedQuantity = requisitionLineItem.getRequestedQuantity();
    this.requestedQuantityExplanation = requisitionLineItem.getRequestedQuantityExplanation();
    this.totalStockoutDays = requisitionLineItem.getTotalStockoutDays();
    this.total = requisitionLineItem.getTotal();
    this.numberOfNewPatientsAdded = requisitionLineItem.getNumberOfNewPatientsAdded();
    this.maximumStockQuantity = requisitionLineItem.getMaximumStockQuantity();
    this.calculatedOrderQuantity = requisitionLineItem.getCalculatedOrderQuantity();
    this.calculatedOrderQuantityIsa = requisitionLineItem.getCalculatedOrderQuantityIsa();
    this.additionalQuantityRequired = requisitionLineItem.getAdditionalQuantityRequired();
    if (requisitionLineItem.getSkipped() != null) {
      this.skipped = requisitionLineItem.getSkipped();
    } else {
      this.skipped = false;
    }

    if (null == this.stockAdjustments) {
      this.stockAdjustments = new ArrayList<>();
    } else {
      this.stockAdjustments.clear();
    }

    if (null != requisitionLineItem.getStockAdjustments()) {
      stockAdjustments.addAll(requisitionLineItem.getStockAdjustments());
    }

    this.adjustedConsumption = requisitionLineItem.getAdjustedConsumption();
    this.averageConsumption = requisitionLineItem.getAverageConsumption();
    // [SIGLUS change start]
    // [change reason]: should support updating normal data when approve.
    // }
    // [SIGLUS change end]
  }

  /**
   * Check if all required calculation fields are not filled.
   */
  public boolean allRequiredCalcFieldsNotFilled(String field) {
    if (TOTAL_CONSUMED_QUANTITY.equals(field)) {
      return null == stockOnHand;
    } else if (STOCK_ON_HAND.equals(field)) {
      return null == totalConsumedQuantity;
    }
    return false;
  }

  /**
   * Returns order quantity.
   */
  int getOrderQuantity() {
    if (!requisition.getStatus().isPreAuthorize()) {
      if (null == approvedQuantity) {
        return 0;
      }
      return approvedQuantity;
    }

    if (requestedQuantity != null) {
      return requestedQuantity;
    }

    if (requisition.getTemplate().isPopulateStockOnHandFromStockCards()) {
      if (null != calculatedOrderQuantityIsa) {
        return calculatedOrderQuantityIsa;
      }
    } else {
      if (calculatedOrderQuantity != null) {
        return calculatedOrderQuantity;
      }
    }

    return 0;
  }

  /**
   * Export this object to the specified exporter (DTO).
   *
   * @param exporter exporter to export to
   */
  public void export(Exporter exporter, OrderableDto orderableDto,
      ApprovedProductDto approvedProductDto) {
    exporter.setId(id);
    exporter.setOrderable(orderableDto);
    exporter.setApprovedProduct(approvedProductDto);
    exporter.setApprovedQuantity(approvedQuantity);
    exporter.setPricePerPack(Optional
        .ofNullable(orderableDto)
        // [SIGLUS change start]
        // [change reason]: enable print requisition
        //.map(item -> item.findProgramOrderable(requisition.getProgramId()))
        .map(item -> Optional
        .ofNullable(orderableDto.getPrograms())
            .orElse(Sets.newHashSet(new ProgramOrderableDto())).stream().findFirst())
        // [SIGLUS change end]
        .orElse(Optional.of(new ProgramOrderableDto()))
        .map(ProgramOrderableDto::getPricePerPack)
        .orElse(Money.of(CurrencyUnit.of(currencyCode), PRICE_PER_PACK_IF_NULL)));
    exporter.setTotalCost(totalCost);
    exporter.setSkipped(skipped);
    exporter.setBeginningBalance(beginningBalance);
    exporter.setTotalReceivedQuantity(totalReceivedQuantity);
    exporter.setTotalLossesAndAdjustments(totalLossesAndAdjustments);
    exporter.setStockOnHand(stockOnHand);
    exporter.setRequestedQuantity(requestedQuantity);
    exporter.setTotalConsumedQuantity(totalConsumedQuantity);
    exporter.setRequestedQuantityExplanation(requestedQuantityExplanation);
    exporter.setRemarks(remarks);
    exportStockAdjustments(exporter);
    exporter.setTotalStockoutDays(totalStockoutDays);
    exporter.setTotal(total);
    exporter.setPacksToShip(packsToShip);
    exporter.setNumberOfNewPatientsAdded(numberOfNewPatientsAdded);
    exporter.setAdjustedConsumption(adjustedConsumption);
    if (exporter.supportsPreviousAdjustedConsumptions()) {
      exporter.setPreviousAdjustedConsumptions(previousAdjustedConsumptions);
    }
    exporter.setMaximumStockQuantity(maximumStockQuantity);
    exporter.setMaxPeriodsOfStock(Optional
            .ofNullable(approvedProductDto)
            .map(ApprovedProductDto::getMaxPeriodsOfStock)
            .map(BigDecimal::new)
            .orElse(null));
    exporter.setAverageConsumption(averageConsumption);
    exporter.setCalculatedOrderQuantity(calculatedOrderQuantity);
    exporter.setIdealStockAmount(idealStockAmount);
    exporter.setCalculatedOrderQuantityIsa(calculatedOrderQuantityIsa);
    exporter.setAdditionalQuantityRequired(additionalQuantityRequired);
  }

  private void exportStockAdjustments(Exporter exporter) {
    Optional<Supplier<StockAdjustment.Exporter>> factory =
        exporter.provideStockAdjustmentExporter();

    if (factory.isPresent()) {
      Supplier<StockAdjustment.Exporter> generator = factory.get();

      for (StockAdjustment stockAdjustment : getStockAdjustments()) {
        StockAdjustment.Exporter container = generator.get();
        stockAdjustment.export(container);

        exporter.addStockAdjustment(container);
      }
    }
  }

  /**
   * Resets all quantities and adjustments of a line item.
   */
  void resetData() {
    setTotalReceivedQuantity(null);
    setTotalLossesAndAdjustments(null);
    setStockOnHand(null);
    setRequestedQuantityExplanation(null);
    setRemarks(null);
    setApprovedQuantity(null);
    setRequestedQuantity(null);
    setTotalConsumedQuantity(null);
    setTotal(null);
    setRequestedQuantityExplanation(null);
    setTotalStockoutDays(null);
    setPacksToShip(null);
    setTotalCost(null);
    setNumberOfNewPatientsAdded(null);
    setAdjustedConsumption(null);
    setAverageConsumption(null);
    setMaximumStockQuantity(null);
    setCalculatedOrderQuantity(null);
    setCalculatedOrderQuantityIsa(null);
    stockAdjustments.clear();
    previousAdjustedConsumptions.clear();
  }

  /**
   * Skip requisition line item of column is displayed on template.
   *
   * @param template on which we check if column is displayed
   */
  public void skipLineItem(RequisitionTemplate template) {
    if (template.isColumnDisplayed(RequisitionLineItem.SKIPPED_COLUMN)) {
      skipped = true;
    } else {
      throw new ValidationMessageException(new Message(MessageKeys.ERROR_CAN_NOT_SKIP));
    }
  }

  /**
   * Calculate and set all calculated fields in this requisition line item.
   */
  void calculateAndSetFields(RequisitionTemplate template,
      Collection<StockAdjustmentReason> stockAdjustmentReasons,
      Integer numberOfMonthsInPeriod,
      Map<VersionIdentityDto, ApprovedProductDto> approvedProducts) {
    calculateAndSetTotalLossesAndAdjustments(stockAdjustmentReasons, template);
    calculateAndSetStockOnHand(template);
    calculateAndSetTotalConsumedQuantity(template);
    calculateAndSetTotal(template);
    calculateAndSetAdjustedConsumption(template, numberOfMonthsInPeriod);
    calculateAndSetAverageConsumption(template);
    calculateAndSetMaximumStockQuantity(template, approvedProducts);
    calculateAndSetCalculatedOrderQuantity(template, approvedProducts);
    calculateAndSetCalculatedOrderQuantityIsa(template);
  }

  /**
   * Sets value to Total Consumed Quantity column based on stock range summaries.
   */
  void calculateAndSetStockBasedTotalConsumedQuantity(RequisitionTemplate template,
      StockCardRangeSummaryDto stockCardRangeSummaryDto) {
    setTotalConsumedQuantity(calculateStockBasedTotalConsumedQuantity(
            template, stockCardRangeSummaryDto, this.orderable.getId()));
  }

  /**
   * Sets value to Total Received Quantity column based on stock range summaries.
   */
  void calculateAndSetStockBasedTotalReceivedQuantity(RequisitionTemplate template,
      StockCardRangeSummaryDto stockCardRangeSummaryDto) {
    setTotalReceivedQuantity(calculateStockBasedTotalReceivedQuantity(
            template, stockCardRangeSummaryDto, this.orderable.getId()));
  }

  /**
   * Sets value to Total Stockout Days column based on stock range summaries.
   */
  void calculateAndSetStockBasedTotalStockoutDays(
      StockCardRangeSummaryDto stockCardRangeSummaryDtos, Integer numberOfMonthsInPeriod) {
    setTotalStockoutDays(calculateStockBasedTotalStockoutDays(
            stockCardRangeSummaryDtos, numberOfMonthsInPeriod));
  }

  /**
   * Sets value to Total Losses and Adjustments column based on stock range summaries.
   */
  void calculateAndSetStockBasedTotalLossesAndAdjustments(RequisitionTemplate template,
      StockCardRangeSummaryDto stockCardRangeSummaryDto) {
    setTotalLossesAndAdjustments(calculateStockBasedTotalLossesAndAdjustments(
            template, stockCardRangeSummaryDto));
  }

  /**
   * Sets value to Average Consumption column based on stock range summaries.
   */
  void calculateAndSetStockBasedAverageConsumption(
      StockCardRangeSummaryDto stockCardRangeSummaryToAverage, RequisitionTemplate template,
      List<ProcessingPeriodDto> periods, List<Requisition> previousRequisitions) {
    setAverageConsumption(calculateStockBasedAverageConsumption(stockCardRangeSummaryToAverage,
        this.orderable.getId(), template, periods,
        template.isColumnDisplayed(ADDITIONAL_QUANTITY_REQUIRED)
            ? getSumOfAdditionalQuantitiesFromPreviousLineItems(previousRequisitions) : null));
  }

  /**
   * Recalculates packs to ship.
   *
   * @param product Orderable product.
   */
  void updatePacksToShip(OrderableDto product) {
    this.packsToShip = Optional
        .ofNullable(product)
        .map(item -> item.packsToOrder(getOrderQuantity()))
        .orElse(null);
  }

  /**
   * Sets appropriate value for Average Consumption field in {@link RequisitionLineItem}.
   */
  void calculateAndSetAverageConsumption() {
    List<Integer> previous = new ArrayList<>(getPreviousAdjustedConsumptions());
    previous.add(getAdjustedConsumption());
    Integer calculated = calculateAverageConsumption(previous);
    setAverageConsumption(calculated);
  }

  /**
   * Sets appropriate value for Adjusted Consumption field in {@link RequisitionLineItem} on update.
   */
  private void calculateAndSetAverageConsumption(RequisitionTemplate template) {
    if (template.isColumnInTemplate(AVERAGE_CONSUMPTION)) {
      Integer averageConsumptionPassed = this.getAverageConsumption();
      calculateAndSetAverageConsumption();

      if (averageConsumptionPassed != null
          && !Objects.equals(averageConsumptionPassed, getAverageConsumption())) {
        LOGGER.warn("Passed Average Consumption does not match calculated one.");
      }
    }
  }

  /**
   * Sets appropriate value for Total Consumed Quantity field in {@link RequisitionLineItem}.
   */
  private void calculateAndSetTotalConsumedQuantity(RequisitionTemplate template) {
    if (template.isColumnInTemplateAndDisplayed(TOTAL_CONSUMED_QUANTITY)) {
      if (template.isColumnCalculated(TOTAL_CONSUMED_QUANTITY)) {
        int calculated = calculateTotalConsumedQuantity(this);
        if (getTotalConsumedQuantity() != null
            && !Objects.equals(getTotalConsumedQuantity(), calculated)) {
          LOGGER.warn("Passed TotalConsumedQuantity does not match calculated one.");
        }
        setTotalConsumedQuantity(calculated);
      }
    } else {
      setTotalConsumedQuantity(null);
    }
  }

  /**
   * Sets appropriate value for Total field in {@link RequisitionLineItem}.
   */
  private void calculateAndSetTotal(RequisitionTemplate template) {
    if (template.isColumnInTemplateAndDisplayed(TOTAL_COLUMN)) {
      int calculated = calculateTotal(this);
      if (getTotal() != null
          && !Objects.equals(getTotal(), calculated)) {
        LOGGER.warn("Passed Total does not match calculated one.");
      }
      setTotal(calculated);
    }
  }

  /**
   * Sets appropriate value for Stock On Hand field in {@link RequisitionLineItem}.
   */
  private void calculateAndSetStockOnHand(RequisitionTemplate template) {
    if (template.isColumnInTemplateAndDisplayed(STOCK_ON_HAND)) {
      if (template.isColumnCalculated(STOCK_ON_HAND)) {
        int calculated = calculateStockOnHand(this);
        if (getStockOnHand() != null
            && !Objects.equals(getStockOnHand(), calculated)) {
          LOGGER.warn("Passed StockOnHand does not match calculated one.");
        }
        setStockOnHand(calculated);
      }
    } else {
      setStockOnHand(null);
    }
  }

  /**
   * Sets appropriate value for Total Consumed Quantity field in {@link RequisitionLineItem}.
   */
  private void calculateAndSetTotalLossesAndAdjustments(
      Collection<StockAdjustmentReason> reasons,
      RequisitionTemplate template) {
    if (!template.isPopulateStockOnHandFromStockCards()) {
      int calculated = calculateTotalLossesAndAdjustments(this, reasons);
      if (getTotalLossesAndAdjustments() != null
          && !Objects.equals(getTotalLossesAndAdjustments(), calculated)) {
        LOGGER.warn("Passed TotalLossesAndAdjustments does not match calculated one.");
      }
      setTotalLossesAndAdjustments(calculated);
    }
  }

  /**
   * Sets appropriate value for Adjusted Consumption field in {@link RequisitionLineItem}.
   */
  private void calculateAndSetAdjustedConsumption(RequisitionTemplate template,
                                                  Integer monthsInThePeriod) {
    Boolean additionalQuantityRequiredVisible = template
        .isColumnInTemplateAndDisplayed(ADDITIONAL_QUANTITY_REQUIRED);
    if (template.isColumnInTemplate(ADJUSTED_CONSUMPTION)) {
      int calculated = calculateAdjustedConsumption(this,
          monthsInThePeriod, additionalQuantityRequiredVisible);

      if (getAdjustedConsumption() != null
          && !Objects.equals(getAdjustedConsumption(), calculated)) {
        LOGGER.warn("Passed Adjusted Consumption does not match calculated one.");
      }
      setAdjustedConsumption(calculated);
    }
  }

  /**
   * Sets appropriate value for Maximum Stock Quantity field in {@link RequisitionLineItem}.
   */
  private void calculateAndSetMaximumStockQuantity(RequisitionTemplate template,
      Map<VersionIdentityDto, ApprovedProductDto> approvedProducts) {
    if (template.isColumnInTemplateAndDisplayed(MAXIMUM_STOCK_QUANTITY)) {
      int calculated = calculateMaximumStockQuantity(this, template,
          getMaxPeriodsOfStockFromApprovedProduct(approvedProducts));
      if (getMaximumStockQuantity() != null
          && !Objects.equals(getMaximumStockQuantity(), calculated)) {
        LOGGER.warn("Passed MaximumStockQuantity does not match calculated one.");
      }
      setMaximumStockQuantity(calculated);
    }
  }

  /**
   * Sets appropriate value for Calculated Order Quantity field in {@link RequisitionLineItem}.
   */
  private void calculateAndSetCalculatedOrderQuantity(RequisitionTemplate template,
      Map<VersionIdentityDto, ApprovedProductDto> approvedProducts) {
    if (template.isColumnInTemplateAndDisplayed(CALCULATED_ORDER_QUANTITY)) {
      int calculated = calculateCalculatedOrderQuantity(this, template,
          getMaxPeriodsOfStockFromApprovedProduct(approvedProducts));
      if (getCalculatedOrderQuantity() != null
          && !Objects.equals(getCalculatedOrderQuantity(), calculated)) {
        LOGGER.warn("Passed CalculatedOrderQuantity does not match calculated one.");
      }
      setCalculatedOrderQuantity(calculated);
    }
  }

  private void calculateAndSetCalculatedOrderQuantityIsa(RequisitionTemplate template) {
    if (template.isColumnInTemplateAndDisplayed(CALCULATED_ORDER_QUANTITY_ISA)) {
      Integer calculated = calculateCalculatedOrderQuantityIsa(this);
      if (getCalculatedOrderQuantityIsa() != null
          && !Objects.equals(getCalculatedOrderQuantityIsa(), calculated)) {
        LOGGER.warn("Passed CalculatedOrderQuantityIsa does not match calculated one.");
      }
      setCalculatedOrderQuantityIsa(calculated);
    }
  }

  /**
   * checks if line is skipped. Return false if null.
   */
  public boolean isLineSkipped() {
    if (skipped == null) {
      return false;
    }
    return skipped;
  }

  private Integer getSumOfAdditionalQuantitiesFromPreviousLineItems(
      List<Requisition> previousRequisitions) {
    return previousRequisitions.stream()
        .map(req -> req.findLineByProduct(orderable.getId(), orderable.getVersionNumber()))
        .filter(Objects::nonNull)
        .filter(lineItem -> Objects.nonNull(lineItem.getAdditionalQuantityRequired()))
        .mapToInt(RequisitionLineItem::getAdditionalQuantityRequired)
        .sum() + (additionalQuantityRequired != null ? additionalQuantityRequired : 0);
  }

  private Double getMaxPeriodsOfStockFromApprovedProduct(
      Map<VersionIdentityDto, ApprovedProductDto> approvedProducts) {
    ApprovedProductDto approvedProductDto = approvedProducts
        .get(new VersionIdentityDto(this.facilityTypeApprovedProduct));

    return approvedProductDto.getMaxPeriodsOfStock();
  }

  public interface Exporter {
    void setId(UUID id);

    void setBeginningBalance(Integer beginningBalance);

    void setTotalReceivedQuantity(Integer totalReceivedQuantity);

    void setTotalLossesAndAdjustments(Integer totalLossesAndAdjustments);

    void setStockOnHand(Integer stockOnHand);

    void setRequestedQuantity(Integer requestedQuantity);

    void setTotalConsumedQuantity(Integer totalConsumedQuantity);

    void setRequestedQuantityExplanation(String requestedQuantityExplanation);

    void setRemarks(String remarks);

    void setApprovedQuantity(Integer approvedQuantity);

    void setTotalStockoutDays(Integer totalStockoutDays);

    void setTotal(Integer total);

    void setPacksToShip(Long packsToShip);

    void setOrderable(OrderableDto orderableDto);

    void setApprovedProduct(ApprovedProductDto approvedProduct);

    void setPricePerPack(Money pricePerPack);

    void setNumberOfNewPatientsAdded(Integer numberOfNewPatientsAdded);

    void setTotalCost(Money totalCost);

    void setSkipped(Boolean skipped);

    void setAdjustedConsumption(Integer adjustedConsumption);

    void setPreviousAdjustedConsumptions(List<Integer> previousAdjustedConsupmtions);

    void setAverageConsumption(Integer averageConsumption);

    void setMaxPeriodsOfStock(BigDecimal maxPeriodsOfStock);

    void setMaximumStockQuantity(Integer maximumStockQuantity);

    void setCalculatedOrderQuantity(Integer calculatedOrderQuantity);

    Optional<Supplier<StockAdjustment.Exporter>> provideStockAdjustmentExporter();

    boolean supportsPreviousAdjustedConsumptions();

    void addStockAdjustment(StockAdjustment.Exporter stockAdjustmentExporter);

    void setIdealStockAmount(Integer idealStockAmount);

    void setCalculatedOrderQuantityIsa(Integer calculatedOrderQuantityIsa);

    void setAdditionalQuantityRequired(Integer additionalQuantityRequired);
  }

  public interface Importer {
    UUID getId();

    Integer getBeginningBalance();

    Integer getTotalReceivedQuantity();

    VersionIdentityDto getOrderableIdentity();

    VersionIdentityDto getApprovedProductIdentity();

    Integer getTotalLossesAndAdjustments();

    Integer getStockOnHand();

    Integer getRequestedQuantity();

    Integer getTotalConsumedQuantity();

    String getRequestedQuantityExplanation();

    String getRemarks();

    Integer getApprovedQuantity();

    List<StockAdjustment.Importer> getStockAdjustments();

    Integer getTotalStockoutDays();

    Integer getTotal();

    Long getPacksToShip();

    Integer getNumberOfNewPatientsAdded();

    Money getTotalCost();

    Boolean getSkipped();

    Integer getAdjustedConsumption();

    Integer getAverageConsumption();

    Integer getMaximumStockQuantity();

    Integer getCalculatedOrderQuantity();

    Integer getIdealStockAmount();

    Integer getCalculatedOrderQuantityIsa();

    Integer getAdditionalQuantityRequired();

    // [SIGLUS change start]
    // [change reason]: add for requisition line item draft.
    // }
    List<Integer> getPreviousAdjustedConsumptions();
    // [SIGLUS change end]
  }
}
