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
            + "  sc.lotid ,\n"
            + "  fl.locationcode ,\n"
            + "  fl.area, \n"
            + "  csohl.stockonhand\n"
            + "from\n"
            + "  (\n"
            + "  select\n"
            + "    *,\n"
            + "    row_number() over (partition by locationcode,area,stockcardid\n"
            + "  order by\n"
            + "    occurreddate desc)\n"
            + "  from\n"
            + "    siglusintegration.calculated_stocks_on_hand_by_location) csohl\n"
            + "left join stockmanagement.stock_cards sc on\n"
            + "  sc.id = csohl.stockcardid\n"
            + "left join siglusintegration.facility_locations fl on\n"
            + "  csohl.locationcode = fl.locationcode  and csohl.area = fl.area and sc.facilityid = fl.facilityid \n"
            + "where\n"
            + "  csohl.row_number = 1\n"
            + "  and csohl.stockonhand > 0\n"
            + "  and sc.lotid in :lotIds",
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
                    @ColumnResult(name = "lotid",
                        type = UUID.class),
                    @ColumnResult(name = "locationcode",
                        type = String.class),
                    @ColumnResult(name = "stockonhand",
                        type = Integer.class),
                    @ColumnResult(name = "area",
                        type = String.class)

                }
            )
        }
    )
})
@Data
@Builder
@AllArgsConstructor
public class LotLocationSohDto {

  private UUID lotId;

  private String locationCode;

  private String area;

  private Integer stockOnHand;
}
