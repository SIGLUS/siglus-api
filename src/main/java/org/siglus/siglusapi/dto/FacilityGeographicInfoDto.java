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
import javax.persistence.NamedNativeQuery;
import javax.persistence.SqlResultSetMapping;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NamedNativeQuery(
    name = "FacilityGeographicInfo.getAllFacilityGeographicInfo",
    query = "select f.code as facilitycode, gz.id as districtid, "
        + "case "
        + "  when gzp.id is null then gz.id "
        + "  else gzp.id "
        + "end as provinceid "
        + "from referencedata.facilities f "
        + "left join referencedata.geographic_zones gz on f.geographiczoneid = gz.id "
        + "left join referencedata.geographic_zones gzp on gz.parentid = gzp.id "
        + "left join referencedata.geographic_levels gl on gzp.levelid = gl.id "
        + "order by provinceid, districtid",
    resultSetMapping = "FacilityGeographicInfo.FacilityGeographicInfoDto")

@MappedSuperclass
@SqlResultSetMapping(
    name = "FacilityGeographicInfo.FacilityGeographicInfoDto",
    classes = @ConstructorResult(
        targetClass = FacilityGeographicInfoDto.class,
        columns = {
            @ColumnResult(name = "facilitycode", type = String.class),
            @ColumnResult(name = "districtid", type = UUID.class),
            @ColumnResult(name = "provinceid", type = UUID.class)
        }
    )
)

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class FacilityGeographicInfoDto {

  private String facilityCode;
  private UUID districtId;
  private UUID provinceId;
}

