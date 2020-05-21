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

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.openlmis.requisition.domain.requisition.RequisitionLineItem;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderLineItemDto {
  private UUID id;
  private OrderableDto orderable;
  private Long orderedQuantity;

  /**
   * Static factory method for constructing new OrderLineItem based on RequisitionLineItem.
   *
   * @param lineItem RequisitionLineItem to create instance from.
   */
  public static OrderLineItemDto newOrderLineItem(RequisitionLineItem lineItem,
                                                  OrderableDto productDto) {
    OrderLineItemDto orderLineItem = new OrderLineItemDto();
    orderLineItem.setOrderable(productDto);
    orderLineItem.setOrderedQuantity(lineItem.getPacksToShip());

    return orderLineItem;
  }
}
