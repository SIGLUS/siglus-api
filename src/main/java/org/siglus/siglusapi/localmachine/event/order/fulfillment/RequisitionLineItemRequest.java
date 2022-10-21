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

package org.siglus.siglusapi.localmachine.event.order.fulfillment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.joda.money.Money;
import org.openlmis.requisition.domain.requisition.VersionEntityReference;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RequisitionLineItemRequest {

  private VersionEntityReference orderable;
  private Integer approvedQuantity;
  private VersionEntityReference facilityTypeApprovedProduct;
  private Integer beginningBalance;
  private Integer totalReceivedQuantity;
  private Integer totalLossesAndAdjustments;
  private Integer stockOnHand;
  private Integer requestedQuantity;
  private Integer totalConsumedQuantity;
  private Integer total;
  private String requestedQuantityExplanation;
  private String remarks;
  private Integer totalStockoutDays;
  private Long packsToShip;
  private Boolean skipped;
  private Money totalCost;
  private Integer numberOfNewPatientsAdded;
  private Integer additionalQuantityRequired;
  private Integer adjustedConsumption;
  private Integer averageConsumption;
  private Integer maximumStockQuantity;
  private Integer calculatedOrderQuantity;
  private Integer idealStockAmount;
  private Integer calculatedOrderQuantityIsa;
}
