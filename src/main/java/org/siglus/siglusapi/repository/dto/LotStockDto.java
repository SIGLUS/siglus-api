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
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.SqlResultSetMapping;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@NamedNativeQueries({
    @NamedNativeQuery(
        name = "StockCard.queryExpiredLotStockDtoByFacility",
        query = "select sc.id as stockCardId, sc.programid,p.\"name\", p.code, sc.orderableid, "
            + "  o.fullproductname as productName, o.code as productCode, sc.lotid, l.lotcode, "
            + "  soh.stockonhand, l.expirationdate, null as area, null as locationcode "
            + "from stockmanagement.stock_cards sc "
            + "left join referencedata.programs p on sc.programid = p.id "
            + "left join ( "
            + "  select distinct on(id) id, fullproductname, code, versionnumber "
            + "  from referencedata.orderables "
            + "  order by id, versionnumber desc "
            + ") o on sc.orderableid = o.id "
            + "left join referencedata.lots l on sc.lotid = l.id "
            + "left join (select distinct on(tsoh.stockcardid) "
            + "            tsoh.id, tsoh.stockcardid, tsoh.stockonhand, tsoh.processeddate "
            + "          from (select * from stockmanagement.calculated_stocks_on_hand "
            + "          where stockcardid in ( select distinct sc.id from stockmanagement.stock_cards sc  "
            + "                                   left join referencedata.lots l on sc.lotid = l.id "
            + "                            where sc.facilityid = :facilityId and l.expirationdate < current_date) "
            + "           ) tsoh order by tsoh.stockcardid, tsoh.occurreddate desc "
            + "       ) soh on sc.id = soh.stockcardid "
            + "where sc.lotid is not null and sc.facilityid = :facilityId and l.expirationdate < current_date "
            + "      and soh.stockonhand > 0 "
            + ";",
        resultSetMapping = "StockCard.LotStockDto"),

    @NamedNativeQuery(
        name = "StockCard.queryExpiredLotStockDtoByFacilityWithLocation",
        query = "select sc.id as stockCardId, sc.programid,p.\"name\", p.code, sc.orderableid, "
            + " o.fullproductname as productName, o.code as productCode, sc.lotid, l.lotcode, "
            + " soh.stockonhand, l.expirationdate, soh.area, soh.locationcode "
            + "from stockmanagement.stock_cards sc "
            + "left join referencedata.programs p on sc.programid = p.id "
            + "left join ( "
            + "  select distinct on(id) id, fullproductname, code, versionnumber "
            + "  from referencedata.orderables "
            + "  order by id, versionnumber desc "
            + ") o on sc.orderableid = o.id "
            + "left join referencedata.lots l on sc.lotid = l.id "
            + "left join (select distinct on(tsoh.stockcardid, tsoh.locationcode) tsoh.id, tsoh.stockcardid, "
            + "             tsoh.stockonhand, tsoh.processeddate, tsoh.area, tsoh.locationcode "
            + "           from (select * from siglusintegration.calculated_stocks_on_hand_by_location "
            + "                 where stockcardid in ( select distinct sc.id from stockmanagement.stock_cards sc  "
            + "                                   left join referencedata.lots l on sc.lotid = l.id "
            + "                               where sc.facilityid = :facilityId and l.expirationdate < current_date ) "
            + "           ) tsoh order by tsoh.stockcardid, tsoh.locationcode, tsoh.occurreddate desc "
            + "        ) soh on sc.id = soh.stockcardid "
            + "where sc.lotid is not null and sc.facilityid = :facilityId and l.expirationdate < current_date "
            + "      and soh.stockonhand > 0 "
            + ";",
        resultSetMapping = "StockCard.LotStockDto"),
})

@MappedSuperclass
@SqlResultSetMapping(
        name = "StockCard.LotStockDto",
        classes = @ConstructorResult(
                targetClass = LotStockDto.class,
                columns = {
                    @ColumnResult(name = "stockCardId", type = UUID.class),
                    @ColumnResult(name = "programid", type = UUID.class),
                    @ColumnResult(name = "name", type = String.class),
                    @ColumnResult(name = "code", type = String.class),
                    @ColumnResult(name = "orderableid", type = UUID.class),
                    @ColumnResult(name = "productName", type = String.class),
                    @ColumnResult(name = "productCode", type = String.class),
                    @ColumnResult(name = "lotid", type = UUID.class),
                    @ColumnResult(name = "lotcode", type = String.class),
                    @ColumnResult(name = "stockonhand", type = Integer.class),
                    @ColumnResult(name = "expirationdate", type = LocalDate.class),
                    @ColumnResult(name = "area", type = String.class),
                    @ColumnResult(name = "locationcode", type = String.class),
                }
        )
)

@Data
@Builder
@AllArgsConstructor
public class LotStockDto {
  private UUID stockCardId;
  private UUID programId;
  private String programName;
  private String programCode;
  private UUID orderableId;
  private String productName;
  private String productCode;
  private UUID lotId;
  private String lotCode;
  private Integer soh;
  private LocalDate expirationDate;
  private String area;
  private String locationCode;
}
