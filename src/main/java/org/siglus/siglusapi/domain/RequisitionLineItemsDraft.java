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

import java.util.List;
import java.util.UUID;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
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
import org.openlmis.requisition.domain.requisition.VersionEntityReference;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "requisition_line_items_draft", schema = "siglusintegration")
public class RequisitionLineItemsDraft extends BaseEntity {

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

  private Integer averageConsumption;

  private Integer maximumStockQuantity;

  private Integer calculatedOrderQuantity;

  @OneToMany(
      cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.REMOVE},
      fetch = FetchType.LAZY,
      orphanRemoval = true)
  @Getter
  @Setter
  @JoinColumn(name = "requisitionLineItemId")
  @BatchSize(size = STANDARD_BATCH_SIZE)
  private List<StockAdjustmentDraft> stockAdjustments;

  private Integer idealStockAmount;

  private Integer calculatedOrderQuantityIsa;

  private Integer authorizedQuantity;

}
