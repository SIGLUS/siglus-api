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

package org.openlmis.fulfillment.testutils;

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.openlmis.fulfillment.service.referencedata.FacilityDto;
import org.openlmis.fulfillment.service.referencedata.FacilityOperatorDto;
import org.openlmis.fulfillment.service.referencedata.FacilityTypeDto;
import org.openlmis.fulfillment.service.referencedata.GeographicZoneDto;
import org.openlmis.fulfillment.service.referencedata.ProgramDto;

public class FacilityDataBuilder {

  private static int instanceNumber = 0;

  private UUID id;
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
   * Builder for {@link FacilityDto}.
   */
  public FacilityDataBuilder() {
    instanceNumber++;

    id = UUID.randomUUID();
    code = "F" + instanceNumber;
    name = "Facility " + instanceNumber;
    description = "desc";
    active = true;
    goLiveDate = LocalDate.of(2018, Month.JANUARY, 23);
    goDownDate = LocalDate.of(2019, Month.JANUARY, 23);
    comment = "comment";
    enabled = true;
    openLmisAccessible = true;
    supportedPrograms = new ArrayList<>();
    geographicZone = new GeographicZoneDataBuilder().build();
    operator = new FacilityOperatorDataBuilder().build();
    type = new FacilityTypeDataBuilder().build();
  }

  /**
   * Builds instance of {@link FacilityDto}.
   */
  public FacilityDto build() {
    FacilityDto facility = new FacilityDto(code, name, description, active, goLiveDate, goDownDate,
        comment, enabled, openLmisAccessible, supportedPrograms, geographicZone, operator, type);
    facility.setId(id);
    return facility;
  }

  public FacilityDataBuilder withName(String name) {
    this.name = name;
    return this;
  }

  public FacilityDataBuilder withId(UUID id) {
    this.id = id;
    return this;
  }

  public FacilityDataBuilder withSupportedPrograms(List<ProgramDto> supportedPrograms) {
    this.supportedPrograms = supportedPrograms;
    return this;
  }
}
