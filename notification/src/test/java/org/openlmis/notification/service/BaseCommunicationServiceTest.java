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

package org.openlmis.notification.service;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.UUID;
import lombok.Getter;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.notification.service.request.RequestParameters;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("PMD.TooManyMethods")
public abstract class BaseCommunicationServiceTest<T> {
  private static final String TOKEN = UUID.randomUUID().toString();

  @Mock
  protected RestTemplate restTemplate;

  @Mock
  protected AuthService authService;

  @Mock
  @Getter
  private ResponseEntity arrayResponse;

  @Captor
  protected ArgumentCaptor<URI> uriCaptor;

  @Captor
  protected ArgumentCaptor<HttpEntity<String>> entityCaptor;

  @Rule
  public final ExpectedException expectedException = ExpectedException.none();

  protected boolean checkAuth = true;

  @Before
  public void setUp() throws Exception {
    mockAuth();
  }

  @After
  public void tearDown() throws Exception {
    checkAuth();
  }

  @Test
  public void shouldRetryObtainingAccessToken() throws Exception {
    // given
    BaseCommunicationService<T> service = prepareService();
    HttpStatusCodeException exception = mock(HttpStatusCodeException.class);
    when(exception.getStatusCode()).thenReturn(HttpStatus.UNAUTHORIZED);
    when(exception.getResponseBodyAsString()).thenReturn(
        "{\"error\":\"invalid_token\",\"error_description\":\"" + UUID.randomUUID() + "}");

    // when
    when(restTemplate.exchange(
        any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class),
        eq(service.getArrayResultClass())
    )).thenThrow(exception);

    expectedException.expect(DataRetrievalException.class);
    service.findAll("", RequestParameters.init());

    verify(authService, times(1)).clearTokenCache();
    verify(authService, times(2)).obtainAccessToken();
  }

  @Test
  public void shouldRetryObtainingAccessTokenIfResponseBodyIsEmpty() throws Exception {
    // given
    BaseCommunicationService<T> service = prepareService();
    HttpStatusCodeException exception = mock(HttpStatusCodeException.class);
    when(exception.getStatusCode()).thenReturn(HttpStatus.UNAUTHORIZED);
    when(exception.getResponseBodyAsString()).thenReturn("");

    // when
    when(restTemplate.exchange(
        any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class),
        eq(service.getArrayResultClass())
    )).thenThrow(exception);

    expectedException.expect(DataRetrievalException.class);
    service.findAll("", RequestParameters.init());

    verify(authService, times(1)).clearTokenCache();
    verify(authService, times(2)).obtainAccessToken();
  }

  protected abstract BaseCommunicationService<T> getService();

  protected abstract T generateInstance();

  protected BaseCommunicationService prepareService() {
    BaseCommunicationService service = getService();
    service.setRestTemplate(restTemplate);
    ReflectionTestUtils.setField(service, "authService", authService);

    ReflectionTestUtils.setField(service, "maxUrlLength", 2000);

    return service;
  }

  protected void assertAuthHeader(HttpEntity entity) {
    assertThat(entity.getHeaders().get(HttpHeaders.AUTHORIZATION),
            is(singletonList("Bearer " + TOKEN)));
  }

  private void mockAuth() {
    when(authService.obtainAccessToken()).thenReturn(TOKEN);
  }

  private void checkAuth() {
    if (checkAuth) {
      verify(authService, atLeastOnce()).obtainAccessToken();
    }
  }
}
