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
    query = "select o.id, o.emergency, o.receivingfacilityid, o.processingperiodid, "
        + "f.code as receivingfacilitycode, p.enddate as periodenddate "
        + "from fulfillment.orders o "
        + "left join referencedata.processing_periods p on (o.processingperiodid = p.id) and o.id = :id "
        + "left join referencedata.facilities f on (o.supplyingfacilityid = f.id)",
    resultSetMapping = "Order.OrderDto")

@MappedSuperclass
@SqlResultSetMapping(
    name = "Order.OrderDto",
    classes = @ConstructorResult(
        targetClass = OrderDto.class,
        columns = {
            @ColumnResult(name = "id", type = UUID.class),
            @ColumnResult(name = "emergency", type = Boolean.class),
            @ColumnResult(name = "receivingfacilityid", type = UUID.class),
            @ColumnResult(name = "receivingfacilitycode", type = String.class),
            @ColumnResult(name = "processingperiodid", type = UUID.class),
            @ColumnResult(name = "periodenddate", type = Date.class),
        }
    )
)

@Data
@Builder
@AllArgsConstructor
public class OrderDto {

  private UUID id;
  private Boolean emergency;
  private UUID receivingFacilityId;
  private String receivingFacilityCode;

  private UUID processingPeriodId;
  private Date periodEndDate;
}
