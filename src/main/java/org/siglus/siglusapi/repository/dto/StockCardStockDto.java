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
import lombok.NoArgsConstructor;

@NamedNativeQueries({
    @NamedNativeQuery(
        name = "StockCard.queryStockCardStockDtoById",
        query = "SELECT DISTINCT ON (csoh.stockcardid) csoh.stockcardid AS stockCardId, "
                + "  csoh.stockonhand AS stockOnHand, NULL as area, NULL as locationCode, "
                + "  NULL as orderableId, NULL as lotId "
                + "FROM stockmanagement.calculated_stocks_on_hand csoh "
                + "where stockcardid in (:stockCardIds) "
                + "ORDER BY csoh.stockcardid, csoh.occurreddate DESC "
                + ";",
        resultSetMapping = "StockCard.StockCardStockDto"),
})

@MappedSuperclass
@SqlResultSetMapping(
        name = "StockCard.StockCardStockDto",
        classes = @ConstructorResult(
                targetClass = StockCardStockDto.class,
                columns = {
                        @ColumnResult(name = "stockCardId", type = UUID.class),
                        @ColumnResult(name = "stockOnHand", type = Integer.class),
                        @ColumnResult(name = "area", type = String.class),
                        @ColumnResult(name = "locationCode", type = String.class),
                        @ColumnResult(name = "orderableId", type = UUID.class),
                        @ColumnResult(name = "lotId", type = UUID.class),
                }
        )
)

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StockCardStockDto {
  private UUID stockCardId;
  private Integer stockOnHand;
  private String area;
  private String locationCode;
  private UUID orderableId;
  private UUID lotId;
}
