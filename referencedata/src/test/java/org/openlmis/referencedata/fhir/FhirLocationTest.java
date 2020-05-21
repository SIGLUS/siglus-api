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

import static org.assertj.core.api.Assertions.assertThat;
import static org.openlmis.referencedata.fhir.FhirCoding.AREA;
import static org.openlmis.referencedata.fhir.FhirCoding.SITE;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import java.util.Optional;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.apache.commons.collections.CollectionUtils;
import org.junit.Test;
import org.openlmis.referencedata.ToStringTestUtils;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.domain.GeographicZone;
import org.openlmis.referencedata.testbuilder.FacilityDataBuilder;
import org.openlmis.referencedata.testbuilder.FacilityOperatorDataBuilder;
import org.openlmis.referencedata.testbuilder.GeographicZoneDataBuilder;
import org.openlmis.referencedata.testbuilder.ProgramDataBuilder;
import org.openlmis.referencedata.web.FacilityOperatorController;
import org.openlmis.referencedata.web.FacilityTypeController;
import org.openlmis.referencedata.web.GeographicLevelController;
import org.openlmis.referencedata.web.LocationController;
import org.openlmis.referencedata.web.ProgramController;

public class FhirLocationTest {

  private static final String SERVICE_URL = "http://localhost";

  @Test
  public void equalsContract() {
    EqualsVerifier
        .forClass(FhirLocation.class)
        .withRedefinedSuperclass()
        .suppress(Warning.NONFINAL_FIELDS) // we can't make fields as final in DTO
        .verify();
  }

  @Test
  public void shouldImplementToString() {
    FhirLocation dto = FhirLocation.newInstance("service-url", new FacilityDataBuilder().build());
    ToStringTestUtils.verify(FhirLocation.class, dto, "LOCATION");
  }

  @Test
  public void shouldCreateInstanceFromGeographicZoneWithOnlyMandatoryFields() {
    GeographicZone zone = new GeographicZoneDataBuilder()
        .withoutOptionalFields()
        .build();

    testCreateInstanceFromGeographicZone(zone);
  }

  @Test
  public void shouldCreateInstanceFromGeographicZoneWithOptionalFields() {
    GeographicZone zone = new GeographicZoneDataBuilder()
        .withName("abc")
        .withParent(new GeographicZoneDataBuilder().build())
        .withLatitude(10)
        .withLongitude(10)
        .build();

    testCreateInstanceFromGeographicZone(zone);
  }

  private void testCreateInstanceFromGeographicZone(GeographicZone zone) {
    FhirLocation fhirLocation = FhirLocation.newInstance(SERVICE_URL, zone);

    assertThat(fhirLocation.getId())
        .isEqualTo(zone.getId());
    assertThat(fhirLocation.getResourceType())
        .isEqualTo(FhirLocation.RESOURCE_TYPE_NAME);
    assertThat(fhirLocation.getAlias())
        .hasSize(1)
        .contains(zone.getCode());
    assertThat(fhirLocation.getIdentifier())
        .hasSize(1)
        .contains(new FhirIdentifier(SERVICE_URL,
            GeographicLevelController.RESOURCE_PATH, zone.getLevel().getId()));
    assertThat(fhirLocation.getName())
        .isEqualTo(zone.getName());

    if (null != zone.getLatitude() && null != zone.getLongitude()) {
      assertThat(fhirLocation.getPosition())
          .isEqualTo(new FhirPosition(zone.getLongitude(), zone.getLatitude()));
    } else {
      assertThat(fhirLocation.getPosition())
          .isNull();
    }

    assertThat(fhirLocation.getPhysicalType())
        .isEqualTo(new FhirPhysicalType(AREA));

    Optional
        .ofNullable(zone.getParent())
        .ifPresent(parent ->
            assertThat(fhirLocation.getPartOf())
                .isEqualTo(new FhirReference(SERVICE_URL,
                    LocationController.RESOURCE_PATH, parent.getId())));
    assertThat(fhirLocation.getDescription())
        .isNull();
    assertThat(fhirLocation.getStatus())
        .isNull();
  }

  @Test
  public void shouldCreateInstanceFromFacilityWithOnlyMandatoryFields() {
    Facility facility = new FacilityDataBuilder()
        .withoutOptionalFields()
        .build();

    testCreateInstanceFromFacility(facility);
  }

  @Test
  public void shouldCreateInstanceFromFacilityWithOptionalFields() {
    Facility facility = new FacilityDataBuilder()
        .withName("def")
        .withDescription("sample description")
        .withOperator(new FacilityOperatorDataBuilder().build())
        .withSupportedProgram(new ProgramDataBuilder().build())
        .withLocation(new GeometryFactory().createPoint(new Coordinate(0, 0)))
        .build();

    testCreateInstanceFromFacility(facility);
  }

  private void testCreateInstanceFromFacility(Facility facility) {
    FhirLocation fhirLocation = FhirLocation.newInstance(SERVICE_URL, facility);

    assertThat(fhirLocation.getId())
        .isEqualTo(facility.getId());
    assertThat(fhirLocation.getResourceType())
        .isEqualTo(FhirLocation.RESOURCE_TYPE_NAME);
    assertThat(fhirLocation.getAlias())
        .hasSize(1)
        .contains(facility.getCode());

    int identifierCount = CollectionUtils.size(facility.getSupportedPrograms())
        + 1 // facility type is always set
        + (null == facility.getOperator() ? 0 : 1);

    assertThat(fhirLocation.getIdentifier())
        .hasSize(identifierCount)
        .contains(new FhirIdentifier(SERVICE_URL,
            FacilityTypeController.RESOURCE_PATH, facility.getType().getId()));

    Optional
        .ofNullable(facility.getOperator())
        .ifPresent(operator ->
            assertThat(fhirLocation.getIdentifier())
                .contains(new FhirIdentifier(SERVICE_URL,
                    FacilityOperatorController.RESOURCE_PATH, operator.getId())));

    facility
        .getSupportedPrograms()
        .forEach(supported ->
            assertThat(fhirLocation.getIdentifier())
                .contains(new FhirIdentifier(SERVICE_URL,
                    ProgramController.RESOURCE_PATH, supported.programId())));

    assertThat(fhirLocation.getName())
        .isEqualTo(facility.getName());
    Optional
        .ofNullable(facility.getLocation())
        .ifPresent(location ->
            assertThat(fhirLocation.getPosition())
                .isEqualTo(new FhirPosition(location.getX(), location.getY())));
    assertThat(fhirLocation.getPhysicalType())
        .isEqualTo(new FhirPhysicalType(SITE));
    assertThat(fhirLocation.getPartOf())
        .isEqualTo(new FhirReference(SERVICE_URL,
            LocationController.RESOURCE_PATH, facility.getGeographicZone().getId()));
    assertThat(fhirLocation.getDescription())
        .isEqualTo(facility.getDescription());
    assertThat(fhirLocation.getStatus())
        .isEqualTo(Status.ACTIVE.toString());
  }

}
