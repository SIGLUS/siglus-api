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

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.BooleanUtils.isFalse;
import static org.apache.commons.lang3.BooleanUtils.isNotTrue;
import static org.apache.commons.lang3.BooleanUtils.isTrue;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.openlmis.requisition.CurrencyConfig.currencyCode;
import static org.openlmis.requisition.domain.requisition.LineItemFieldsCalculator.calculateStockBasedTotalReceivedQuantity;
import static org.openlmis.requisition.domain.requisition.RequisitionLineItem.ADDITIONAL_QUANTITY_REQUIRED;
import static org.openlmis.requisition.domain.requisition.RequisitionLineItem.ADJUSTED_CONSUMPTION;
import static org.openlmis.requisition.domain.requisition.RequisitionLineItem.AVERAGE_CONSUMPTION;
import static org.openlmis.requisition.domain.requisition.RequisitionLineItem.CALCULATED_ORDER_QUANTITY;
import static org.openlmis.requisition.domain.requisition.RequisitionLineItem.CALCULATED_ORDER_QUANTITY_ISA;
import static org.openlmis.requisition.domain.requisition.RequisitionLineItem.SKIPPED_COLUMN;
import static org.openlmis.requisition.domain.requisition.RequisitionLineItem.TOTAL_LOSSES_AND_ADJUSTMENTS;
import static org.openlmis.requisition.domain.requisition.RequisitionLineItem.TOTAL_RECEIVED_QUANTITY;
import static org.openlmis.requisition.domain.requisition.RequisitionLineItem.TOTAL_STOCKOUT_DAYS;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_FIELD_MUST_HAVE_VALUES;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_MUST_BE_INITIATED_TO_BE_SUBMMITED;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_MUST_BE_SUBMITTED_TO_BE_AUTHORIZED;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_PROGRAM_DOES_NOT_ALLOW_SKIP;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_SKIP_FAILED_EMERGENCY;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_SKIP_FAILED_WRONG_STATUS;
import static org.siglus.common.constant.ExtraDataConstants.ACTUAL_END_DATE;
import static org.siglus.common.constant.ExtraDataConstants.ACTUAL_START_DATE;
import static org.siglus.common.constant.ExtraDataConstants.IS_SAVED;
import static org.siglus.common.constant.ExtraDataConstants.SIGNATURE;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.beanutils.PropertyUtils;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Type;
import org.javers.core.metamodel.annotation.DiffIgnore;
import org.javers.core.metamodel.annotation.TypeName;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.openlmis.requisition.domain.BaseTimestampedEntity;
import org.openlmis.requisition.domain.ExtraDataEntity;
import org.openlmis.requisition.domain.ExtraDataEntity.ExtraDataExporter;
import org.openlmis.requisition.domain.ExtraDataEntity.ExtraDataImporter;
import org.openlmis.requisition.domain.OpenLmisNumberUtils;
import org.openlmis.requisition.domain.RequisitionTemplate;
import org.openlmis.requisition.domain.RequisitionTemplateColumn;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.requisition.dto.BasicRequisitionTemplateDto;
import org.openlmis.requisition.dto.OrderableDto;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.openlmis.requisition.dto.ProgramOrderableDto;
import org.openlmis.requisition.dto.ProofOfDeliveryDto;
import org.openlmis.requisition.dto.ProofOfDeliveryLineItemDto;
import org.openlmis.requisition.dto.ReasonDto;
import org.openlmis.requisition.dto.SupplyLineDto;
import org.openlmis.requisition.dto.VersionIdentityDto;
import org.openlmis.requisition.dto.stockmanagement.StockCardRangeSummaryDto;
import org.openlmis.requisition.errorhandling.ValidationResult;
import org.openlmis.requisition.exception.ValidationMessageException;
import org.openlmis.requisition.service.PermissionService;
import org.openlmis.requisition.utils.Message;
import org.siglus.common.domain.RequisitionTemplateExtension;
import org.siglus.common.domain.referencedata.SupervisoryNode;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.slf4j.profiler.Profiler;
import org.springframework.util.CollectionUtils;

@SuppressWarnings({"PMD.TooManyMethods", "PMD.AvoidDuplicateLiterals"})
@Entity
@TypeName("Requisition")
@Table(name = "requisitions", schema = "requisition")
@NoArgsConstructor
@AllArgsConstructor
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class Requisition extends BaseTimestampedEntity {

  private static final XLogger LOGGER = XLoggerFactory.getXLogger(Requisition.class);

  static final String FACILITY_ID = "facilityId";
  static final String PROGRAM_ID = "programId";
  static final String PROCESSING_PERIOD_ID = "processingPeriodId";
  public static final String TOTAL_CONSUMED_QUANTITY = "totalConsumedQuantity";
  public static final String STOCK_ON_HAND = "stockOnHand";
  static final String SUPERVISORY_NODE_ID = "supervisoryNodeId";
  static final String NUMBER_OF_MONTHS_IN_PERIOD = "numberOfMonthsInPeriod";
  public static final String EMERGENCY_FIELD = "emergency";
  static final String DATE_PHYSICAL_STOCK_COUNT_COMPLETED = "datePhysicalStockCountCompleted";
  public static final String REQUISITION_LINE_ITEMS = "requisitionLineItems";
  public static final String STATUS_CHANGES = "statusChanges";
  static final String EXTRA_DATA_ORIGINAL_REQUISITION_ID = "originalRequisition";

  private static final int LINE_ITEMS_BATCH_SIZE = 100;
  private static final int AVAILABLE_PRODUCTS_BATCH_SIZE = 1000;

  @OneToMany(
      mappedBy = "requisition",
      cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.REMOVE},
      fetch = FetchType.LAZY,
      orphanRemoval = true)
  @BatchSize(size = LINE_ITEMS_BATCH_SIZE)
  @DiffIgnore
  @Getter
  @Setter
  private List<RequisitionLineItem> requisitionLineItems;

  @Version
  @Getter
  @Setter
  private Long version;

  @Getter
  private String draftStatusMessage;

  @ManyToOne
  @JoinColumn(name = "templateId", nullable = false)
  @DiffIgnore
  @Getter
  @Setter
  private RequisitionTemplate template;

  @Column(nullable = false)
  @Getter
  @Setter
  @Type(type = UUID_TYPE)
  private UUID facilityId;

  @Column(nullable = false)
  @Getter
  @Setter
  @Type(type = UUID_TYPE)
  private UUID programId;

  @Column(nullable = false)
  @Getter
  @Setter
  @Type(type = UUID_TYPE)
  private UUID processingPeriodId;

  @Getter
  @Setter
  @Type(type = UUID_TYPE)
  private UUID supplyingFacilityId;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  @Getter
  @Setter
  private RequisitionStatus status;

  @OneToMany(
      mappedBy = "requisition",
      cascade = CascadeType.ALL)
  @BatchSize(size = STANDARD_BATCH_SIZE)
  @DiffIgnore
  @Getter
  @Setter
  private List<StatusChange> statusChanges = new ArrayList<>();

  @Column(nullable = false)
  @Getter
  @Setter
  private Boolean emergency;

  @Getter
  @Setter
  private Boolean reportOnly;

  @Column(nullable = false)
  @Getter
  @Setter
  private Integer numberOfMonthsInPeriod;

  @Getter
  @Setter
  @Type(type = UUID_TYPE)
  private UUID supervisoryNodeId;

  // [SIGLUS change start]
  // [change reason]: support for filter approve list for internal approve.
  @ManyToOne
  @JoinColumn(name = "supervisoryNodeId", insertable = false, updatable = false)
  @DiffIgnore
  @Getter
  @Setter
  private SupervisoryNode supervisoryNode;
  // [SIGLUS change end]

  @ManyToMany
  @JoinTable(name = "requisitions_previous_requisitions", schema = "requisition",
      joinColumns = {@JoinColumn(name = "requisitionId")},
      inverseJoinColumns = {@JoinColumn(name = "previousRequisitionId")})
  @DiffIgnore
  @Getter
  @Setter
  private List<Requisition> previousRequisitions;

  @ElementCollection(fetch = FetchType.LAZY)
  @BatchSize(size = AVAILABLE_PRODUCTS_BATCH_SIZE)
  @CollectionTable(name = "available_products", schema = "requisition",
      joinColumns = @JoinColumn(name = "requisitionId")
  )
  @Getter
  @Setter
  private Set<ApprovedProductReference> availableProducts;

  @Getter
  @Setter
  @Embedded
  @AttributeOverrides({
      @AttributeOverride(name = "localDate",
          column = @Column(name = "datephysicalstockcountcompleted"))
  })
  private DatePhysicalStockCountCompleted datePhysicalStockCountCompleted;

  @OneToMany(
      cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.REMOVE},
      fetch = FetchType.LAZY,
      orphanRemoval = true)
  @BatchSize(size = STANDARD_BATCH_SIZE)
  @JoinColumn(name = "requisitionId")
  @DiffIgnore
  @Getter
  @Setter
  private List<StockAdjustmentReason> stockAdjustmentReasons = new ArrayList<>();

  @OneToMany(
      mappedBy = "requisition",
      cascade = CascadeType.ALL,
      orphanRemoval = true)
  @BatchSize(size = STANDARD_BATCH_SIZE)
  @DiffIgnore
  @Getter
  private List<RequisitionPermissionString> permissionStrings = new ArrayList<>();

  @Embedded
  private ExtraDataEntity extraData = new ExtraDataEntity();

  /**
   * Constructor.
   *
   * @param facilityId         id of the Facility
   * @param programId          id of the Program
   * @param processingPeriodId id of the ProcessingPeriod
   * @param status             status of the Requisition
   * @param emergency          whether this Requisition is emergency
   */
  public Requisition(UUID facilityId, UUID programId, UUID processingPeriodId,
      RequisitionStatus status, Boolean emergency) {
    this.facilityId = facilityId;
    this.programId = programId;
    this.processingPeriodId = processingPeriodId;
    this.status = status;
    this.emergency = emergency;
    permissionStrings.add(RequisitionPermissionString.newRequisitionPermissionString(this,
        PermissionService.REQUISITION_VIEW, facilityId, programId));
  }

  /**
   * Copy constructor.
   *
   * @param original an original requisition with data that will be placed in a new requisition.
   */
  public Requisition(Requisition original) {
    this(original.requisitionLineItems, original.version, original.draftStatusMessage,
        original.template, original.facilityId, original.programId, original.processingPeriodId,
        original.supplyingFacilityId, original.status, original.statusChanges, original.emergency,
        original.reportOnly, original.numberOfMonthsInPeriod, original.supervisoryNodeId,
        // [SIGLUS change start]
        // [change reason]: support for filter approve list for internal approve.
        original.supervisoryNode,
        // [SIGLUS change end]
        original.previousRequisitions, original.availableProducts,
        original.datePhysicalStockCountCompleted, null,
        null, new ExtraDataEntity());

    setId(original.getId());

    setCreatedDate(original.getCreatedDate());
    setModifiedDate(original.getModifiedDate());

    setExtraData(original.getExtraData());

    permissionStrings = original
        .permissionStrings
        .stream()
        .map(rps -> new RequisitionPermissionString(this, rps.getPermissionString()))
        .collect(toList());

    this.stockAdjustmentReasons = original
        .stockAdjustmentReasons
        .stream()
        .map(StockAdjustmentReason::new)
        .peek(item -> item.setId(null))
        .collect(Collectors.toList());
  }

  /**
   * Validates if requisition can be updated.
   */
  public ValidationResult validateCanBeUpdated(
      RequisitionValidationService validationService) {
    return validationService.validateRequisitionCanBeUpdated();
  }

  /**
   * Validates if requisition can be change status.
   */
  public ValidationResult validateCanChangeStatus(LocalDate currentDate,
      boolean isDatePhysicalStockCountCompletedEnabled,
      Map<VersionIdentityDto, OrderableDto> orderables,
      Map<VersionIdentityDto, ApprovedProductDto> approvedProducts) {
    return new StatusChangeValidationService(this, currentDate,
        isDatePhysicalStockCountCompletedEnabled, orderables, approvedProducts)
        .validateRequisitionCanChangeStatus();
  }

  // [SIGLUS change start]
  // [change reason]: override validateCanChangeStatus with new params isInternalFacility
  public ValidationResult validateCanChangeStatus(LocalDate currentDate,
      boolean isDatePhysicalStockCountCompletedEnabled,
      Map<VersionIdentityDto, OrderableDto> orderables,
      Map<VersionIdentityDto, ApprovedProductDto> approvedProducts,
      boolean isInternalFacility) {
    return new StatusChangeValidationService(this, currentDate,
        isDatePhysicalStockCountCompletedEnabled, orderables, approvedProducts,
        isInternalFacility)
        .validateRequisitionCanChangeStatus();
  }
  // [SIGLUS change end]

  /**
   * Returns a set of all orderable IDs in this requisition.
   */
  public Set<VersionEntityReference> getAllOrderables() {
    Set<VersionEntityReference> orderableIds = Optional
        .ofNullable(requisitionLineItems)
        .orElse(Collections.emptyList())
        .stream()
        .map(RequisitionLineItem::getOrderable)
        .collect(Collectors.toSet());

    Optional
        .ofNullable(availableProducts)
        .orElse(Collections.emptySet())
        .stream()
        .map(ApprovedProductReference::getOrderable)
        .forEach(orderableIds::add);

    return orderableIds;
  }

  /**
   * Returns a set of all approved product identities in this requisition.
   */
  public Set<VersionEntityReference> getAllApprovedProductIdentities() {
    return Optional
        .ofNullable(requisitionLineItems)
        .orElse(Collections.emptyList())
        .stream()
        .map(RequisitionLineItem::getFacilityTypeApprovedProduct)
        .collect(Collectors.toSet());
  }

  /**
   * Copy values of attributes into new or updated Requisition.
   *
   * @param requisition Requisition with new values.
   * @param products    Collection of orderables.
   */
  public void updateFrom(Requisition requisition, Map<VersionIdentityDto, OrderableDto> products,
      Map<VersionIdentityDto, ApprovedProductDto> approvedProducts,
      boolean isDatePhysicalStockCountCompletedEnabled) {
    LOGGER.entry(requisition, products, isDatePhysicalStockCountCompletedEnabled);
    Profiler profiler = new Profiler("REQUISITION_UPDATE_FROM");
    profiler.setLogger(LOGGER);

    profiler.start("SET_DRAFT_STATUS_MESSAGE");
    this.draftStatusMessage = requisition.draftStatusMessage;

    profiler.start("UPDATE_LINE_ITEMS");
    updateReqLines(requisition.getRequisitionLineItems());

    // [SIGLUS change start]
    // [change reason]: siglus emergency need same logic with regular.
    // if (!emergency) {
    // [SIGLUS change end]
    profiler.start("CALCULATE_AND_VALIDATE_TEMPLATE_FIELDS");
    calculateAndValidateTemplateFields(this.template, products, approvedProducts);
    // [SIGLUS change start]
    // [change reason]: siglus emergency need same logic with regular.
    // }
    // [SIGLUS change end]

    profiler.start("UPDATE_TOTAL_COST_AND_PACKS_TO_SHIP");
    updateTotalCostAndPacksToShip(products);

    if (isDatePhysicalStockCountCompletedEnabled) {
      profiler.start("SET_DATE_PHYSICAL_STOCK_COUNT_COMPLETED");
      setDatePhysicalStockCountCompleted(requisition.getDatePhysicalStockCountCompleted());
    }

    profiler.start("SET_EXTRA_DATA");
    extraData = new ExtraDataEntity(requisition.getExtraData());
    // [SIGLUS change start]
    // [change reason]: for our frontend difference initial && second time to fill some filed.
    setIsSavedExtraData(true);
    // [SIGLUS change end]

    // do this manually here, since JPA won't catch updates to collections (line items)
    profiler.start("SET_MODIFIED_DATE");
    setModifiedDate(ZonedDateTime.now());

    profiler.stop().log();
    LOGGER.exit();
  }

  /**
   * Initiates the state of a requisition by creating line items based on products.
   *
   * @param template             the requisition template for this requisition to use (based on
   *                             program)
   * @param fullSupplyProducts   the full supply products for this requisitions facility to build
   *                             requisition lines for
   * @param previousRequisitions the previous requisitions for this program/facility. Used for field
   *                             calculations and set previous adjusted consumptions. Pass empty
   *                             list if there are no previous requisitions.
   */
  public void initiate(
      RequisitionTemplate template,
      Collection<ApprovedProductDto> fullSupplyProducts,
      List<Requisition> previousRequisitions,
      int numberOfPreviousPeriodsToAverage,
      ProofOfDeliveryDto proofOfDelivery,
      Map<UUID, Integer> idealStockAmounts,
      UUID initiator,
      StockData stockData,
      List<StockCardRangeSummaryDto> stockCardRangeSummaries,
      List<StockCardRangeSummaryDto> stockCardRangeSummariesToAverage,
      List<ProcessingPeriodDto> periods) {

    Profiler profiler = new Profiler("REQUISITION_INITIATE_ENTITY");
    profiler.setLogger(LOGGER);
    this.template = template;
    this.previousRequisitions = previousRequisitions;

    profiler.start("SET_LINE_ITEMS");
    // [SIGLUS change start]
    // [change reason]: support usage report which is no product section.
    RequisitionTemplateExtension extension = template.getTemplateExtension();
    if (extension != null && extension.getEnableProduct()) {
      // [SIGLUS change end]
      if (template.isPopulateStockOnHandFromStockCards()) {
        initiateLineItems(fullSupplyProducts, idealStockAmounts, stockData, stockCardRangeSummaries,
            stockCardRangeSummariesToAverage, periods);
      } else {
        initiateLineItems(fullSupplyProducts, idealStockAmounts, proofOfDelivery, profiler);

        profiler.start("SET_PREV_ADJ_CONSUMPTION");
        setPreviousAdjustedConsumptions(numberOfPreviousPeriodsToAverage);
      }

      profiler.start("SET_SKIPPED_FROM_PREV_REQUISITION");
      if (isNotTrue(emergency)
          && template.isColumnInTemplateAndDisplayed(SKIPPED_COLUMN)
          && template.isColumnFromPreviousRequisition(SKIPPED_COLUMN)) {
        copySkippedValuesFromPreviousRequisition();
      }
    }

    status = RequisitionStatus.INITIATED;

    profiler.start("SET_STATUS_CHANGES");
    statusChanges.add(StatusChange.newStatusChange(this, initiator));

    // [SIGLUS change start]
    // [change reason]: for our frontend difference initial && second time to fill some filed.
    profiler.start("SET_EXTRA_DATA");
    setIsSavedExtraData(false);
    // [SIGLUS change end]

    profiler.stop().log();
  }

  // [SIGLUS change start]
  // [change reason]: for our frontend difference initial && second time to fill some filed.
  private void setIsSavedExtraData(Boolean isSaved) {
    this.extraData.put(IS_SAVED, isSaved);
  }
  // [SIGLUS change end]


  // [SIGLUS change start]
  // [change reason]: get actual start date && get end date
  public LocalDate getActualStartDate() {
    if (extraData != null && extraData.get(ACTUAL_START_DATE) != null) {
      DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
      return LocalDate.parse((String) extraData.get(ACTUAL_START_DATE), dateTimeFormatter);
    }
    return null;
  }

  public LocalDate getActualEndDate() {
    if (extraData != null && extraData.get(ACTUAL_END_DATE) != null) {
      DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
      return LocalDate.parse((String) extraData.get(ACTUAL_END_DATE), dateTimeFormatter);
    }
    return null;
  }
  // [SIGLUS change end]

  private void copySkippedValuesFromPreviousRequisition() {
    if (!previousRequisitions.isEmpty() && null != previousRequisitions.get(0)) {
      Map<VersionEntityReference, RequisitionLineItem> productIdToPreviousLine =
          previousRequisitions
              .get(0)
              .getRequisitionLineItems()
              .stream()
              .collect(toMap(RequisitionLineItem::getOrderable, identity(),
                  (item1, item2) -> item1));

      requisitionLineItems.forEach(currentLine -> {
        RequisitionLineItem previousLine = productIdToPreviousLine
            .getOrDefault(currentLine.getOrderable(), null);
        currentLine.setSkipped(LineItemFieldsCalculator.canSkipLineItem(currentLine, previousLine));
      });
    }
  }

  private void initiateLineItems(Collection<ApprovedProductDto> fullSupplyProducts,
      Map<UUID, Integer> idealStockAmounts, StockData stockData,
      List<StockCardRangeSummaryDto> stockCardRangeSummaries,
      List<StockCardRangeSummaryDto> stockCardRangeSummariesToAverage,
      List<ProcessingPeriodDto> periods) {
    this.requisitionLineItems = new ArrayList<>();

    if (isNotTrue(emergency)) {
      for (ApprovedProductDto product : fullSupplyProducts) {
        UUID orderableId = product.getOrderable().getId();

        if (isNotTrue(stockData.hasDataFor(orderableId))) {
          continue;
        }

        RequisitionLineItem lineItem = new RequisitionLineItem(this, product);
        lineItem.setIdealStockAmount(extractIdealStockAmount(idealStockAmounts, product));
        lineItem.setStockOnHand(initiateQuantity(stockData.getStockOnHand(orderableId)));
        lineItem.setBeginningBalance(initiateQuantity(stockData.getBeginningBalance(orderableId)));

        StockCardRangeSummaryDto summary = findStockCardRangeSummary(
            stockCardRangeSummaries, lineItem.getOrderable().getId());
        StockCardRangeSummaryDto summaryToAverage = findStockCardRangeSummary(
            stockCardRangeSummariesToAverage, lineItem.getOrderable().getId());

        lineItem.calculateAndSetStockBasedTotalReceivedQuantity(template, summary);
        lineItem.calculateAndSetStockBasedTotalStockoutDays(summary, numberOfMonthsInPeriod);
        lineItem.calculateAndSetStockBasedTotalConsumedQuantity(template, summary);
        lineItem.calculateAndSetStockBasedTotalLossesAndAdjustments(template, summary);
        lineItem.calculateAndSetStockBasedAverageConsumption(summaryToAverage, template, periods,
            previousRequisitions);

        this.requisitionLineItems.add(lineItem);
      }
    }
  }

  private void initiateLineItems(Collection<ApprovedProductDto> fullSupplyProducts,
      Map<UUID, Integer> idealStockAmounts, ProofOfDeliveryDto proofOfDelivery,
      Profiler profiler) {
    this.requisitionLineItems = new ArrayList<>();

    if (isNotTrue(emergency)) {
      for (ApprovedProductDto product : fullSupplyProducts) {
        RequisitionLineItem lineItem = new RequisitionLineItem(this, product);
        lineItem.setIdealStockAmount(extractIdealStockAmount(idealStockAmounts, product));

        this.requisitionLineItems.add(lineItem);
      }
    }

    profiler.start("GET_PREV_BEGINNING_BALANCE");
    List<RequisitionLineItem> nonSkippedFullSupplyItems = null;
    Map<VersionIdentityDto, OrderableDto> orderables = fullSupplyProducts
        .stream()
        .map(ApprovedProductDto::getOrderable)
        .collect(toMap(OrderableDto::getIdentity, identity()));

    // Firstly, if we display the column ...
    // ... and if the previous requisition exists ...
    if (!previousRequisitions.isEmpty()
        && null != previousRequisitions.get(0)
        && template.isColumnDisplayed(RequisitionLineItem.BEGINNING_BALANCE)) {
      // .. for each line from the current requisition ...
      nonSkippedFullSupplyItems = getNonSkippedFullSupplyRequisitionLineItems(orderables);

      Map<UUID, RequisitionLineItem> productIdToPreviousLine = previousRequisitions
          .get(0)
          .getRequisitionLineItems()
          .stream()
          .collect(toMap(rli -> rli.getOrderable().getId(), identity(), (item1, item2) -> item1));

      nonSkippedFullSupplyItems.forEach(currentLine -> {
        // ... we try to find line in the previous requisition for the same product ...
        RequisitionLineItem previousLine = productIdToPreviousLine.getOrDefault(
            currentLine.getOrderable().getId(), null);

        // ... and in the end we use it to calculate beginning balance in a new line.
        currentLine.setBeginningBalance(
            LineItemFieldsCalculator.calculateBeginningBalance(previousLine));
      });
    }

    // Secondly, if Proof Of Delivery exists and it is submitted ...
    profiler.start("SET_RECV_QTY");
    if (null != proofOfDelivery && proofOfDelivery.isSubmitted()) {
      // .. for each line from the current requisition ...
      if (nonSkippedFullSupplyItems == null) {
        nonSkippedFullSupplyItems = getNonSkippedFullSupplyRequisitionLineItems(orderables);
      }

      Map<UUID, ProofOfDeliveryLineItemDto> productIdToPodLine = proofOfDelivery
          .getLineItems()
          .stream()
          .filter(li -> null != li.getOrderable())
          .collect(toMap(li -> li.getOrderable().getId(), identity(), (one, two) -> one));

      nonSkippedFullSupplyItems.forEach(requisitionLine -> {
        // ... we try to find line in POD for the same product ...
        ProofOfDeliveryLineItemDto proofOfDeliveryLine = productIdToPodLine.getOrDefault(
            requisitionLine.getOrderable().getId(), null);

        // ... and if line exists we set value for Total Received Quantity (B) column
        if (null != proofOfDeliveryLine) {
          requisitionLine.setTotalReceivedQuantity(
              OpenLmisNumberUtils.zeroIfNull(proofOfDeliveryLine.getQuantityAccepted())
          );
        }
      });
    }
  }

  private Integer extractIdealStockAmount(Map<UUID, Integer> idealStockAmounts,
      ApprovedProductDto product) {
    String commodityType = product.getOrderable().getCommodityTypeIdentifier();
    return isNotBlank(commodityType)
        ? idealStockAmounts.get(UUID.fromString(commodityType))
        : null;
  }

  /**
   * Submits this requisition.
   */
  public void submit(Map<VersionIdentityDto, OrderableDto> products, UUID submitter,
      boolean skipAuthorize) {
    if (!status.isSubmittable()) {
      throw new ValidationMessageException(
          new Message(ERROR_MUST_BE_INITIATED_TO_BE_SUBMMITED, getId()));
    }

    if (isNotTrue(emergency)) {
      getNonSkippedFullSupplyRequisitionLineItems(products)
          .forEach(line -> {
            if (template.isColumnCalculated(Requisition.TOTAL_CONSUMED_QUANTITY)
                && line.allRequiredCalcFieldsNotFilled(Requisition.TOTAL_CONSUMED_QUANTITY)) {
              throw new ValidationMessageException(
                  new Message(ERROR_FIELD_MUST_HAVE_VALUES, id,
                      Requisition.STOCK_ON_HAND,
                      Requisition.TOTAL_CONSUMED_QUANTITY));
            }

            if (template.isColumnCalculated(Requisition.STOCK_ON_HAND)
                && line.allRequiredCalcFieldsNotFilled(Requisition.STOCK_ON_HAND)) {
              throw new ValidationMessageException(
                  new Message(ERROR_FIELD_MUST_HAVE_VALUES, id,
                      Requisition.TOTAL_CONSUMED_QUANTITY, Requisition.STOCK_ON_HAND));
            }
          });
    }

    updateConsumptions(products);
    updateTotalCostAndPacksToShip(products);

    status = RequisitionStatus.SUBMITTED;
    statusChanges.add(StatusChange.newStatusChange(this, submitter));

    if (skipAuthorize) {
      LOGGER.debug("Skipping authorize step.");
      prepareRequisitionForApproval(submitter);
    }
    setModifiedDate(ZonedDateTime.now());
    // [SIGLUS change start]
    // [change reason]: for our frontend difference initial && second time to fill some filed.
    setIsSavedExtraData(false);
    // [SIGLUS change end]
  }

  /**
   * Authorize this Requisition.
   */
  public void authorize(Map<VersionIdentityDto, OrderableDto> products, UUID authorizer) {
    if (!RequisitionStatus.SUBMITTED.equals(status)) {
      throw new ValidationMessageException(
          new Message(ERROR_MUST_BE_SUBMITTED_TO_BE_AUTHORIZED, getId()));
    }

    updateConsumptions(products);
    updateTotalCostAndPacksToShip(products);
    prepareRequisitionForApproval(authorizer);
    setModifiedDate(ZonedDateTime.now());
    // [SIGLUS change start]
    // [change reason]: for our frontend difference initial && second time to fill some filed.
    setIsSavedExtraData(false);
    // [SIGLUS change end]
  }

  /**
   * Check if the requisition is approvable.
   */
  public boolean isApprovable() {
    return status.duringApproval();
  }

  /**
   * Checks whether the requisition status allows for its deletion.
   *
   * @return true if the requisition status is within those that allow deletion; false otherwise
   */
  public boolean isDeletable() {
    return isPreAuthorize() || status.isSkipped();
  }

  /**
   * Approves given requisition.
   *
   * @param nodeId      supervisoryNode that has a supply line for the requisition's program.
   * @param products    orderable products that will be used by line items to update packs to ship.
   * @param supplyLines supplyLineDtos of the supervisoryNode that has a supply line for the
   *                    requisition's program.
   * @param approver    user who approves this requisition.
   */
  public void approve(UUID nodeId, Map<VersionIdentityDto, OrderableDto> products,
      Collection<SupplyLineDto> supplyLines, UUID approver) {
    // [SIGLUS change start]
    // [change reason]: for reportOnly, status change to RELEASED_WITHOUT_ORDER when final
    //                  approval.
    // if (isTrue(reportOnly)) {
    //   status = RequisitionStatus.RELEASED_WITHOUT_ORDER;
    // } else {
    // [SIGLUS change end]
    if (CollectionUtils.isEmpty(supplyLines) && nodeId != null) {
      status = RequisitionStatus.IN_APPROVAL;
      supervisoryNodeId = nodeId;
    } else {
      // [SIGLUS change start]
      // [change reason]: for reportOnly, status change to RELEASED_WITHOUT_ORDER when final
      //                  approval.
      if (isTrue(reportOnly)) {
        status = RequisitionStatus.RELEASED_WITHOUT_ORDER;
      } else {
        // [SIGLUS change end]
        status = RequisitionStatus.APPROVED;
      }
    }

    updateConsumptions(products);
    updateTotalCostAndPacksToShip(products);
    setModifiedDate(ZonedDateTime.now());

    statusChanges.add(StatusChange.newStatusChange(this, approver));
    // [SIGLUS change start]
    // [change reason]: for our frontend difference initial && second time to fill some filed.
    setIsSavedExtraData(false);
    // [SIGLUS change end]
  }

  /**
   * Rejects given requisition.
   */
  public void reject(Map<VersionIdentityDto, OrderableDto> products, UUID rejector) {
    status = RequisitionStatus.REJECTED;
    updateConsumptions(products);
    updateTotalCostAndPacksToShip(products);
    setModifiedDate(ZonedDateTime.now());
    supervisoryNodeId = null;

    // [SIGLUS change start]
    // [change reason]: #344 clear all signature and save flag after rejection
    clearSignature();
    setIsSavedExtraData(false);
    // [SIGLUS change end]

    statusChanges.add(StatusChange.newStatusChange(this, rejector));
  }

  /**
   * Release the requisition.
   */
  public void release(UUID releaser) {
    status = RequisitionStatus.RELEASED;
    setModifiedDate(ZonedDateTime.now());

    statusChanges.add(StatusChange.newStatusChange(this, releaser));
  }

  /**
   * Release the requisition.
   */
  public void releaseWithoutOrder(UUID releaser) {
    status = RequisitionStatus.RELEASED_WITHOUT_ORDER;
    setModifiedDate(ZonedDateTime.now());

    statusChanges.add(StatusChange.newStatusChange(this, releaser));
  }

  /**
   * Skip the requisition.
   */
  public void skip(boolean periodsSkippable, UUID userId) {
    if (!periodsSkippable) {
      throw new ValidationMessageException(new Message(ERROR_PROGRAM_DOES_NOT_ALLOW_SKIP));
    }

    if (!status.isSubmittable()) {
      throw new ValidationMessageException(new Message(ERROR_SKIP_FAILED_WRONG_STATUS));
    }

    if (isTrue(emergency)) {
      throw new ValidationMessageException(new Message(ERROR_SKIP_FAILED_EMERGENCY));
    }

    for (RequisitionLineItem item : requisitionLineItems) {
      item.skipLineItem(template);
    }

    status = RequisitionStatus.SKIPPED;
    statusChanges.add(StatusChange.newStatusChange(this, userId));
    setModifiedDate(ZonedDateTime.now());
  }

  // [SIGLUS change start]
  // [change reason]: create lineitem for add-product
  public RequisitionLineItem constructLineItem(
      RequisitionTemplate template,
      Integer stockOnHand, Integer beginningBalance,
      ApprovedProductDto productDto,
      int numberOfPreviousPeriodsToAverage,
      Map<UUID, Integer> idealStockAmounts,
      List<StockCardRangeSummaryDto> stockCardRangeSummaries,
      List<StockCardRangeSummaryDto> stockCardRangeSummariesToAverage,
      List<ProcessingPeriodDto> periods,
      ProofOfDeliveryDto pod,
      Collection<ApprovedProductDto> fullSupplyProducts) {
    if (requisitionLineItems == null) {
      requisitionLineItems = new ArrayList<>();
    }

    RequisitionLineItem lineItem;
    if (template.isPopulateStockOnHandFromStockCards()) {
      lineItem = buildLineItem(idealStockAmounts,
          stockOnHand,
          beginningBalance,
          stockCardRangeSummaries,
          stockCardRangeSummariesToAverage,
          periods,
          productDto);
    } else {
      lineItem = new RequisitionLineItem(this, productDto);
      lineItem.setIdealStockAmount(extractIdealStockAmount(idealStockAmounts, productDto));

      setLineItemBeginningBalance(lineItem);
      setLineItemTotalReceivedQuantity(pod, lineItem);

      setPreviousAdjustedConsumptions(numberOfPreviousPeriodsToAverage);
    }

    return lineItem;
  }

  private void setLineItemBeginningBalance(RequisitionLineItem requisitionLineItem) {
    // Firstly, if we display the column ...
    // ... and if the previous requisition exists ...
    // .. for each line from the current requisition ...
    if (!previousRequisitions.isEmpty()
        && null != previousRequisitions.get(0)
        && template.isColumnDisplayed(RequisitionLineItem.BEGINNING_BALANCE)
        && !requisitionLineItem.isLineSkipped()) {
      Map<VersionEntityReference, RequisitionLineItem> productIdToPreviousLine =
          previousRequisitions.get(0).getRequisitionLineItems().stream().collect(
              toMap(RequisitionLineItem::getOrderable, identity(),
                  (item1, item2) -> item1));

      // ... we try to find line in the previous requisition for the same product ...
      RequisitionLineItem previousLine = productIdToPreviousLine.getOrDefault(
          requisitionLineItem.getOrderable(), null);

      // ... and in the end we use it to calculate beginning balance in a new line.
      requisitionLineItem.setBeginningBalance(
          LineItemFieldsCalculator.calculateBeginningBalance(previousLine));
    }
  }

  private void setLineItemTotalReceivedQuantity(ProofOfDeliveryDto proofOfDelivery,
      RequisitionLineItem requisitionLineItem) {
    // Secondly, if Proof Of Delivery exists and it is submitted ...
    // .. for each line from the current requisition ...
    if (null != proofOfDelivery && proofOfDelivery.isSubmitted()
        && requisitionLineItem.isLineSkipped()) {
      Map<UUID, ProofOfDeliveryLineItemDto> productIdToPodLine = proofOfDelivery
          .getLineItems()
          .stream()
          .filter(li -> null != li.getOrderable())
          .collect(toMap(li -> li.getOrderable().getId(), identity(), (one, two) -> one));
      // ... we try to find line in POD for the same product ...
      ProofOfDeliveryLineItemDto proofOfDeliveryLine = productIdToPodLine.getOrDefault(
          requisitionLineItem.getOrderable().getId(), null);

      // ... and if line exists we set value for Total Received Quantity (B) column
      if (null != proofOfDeliveryLine) {
        requisitionLineItem.setTotalReceivedQuantity(
            OpenLmisNumberUtils.zeroIfNull(proofOfDeliveryLine.getQuantityAccepted())
        );
      }
    }
  }

  private RequisitionLineItem buildLineItem(
      Map<UUID, Integer> idealStockAmounts,
      Integer stockOnHand,
      Integer beginningBalance,
      List<StockCardRangeSummaryDto> stockCardRangeSummaries,
      List<StockCardRangeSummaryDto> stockCardRangeSummariesToAverage,
      List<ProcessingPeriodDto> periods,
      ApprovedProductDto product) {

    RequisitionLineItem lineItem = new RequisitionLineItem(this, product);
    lineItem.setIdealStockAmount(extractIdealStockAmount(idealStockAmounts, product));
    lineItem.setStockOnHand(initiateQuantity(stockOnHand));
    lineItem.setBeginningBalance(initiateQuantity(beginningBalance));

    StockCardRangeSummaryDto summary =
        findStockCardRangeSummary(stockCardRangeSummaries, lineItem.getOrderable().getId());

    setTotalReceivedQuantity(template, lineItem, summary);
    setTotalStockoutDays(lineItem, summary, numberOfMonthsInPeriod);
    setTotalConsumedQuantity(template, lineItem, summary);
    StockCardRangeSummaryDto summaryToAverage =
        findStockCardRangeSummary(stockCardRangeSummariesToAverage,
            lineItem.getOrderable().getId());
    setAverageConsumption(summaryToAverage, template, periods,
        previousRequisitions, lineItem);
    setTotalLossesAndAdjustments(template, lineItem, summary);

    return lineItem;
  }

  private void setTotalReceivedQuantity(RequisitionTemplate template,
      RequisitionLineItem lineItem,
      StockCardRangeSummaryDto summary) {
    if (isDisplayedTemplateColumn(TOTAL_RECEIVED_QUANTITY)) {
      lineItem.calculateAndSetStockBasedTotalReceivedQuantity(template, summary);
    } else {
      lineItem.setTotalReceivedQuantity(null);
    }
  }

  private void setTotalStockoutDays(RequisitionLineItem lineItem,
      StockCardRangeSummaryDto summary,
      Integer numberOfMonthsInPeriod) {
    if (isDisplayedTemplateColumn(TOTAL_STOCKOUT_DAYS)) {
      lineItem.calculateAndSetStockBasedTotalStockoutDays(summary,
          numberOfMonthsInPeriod);
    } else {
      lineItem.setTotalStockoutDays(null);
    }
  }

  private void setTotalConsumedQuantity(RequisitionTemplate template,
      RequisitionLineItem lineItem,
      StockCardRangeSummaryDto summary) {
    if (isDisplayedTemplateColumn(TOTAL_CONSUMED_QUANTITY)) {
      lineItem.calculateAndSetStockBasedTotalConsumedQuantity(template, summary);
    } else {
      lineItem.setTotalConsumedQuantity(null);
    }
  }

  private void setAverageConsumption(StockCardRangeSummaryDto summaryToAverage,
      RequisitionTemplate template,
      List<ProcessingPeriodDto> periods,
      List<Requisition> previousRequisitions,
      RequisitionLineItem lineItem) {
    if (isDisplayedTemplateColumn(AVERAGE_CONSUMPTION)) {
      lineItem.calculateAndSetStockBasedAverageConsumption(summaryToAverage, template, periods,
          previousRequisitions);
    } else {
      lineItem.setAverageConsumption(null);
    }
  }

  private void setTotalLossesAndAdjustments(RequisitionTemplate template,
      RequisitionLineItem lineItem,
      StockCardRangeSummaryDto summary) {
    if (isDisplayedTemplateColumn(TOTAL_LOSSES_AND_ADJUSTMENTS)) {
      lineItem.calculateAndSetStockBasedTotalLossesAndAdjustments(template, summary);
    } else {
      lineItem.setTotalLossesAndAdjustments(null);
    }
  }

  private Integer initiateQuantity(Integer quantity) {
    return quantity == null ? 0 : quantity;
  }

  private boolean isDisplayedTemplateColumn(String name) {
    RequisitionTemplateColumn column = template.findColumn(name);
    return column != null && column.getIsDisplayed();
  }
  // [SIGLUS change end]

  // [SIGLUS change start]
  // [change reason]: #344 clear the signature from extra data
  private void clearSignature() {
    if (this.extraData.containsKey(SIGNATURE)) {
      Map<String, Object> extra = this.extraData.getExtraData();
      extra.entrySet().removeIf(entry -> entry.getKey().equals(SIGNATURE));
    }
  }
  // [SIGLUS change end]

  /**
   * Finds first RequisitionLineItem that have productId property equals to the given productId
   * argument.
   *
   * @param productId orderable id
   * @param productVersionId orderable version id
   * @return first RequisitionLineItem that have productId property equals to the given productId
   *         argument; otherwise null;
   */
  public RequisitionLineItem findLineByProduct(UUID productId, Long productVersionId) {
    if (null == requisitionLineItems) {
      return null;
    }

    VersionEntityReference arg = new VersionEntityReference(productId, productVersionId);

    return requisitionLineItems
        .stream()
        .filter(e -> Objects.equals(arg, e.getOrderable()))
        .findFirst()
        .orElse(null);
  }

  public void setDraftStatusMessage(String draftStatusMessage) {
    this.draftStatusMessage = (draftStatusMessage == null) ? "" : draftStatusMessage;
  }

  public boolean isPreAuthorize() {
    return status.isPreAuthorize();
  }

  /**
   * Filter out requisitionLineItems that are skipped.
   *
   * @return requisitionLineItems that are not skipped
   */
  public List<RequisitionLineItem> getNonSkippedRequisitionLineItems() {
    return filterLineItems(false, null, null);
  }

  /**
   * Filter out requisitionLineItems that are skipped and not-full supply.
   *
   * @return non-skipped full supply requisition line items
   */
  public List<RequisitionLineItem> getNonSkippedFullSupplyRequisitionLineItems(
      Map<VersionIdentityDto, OrderableDto> orderables) {
    return filterLineItems(false, false, orderables);
  }

  /**
   * Filter out requisitionLineItems that are skipped and full supply.
   *
   * @return non-skipped non-full supply requisition line items
   */
  public List<RequisitionLineItem> getNonSkippedNonFullSupplyRequisitionLineItems(
      Map<VersionIdentityDto, OrderableDto> orderables) {
    return filterLineItems(false, true, orderables);
  }

  /**
   * Calculates combined cost of all requisition line items.
   *
   * @return sum of total costs.
   */
  public Money getTotalCost() {
    return calculateTotalCostForLines(requisitionLineItems);
  }

  /**
   * Calculates combined cost of non-full supply non-skipped requisition line items.
   *
   * @return sum of total costs.
   */
  public Money getNonFullSupplyTotalCost(Map<VersionIdentityDto, OrderableDto> orderables) {
    return calculateTotalCostForLines(getNonSkippedNonFullSupplyRequisitionLineItems(orderables));
  }

  /**
   * Calculates combined cost of full supply non-skipped requisition line items.
   *
   * @return sum of total costs.
   */
  public Money getFullSupplyTotalCost(Map<VersionIdentityDto, OrderableDto> orderables) {
    return calculateTotalCostForLines(getNonSkippedFullSupplyRequisitionLineItems(orderables));
  }

  // [SIGLUS change start]
  // [change reason]: received kit quantity
  public Integer getTotalReceivedQuantity(UUID orderableId,
      List<StockCardRangeSummaryDto> stockCardRangeSummaries) {
    StockCardRangeSummaryDto summary =
        findStockCardRangeSummary(stockCardRangeSummaries, orderableId);
    return calculateStockBasedTotalReceivedQuantity(template, summary, orderableId);
  }
  // [SIGLUS change end]

  /**
   * Filter out requisitionLineItems that are not skipped.
   *
   * @return requisitionLineItems that are skipped
   */
  List<RequisitionLineItem> getSkippedRequisitionLineItems() {
    return filterLineItems(true, null, null);
  }

  List<RequisitionLineItem> getFullSupplyRequisitionLineItems(
      Map<VersionIdentityDto, OrderableDto> orderables) {
    return filterLineItems(null, false, orderables);
  }

  private List<RequisitionLineItem> filterLineItems(Boolean skipped, Boolean nonFullSupply,
      Map<VersionIdentityDto, OrderableDto> orderables) {
    List<RequisitionLineItem> list = new ArrayList<>();

    if (null == requisitionLineItems) {
      return list;
    }

    for (RequisitionLineItem line : requisitionLineItems) {
      if (null != skipped && !Objects.equals(skipped, line.isLineSkipped())) {
        continue;
      }

      if (null != nonFullSupply) {
        OrderableDto orderable = orderables.get(new VersionIdentityDto(line.getOrderable()));

        // [SIGLUS change start]:
        // [change reason]: siglus all full supply(ordeable relate to real program).
        // ProgramOrderableDto programOrderable = orderable.getProgramOrderable(programId);
        ProgramOrderableDto programOrderable = orderable.getPrograms().stream().findFirst()
            .orElse(null);
        // [SIGLUS change end]
        if (Objects.equals(nonFullSupply, isFalse(programOrderable.getFullSupply()))) {
          list.add(line);
        }
      } else {
        list.add(line);
      }
    }

    return list;
  }

  /**
   * Export this object to the specified exporter (DTO).
   *
   * @param exporter exporter to export to
   */
  public void export(Exporter exporter) {
    exporter.setId(id);
    exporter.setCreatedDate(getCreatedDate());
    exporter.setModifiedDate(getModifiedDate());
    exporter.setStatus(status);
    exportStatusChanges(exporter);
    exporter.setEmergency(emergency);
    exporter.setReportOnly(reportOnly);
    exporter.setSupplyingFacility(supplyingFacilityId);
    exporter.setSupervisoryNode(supervisoryNodeId);
    exporter.setDraftStatusMessage(draftStatusMessage);
    if (datePhysicalStockCountCompleted != null) {
      exporter.setDatePhysicalStockCountCompleted(
          datePhysicalStockCountCompleted.getLocalDate());
    }

    extraData = ExtraDataEntity.defaultEntity(extraData);
    extraData.export(exporter);
  }

  private void exportStatusChanges(Exporter exporter) {
    Optional<Supplier<StatusChange.Exporter>> factory = exporter.provideStatusChangeExporter();

    if (factory.isPresent()) {
      Supplier<StatusChange.Exporter> generator = factory.get();

      // we use the array here to create a new collection object
      // that will be resistant to changes that could be
      // provided by other threads and could cause
      // the java.util.ConcurrentModificationException exception
      StatusChange[] statusChangesArray = Optional
          .ofNullable(statusChanges)
          .orElse(Collections.emptyList())
          .toArray(new StatusChange[0]);

      for (StatusChange statusChange : statusChangesArray) {
        StatusChange.Exporter statusChangeExporter = generator.get();
        statusChange.export(statusChangeExporter);

        exporter.addStatusChange(statusChangeExporter);
      }
    }
  }

  /**
   * Find latest status change by date created.
   *
   * @return recent status change
   */
  public StatusChange getLatestStatusChange() {
    return statusChanges.stream()
        .max(Comparator.comparing(BaseTimestampedEntity::getCreatedDate,
            Comparator.nullsFirst(Comparator.naturalOrder())))
        .orElse(null);
  }

  /**
   * Sets appropriate value for Previous Adjusted Consumptions field in each {@link
   * RequisitionLineItem}.
   */
  void setPreviousAdjustedConsumptions(int numberOfPreviousPeriodsToAverage) {
    Map<VersionEntityReference, List<RequisitionLineItem>> orderables = previousRequisitions
        .subList(0, numberOfPreviousPeriodsToAverage)
        .stream()
        .map(Requisition::getNonSkippedRequisitionLineItems)
        .flatMap(Collection::stream)
        .collect(Collectors.groupingBy(RequisitionLineItem::getOrderable));

    requisitionLineItems
        .forEach(line -> line.setPreviousAdjustedConsumptions(Optional
            .ofNullable(orderables.get(line.getOrderable()))
            .orElse(Collections.emptyList())
            .stream()
            .map(RequisitionLineItem::getAdjustedConsumption)
            .filter(Objects::nonNull)
            .collect(toList())));
  }

  Map<VersionEntityReference, Object> getAllColumnsValuesByColumnName(String columnName) {
    return requisitionLineItems
        .stream()
        .collect(
            HashMap::new,
            (map, line) -> map.put(line.getOrderable(), getColumnValue(line, columnName)),
            HashMap::putAll);
  }

  private static Object getColumnValue(RequisitionLineItem lineItem, String columnName) {
    try {
      return PropertyUtils.getProperty(lineItem, columnName);
    } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException exp) {
      throw new IllegalStateException(exp);
    }
  }

  private void prepareRequisitionForApproval(UUID user) {
    // [SIGLUS change start]
    // [change reason]:dont populate Approved Quantity, all logic by frontend
    // populateApprovedQuantity();
    // [SIGLUS change end]
    getSkippedRequisitionLineItems().forEach(RequisitionLineItem::resetData);
    status = RequisitionStatus.AUTHORIZED;
    statusChanges.add(StatusChange.newStatusChange(this, user));
  }

  private Money calculateTotalCostForLines(List<RequisitionLineItem> requisitionLineItems) {
    return Optional
        .ofNullable(requisitionLineItems)
        .orElse(Collections.emptyList())
        .stream()
        .map(RequisitionLineItem::getTotalCost)
        .filter(Objects::nonNull)
        .reduce(Money::plus)
        .orElseGet(() -> Money.of(CurrencyUnit.of(currencyCode), 0));
  }

  private void calculateAndValidateTemplateFields(RequisitionTemplate template,
      Map<VersionIdentityDto, OrderableDto> orderables,
      Map<VersionIdentityDto, ApprovedProductDto> approvedProducts) {
    getNonSkippedFullSupplyRequisitionLineItems(orderables)
        .forEach(line ->
            line.calculateAndSetFields(template, stockAdjustmentReasons,
                numberOfMonthsInPeriod, approvedProducts));
  }

  private void updateConsumptions(Map<VersionIdentityDto, OrderableDto> orderables) {
    if (template.isColumnInTemplateAndDisplayed(ADJUSTED_CONSUMPTION)) {
      getNonSkippedFullSupplyRequisitionLineItems(orderables)
          .forEach(line -> line.setAdjustedConsumption(
              LineItemFieldsCalculator.calculateAdjustedConsumption(line, numberOfMonthsInPeriod,
                  this.template.isColumnInTemplateAndDisplayed(ADDITIONAL_QUANTITY_REQUIRED))
          ));
    }

    if (template.isColumnInTemplateAndDisplayed(AVERAGE_CONSUMPTION)) {
      getNonSkippedFullSupplyRequisitionLineItems(orderables).forEach(
          RequisitionLineItem::calculateAndSetAverageConsumption);
    }
  }

  private void updateTotalCostAndPacksToShip(Map<VersionIdentityDto, OrderableDto> products) {
    getNonSkippedRequisitionLineItems()
        .forEach(line -> {
          OrderableDto product = products.get(new VersionIdentityDto(line.getOrderable()));
          ProgramOrderableDto programOrderable = product.getProgramOrderable(programId);

          line.updatePacksToShip(product);
          line.setTotalCost(LineItemFieldsCalculator
              .calculateTotalCost(line, programOrderable, CurrencyUnit.of(currencyCode)));
        });
  }

  private void populateApprovedQuantity() {
    for (RequisitionLineItem line : getNonSkippedRequisitionLineItems()) {
      if (template.isColumnInTemplate(CALCULATED_ORDER_QUANTITY_ISA)
          && template.isColumnDisplayed(CALCULATED_ORDER_QUANTITY_ISA)) {
        line.setApprovedQuantity(Optional
            .ofNullable(line.getRequestedQuantity())
            .orElse(line.getCalculatedOrderQuantityIsa()));
      } else if (template.isColumnDisplayed(CALCULATED_ORDER_QUANTITY)) {
        line.setApprovedQuantity(Optional
            .ofNullable(line.getRequestedQuantity())
            .orElse(line.getCalculatedOrderQuantity()));
      } else {
        line.setApprovedQuantity(line.getRequestedQuantity());
      }
    }
  }

  private void updateReqLines(Collection<RequisitionLineItem> newLineItems) {
    if (null == newLineItems) {
      return;
    }

    if (null == requisitionLineItems) {
      requisitionLineItems = new ArrayList<>();
    }

    List<RequisitionLineItem> updatedList = new ArrayList<>();

    for (RequisitionLineItem item : newLineItems) {
      RequisitionLineItem existing = requisitionLineItems
          .stream()
          .filter(l -> l.getId().equals(item.getId()))
          .findFirst()
          .orElse(null);

      if (null == existing) {
        item.setRequisition(this);
        updatedList.add(item);
      } else {
        existing.setRequisition(this);
        existing.updateFrom(item);
        updatedList.add(existing);
      }
    }

    requisitionLineItems.clear();
    requisitionLineItems.addAll(updatedList);
  }

  private StockCardRangeSummaryDto findStockCardRangeSummary(
      List<StockCardRangeSummaryDto> stockCardRangeSummaryDtos, UUID orderableId) {
    return stockCardRangeSummaryDtos
        .stream()
        .filter(range -> range.getOrderable().getId().equals(orderableId))
        .findFirst()
        .orElse(null);
  }

  public Map<String, Object> getExtraData() {
    return this.extraData.getExtraData();
  }

  public void setExtraData(Map<String, Object> extraData) {
    this.extraData = ExtraDataEntity.defaultEntity(this.extraData);
    this.extraData.updateFrom(extraData);
  }

  /**
   * Gets a value of extra data originalRequisition key.
   */
  public UUID getOriginalRequisitionId() {
    Object value = this.extraData.get(EXTRA_DATA_ORIGINAL_REQUISITION_ID);
    String asString = null == value ? null : value.toString();
    return null == asString ? null : UUID.fromString(asString);
  }

  public void setOriginalRequisitionId(UUID originalRequisitionId) {
    this.extraData.put(EXTRA_DATA_ORIGINAL_REQUISITION_ID, originalRequisitionId);
  }

  public boolean hasOriginalRequisitionId() {
    return this.extraData.containsKey(EXTRA_DATA_ORIGINAL_REQUISITION_ID);
  }

  @PrePersist
  private void initModifiedDate() {
    this.setModifiedDate(this.getCreatedDate());
  }

  public interface Exporter extends ExtraDataExporter {

    void setId(UUID id);

    void setCreatedDate(ZonedDateTime createdDate);

    void setModifiedDate(ZonedDateTime createdDate);

    void setStatus(RequisitionStatus status);

    void setEmergency(Boolean emergency);

    void setReportOnly(Boolean reportOnly);

    void setSupplyingFacility(UUID supplyingFacility);

    void setSupervisoryNode(UUID supervisoryNode);

    void setTemplate(BasicRequisitionTemplateDto template);

    void setDraftStatusMessage(String draftStatusMessage);

    void setDatePhysicalStockCountCompleted(LocalDate localDate);

    void setStockAdjustmentReasons(List<ReasonDto> reasonDto);

    Optional<Supplier<StatusChange.Exporter>> provideStatusChangeExporter();

    void addStatusChange(StatusChange.Exporter providedExporter);
  }

  public interface Importer extends ExtraDataImporter {

    UUID getId();

    ZonedDateTime getCreatedDate();

    ZonedDateTime getModifiedDate();

    List<RequisitionLineItem.Importer> getRequisitionLineItems();

    UUID getFacilityId();

    UUID getProgramId();

    UUID getProcessingPeriodId();

    RequisitionStatus getStatus();

    Boolean getEmergency();

    UUID getSupplyingFacility();

    UUID getSupervisoryNode();

    String getDraftStatusMessage();

    Set<VersionIdentityDto> getAvailableNonFullSupplyProductsIdentities();

    LocalDate getDatePhysicalStockCountCompleted();
  }
}
