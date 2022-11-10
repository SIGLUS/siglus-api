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
import java.time.ZonedDateTime;
import java.util.Date;
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
    name = "Order.findOrderDto",
    query = "select o.*, \n"
        + "f1.code as receivingfacilitycode, f1.name as receivingfacilityname, \n"
        + "f2.code as supplyingfacilitycode, f2.name as supplyingfacilityname, \n"
        + "pp.enddate as periodenddate,\n"
        + "pod.deliveredby, pod.receivedby, pod.receiveddate,\n"
        + "oei.requisitionid,\n"
        + "s.shippeddate as shippeddate, \n"
        + "p.code as programcode \n"
        + "from (select * from fulfillment.orders where id = :orderId ) o \n"
        + "left join referencedata.facilities f1 on f1.id = o.receivingfacilityid\n"
        + "left join referencedata.facilities f2 on f2.id = o.supplyingfacilityid\n"
        + "left join referencedata.processing_periods pp on (o.processingperiodid = pp.id)\n"
        + "left join fulfillment.shipments s on (s.orderid = o.id)\n"
        + "left join fulfillment.proofs_of_delivery pod on (s.id = pod.shipmentid)\n"
        + "left join siglusintegration.order_external_ids oei on (oei.id = o.externalid)\n"
        + "left join referencedata.programs p on (p.id = o.programid)",
    resultSetMapping = "Order.OrderDto")

@MappedSuperclass
@SqlResultSetMapping(
    name = "Order.OrderDto",
    classes = @ConstructorResult(
        targetClass = OrderDto.class,
        columns = {
            @ColumnResult(name = "id", type = UUID.class),
            @ColumnResult(name = "ordercode", type = String.class),
            @ColumnResult(name = "emergency", type = Boolean.class),
            @ColumnResult(name = "programid", type = UUID.class),
            @ColumnResult(name = "programcode", type = String.class),
            @ColumnResult(name = "receivingfacilityid", type = UUID.class),
            @ColumnResult(name = "supplyingfacilityid", type = UUID.class),
            @ColumnResult(name = "shippeddate", type = ZonedDateTime.class),
            @ColumnResult(name = "externalid", type = UUID.class),
            @ColumnResult(name = "requisitionid", type = UUID.class),
            @ColumnResult(name = "receivingfacilitycode", type = String.class),
            @ColumnResult(name = "receivingfacilityname", type = String.class),
            @ColumnResult(name = "supplyingfacilitycode", type = String.class),
            @ColumnResult(name = "supplyingfacilityname", type = String.class),
            @ColumnResult(name = "processingperiodid", type = UUID.class),
            @ColumnResult(name = "periodenddate", type = Date.class),
            @ColumnResult(name = "deliveredby", type = String.class),
            @ColumnResult(name = "receivedby", type = String.class),
            @ColumnResult(name = "receiveddate", type = LocalDate.class),
        }
    )
)

@Data
@Builder
@AllArgsConstructor
public class OrderDto {

  private UUID id;
  private String orderCode;
  private Boolean emergency;
  private UUID programId;
  private String programCode;
  private UUID receivingFacilityId;
  private UUID supplyingFacilityId;
  private ZonedDateTime shippedDate;
  private UUID externalId;
  private UUID requisitionId;

  private String receivingFacilityCode;
  private String receivingFacilityName;
  private String supplyingFacilityCode;
  private String supplyingFacilityName;

  private UUID processingPeriodId;
  private Date periodEndDate;

  private String podDeliveredBy;
  private String podReceivedBy;
  private LocalDate podReceivedDate;
}
