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

package org.siglus.siglusapi.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
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
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Type;
import org.joda.money.Money;
import org.openlmis.requisition.domain.requisition.RequisitionLineItem;
import org.openlmis.requisition.domain.requisition.VersionEntityReference;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.requisition.dto.MetadataDto;
import org.openlmis.requisition.dto.OrderableDto;
import org.openlmis.requisition.dto.RequisitionLineItemV2Dto;
import org.siglus.common.domain.BaseEntity;
import org.springframework.beans.BeanUtils;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "requisition_line_items_draft", schema = "siglusintegration")
public class RequisitionLineItemDraft extends BaseEntity {

  @ManyToOne(cascade = CascadeType.REFRESH)
  @JoinColumn(name = "requisitionDraftId")
  @Getter
  @Setter
  private RequisitionDraft requisitionDraft;

  @Embedded
  @AttributeOverrides({
      @AttributeOverride(name = "id", column = @Column(name = "orderableId")),
      @AttributeOverride(name = "versionNumber", column = @Column(name = "orderableVersionNumber"))
  })
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

  private UUID requisitionLineItemId;

  private UUID requisitionId;

  private Integer beginningBalance;

  private Integer totalReceivedQuantity;

  private Integer totalLossesAndAdjustments;

  private Integer stockOnHand;

  private Integer requestedQuantity;

  private Integer totalConsumedQuantity;

  private Integer total;

  private String requestedQuantityExplanation;

  @Column(length = 250)
  private String remarks;

  private Integer approvedQuantity;

  private Integer totalStockoutDays;

  private Long packsToShip;

  private Boolean skipped;

  @Type(type = "org.openlmis.requisition.domain.type.CustomSingleColumnMoneyUserType")
  private Money totalCost;

  private Integer numberOfNewPatientsAdded;

  private Integer additionalQuantityRequired;

  private Integer adjustedConsumption;

  @ElementCollection
  @CollectionTable(name = "previous_adjusted_consumptions_draft", schema = "siglusintegration",
      joinColumns = @JoinColumn(name = "draftLineItemId"))
  @Column(name = "previousAdjustedConsumption")
  @Setter
  @Getter
  private List<Integer> previousAdjustedConsumptions;

  private Integer averageConsumption;

  private Integer maximumStockQuantity;

  private Integer calculatedOrderQuantity;

  @OneToMany(
      cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.REMOVE},
      fetch = FetchType.LAZY,
      orphanRemoval = true)
  @Getter
  @Setter
  @JoinColumn(name = "draftLineItemId")
  @BatchSize(size = STANDARD_BATCH_SIZE)
  private List<StockAdjustmentDraft> stockAdjustments;

  private Integer idealStockAmount;

  private Integer calculatedOrderQuantityIsa;

  private Integer authorizedQuantity;

  private Integer suggestedQuantity;

  private LocalDate expirationDate;

  public static RequisitionLineItemDraft from(RequisitionDraft draft,
      RequisitionLineItem.Importer lineItemV2Dto) {
    RequisitionLineItemDraft lineItemDraft = new RequisitionLineItemDraft();
    BeanUtils.copyProperties(lineItemV2Dto, lineItemDraft);
    lineItemDraft.setId(null);
    lineItemDraft.setRequisitionLineItemId(lineItemV2Dto.getId());
    VersionEntityReference orderable = new VersionEntityReference();
    orderable.setId(lineItemV2Dto.getOrderableIdentity().getId());
    orderable.setVersionNumber(lineItemV2Dto.getOrderableIdentity().getVersionNumber());
    lineItemDraft.setOrderable(orderable);
    VersionEntityReference approvedProduct = new VersionEntityReference();
    approvedProduct.setId(lineItemV2Dto.getApprovedProductIdentity().getId());
    approvedProduct.setVersionNumber(lineItemV2Dto.getApprovedProductIdentity().getVersionNumber());
    lineItemDraft.setRequisitionDraft(draft);
    lineItemDraft.setPreviousAdjustedConsumptions(lineItemV2Dto.getPreviousAdjustedConsumptions());
    lineItemDraft.setFacilityTypeApprovedProduct(approvedProduct);
    lineItemDraft.setRequisitionId(draft.getRequisitionId());
    lineItemDraft.setStockAdjustments(lineItemV2Dto.getStockAdjustments().stream()
        .map(StockAdjustmentDraft::from).collect(Collectors.toList()));
    return lineItemDraft;
  }

  public static RequisitionLineItemV2Dto getLineItemDto(RequisitionLineItemDraft lineItemDraft) {
    RequisitionLineItemV2Dto dto = new RequisitionLineItemV2Dto();
    BeanUtils.copyProperties(lineItemDraft, dto);
    dto.setId(lineItemDraft.getRequisitionLineItemId());
    OrderableDto orderable = new OrderableDto();
    orderable.setId(lineItemDraft.getOrderable().getId());
    orderable.setMeta(new MetadataDto(lineItemDraft.getOrderable().getVersionNumber(), null));
    ApprovedProductDto approvedProduct = new ApprovedProductDto(
        lineItemDraft.getFacilityTypeApprovedProduct().getId(), null, null, null,
        null, null, new MetadataDto(
        lineItemDraft.getFacilityTypeApprovedProduct().getVersionNumber(), null));
    dto.setOrderable(orderable);
    dto.setApprovedProduct(approvedProduct);
    dto.setStockAdjustments(lineItemDraft.getStockAdjustments()
        .stream()
        .map(StockAdjustmentDraft::getStockAdjustmentDto)
        .collect(Collectors.toList()));
    return dto;
  }

  public static RequisitionLineItem getLineItem(RequisitionLineItemDraft lineItemDraft) {
    RequisitionLineItem lineItem = new RequisitionLineItem();
    BeanUtils.copyProperties(lineItemDraft, lineItem);
    lineItem.setId(lineItemDraft.getRequisitionLineItemId());
    return lineItem;
  }
}
