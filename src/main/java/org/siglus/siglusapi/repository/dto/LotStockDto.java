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
        name = "StockCard.queryExpiredLotStockDtoByFacility",
        query = "select sc_1.programid, p.\"name\", p.code, sc_1.orderableid, o.fullproductname, "
                + " o.code AS productCode, sc_1.lotid, el.lotcode, cs_1.stockonhand, el.expirationdate "
                + "from ( "
                + "  select sc.lotid, sc.id, sc.orderableid, sc.programid from stockmanagement.stock_cards sc "
                + "  where sc.facilityid = :facilityId and sc.lotid is not null ) sc_1 "
                + "join ( "
                + "  select l.id, l.lotcode, l.expirationdate from referencedata.lots l "
                + "  where l.expirationdate < current_date ) el on el.id = sc_1.lotid "
                + "left join referencedata.orderables o on o.id = sc_1.orderableid "
                + "left join referencedata.programs p on p.id  = sc_1.programid "
                + "left join ( "
                + "  SELECT cs.stockcardid, cs.stockonhand, cs.row_number "
                + "  FROM ( SELECT csoh.stockcardid, csoh.stockonhand, csoh.occurreddate, "
                + "           row_number() OVER (PARTITION BY csoh.stockcardid ORDER BY csoh.occurreddate DESC) "
                + "              AS row_number "
                + "         FROM stockmanagement.calculated_stocks_on_hand csoh"
                + "         where csoh.stockcardid in ("
                + "             select sc1.id from stockmanagement.stock_cards sc1  "
                + "             where sc1.facilityid = :facilityId and sc1.lotid is not null ) ) cs "
                + "  WHERE cs.row_number = 1) cs_1 on sc_1.id = cs_1.stockcardid "
                + ";",
        resultSetMapping = "StockCard.LotStockDto")

@MappedSuperclass
@SqlResultSetMapping(
        name = "StockCard.LotStockDto",
        classes = @ConstructorResult(
                targetClass = LotStockDto.class,
                columns = {
                        @ColumnResult(name = "programid", type = UUID.class),
                        @ColumnResult(name = "name", type = String.class),
                        @ColumnResult(name = "code", type = String.class),
                        @ColumnResult(name = "orderableid", type = UUID.class),
                        @ColumnResult(name = "fullproductname", type = String.class),
                        @ColumnResult(name = "productCode", type = String.class),
                        @ColumnResult(name = "lotid", type = UUID.class),
                        @ColumnResult(name = "lotcode", type = String.class),
                        @ColumnResult(name = "stockonhand", type = Integer.class),
                        @ColumnResult(name = "expirationdate", type = LocalDate.class),
                }
        )
)

@Data
@Builder
@AllArgsConstructor
public class LotStockDto {
  private UUID programId;
  private String programName;
  private String programCode;
  private UUID orderableId;
  private String fullProductName;
  private String productCode;
  private UUID lotId;
  private String lotCode;
  private Integer soh;
  private LocalDate expirationDate;
}
