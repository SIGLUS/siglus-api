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
import java.util.UUID;
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

@NamedNativeQueries({
    @NamedNativeQuery(name = "LotLocationSoh.findLocationSoh",
        query = "select\n"
            + "  concat(sc.orderableid,sc.lotid) as identify,\n"
            + "  sc.orderableid ,\n"
            + "  sc.lotid ,\n"
            + "  fl.locationcode ,\n"
            + "  fl.area,\n"
            + "  csohl.stockonhand,\n"
            + "  csohl.occurreddate as lastupdate\n"
            + "from\n"
            + "  (\n"
            + "  select\n"
            + "    *,\n"
            + "    row_number() over (partition by locationcode,\n"
            + "    area,\n"
            + "    stockcardid\n"
            + "  order by\n"
            + "    occurreddate desc)\n"
            + "  from\n"
            + "    siglusintegration.calculated_stocks_on_hand_by_location) csohl\n"
            + "left join stockmanagement.stock_cards sc on\n"
            + "  sc.id = csohl.stockcardid\n"
            + "join siglusintegration.facility_locations fl on\n"
            + "  csohl.locationcode = fl.locationcode\n"
            + "  and sc.facilityid = fl.facilityid\n"
            + "where\n"
            + "  csohl.row_number = 1\n"
            + "  and csohl.stockonhand > 0\n"
            + "  and (sc.lotid in :lotIds\n"
            + "    or sc.orderableId in (\n"
            + "    select\n"
            + "      id\n"
            + "    from\n"
            + "      referencedata.orderables o\n"
            + "    where\n"
            + "      code in ('26A01', '26B01', '26A02', '26B02')\n"
            + "    group by\n"
            + "      id))",
        resultSetMapping = "LotLocationSoh.LotLocationSohDto")
})
@MappedSuperclass
@SqlResultSetMappings({
    @SqlResultSetMapping(
        name = "LotLocationSoh.LotLocationSohDto",
        classes = {
            @ConstructorResult(
                targetClass = LotLocationSohDto.class,
                columns = {
                    @ColumnResult(name = "identify", type = String.class),
                    @ColumnResult(name = "orderableId", type = UUID.class),
                    @ColumnResult(name = "lotid", type = UUID.class),
                    @ColumnResult(name = "locationcode", type = String.class),
                    @ColumnResult(name = "area", type = String.class),
                    @ColumnResult(name = "stockonhand", type = Integer.class),
                    @ColumnResult(name = "lastUpdate", type = LocalDate.class)


                }
            )
        }
    )
})
@Data
@Builder
@AllArgsConstructor
public class LotLocationSohDto {
  private String identify;

  private UUID orderableId;

  private UUID lotId;

  private String locationCode;

  private String area;

  private Integer stockOnHand;

  private LocalDate lastUpdate;
}
