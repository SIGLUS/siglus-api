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
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.SqlResultSetMapping;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@NamedNativeQueries({
    @NamedNativeQuery(
        name = "RequisitionGeographicZone.findAllZones",
        query = " select gz.name         as districtName,\n"
            + "                 gz.code         as districtCode,\n"
            + "                 gz_prov.name    as provinceName,\n"
            + "                 gz_prov.code    as provinceCode,\n"
            + "                 gl.name         as districtLevel,\n"
            + "                 gl.levelnumber  as districtLevelCode,\n"
            + "                 gl_prov.name    as provinceLevel,\n"
            + "                 gl_prov.levelnumber as provinceLevelCode,\n"
            + "                 vfs.facilitycode   as facilityCode,\n"
            + "                 vfs.districtfacilitycode  as districtFacilityCode,\n"
            + "                 vfs.provincefacilitycode  as provinceFacilityCode\n"
            + "          from dashboard.vw_facility_supplier vfs\n"
            + "                   left join referencedata.facilities f\n"
            + "                             on vfs.facilitycode = f.code\n"
            + "                   left join referencedata.geographic_zones gz\n"
            + "                             ON gz.id = f.geographiczoneid\n"
            + "                   left join referencedata.geographic_levels gl\n"
            + "                             on gz.levelid = gl.id\n"
            + "                   left join referencedata.geographic_zones gz_prov\n"
            + "                             on gz_prov.id = gz.parentid\n"
            + "                   left join referencedata.geographic_levels gl_prov\n"
            + "                             on gz_prov.levelid = gl_prov.id ",
        resultSetMapping = "RequisitionGeographicZone.RequisitionGeographicZonesDto"),
})

@MappedSuperclass
@SqlResultSetMapping(
    name = "RequisitionGeographicZone.RequisitionGeographicZonesDto",
    classes = @ConstructorResult(
        targetClass = RequisitionGeographicZonesDto.class,
        columns = {
            @ColumnResult(name = "districtName", type = String.class),
            @ColumnResult(name = "districtCode", type = String.class),
            @ColumnResult(name = "provinceName", type = String.class),
            @ColumnResult(name = "provinceCode", type = String.class),
            @ColumnResult(name = "districtLevel", type = String.class),
            @ColumnResult(name = "districtLevelCode", type = Integer.class),
            @ColumnResult(name = "provinceLevel", type = String.class),
            @ColumnResult(name = "provinceLevelCode", type = Integer.class),
            @ColumnResult(name = "facilityCode", type = String.class),
            @ColumnResult(name = "districtFacilityCode", type = String.class),
            @ColumnResult(name = "provinceFacilityCode", type = String.class)
        }
    )
)

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class RequisitionGeographicZonesDto {

  private String districtName;
  private String districtCode;
  private String provinceName;
  private String provinceCode;
  private String districtLevel;
  private Integer districtLevelCode;
  private String provinceLevel;
  private Integer provinceLevelCode;
  private String facilityCode;
  private String districtFacilityCode;
  private String provinceFacilityCode;


  public AssociatedGeographicZoneDto getDistrictGeographicZoneDtoFrom(
      RequisitionGeographicZonesDto requisitionGeographicZonesDto) {
    return AssociatedGeographicZoneDto
        .builder()
        .name(requisitionGeographicZonesDto.districtName)
        .code(requisitionGeographicZonesDto.districtCode)
        .parentCode(requisitionGeographicZonesDto.provinceCode)
        .level(requisitionGeographicZonesDto.districtLevel)
        .levelCode(requisitionGeographicZonesDto.districtLevelCode)
        .build();
  }

  public AssociatedGeographicZoneDto getProvinceGeographicZoneDtoFrom(
      RequisitionGeographicZonesDto requisitionGeographicZonesDto) {
    return AssociatedGeographicZoneDto
        .builder()
        .name(requisitionGeographicZonesDto.provinceName)
        .code(requisitionGeographicZonesDto.provinceCode)
        .level(requisitionGeographicZonesDto.provinceLevel)
        .levelCode(requisitionGeographicZonesDto.provinceLevelCode)
        .build();
  }

}
