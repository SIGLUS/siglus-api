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

package org.openlmis.requisition.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.joda.money.Money;
import org.openlmis.requisition.domain.requisition.RequisitionLineItem;
import org.openlmis.requisition.domain.requisition.StockAdjustment;
import org.openlmis.requisition.dto.stockmanagement.StockAdjustmentDto;
import org.openlmis.requisition.utils.MoneyDeserializer;
import org.openlmis.requisition.utils.MoneySerializer;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseRequisitionLineItemDto extends BaseDto
    implements RequisitionLineItem.Exporter, RequisitionLineItem.Importer {

  private Integer beginningBalance;
  private Integer totalReceivedQuantity;
  private Integer totalLossesAndAdjustments;
  private Integer stockOnHand;
  private Integer requestedQuantity;
  private Integer totalConsumedQuantity;
  private String requestedQuantityExplanation;
  private String remarks;
  private Integer approvedQuantity;
  private Integer totalStockoutDays;
  private Integer total;
  private Long packsToShip;
  private Integer numberOfNewPatientsAdded;
  @JsonSerialize(using = MoneySerializer.class)
  @JsonDeserialize(using = MoneyDeserializer.class)
  private Money totalCost;
  private Boolean skipped;
  private Integer adjustedConsumption;
  private List<Integer> previousAdjustedConsumptions;
  private Integer averageConsumption;
  private Integer maximumStockQuantity;
  private Integer calculatedOrderQuantity;
  private Integer idealStockAmount;
  private Integer calculatedOrderQuantityIsa;
  private Integer additionalQuantityRequired;

  @JsonProperty
  private List<StockAdjustmentDto> stockAdjustments = new ArrayList<>();

  @JsonIgnore
  @Override
  public List<StockAdjustment.Importer> getStockAdjustments() {
    return stockAdjustments == null
        ? Collections.emptyList()
        : new ArrayList<>(stockAdjustments);
  }

  @Override
  public void addStockAdjustment(StockAdjustment.Exporter stockAdjustmentExporter) {
    stockAdjustments.add((StockAdjustmentDto) stockAdjustmentExporter);
  }

  public Optional<Supplier<StockAdjustment.Exporter>> provideStockAdjustmentExporter() {
    return Optional.of(StockAdjustmentDto::new);
  }

  @Override
  public boolean supportsPreviousAdjustedConsumptions() {
    return true;
  }

}
