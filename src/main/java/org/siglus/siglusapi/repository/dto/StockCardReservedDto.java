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

import java.math.BigInteger;
import java.util.UUID;
import javax.persistence.ColumnResult;
import javax.persistence.ConstructorResult;
import javax.persistence.MappedSuperclass;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.SqlResultSetMapping;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@NamedNativeQueries({
        @NamedNativeQuery(
                name = "StockCard.queryStockCardReservedDto",
                query = "SELECT orderableid AS orderableId, orderableversionnumber AS orderableVersionNumber, "
                        + "  lotid AS lotId, SUM(quantityshipped) AS reserved "
                        + "FROM fulfillment.shipment_draft_line_items sdli "
                        + "WHERE sdli.shipmentdraftid IN ( "
                        + "       SELECT sd.id "
                        + "       FROM fulfillment.shipment_drafts sd "
                        + "       LEFT JOIN fulfillment.orders o on o.id = sd.orderid "
                        + "       WHERE o.facilityid = :facilityId and o.programid = :programId) "
                        + "GROUP BY orderableid, orderableversionnumber, lotid;",
                resultSetMapping = "StockCard.StockCardReservedDto"),

        @NamedNativeQuery(
                name = "StockCard.queryStockCardReservedExcludeDto",
                query = "SELECT orderableid AS orderableId, orderableversionnumber AS orderableVersionNumber, "
                        + "  lotid AS lotId, SUM(quantityshipped) AS reserved "
                        + "FROM fulfillment.shipment_draft_line_items sdli "
                        + "WHERE sdli.shipmentdraftid IN ( "
                        + "       SELECT sd.id "
                        + "       FROM fulfillment.shipment_drafts sd "
                        + "       LEFT JOIN fulfillment.orders o on o.id = sd.orderid "
                        + "       WHERE o.facilityid = :facilityId and o.programid = :programId "
                        + "             and sd.id != :shipmentDraftId) "
                        + "GROUP BY orderableid, orderableversionnumber, lotid;",
                resultSetMapping = "StockCard.StockCardReservedDto")
})

@MappedSuperclass
@SqlResultSetMapping(
        name = "StockCard.StockCardReservedDto",
        classes = @ConstructorResult(
                targetClass = StockCardReservedDto.class,
                columns = {
                        @ColumnResult(name = "orderableId", type = UUID.class),
                        @ColumnResult(name = "orderableVersionNumber", type = Integer.class),
                        @ColumnResult(name = "lotId", type = UUID.class),
                        @ColumnResult(name = "reserved", type = BigInteger.class),
                }
        )
)

@Data
@Builder
@AllArgsConstructor
public class StockCardReservedDto {
  private UUID orderableId;
  private Integer orderableVersionNumber;
  private UUID lotId;
  private BigInteger reserved;
}
