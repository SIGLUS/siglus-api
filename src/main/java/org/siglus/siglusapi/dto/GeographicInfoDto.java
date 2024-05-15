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
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NamedNativeQuery(
    name = "GeographicInfo.getGeographicInfo",
    query = "select gzp.id as provinceid, gzp.name as provincename, "
        + "gz.id as districtid, gz.name as districtname "
        + "from referencedata.geographic_zones gz "
        + "left join referencedata.geographic_zones gzp on gz.parentid = gzp.id "
        + "left join referencedata.geographic_levels gl on gzp.levelid = gl.id "
        + "where gl.code = 'province' "
        + "order by provincename",
    resultSetMapping = "GeographicInfo.GeographicInfoDto")

@MappedSuperclass
@SqlResultSetMapping(
    name = "GeographicInfo.GeographicInfoDto",
    classes = @ConstructorResult(
        targetClass = GeographicInfoDto.class,
        columns = {
            @ColumnResult(name = "provinceid", type = UUID.class),
            @ColumnResult(name = "provincename", type = String.class),
            @ColumnResult(name = "districtid", type = UUID.class),
            @ColumnResult(name = "districtname", type = String.class),
        }
    )
)

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class GeographicInfoDto {

  @NotNull
  private UUID provinceId;
  private String provinceName;
  @NotNull
  private UUID districtId;
  private String districtName;
}

