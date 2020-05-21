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
import java.time.LocalDate;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class FacilityDto extends MinimalFacilityDto {

  private String description;
  private LocalDate goLiveDate;
  private LocalDate goDownDate;
  private String comment;
  private Boolean enabled;
  private Boolean openLmisAccessible;
  private List<SupportedProgramDto> supportedPrograms;
  private FacilityOperatorDto operator;

  /**
   * Get zone with given level number by traversing up geographicZone hierarchy if needed.
   * @return zone of the facility with given level number.
   */
  @Override
  @JsonIgnore
  public GeographicZoneDto getZoneByLevelNumber(Integer levelNumber) {
    GeographicZoneDto district = this.geographicZone;
    while (null != district && null != district.getParent()
        && district.getLevel().getLevelNumber() > levelNumber) {
      district = district.getParent();
    }
    return district;
  }

  public interface Exporter extends MinimalFacilityDto.Exporter {
    void setDescription(String description);

    void setActive(Boolean active);

    void setGoLiveDate(LocalDate goLiveDate);

    void setGoDownDate(LocalDate goDownDate);

    void setComment(String comment);

    void setEnabled(Boolean enabled);

    void setOpenLmisAccessible(Boolean openLmisAccessible);

    void setSupportedPrograms(List<SupportedProgramDto> supportedPrograms);

    void setGeographicZone(GeographicZoneDto geographicZone);

    void setOperator(FacilityOperatorDto operator);

    void setType(FacilityTypeDto type);
  }

  /**
   * Export this object to the specified exporter (DTO).
   *
   * @param exporter exporter to export to
   */
  public void export(Exporter exporter) {
    super.export(exporter);
    exporter.setDescription(description);
    exporter.setActive(active);
    exporter.setGoLiveDate(goLiveDate);
    exporter.setGoDownDate(goDownDate);
    exporter.setComment(comment);
    exporter.setEnabled(enabled);
    exporter.setOpenLmisAccessible(openLmisAccessible);
    exporter.setSupportedPrograms(supportedPrograms);
    exporter.setGeographicZone(geographicZone);
    exporter.setOperator(operator);
    exporter.setType(type);
  }
}
