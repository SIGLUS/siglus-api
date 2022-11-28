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
import javax.persistence.ColumnResult;
import javax.persistence.ConstructorResult;
import javax.persistence.MappedSuperclass;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.SqlResultSetMappings;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@MappedSuperclass
@SqlResultSetMappings({
    @SqlResultSetMapping(
      name = "TracerDrugReport.ProductLotSohDto",
      classes =
          @ConstructorResult(
              targetClass = ProductLotSohDto.class,
              columns = {
                @ColumnResult(name = "facilitycode", type = String.class),
                @ColumnResult(name = "productcode", type = String.class),
                @ColumnResult(name = "lotid", type = String.class),
                @ColumnResult(name = "stockonhand", type = Integer.class),
                @ColumnResult(name = "occurredDate", type = LocalDate.class),
              })),
})
@NamedNativeQueries({
    @NamedNativeQuery(
      name = "TracerDrugReport.getTracerDrugSoh",
      query =
          "select fa.code as facilitycode,"
              + " od.code as productcode,"
              + " cast(sc.lotid as varchar) as lotid,"
              + " soh.stockonhand,"
              + " soh.occurreddate from "
              + "stockmanagement.calculated_stocks_on_hand soh join "
              + "    stockmanagement.stock_cards sc on sc.id = soh.stockcardid join "
              + "    referencedata.facilities fa on sc.facilityid=fa.id join "
              + "    referencedata.orderables od on od.id=sc.orderableid "
              + "    where fa.code in :facilityCodes "
              + "        and sc.orderableid in "
              + "            (select id from referencedata.orderables "
              + "                where cast(orderables.extradata as jsonb) @> cast('{\"isTracer\": true}' as jsonb)) "
              + "        and soh.occurreddate between :beginDate and :endDate",
      resultSetMapping = "TracerDrugReport.ProductLotSohDto"),
    @NamedNativeQuery(
      name = "TracerDrugReport.getLastTracerDrugSohTillDate",
      query =
          "select * from (select\n"
              + "    fa.code as facilitycode, od.code as productcode, cast(sc.lotid as varchar), "
              + "    stockonhand, occurreddate,\n"
              + "    row_number() over (partition by stockcardid order by occurreddate desc) from "
              + "    stockmanagement.calculated_stocks_on_hand csoh\n"
              + "    join stockmanagement.stock_cards sc on csoh.stockcardid = sc.id\n"
              + "    join referencedata.orderables od on od.id=sc.orderableid\n"
              + "    join referencedata.facilities fa on fa.id=sc.facilityid\n"
              + "    where fa.code in :facilityCodes and "
              + "       cast(od.extradata as jsonb) @> cast('{\"isTracer\": true}' as jsonb) and "
              + "       csoh.occurreddate < :endDate)\n"
              + "    soh where soh.row_number=1;",
      resultSetMapping = "TracerDrugReport.ProductLotSohDto")
})
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductLotSohDto {
  private String facilityCode;
  private String productCode;
  private String lotId;
  private int stockOnHand;
  private LocalDate occurredDate;
}
