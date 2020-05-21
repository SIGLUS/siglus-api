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

package org.openlmis.referencedata.dto;

import com.vividsolutions.jts.geom.Point;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.domain.FacilityOperator;
import org.openlmis.referencedata.domain.FacilityType;
import org.openlmis.referencedata.domain.GeographicZone;
import org.openlmis.referencedata.domain.SupportedProgram;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MinimalFacilityDto extends BaseDto implements Facility.Exporter {

  @Getter
  @Setter
  private String name;

  /**
   * Creates new instance of {@link MinimalFacilityDto} based on passed facility.
   */
  public static MinimalFacilityDto newInstance(Facility facility) {
    MinimalFacilityDto dto = new MinimalFacilityDto();
    facility.export(dto);

    return dto;
  }

  @Override
  public void setGeographicZone(GeographicZone geographicZone) {
    // unsupported operation
  }

  @Override
  public void setType(FacilityType type) {
    // unsupported operation
  }

  @Override
  public void setCode(String code) {
    // unsupported operation
  }

  @Override
  public void setDescription(String description) {
    // unsupported operation
  }

  @Override
  public void setOperator(FacilityOperator operator) {
    // unsupported operation
  }

  @Override
  public void setActive(Boolean active) {
    // unsupported operation
  }

  @Override
  public void setGoLiveDate(LocalDate goLiveDate) {
    // unsupported operation
  }

  @Override
  public void setGoDownDate(LocalDate goDownDate) {
    // unsupported operation
  }

  @Override
  public void setComment(String comment) {
    // unsupported operation
  }

  @Override
  public void setEnabled(Boolean enabled) {
    // unsupported operation
  }

  @Override
  public void setOpenLmisAccessible(Boolean openLmisAccessible) {
    // unsupported operation
  }

  @Override
  public void setSupportedPrograms(Set<SupportedProgram> supportedPrograms) {
    // unsupported operation
  }

  @Override
  public void setLocation(Point location) {
    // unsupported operation
  }

  @Override
  public void setExtraData(Map<String, Object> extraData) {
    // unsupported operation
  }
}
