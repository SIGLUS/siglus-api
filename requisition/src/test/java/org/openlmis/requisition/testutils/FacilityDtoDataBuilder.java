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

package org.openlmis.requisition.testutils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.openlmis.requisition.dto.FacilityDto;
import org.openlmis.requisition.dto.FacilityOperatorDto;
import org.openlmis.requisition.dto.FacilityTypeDto;
import org.openlmis.requisition.dto.GeographicZoneDto;
import org.openlmis.requisition.dto.SupportedProgramDto;
import org.openlmis.requisition.testutils.api.DtoDataBuilder;

@SuppressWarnings("PMD.TooManyMethods")
public class FacilityDtoDataBuilder implements DtoDataBuilder<FacilityDto> {

  private static int instanceNumber = 0;

  private UUID id;
  private String code;
  private String name;
  private Boolean active;
  private GeographicZoneDto geographicZone;
  private FacilityTypeDto type;
  private String description;
  private LocalDate goLiveDate;
  private LocalDate goDownDate;
  private String comment;
  private Boolean enabled;
  private Boolean openLmisAccessible;
  private List<SupportedProgramDto> supportedPrograms;
  private FacilityOperatorDto operator;

  /**
   * Creates builder for creating new instance of {@link FacilityDto}.
   */
  public FacilityDtoDataBuilder() {
    id = UUID.randomUUID();
    code = "F" + instanceNumber;
    name = "Facility " + instanceNumber;
    active = true;
    geographicZone = new GeographicZoneDtoDataBuilder().buildAsDto();
    type = new FacilityTypeDto();
    description = "facility desc " + instanceNumber;
    goLiveDate = LocalDate.now();
    goDownDate = null;
    comment = "some facility comment " + instanceNumber;
    enabled = true;
    openLmisAccessible = true;
    supportedPrograms = new ArrayList<>();
    operator = new FacilityOperatorDto();
  }

  /**
   * Creates new instance of {@link FacilityDto} with properties.
   * @return created facility.
   */
  public FacilityDto buildAsDto() {
    FacilityDto facility = new FacilityDto();
    facility.setId(id);
    facility.setCode(code);
    facility.setName(name);
    facility.setActive(active);
    facility.setGeographicZone(geographicZone);
    facility.setType(type);
    facility.setDescription(description);
    facility.setGoLiveDate(goLiveDate);
    facility.setGoDownDate(goDownDate);
    facility.setComment(comment);
    facility.setEnabled(enabled);
    facility.setOpenLmisAccessible(openLmisAccessible);
    facility.setSupportedPrograms(supportedPrograms);
    facility.setOperator(operator);

    return facility;
  }

  public FacilityDtoDataBuilder withId(UUID id) {
    this.id = id;
    return this;
  }

  public FacilityDtoDataBuilder withCode(String code) {
    this.code = code;
    return this;
  }

  public FacilityDtoDataBuilder withName(String name) {
    this.name = name;
    return this;
  }

  public FacilityDtoDataBuilder withActive(Boolean active) {
    this.active = active;
    return this;
  }

  public FacilityDtoDataBuilder withGeographicZone(GeographicZoneDto geographicZone) {
    this.geographicZone = geographicZone;
    return this;
  }

  public FacilityDtoDataBuilder withType(FacilityTypeDto type) {
    this.type = type;
    return this;
  }

  public FacilityDtoDataBuilder withDescription(String description) {
    this.description = description;
    return this;
  }

  public FacilityDtoDataBuilder withGoLiveDate(LocalDate goLiveDate) {
    this.goLiveDate = goLiveDate;
    return this;
  }

  public FacilityDtoDataBuilder withGoDownDate(LocalDate goDownDate) {
    this.goDownDate = goDownDate;
    return this;
  }

  public FacilityDtoDataBuilder withComment(String comment) {
    this.comment = comment;
    return this;
  }

  public FacilityDtoDataBuilder withEnabled(Boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  public FacilityDtoDataBuilder withOpenLmisAccessible(Boolean openLmisAccessible) {
    this.openLmisAccessible = openLmisAccessible;
    return this;
  }

  public FacilityDtoDataBuilder withSupportedPrograms(List<SupportedProgramDto> supportedPrograms) {
    this.supportedPrograms = supportedPrograms;
    return this;
  }

  public FacilityDtoDataBuilder withOperator(FacilityOperatorDto operator) {
    this.operator = operator;
    return this;
  }
}
