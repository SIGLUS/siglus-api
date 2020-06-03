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

package org.openlmis.fulfillment.service.referencedata;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.openlmis.fulfillment.web.util.BaseDto;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class FacilityDto extends BaseDto {
  private String code;
  private String name;
  private String description;
  private Boolean active;
  private LocalDate goLiveDate;
  private LocalDate goDownDate;
  private String comment;
  private Boolean enabled;
  private Boolean openLmisAccessible;
  private List<ProgramDto> supportedPrograms;
  private GeographicZoneDto geographicZone;
  private FacilityOperatorDto operator;
  private FacilityTypeDto type;

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
