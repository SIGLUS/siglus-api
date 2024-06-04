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
    name = "GeographicInfo.getGeographicProvinceDistrictInfo",
    query = "select gzp.code as provincecode, gzp.name as provincename, "
        + "gz.code as districtcode, gz.name as districtname "
        + "from referencedata.facilities f "
        + "left join referencedata.geographic_zones gz on f.geographiczoneid = gz.id "
        + "left join referencedata.geographic_zones gzp on gz.parentid = gzp.id "
        + "where f.code = :facilityCode",
    resultSetMapping = "GeographicInfo.GeographicProvinceDistrictDto")

@MappedSuperclass
@SqlResultSetMapping(
    name = "GeographicInfo.GeographicProvinceDistrictDto",
    classes = @ConstructorResult(
        targetClass = GeographicProvinceDistrictDto.class,
        columns = {
            @ColumnResult(name = "provincecode", type = String.class),
            @ColumnResult(name = "provincename", type = String.class),
            @ColumnResult(name = "districtcode", type = String.class),
            @ColumnResult(name = "districtname", type = String.class),
        }
    )
)

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class GeographicProvinceDistrictDto {

  private String provinceCode;
  private String provinceName;
  private String districtCode;
  private String districtName;
}
