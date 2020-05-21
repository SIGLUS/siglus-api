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

import org.junit.Before;
import org.junit.Test;
import org.openlmis.referencedata.testbuilder.FacilityDataBuilder;
import org.openlmis.referencedata.testbuilder.GeographicZoneDataBuilder;
import org.springframework.test.util.ReflectionTestUtils;

public class LocationFactoryTest {

  private static final String SERVICE_URL = "http://localhost";

  private LocationFactory locationFactory;

  @Before
  public void setUp() throws Exception {
    locationFactory = new LocationFactory();
    ReflectionTestUtils.setField(locationFactory, "serviceUrl", SERVICE_URL);
  }

  @Test
  public void shouldCreateLocationForFacility() {
    assertThat(locationFactory.createFor(new FacilityDataBuilder().build()))
        .isNotNull();
  }

  @Test
  public void shouldCreateLocationForGeo() {
    assertThat(locationFactory.createFor(new GeographicZoneDataBuilder().build()))
        .isNotNull();
  }
}
