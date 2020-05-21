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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.domain.GeographicZone;
import org.openlmis.referencedata.testbuilder.FacilityDataBuilder;
import org.openlmis.referencedata.testbuilder.GeographicZoneDataBuilder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("PMD.AvoidUsingHardCodedIP")
public class DefaultFhirClientTest {

  private static final String SERVICE_URL = "http://localhost";
  private static final String API_KEY_PREFIX = "prefix";

  @Mock
  private LocationFactory locationFactory;

  @Mock
  private LocationConverter locationConvert;

  @Mock
  private LocationSynchronizer locationSynchronizer;

  @Mock
  private IBaseResource resource;

  @Mock
  private SecurityContext securityContext;

  @Mock
  private OAuth2Authentication authentication;


  private DefaultFhirClient client;

  @Before
  public void setUp() {
    client = new DefaultFhirClient();
    client.setApiKeyPrefix(API_KEY_PREFIX);
    client.setLocationSynchronizer(locationSynchronizer);
    client.setLocationFactory(locationFactory);
    client.setLocationConvert(locationConvert);

    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.isClientOnly()).thenReturn(true);
    when(authentication.getOAuth2Request()).thenReturn(createAuthRequest(API_KEY_PREFIX));

    SecurityContextHolder.setContext(securityContext);
  }

  @Test
  public void shouldSynchronizeFacility() {
    //given
    Facility facility = new FacilityDataBuilder().build();
    FhirLocation location = FhirLocation.newInstance(SERVICE_URL, facility);

    // when
    when(locationFactory.createFor(facility)).thenReturn(location);
    when(locationConvert.convert(location)).thenReturn(resource);
    client.synchronizeFacility(facility);

    // then
    verify(locationFactory).createFor(facility);
    verify(locationConvert).convert(location);
    verify(locationSynchronizer).synchronize(location, resource);
  }

  @Test
  public void shouldNotSynchronizeFacilityIfRequestCameFromFhirServer() {
    // given
    Facility facility = new FacilityDataBuilder().build();

    // when
    when(authentication.getOAuth2Request()).thenReturn(createAuthRequest("service-token"));
    client.synchronizeFacility(facility);

    // then
    verifyZeroInteractions(locationFactory, locationConvert, locationSynchronizer);
  }

  @Test
  public void shouldSynchronizeGeographicZone() {
    //given
    GeographicZone geographicZone = new GeographicZoneDataBuilder().build();
    FhirLocation location = FhirLocation.newInstance(SERVICE_URL, geographicZone);

    // when
    when(locationFactory.createFor(geographicZone)).thenReturn(location);
    when(locationConvert.convert(location)).thenReturn(resource);
    client.synchronizeGeographicZone(geographicZone);

    // then
    verify(locationFactory).createFor(geographicZone);
    verify(locationConvert).convert(location);
    verify(locationSynchronizer).synchronize(location, resource);
  }

  @Test
  public void shouldNotSynchronizeGeographicZoneIfRequestCameFromFhirServer() {
    // given
    GeographicZone geographicZone = new GeographicZoneDataBuilder().build();

    // when
    when(authentication.getOAuth2Request()).thenReturn(createAuthRequest("service-token"));
    client.synchronizeGeographicZone(geographicZone);

    // then
    verifyZeroInteractions(locationFactory, locationConvert, locationSynchronizer);
  }

  private OAuth2Request createAuthRequest(String clientId) {
    return new OAuth2Request(null, clientId, null, true, null, null, null, null, null);
  }
}
