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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.vividsolutions.jts.geom.Point;
import java.time.LocalDate;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.domain.FacilityOperator;
import org.openlmis.referencedata.domain.FacilityType;
import org.openlmis.referencedata.domain.GeographicZone;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class BasicFacilityDto extends MinimalFacilityDto
    implements Facility.Importer, Facility.Exporter {
  private String code;
  private Boolean active;
  private Boolean enabled;
  private FacilityTypeDto type;
  private GeographicZoneSimpleDto geographicZone;

  /**
   * Creates new instance of {@link BasicFacilityDto} based on passed facility.
   */
  public static BasicFacilityDto newInstance(Facility facility) {
    BasicFacilityDto dto = new BasicFacilityDto();
    facility.export(dto);

    return dto;
  }

  @JsonSetter("geographicZone")
  public void setGeographicZone(GeographicZoneSimpleDto geographicZone) {
    this.geographicZone = geographicZone;
  }

  @Override
  @JsonIgnore
  public void setGeographicZone(GeographicZone geographicZone) {
    this.geographicZone = new GeographicZoneSimpleDto();
    geographicZone.export(this.geographicZone);
  }

  @JsonSetter("type")
  public void setType(FacilityTypeDto type) {
    this.type = type;
  }

  @Override
  @JsonIgnore
  public void setType(FacilityType type) {
    this.type = new FacilityTypeDto();
    type.export(this.type);
  }

  @Override
  public String getDescription() {
    // unsupported operation
    return null;
  }

  @Override
  public FacilityOperator.Importer getOperator() {
    // unsupported operation
    return null;
  }

  @Override
  public LocalDate getGoLiveDate() {
    // unsupported operation
    return null;
  }

  @Override
  public LocalDate getGoDownDate() {
    // unsupported operation
    return null;
  }

  @Override
  public String getComment() {
    // unsupported operation
    return null;
  }

  @Override
  public Boolean getOpenLmisAccessible() {
    // unsupported operation
    return null;
  }

  @Override
  public Point getLocation() {
    // unsupported operation
    return null;
  }

  @Override
  public Map<String, Object> getExtraData() {
    // unsupported operation
    return null;
  }
}
