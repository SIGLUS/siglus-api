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

package org.openlmis.referencedata.fhir;

import static org.apache.commons.lang3.BooleanUtils.isTrue;
import static org.openlmis.referencedata.fhir.FhirCoding.AREA;
import static org.openlmis.referencedata.fhir.FhirCoding.SITE;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.domain.GeographicZone;
import org.openlmis.referencedata.web.FacilityOperatorController;
import org.openlmis.referencedata.web.FacilityTypeController;
import org.openlmis.referencedata.web.GeographicLevelController;
import org.openlmis.referencedata.web.LocationController;
import org.openlmis.referencedata.web.ProgramController;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public final class FhirLocation extends FhirResource {
  static final String RESOURCE_TYPE_NAME = "Location";

  private final List<String> alias;
  private final List<FhirIdentifier> identifier;

  private String name;
  private FhirPosition position;
  private FhirPhysicalType physicalType;
  private FhirReference partOf;
  private String description;
  private String status;

  private FhirLocation(UUID id) {
    super(id, RESOURCE_TYPE_NAME);

    this.alias = Lists.newArrayList();
    this.identifier = Lists.newArrayList();
  }

  /**
   * Creates new instance of FHIR Location based on data from {@link GeographicZone}.
   */
  static FhirLocation newInstance(String serviceUrl, GeographicZone zone) {
    FhirLocation fhirLocation = new FhirLocation(zone.getId());

    // mandatory
    fhirLocation.addAlias(zone.getCode());
    fhirLocation.addIdentifier(
        serviceUrl, GeographicLevelController.RESOURCE_PATH, zone.getLevel().getId());
    fhirLocation.physicalType = new FhirPhysicalType(AREA);

    // optional
    Optional
        .ofNullable(zone.getName())
        .ifPresent(name -> fhirLocation.name = name);
    Optional
        .ofNullable(zone.getParent())
        .ifPresent(parent ->
            fhirLocation.partOf =
                new FhirReference(serviceUrl, LocationController.RESOURCE_PATH, parent.getId()));

    if (null != zone.getLatitude() && null != zone.getLongitude()) {
      fhirLocation.position = new FhirPosition(zone.getLongitude(), zone.getLatitude());
    }

    return fhirLocation;
  }

  /**
   * Creates new instance of FHIR Location based on data from {@link Facility}.
   */
  static FhirLocation newInstance(String serviceUrl, Facility facility) {
    FhirLocation fhirLocation = new FhirLocation(facility.getId());

    // mandatory
    fhirLocation.addAlias(facility.getCode());
    fhirLocation.partOf = new FhirReference(serviceUrl, LocationController.RESOURCE_PATH,
        facility.getGeographicZone().getId());
    fhirLocation.addIdentifier(serviceUrl, FacilityTypeController.RESOURCE_PATH,
        facility.getType().getId());
    fhirLocation.status = isTrue(facility.getActive())
        ? Status.ACTIVE.toString()
        : Status.INACTIVE.toString();
    fhirLocation.physicalType = new FhirPhysicalType(SITE);

    // optional
    Optional
        .ofNullable(facility.getName())
        .ifPresent(name -> fhirLocation.name = name);
    Optional
        .ofNullable(facility.getDescription())
        .ifPresent(description -> fhirLocation.description = description);
    Optional
        .ofNullable(facility.getOperator())
        .ifPresent(operator -> fhirLocation.addIdentifier(
            serviceUrl, FacilityOperatorController.RESOURCE_PATH, operator.getId()));
    Optional
        .ofNullable(facility.getSupportedPrograms())
        .orElse(Collections.emptySet())
        .forEach(sp -> fhirLocation.addIdentifier(
            serviceUrl, ProgramController.RESOURCE_PATH, sp.programId()));
    Optional
        .ofNullable(facility.getLocation())
        .ifPresent(point -> fhirLocation.position = new FhirPosition(point.getX(), point.getY()));

    return fhirLocation;
  }

  private void addAlias(String alias) {
    this.alias.add(alias);
  }

  private void addIdentifier(String serviceUrl, String path, UUID uuid) {
    identifier.add(new FhirIdentifier(serviceUrl, path, uuid));
  }

}
