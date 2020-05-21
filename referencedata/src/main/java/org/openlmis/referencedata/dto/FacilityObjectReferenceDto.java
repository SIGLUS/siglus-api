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

import static java.util.stream.Collectors.toSet;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.vividsolutions.jts.geom.Point;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.domain.FacilityOperator;
import org.openlmis.referencedata.domain.FacilityType;
import org.openlmis.referencedata.domain.GeographicZone;
import org.openlmis.referencedata.domain.SupportedProgram;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, exclude = "serviceUrl")
@ToString(callSuper = true)
public final class FacilityObjectReferenceDto extends ObjectReferenceDto
    implements Facility.Exporter {

  private static final String FACILITIES = "facilities";

  @JsonIgnore
  private String serviceUrl;

  private String name;
  private FacilityTypeDto type;
  private GeographicZoneSimpleDto geographicZone;
  private FacilityOperatorDto operator;
  private String description;
  private LocalDate goLiveDate;
  private LocalDate goDownDate;
  private String comment;
  private Boolean openLmisAccessible;
  private Point location;
  private Map<String, Object> extraData;
  private String code;
  private Boolean active;
  private Boolean enabled;
  private Set<SupportedProgramDto> supportedPrograms;

  public FacilityObjectReferenceDto(UUID id, String serviceUrl) {
    super(serviceUrl, FACILITIES, id);
    this.serviceUrl = serviceUrl;
  }

  @JsonSetter("operator")
  public void setOperator(FacilityOperatorDto operator) {
    this.operator = operator;
  }

  @Override
  @JsonIgnore
  public void setOperator(FacilityOperator operator) {
    this.operator = new FacilityOperatorDto();
    operator.export(this.operator);
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
  @JsonIgnore
  public void setSupportedPrograms(Set<SupportedProgram> supportedPrograms) {
    if (supportedPrograms != null) {
      this.supportedPrograms = supportedPrograms.stream()
          .map(this::export)
          .collect(toSet());
    } else {
      this.supportedPrograms = null;
    }
  }

  private SupportedProgramDto export(SupportedProgram supportedProgram) {
    SupportedProgramDto supportedProgramDto = new SupportedProgramDto();
    supportedProgram.export(supportedProgramDto);

    return supportedProgramDto;
  }
}
