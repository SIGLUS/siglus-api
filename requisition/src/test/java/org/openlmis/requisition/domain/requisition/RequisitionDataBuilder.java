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

import com.google.common.collect.Maps;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.assertj.core.util.Lists;
import org.assertj.core.util.Sets;
import org.openlmis.requisition.domain.ExtraDataEntity;
import org.openlmis.requisition.domain.RequisitionTemplate;
import org.openlmis.requisition.domain.RequisitionTemplateDataBuilder;
import org.openlmis.requisition.dto.RequisitionDto;
import org.openlmis.requisition.service.PermissionService;
import org.openlmis.requisition.testutils.BasicRequisitionTemplateDtoDataBuilder;
import org.openlmis.requisition.testutils.ProcessingPeriodDtoDataBuilder;
import org.openlmis.requisition.testutils.ProgramDtoDataBuilder;
import org.openlmis.requisition.testutils.StatusChangeDataBuilder;
import org.openlmis.requisition.testutils.api.DataBuilder;
import org.openlmis.requisition.testutils.api.DtoDataBuilder;
import org.openlmis.requisition.testutils.api.RepositoryDataBuilder;

@SuppressWarnings("PMD.TooManyMethods")
public class RequisitionDataBuilder implements DataBuilder<Requisition>,
    RepositoryDataBuilder<Requisition>, DtoDataBuilder<RequisitionDto> {
  private UUID id = UUID.randomUUID();
  private List<RequisitionLineItem> requisitionLineItems = Lists.newArrayList();
  private String draftStatusMessage = "";
  private RequisitionTemplate template = new RequisitionTemplateDataBuilder()
      .withAllColumns()
      .build();
  private UUID facilityId = UUID.randomUUID();
  private UUID programId = UUID.randomUUID();
  private UUID processingPeriodId = UUID.randomUUID();
  private UUID supplyingFacilityId = null;
  private RequisitionStatus status = RequisitionStatus.INITIATED;
  private List<StatusChange> statusChanges = new ArrayList<>();
  private Boolean emergency = false;
  private Boolean reportOnly = false;
  private Integer numberOfMonthsInPeriod = 1;
  private Long version = 1L;
  private UUID supervisoryNodeId = null;
  private List<Requisition> previousRequisitions = Lists.newArrayList();
  private Set<ApprovedProductReference> availableProducts = Sets.newHashSet();
  private DatePhysicalStockCountCompleted datePhysicalStockCountCompleted =
      new DatePhysicalStockCountCompleted(LocalDate.now().minusMonths(1));
  private List<StockAdjustmentReason> stockAdjustmentReasons = new ArrayList<>();
  private List<RequisitionPermissionString> permissionStrings = new ArrayList<>();
  private Map<String, Object> extraData = Maps.newHashMap();
  private ZonedDateTime createdDate = ZonedDateTime.now();
  private ZonedDateTime modifiedDate = ZonedDateTime.now();

  /**
   * Creates new instance of {@link Requisition}.
   */
  @Override
  public Requisition build() {
    return buildInitiatedRegularRequisition();
  }

  /**
   * Creates new instance of {@link Requisition} dedicated to repository usage.
   */
  @Override
  public Requisition buildAsNew() {
    Requisition requisition = new Requisition(facilityId, programId, processingPeriodId,
        status, emergency);
    requisition.setCreatedDate(createdDate);
    requisition.setModifiedDate(modifiedDate);
    requisition.setNumberOfMonthsInPeriod(numberOfMonthsInPeriod);
    requisition.setTemplate(template);
    requisition.setSupervisoryNodeId(supervisoryNodeId);
    requisition.setDraftStatusMessage(draftStatusMessage);
    requisition.setRequisitionLineItems(requisitionLineItems);
    requisition.setSupplyingFacilityId(supplyingFacilityId);
    requisition.setStatusChanges(statusChanges);
    requisition.setReportOnly(reportOnly);
    requisition.setNumberOfMonthsInPeriod(numberOfMonthsInPeriod);
    requisition.setPreviousRequisitions(previousRequisitions);
    requisition.setAvailableProducts(availableProducts);
    requisition.setDatePhysicalStockCountCompleted(datePhysicalStockCountCompleted);
    requisition.setStockAdjustmentReasons(stockAdjustmentReasons);
    requisition.setExtraData(extraData);
    return requisition;
  }

  /**
   * Creates new instance of {@link RequisitionDto}.
   */
  @Override
  public RequisitionDto buildAsDto() {
    Requisition requisition = buildInitiatedRegularRequisition();
    RequisitionDto requisitionDto = new RequisitionDto();
    requisition.export(requisitionDto);
    requisitionDto.setRequisitionLineItems(new ArrayList<>());
    requisitionDto.setTemplate(new BasicRequisitionTemplateDtoDataBuilder().buildAsDto());
    requisitionDto.setProgram(new ProgramDtoDataBuilder().buildAsDto());
    requisitionDto.setProcessingPeriod(new ProcessingPeriodDtoDataBuilder().buildAsDto());
    requisitionDto.setStockAdjustmentReasons(new ArrayList<>());
    requisitionDto.setAvailableFullSupplyProducts(new HashSet<>());
    requisitionDto.setAvailableNonFullSupplyProducts(new HashSet<>());
    return requisitionDto;
  }

  /**
   * Creates new initiated instance of {@link Requisition}.
   */
  public Requisition buildInitiatedRegularRequisition() {
    Requisition requisition = new Requisition(
        requisitionLineItems, version, draftStatusMessage, template, facilityId, programId,
        processingPeriodId, supplyingFacilityId, status, statusChanges, emergency, reportOnly,
        numberOfMonthsInPeriod, supervisoryNodeId, previousRequisitions, availableProducts,
        datePhysicalStockCountCompleted, stockAdjustmentReasons, permissionStrings,
        new ExtraDataEntity(extraData)
    );
    requisition.setId(id);
    requisition.setCreatedDate(createdDate);
    requisition.setModifiedDate(modifiedDate);

    requisitionLineItems.forEach(line -> line.setRequisition(requisition));

    return requisition;
  }

  /**
   * Builds a requisition in authorized status.
   */
  public Requisition buildAuthorizedRequisition() {
    Requisition requisition = withStatus(RequisitionStatus.AUTHORIZED)
        .build();

    List<StatusChange> statusChanges = new ArrayList<>();
    statusChanges.add(new StatusChangeDataBuilder().forInitiatedRequisition(requisition).build());
    statusChanges.add(new StatusChangeDataBuilder().forSubmittedRequisition(requisition).build());
    statusChanges.add(new StatusChangeDataBuilder().forAuthorizedRequisition(requisition).build());

    requisition.setStatusChanges(statusChanges);
    return requisition;
  }

  /**
   * Add a requisition line item. Update available products list only if requisition is
   * emergency or line item is related with non full supply product.
   */
  public RequisitionDataBuilder addLineItem(RequisitionLineItem lineItem, boolean nonFullSupply) {
    requisitionLineItems.add(lineItem);

    if (emergency || nonFullSupply) {
      availableProducts.add(new ApprovedProductReference(
          lineItem.getFacilityTypeApprovedProduct(), lineItem.getOrderable()));
    }

    return this;
  }

  /**
   * Sets stock adjustment reason.
   */
  public RequisitionDataBuilder addStockAdjustmentReason(StockAdjustmentReason reason) {
    this.stockAdjustmentReasons.add(reason);
    return this;
  }

  /**
   * Sets id.
   */
  public RequisitionDataBuilder withId(UUID id) {
    this.id = id;
    return this;
  }

  /**
   * Sets physical stock count completion date.
   */
  public RequisitionDataBuilder withDatePhysicalStockCountCompleted(
      LocalDate datePhysicalStockCountCompleted) {
    this.datePhysicalStockCountCompleted =
        new DatePhysicalStockCountCompleted(datePhysicalStockCountCompleted);
    return this;
  }

  public RequisitionDataBuilder withDatePhysicalStockCountCompleted(
      DatePhysicalStockCountCompleted datePhysicalStockCountCompleted) {
    this.datePhysicalStockCountCompleted = datePhysicalStockCountCompleted;
    return this;
  }

  /**
  * Sets status.
  */

  public RequisitionDataBuilder withStatus(RequisitionStatus status) {
    this.status = status;
    return this;
  }

  /**
   * Sets template.
   */
  public RequisitionDataBuilder withTemplate(RequisitionTemplate template) {
    this.template = template;
    return this;
  }

  /**
   * Sets line item list.
   */
  public RequisitionDataBuilder withLineItems(List<RequisitionLineItem> requisitionLineItems,
      boolean nonFullSupply) {
    this.requisitionLineItems.clear();
    requisitionLineItems.forEach(line -> addLineItem(line, nonFullSupply));

    return this;
  }

  /**
   * Sets facility id.
   */
  public RequisitionDataBuilder withFacilityId(UUID facilityId) {
    this.facilityId = facilityId;
    return this;
  }


  /**
   * Sets program id.
   */
  public RequisitionDataBuilder withProgramId(UUID programId) {
    this.programId = programId;
    return this;
  }

  /**
   * Sets processing period id.
   */
  public RequisitionDataBuilder withProcessingPeriodId(UUID processingPeriodId) {
    this.processingPeriodId = processingPeriodId;
    return this;
  }

  /**
   * Sets supervisory node id.
   */
  public RequisitionDataBuilder withSupervisoryNodeId(UUID supervisoryNodeId) {
    this.supervisoryNodeId = supervisoryNodeId;
    return this;
  }

  /**
   * Sets original requisition id.
   */
  public RequisitionDataBuilder withOriginalRequisitionId(UUID originalRequisitionId) {
    this.extraData.put(Requisition.EXTRA_DATA_ORIGINAL_REQUISITION_ID, originalRequisitionId);
    return this;
  }

  /**
   * Sets default view permission string.
   */
  public RequisitionDataBuilder withPermissionStrings() {
    permissionStrings.add(new RequisitionPermissionString(null,
        String.join("|", PermissionService.REQUISITION_VIEW, facilityId.toString(),
            programId.toString())));
    return this;
  }

  /**
   * Sets requisition line items.
   */
  public RequisitionDataBuilder withRequisitionLineItems(
      List<RequisitionLineItem> requisitionLineItems) {
    this.requisitionLineItems = requisitionLineItems;
    return this;
  }

  /**
   * Sets number of months in period.
   */
  public RequisitionDataBuilder withNumberOfMonthsInPeriod(
      Integer numberOfMonthsInPeriod) {
    this.numberOfMonthsInPeriod = numberOfMonthsInPeriod;
    return this;
  }

  /**
   * Sets stock adjustment reasons.
   */
  public RequisitionDataBuilder withStockAdjustmentReasons(
      List<StockAdjustmentReason> stockAdjustmentReasons) {
    this.stockAdjustmentReasons = stockAdjustmentReasons;
    return this;
  }

  /**
   * Sets emergency.
   */
  public RequisitionDataBuilder withEmergency(Boolean emergency) {
    this.emergency = emergency;
    return this;
  }

  public RequisitionDataBuilder withCreatedDate(ZonedDateTime createdDate) {
    this.createdDate = createdDate;
    return this;
  }

  public RequisitionDataBuilder withModifiedDate(ZonedDateTime modifiedDate) {
    this.modifiedDate = modifiedDate;
    return this;
  }

  public RequisitionDataBuilder withSupplyingFacilityId(UUID supplyingFacilityId) {
    this.supplyingFacilityId = supplyingFacilityId;
    return this;
  }

  public RequisitionDataBuilder withDraftStatusMessage(String draftStatusMessage) {
    this.draftStatusMessage = draftStatusMessage;
    return this;
  }

  public RequisitionDataBuilder withStatusChanges(List<StatusChange> statusChanges) {
    this.statusChanges = statusChanges;
    return this;
  }

  /**
   * Add available product.
   */
  public RequisitionDataBuilder addAvailableProduct(UUID facilityTypeApprovedProductId,
      Long facilityTypeApprovedProductVersionNumber, UUID orderableId,
      Long orderableVersionNumber) {
    this.availableProducts.add(new ApprovedProductReference(
        facilityTypeApprovedProductId, facilityTypeApprovedProductVersionNumber,
        orderableId, orderableVersionNumber));
    return this;
  }

  /**
   * Sets all fields empty.
   */
  public RequisitionDataBuilder withEmptyFields() {
    id = null;
    requisitionLineItems = new ArrayList<>();
    draftStatusMessage = null;
    template = null;
    facilityId = null;
    programId = null;
    processingPeriodId = null;
    supplyingFacilityId = null;
    status = null;
    statusChanges = null;
    emergency = null;
    reportOnly = null;
    numberOfMonthsInPeriod = null;
    version = null;
    supervisoryNodeId = null;
    previousRequisitions = new ArrayList<>();
    availableProducts = new HashSet<>();
    datePhysicalStockCountCompleted = null;
    stockAdjustmentReasons = new ArrayList<>();
    permissionStrings = new ArrayList<>();
    extraData = null;
    createdDate = null;
    modifiedDate = null;
    return this;
  }
}

