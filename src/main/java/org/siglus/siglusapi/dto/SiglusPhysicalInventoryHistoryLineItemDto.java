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

package org.siglus.siglusapi.dto;

import java.time.LocalDate;
import javax.persistence.ColumnResult;
import javax.persistence.ConstructorResult;
import javax.persistence.MappedSuperclass;
import javax.persistence.NamedNativeQuery;
import javax.persistence.SqlResultSetMapping;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NamedNativeQuery(
    name = "PhysicalInventoryHistoryLineItem.queryPhysicalInventoryHistoryLineItem",
    query = "select o.code as productcode, o.fullproductname as productname, "
        + "l.lotcode, l.expirationdate as expirydate, soh.stockonhand, "
        + "pili.quantity as currentstock, sclir.name as reasons, pilia.quantity as reasonQuantity, "
        + "pilie.reasonfreetext as comments "
        + "from stockmanagement.physical_inventory_line_items pili "
        + "left join "
        + "( "
        + "  select distinct on(id) id, fullproductname, code, versionnumber "
        + "  from referencedata.orderables "
        + "  order by id, versionnumber desc "
        + ") o on pili.orderableid = o.id "
        + "left join referencedata.lots l on pili.lotid = l.id "
        + "left join "
        + "( "
        + "  select pi.stockeventid, scli.stockcardid, sc.lotid from stockmanagement.physical_inventories pi "
        + "  left join stockmanagement.stock_card_line_items scli on pi.stockeventid = scli.origineventid "
        + "  left join stockmanagement.stock_cards sc on scli.stockcardid = sc.id "
        + "  where pi.id = :physicalInventoryId "
        + ") tsc on tsc.lotid = l.id "
        + "left join "
        + "( "
        + "  select distinct on(tsoh.stockcardid)tsoh.stockcardid, tsoh.stockonhand, tsoh.processeddate "
        + "  from "
        + "  ( "
        + "    select * from stockmanagement.calculated_stocks_on_hand "
        + "    where stockcardid in "
        + "    ( "
        + "      select scli.stockcardid from stockmanagement.physical_inventories pi "
        + "      left join stockmanagement.stock_card_line_items scli on pi.stockeventid = scli.origineventid "
        + "      where pi.id = :physicalInventoryId "
        + "    ) "
        + "  ) tsoh "
        + "  order by tsoh.stockcardid, tsoh.processeddate desc "
        + ") soh on tsc.stockcardid = soh.stockcardid "
        + "left join stockmanagement.physical_inventory_line_item_adjustments pilia "
        + "  on pili.id = pilia.physicalinventorylineitemid "
        + "left join stockmanagement.stock_card_line_item_reasons sclir on pilia.reasonid = sclir.id "
        + "left join siglusintegration.physical_inventory_line_items_extension pilie "
        + "  on pili.id = pilie.physicalinventorylineitemid "
        + "where pili.physicalinventoryid = :physicalInventoryId "
        + "order by productcode, lotcode",
    resultSetMapping = "PhysicalInventoryHistoryLineItem.SiglusPhysicalInventoryHistoryLineItemDto")

@MappedSuperclass
@SqlResultSetMapping(
    name = "PhysicalInventoryHistoryLineItem.SiglusPhysicalInventoryHistoryLineItemDto",
    classes = @ConstructorResult(
        targetClass = SiglusPhysicalInventoryHistoryLineItemDto.class,
        columns = {
            @ColumnResult(name = "productCode", type = String.class),
            @ColumnResult(name = "productName", type = String.class),
            @ColumnResult(name = "lotCode", type = String.class),
            @ColumnResult(name = "expiryDate", type = LocalDate.class),
            @ColumnResult(name = "stockOnHand", type = Integer.class),
            @ColumnResult(name = "currentStock", type = Integer.class),
            @ColumnResult(name = "reasons", type = String.class),
            @ColumnResult(name = "reasonQuantity", type = Integer.class),
            @ColumnResult(name = "comments", type = String.class),
        }
    )
)

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class SiglusPhysicalInventoryHistoryLineItemDto {

  private String productCode;
  private String productName;
  private String lotCode;
  private LocalDate expiryDate;
  private Integer stockOnHand;
  private Integer currentStock;
  private String reasons;
  private Integer reasonQuantity;
  private String comments;
}
