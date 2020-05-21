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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.joda.money.Money;
import org.openlmis.requisition.domain.requisition.RequisitionLineItem;
import org.openlmis.requisition.utils.MoneyDeserializer;
import org.openlmis.requisition.utils.MoneySerializer;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ApproveRequisitionLineItemDto {
  private UUID id;

  @JsonSerialize(as = BasicOrderableDto.class)
  private BasicOrderableDto orderable;

  private Integer approvedQuantity;

  @JsonSerialize(using = MoneySerializer.class)
  @JsonDeserialize(using = MoneyDeserializer.class)
  private Money pricePerPack;
  @JsonSerialize(using = MoneySerializer.class)
  @JsonDeserialize(using = MoneyDeserializer.class)
  private Money totalCost;

  private Boolean skipped;

  /**
   * Creates instance with data from original requisition line item.
   */
  public ApproveRequisitionLineItemDto(RequisitionLineItem.Importer requisitionLineItem,
      OrderableDto orderable, ProgramOrderableDto programOrderable) {
    this.id = requisitionLineItem.getId();
    this.orderable = orderable;
    this.approvedQuantity = requisitionLineItem.getApprovedQuantity();
    this.pricePerPack = programOrderable.getPricePerPack();
    this.totalCost = requisitionLineItem.getTotalCost();
    this.skipped = requisitionLineItem.getSkipped();
  }

}
