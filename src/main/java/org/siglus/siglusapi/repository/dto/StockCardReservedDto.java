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
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.SqlResultSetMapping;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@NamedNativeQueries({
    @NamedNativeQuery(
        name = "StockCard.queryStockCardReservedDto",
        query = "SELECT sdli.orderableid AS orderableId, sdli.orderableversionnumber AS orderableVersionNumber, "
                + "  sdli.lotid AS lotId, COALESCE(sdlie.quantityshipped, sdli.quantityshipped) AS reserved, "
                + "  sdlie.area AS area, sdlie.locationcode AS locationCode "
                + "FROM fulfillment.shipment_draft_line_items sdli "
                + "LEFT JOIN siglusintegration.shipment_draft_line_items_extension sdlie "
                + "       ON sdli.id = sdlie.shipmentdraftlineitemid "
                + "WHERE sdli.shipmentdraftid IN ( "
                + "       SELECT sd.id "
                + "       FROM fulfillment.shipment_drafts sd "
                + "       LEFT JOIN fulfillment.orders o on o.id = sd.orderid "
                + "       WHERE o.supplyingfacilityid = :facilityId AND o.status != 'CLOSED') "
                + ";",
        resultSetMapping = "StockCard.StockCardReservedDto"),

    @NamedNativeQuery(
        name = "StockCard.queryStockCardReservedExcludeDto",
        query = "SELECT sdli.orderableid AS orderableId, sdli.orderableversionnumber AS orderableVersionNumber, "
                + "  sdli.lotid AS lotId, COALESCE(sdlie.quantityshipped, sdli.quantityshipped) AS reserved, "
                + "  sdlie.area AS area, sdlie.locationcode AS locationCode "
                + "FROM fulfillment.shipment_draft_line_items sdli "
                + "LEFT JOIN siglusintegration.shipment_draft_line_items_extension sdlie "
                + "       ON sdli.id = sdlie.shipmentdraftlineitemid "
                + "WHERE sdli.shipmentdraftid IN ( "
                + "       SELECT sd.id "
                + "       FROM fulfillment.shipment_drafts sd "
                + "       LEFT JOIN fulfillment.orders o on o.id = sd.orderid "
                + "       WHERE o.supplyingfacilityid = :facilityId and sd.id != :shipmentDraftId "
                + "             AND o.status != 'CLOSED') "
                + ";",
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
                        @ColumnResult(name = "reserved", type = Integer.class),
                        @ColumnResult(name = "area", type = String.class),
                        @ColumnResult(name = "locationCode", type = String.class),
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
  private Integer reserved;
  private String area;
  private String locationCode;
}
