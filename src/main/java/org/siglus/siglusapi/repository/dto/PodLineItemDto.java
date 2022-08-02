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

import java.time.LocalDate;
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
    name = "PodLineItem.findLineItemDtos",
    query = "select podli.quantityaccepted, podli.orderableid, podli.lotid,\n"
        + "o.code as productcode, o.fullproductname as productname, \n"
        + "l.lotcode, l.expirationdate,\n"
        + "oli.orderedquantity\n"
        + "from fulfillment.proof_of_delivery_line_items podli\n"
        + "left join referencedata.orderables o "
        + "on (o.id = podli.orderableid) and podli.proofofdeliveryid = :podId \n"
        + "left join referencedata.lots l "
        + "on (l.id = podli.lotid)\n"
        + "left join fulfillment.order_line_items oli "
        + "on (oli.orderableid = podli.orderableid) and oli.orderid = :orderId \n",
    resultSetMapping = "PodLineItem.PodLineItemDto")

@MappedSuperclass
@SqlResultSetMapping(
    name = "PodLineItem.PodLineItemDto",
    classes = @ConstructorResult(
        targetClass = PodLineItemDto.class,
        columns = {
            @ColumnResult(name = "productcode", type = String.class),
            @ColumnResult(name = "productname", type = String.class),
            @ColumnResult(name = "lotid", type = UUID.class),
            @ColumnResult(name = "lotcode", type = String.class),
            @ColumnResult(name = "expirationdate", type = LocalDate.class),
            @ColumnResult(name = "orderedquantity", type = Long.class),
            @ColumnResult(name = "quantityaccepted", type = Long.class)
        }
    )
)

@Data
@Builder
@AllArgsConstructor
public class PodLineItemDto {

  private String productCode;
  private String productName;
  private UUID lotId;
  private String lotCode;
  private LocalDate lotExpirationDate;

  //  private Long requestedQuantity;  // TODO ?
  private Long orderedQuantity;
  private Long receivedQuantity;
}
