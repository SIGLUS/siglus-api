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

package org.openlmis.requisition.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class MinimalFacilityDto extends BaseDto {

  protected String code;
  protected String name;
  protected Boolean active;
  protected GeographicZoneDto geographicZone;
  protected FacilityTypeDto type;

  public interface Exporter {
    void setId(UUID id);

    void setCode(String code);

    void setName(String name);

    void setActive(Boolean active);

    void setGeographicZone(GeographicZoneDto geographicZone);

    void setType(FacilityTypeDto type);
  }

  /**
   * Export this object to the specified exporter (DTO).
   *
   * @param exporter exporter to export to
   */
  public void export(Exporter exporter) {
    exporter.setId(id);
    exporter.setCode(code);
    exporter.setName(name);
    exporter.setActive(active);
    exporter.setGeographicZone(geographicZone);
    exporter.setType(type);
  }

  /**
   * Get zone with given level number by traversing up geographicZone hierachy if needed.
   * @return zone of the facility with given level number.
   */
  @JsonIgnore
  public GeographicZoneDto getZoneByLevelNumber(Integer levelNumber) {
    GeographicZoneDto district = geographicZone;
    while (null != district && null != district.getParent()
        && district.getLevel().getLevelNumber() > levelNumber) {
      district = district.getParent();
    }
    return district;
  }
}
