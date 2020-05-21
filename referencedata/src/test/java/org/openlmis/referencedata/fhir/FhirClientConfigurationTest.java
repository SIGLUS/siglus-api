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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.model.api.IFhirVersion;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.referencedata.service.AuthService;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class FhirClientConfigurationTest {

  private static final String FHIR_SERVER_URL = "fhirClient.serverUrl";
  private static final String SERVICE_URL = "service.url";
  private static final String API_KEY_PREFIX = "auth.server.clientId.apiKey.prefix";

  private static final FhirVersionEnum VERSION_2 = FhirVersionEnum.DSTU2;
  private static final FhirVersionEnum VERSION_R4 = FhirVersionEnum.R4;

  @Mock
  private AuthService authService;

  @Mock
  private FhirContext context;

  @Mock
  private IFhirVersion fhirVersion;

  @Mock
  private IGenericClient client;

  @Spy
  private FhirClientConfiguration configuration;

  @Before
  public void setUp() {
    ReflectionTestUtils.setField(configuration, "authService", authService);
    ReflectionTestUtils.setField(configuration, "fhirServerUrl", FHIR_SERVER_URL);
    ReflectionTestUtils.setField(configuration, "serviceUrl", SERVICE_URL);
    ReflectionTestUtils.setField(configuration, "apiKeyPrefix", API_KEY_PREFIX);

    when(context.getVersion()).thenReturn(fhirVersion);
    when(context.newRestfulGenericClient(FHIR_SERVER_URL)).thenReturn(client);

    when(fhirVersion.getVersion()).thenReturn(VERSION_R4);

    when(configuration.fhirContext()).thenReturn(context);
  }

  @Test
  public void shouldCreateLocationSynchronizer() {
    assertThat(configuration.locationSynchronizer())
        .isInstanceOf(R4LocationSynchronizer.class)
        .hasNoNullFieldsOrProperties();
  }

  @Test
  public void shouldNotCreateLocationSynchronizerIfVersionNotSupported() {
    when(fhirVersion.getVersion()).thenReturn(VERSION_2);

    assertThatThrownBy(() -> configuration.locationSynchronizer())
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Unsupported FHIR version: DSTU2");
  }

  @Test
  public void shouldCreateLocationConverterStrategy() {
    assertThat(configuration.converterStrategy())
        .isInstanceOf(R4LocationConverterStrategy.class)
        .hasNoNullFieldsOrProperties();
  }

  @Test
  public void shouldNotCreateLocationConverterStrategyIfVersionNotSupported() {
    when(fhirVersion.getVersion()).thenReturn(VERSION_2);

    assertThatThrownBy(() -> configuration.converterStrategy())
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Unsupported FHIR version: DSTU2");
  }


}
