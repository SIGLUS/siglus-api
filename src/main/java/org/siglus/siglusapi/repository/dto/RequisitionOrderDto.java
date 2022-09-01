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
    name = "Order.findRequisitionOrderDtos",
    query = "select r.id as requisitionId, o.id as orderId, o.status as orderStatus \n"
        + "from requisition.requisitions r \n"
        + "left join fulfillment.orders o \n"
        + "on (o.externalid = r.id) \n"
        + "where r.id in (:requisitionIds)",
    resultSetMapping = "Order.RequisitionOrderDto")

@MappedSuperclass
@SqlResultSetMapping(
    name = "Order.RequisitionOrderDto",
    classes = @ConstructorResult(
        targetClass = RequisitionOrderDto.class,
        columns = {
            @ColumnResult(name = "requisitionId", type = UUID.class),
            @ColumnResult(name = "orderId", type = UUID.class),
            @ColumnResult(name = "orderStatus", type = String.class)
        }
    )
)

@Data
@Builder
@AllArgsConstructor
public class RequisitionOrderDto {

  private UUID requisitionId;
  private UUID orderId;
  private String orderStatus;
}
