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

package org.siglus.siglusapi.repository.dto;

import java.util.UUID;
import javax.persistence.ColumnResult;
import javax.persistence.ConstructorResult;
import javax.persistence.MappedSuperclass;
import javax.persistence.NamedNativeQuery;
import javax.persistence.SqlResultSetMapping;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@NamedNativeQuery(
    name = "Order.findOrderSuggestedQuantityDto",
    query = "select oli.orderableid, olie.suggestedquantity\n"
        + "from fulfillment.order_line_items oli \n"
        + "left join siglusintegration.order_line_item_extension olie \n"
        + "on (oli.id = olie.orderlineitemid)\n"
        + "where oli.id in (:orderLineItemIds);",
    resultSetMapping = "Order.OrderSuggestedQuantityDto")

@MappedSuperclass
@SqlResultSetMapping(
    name = "Order.OrderSuggestedQuantityDto",
    classes = @ConstructorResult(
        targetClass = OrderSuggestedQuantityDto.class,
        columns = {
            @ColumnResult(name = "orderableId", type = UUID.class),
            @ColumnResult(name = "suggestedQuantity", type = double.class),
        }
    )
)

@Data
@Builder
@AllArgsConstructor
public class OrderSuggestedQuantityDto {

  private UUID orderableId;
  private double suggestedQuantity;
}
