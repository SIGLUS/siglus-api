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
import com.vividsolutions.jts.geom.Polygon;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.lang3.RandomUtils;
import org.openlmis.referencedata.domain.ExtraDataEntity;
import org.openlmis.referencedata.domain.GeographicLevel;
import org.openlmis.referencedata.domain.GeographicZone;
import org.openlmis.referencedata.validate.FhirLocationValidator;

@SuppressWarnings("PMD.TooManyMethods")
public class GeographicZoneDataBuilder {

  private static int instanceNumber = 0;

  private UUID id;
  private String code;
  private String name;
  private GeographicLevel level;
  private GeographicZone parent;
  private Integer catchmentPopulation;
  private Double latitude;
  private Double longitude;
  private Polygon boundary;
  private Map<String, Object> extraData;

  /**
   * Returns instance of {@link GeographicZoneDataBuilder} with sample data.
   */
  public GeographicZoneDataBuilder() {
    instanceNumber++;

    id = UUID.randomUUID();
    code = "GZ" + instanceNumber;
    name = "Geographic Zone #" + instanceNumber;
    level = new GeographicLevelDataBuilder().build();
    catchmentPopulation = RandomUtils.nextInt(0, 1000);
    latitude = RandomUtils.nextDouble(0, 200) - 100;
    longitude = RandomUtils.nextDouble(0, 200) - 100;
    extraData = Maps.newHashMap();
  }

  /**
   * Builds instance of {@link GeographicZone} without id.
   */
  public GeographicZone buildAsNew() {
    return new GeographicZone(code, name, level, parent, catchmentPopulation,
        latitude, longitude, boundary, new ExtraDataEntity(extraData));
  }

  /**
   * Builds instance of {@link GeographicZone}.
   */
  public GeographicZone build() {
    GeographicZone zone = buildAsNew();
    zone.setId(id);

    return zone;
  }

  /**
   * Sets a null value or empty collection for all optional fields.
   */
  public GeographicZoneDataBuilder withoutOptionalFields() {
    name = null;
    parent = null;
    catchmentPopulation = null;
    latitude = null;
    longitude = null;
    boundary = null;
    extraData = Maps.newHashMap();

    return this;
  }

  public GeographicZoneDataBuilder withName(String name) {
    this.name = name;
    return this;
  }

  public GeographicZoneDataBuilder withLevel(GeographicLevel level) {
    this.level = level;
    return this;
  }

  public GeographicZoneDataBuilder withParent(GeographicZone parent) {
    this.parent = parent;
    return this;
  }

  public GeographicZoneDataBuilder withBoundary(Polygon boundary) {
    this.boundary = boundary;
    return this;
  }

  public GeographicZoneDataBuilder withLatitude(Number latitude) {
    this.latitude = latitude.doubleValue();
    return this;
  }

  public GeographicZoneDataBuilder withLongitude(Number longitude) {
    this.longitude = longitude.doubleValue();
    return this;
  }

  public GeographicZoneDataBuilder withExtraData(String key, String value) {
    this.extraData.put(key, value);
    return this;
  }

  public GeographicZoneDataBuilder withIsManagedExternallyFlag() {
    return withExtraData(FhirLocationValidator.IS_MANAGED_EXTERNALLY, Boolean.TRUE.toString());
  }

}
