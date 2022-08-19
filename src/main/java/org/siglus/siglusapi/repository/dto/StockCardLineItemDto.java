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
    name = "StockCard.findStockCardLineItemDto",
    query = "select sum(scli.quantity) as issuequantity, scli.occurreddate, sc.orderableid \n"
        + "from stockmanagement.stock_card_line_items scli \n"
        + "inner join stockmanagement.stock_cards sc\n"
        + "on sc.id = scli.stockcardid\n"
        + "where sc.facilityid = :facilityId \n"
        + "and scli.occurreddate >= :startDate \n"
        + "and scli.occurreddate <= :endDate \n"
        + "and scli.destinationid notnull \n"
        + "group by sc.orderableid, scli.occurreddate;",
    resultSetMapping = "StockCard.StockCardLineItemDto")

@MappedSuperclass
@SqlResultSetMapping(
    name = "StockCard.StockCardLineItemDto",
    classes = @ConstructorResult(
        targetClass = StockCardLineItemDto.class,
        columns = {
            @ColumnResult(name = "orderableId", type = UUID.class),
            @ColumnResult(name = "issuequantity", type = Long.class),
            @ColumnResult(name = "occurredDate", type = LocalDate.class),
        }
    )
)

@Data
@Builder
@AllArgsConstructor
public class StockCardLineItemDto {

  private UUID orderableId;
  private Long issueQuantity;
  private LocalDate occurredDate;
}
