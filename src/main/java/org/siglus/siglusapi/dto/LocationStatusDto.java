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
    name = "LocationStatus.findLocationStatusByFacilityId",
    query = "select ls.locationcode, "
        + "  case "
        + "    when sum(ls.stockonhand) > 0 then 'Occupied' "
        + "    else 'Empty' "
        + "  end as locationstatus "
        + "from "
        + "( "
        + "  select distinct on(locationcode, stockcardid) "
        + "  locationcode, stockcardid, stockonhand, occurreddate "
        + "  from siglusintegration.calculated_stocks_on_hand_by_location "
        + "  where stockcardid in "
        + "  ( "
        + "    select id from stockmanagement.stock_cards "
        + "    where facilityid = :facilityId "
        + "  ) "
        + "  order by locationcode, stockcardid, processeddate desc "
        + ") ls "
        + "group by ls.locationcode "
        + "union "
        + "select distinct locationcode, 'Empty' as locationstatus "
        + "from siglusintegration.facility_locations "
        + "where facilityid = :facilityId "
        + "and "
        + "locationcode not in "
        + "( "
        + "  select distinct locationcode "
        + "  from siglusintegration.calculated_stocks_on_hand_by_location "
        + "  where stockcardid in "
        + "  ( "
        + "    select id from stockmanagement.stock_cards "
        + "    where facilityid = :facilityId "
        + "  ) "
        + ") "
        + "order by locationcode "
        + ";",
    resultSetMapping = "LocationStatus.LocationStatusDto")

@MappedSuperclass
@SqlResultSetMapping(
    name = "LocationStatus.LocationStatusDto",
    classes = @ConstructorResult(
        targetClass = LocationStatusDto.class,
        columns = {
            @ColumnResult(name = "locationcode", type = String.class),
            @ColumnResult(name = "locationstatus", type = String.class),
        }
    )
)

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class LocationStatusDto {

  private String locationCode;
  private String locationStatus;
}

