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

package org.openlmis.referencedata.testbuilder;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.domain.FacilityOperator;
import org.openlmis.referencedata.domain.FacilityType;
import org.openlmis.referencedata.domain.GeographicZone;
import org.openlmis.referencedata.domain.Program;
import org.openlmis.referencedata.validate.FhirLocationValidator;

@SuppressWarnings("PMD.TooManyMethods")
public class FacilityDataBuilder {

  private static int instanceNumber = 0;

  private UUID id;
  private String code;
  private String name;
  private String description;
  private GeographicZone geographicZone;
  private FacilityType type;
  private FacilityOperator operator;
  private Boolean active;
  private LocalDate goLiveDate;
  private LocalDate goDownDate;
  private String comment;
  private Boolean enabled;
  private Boolean openLmisAccessible;
  private Set<Program> supportedPrograms;
  private Point location;
  private Map<String, Object> extraData;

  /**
   * Returns instance of {@link FacilityDataBuilder} with sample data.
   */
  public FacilityDataBuilder() {
    instanceNumber++;

    id = UUID.randomUUID();
    code = "F" + instanceNumber;
    name = "Facility #" + instanceNumber;
    description = "Test facility";
    geographicZone = new GeographicZoneDataBuilder().build();
    type = new FacilityTypeDataBuilder().build();
    operator = new FacilityOperatorDataBuilder().build();
    active = true;
    enabled = true;
    openLmisAccessible = true;
    supportedPrograms = new HashSet<>();
    location = new GeometryFactory().createPoint(new Coordinate(54.5, 18.5));
    extraData = new HashMap<>();
  }

  /**
   * Builds instance of {@link Facility} without id.
   */
  public Facility buildAsNew() {

    //TODO: add new constructor, AllArgsConstructor fails 1 integration test
    Facility facility = new Facility(code);
    facility.setName(name);
    facility.setDescription(description);
    facility.setGeographicZone(geographicZone);
    facility.setType(type);
    facility.setOperator(operator);
    facility.setActive(active);
    facility.setGoLiveDate(goLiveDate);
    facility.setGoDownDate(goDownDate);
    facility.setComment(comment);
    facility.setEnabled(enabled);
    facility.setOpenLmisAccessible(openLmisAccessible);
    facility.setLocation(location);
    facility.setExtraData(extraData);
    facility.setSupportedPrograms(supportedPrograms
        .stream()
        .map(p -> new SupportedProgramDataBuilder()
            .withProgram(p)
            .withFacility(facility)
            .build())
        .collect(Collectors.toSet()));

    return facility;
  }

  /**
   * Builds instance of {@link Facility}.
   */
  public Facility build() {
    Facility facility = buildAsNew();
    facility.setId(id);

    return facility;
  }

  /**
   * Sets a null value or empty collection for all optional fields.
   */
  public FacilityDataBuilder withoutOptionalFields() {
    name = null;
    description = null;
    operator = null;
    goLiveDate = null;
    goDownDate = null;
    comment = null;
    openLmisAccessible = null;
    supportedPrograms = Sets.newHashSet();
    location = null;
    extraData = Maps.newHashMap();

    return this;
  }

  public FacilityDataBuilder withName(String name) {
    this.name = name;
    return this;
  }

  public FacilityDataBuilder withDescription(String description) {
    this.description = description;
    return this;
  }

  /**
   * Adds code for new {@link Facility}.
   */
  public FacilityDataBuilder withCode(String code) {
    this.code = code;
    return this;
  }

  /**
   * Set active as false for new {@link Facility}.
   */
  public FacilityDataBuilder nonActive() {
    this.active = false;
    return this;
  }

  /**
   * Adds supported program for new {@link Facility}.
   */
  public FacilityDataBuilder withSupportedProgram(Program program) {
    supportedPrograms.add(program);
    return this;
  }

  public FacilityDataBuilder withGeographicZone(GeographicZone geographicZone) {
    this.geographicZone = geographicZone;
    return this;
  }

  /**
   * Adds geographic zone with parent for new {@link Facility}.
   */
  public FacilityDataBuilder withGeographicZoneWithParent() {
    GeographicZone parent = new GeographicZoneDataBuilder().build();
    this.geographicZone = new GeographicZoneDataBuilder()
        .withParent(parent).build();
    return this;
  }

  public FacilityDataBuilder withType(FacilityType type) {
    this.type = type;
    return this;
  }

  public FacilityDataBuilder withOperator(FacilityOperator operator) {
    this.operator = operator;
    return this;
  }

  public FacilityDataBuilder withoutOperator() {
    return withOperator(null);
  }

  public FacilityDataBuilder withLocation(Point location) {
    this.location = location;
    return this;
  }

  public Facility buildActive() {
    this.active = true;
    return build();
  }

  public FacilityDataBuilder withExtraData(String key, String value) {
    this.extraData.put(key, value);
    return this;
  }

  public FacilityDataBuilder withIsManagedExternallyFlag() {
    return withExtraData(FhirLocationValidator.IS_MANAGED_EXTERNALLY, Boolean.TRUE.toString());
  }
}
