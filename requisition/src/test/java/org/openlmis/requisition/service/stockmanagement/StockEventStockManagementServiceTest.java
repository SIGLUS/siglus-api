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

package org.openlmis.requisition.service.stockmanagement;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.openlmis.requisition.dto.LocalizedMessageDto;
import org.openlmis.requisition.dto.stockmanagement.StockEventDto;
import org.openlmis.requisition.exception.ExternalApiException;
import org.openlmis.requisition.service.BaseCommunicationService;
import org.openlmis.requisition.service.DataRetrievalException;
import org.openlmis.requisition.testutils.DtoGenerator;
import org.openlmis.requisition.utils.RequestHelper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;

@SuppressWarnings("PMD.TooManyMethods")
public class StockEventStockManagementServiceTest
    extends BaseStockmanagementServiceTest<StockEventDto> {

  private static final ResponseEntity<UUID> RESPONSE_ENTITY_OK =
      ResponseEntity.ok(UUID.randomUUID());
  private StockEventStockManagementService service;
  private StockEventDto stockEventDto;

  @Mock
  private ObjectMapper objectMapper;

  @Before
  public void setUp() {
    super.setUp();

    service = (StockEventStockManagementService) prepareService();
    ReflectionTestUtils.setField(service, "objectMapper", objectMapper);

    stockEventDto = generateInstance();
  }

  @Test
  public void shouldSubmitStockEvent() {
    givenMockedOkResponse();

    whenEventIsSubmitted();

    thenPostRequestIsSent();
    andUriIsCorrect();
    andEntityIsCorrect();
  }

  @Test
  public void shouldResubmitEventWhenConflict() {
    givenMockedConflictResponse();

    whenEventIsSubmitted();

    thenPostRequestIsSentTwice();
    andUriIsCorrect();
    andEntityIsCorrect();
  }

  @Test
  public void shouldResubmitEventWhenTokenExpired() {
    givenMockedUnauthorizedResponse();

    whenEventIsSubmitted();

    thenPostRequestIsSentTwice();
    andUriIsCorrect();
    andEntityIsCorrect();
  }

  @Test
  public void shouldResubmitEventTwoTimesWhenTokenExpiredAndThenConflict() {
    givenMockedUnauthorizedAndConflictResponses();

    whenEventIsSubmitted();

    thenPostRequestIsSentThrice();
    andUriIsCorrect();
    andEntityIsCorrect();
  }

  @Test
  public void shouldPassErrorMessageFromExternalService() throws IOException {
    HttpStatusCodeException exp = mock(HttpStatusCodeException.class);
    when(exp.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST);
    when(exp.getResponseBodyAsString()).thenReturn("body");

    when(objectMapper.readValue("body", LocalizedMessageDto.class))
        .thenReturn(new LocalizedMessageDto("key", "message"));

    expectedException.expect(ExternalApiException.class);
    mockRequestFail(exp);
    service.submit(stockEventDto);
  }

  @Test
  public void shouldThrowExceptionIfThereWereProblemWithRequest() {
    HttpStatusCodeException exp = mock(HttpStatusCodeException.class);
    when(exp.getStatusCode()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);
    when(exp.getResponseBodyAsString()).thenReturn("body");

    mockRequestFail(exp);
    expectedException.expect(DataRetrievalException.class);
    service.submit(stockEventDto);
  }

  private void givenMockedOkResponse() {
    given(restTemplate.exchange(any(URI.class), any(HttpMethod.class), any(HttpEntity.class),
        any(Class.class))).willReturn(ResponseEntity.ok(UUID.randomUUID()));
  }

  private void givenMockedConflictResponse() {
    given(restTemplate.exchange(any(URI.class), any(HttpMethod.class), any(HttpEntity.class),
        any(Class.class)))
        .willThrow(new HttpServerErrorException(HttpStatus.valueOf(409)))
        .willReturn(RESPONSE_ENTITY_OK);
  }

  private void givenMockedUnauthorizedResponse() {
    given(restTemplate.exchange(any(URI.class), any(HttpMethod.class), any(HttpEntity.class),
        any(Class.class)))
        .willThrow(new HttpServerErrorException(HttpStatus.UNAUTHORIZED))
        .willReturn(RESPONSE_ENTITY_OK);
  }

  private void givenMockedUnauthorizedAndConflictResponses() {
    given(restTemplate.exchange(any(URI.class), any(HttpMethod.class), any(HttpEntity.class),
        any(Class.class)))
        .willThrow(new HttpServerErrorException(HttpStatus.UNAUTHORIZED))
        .willThrow(new HttpServerErrorException(HttpStatus.valueOf(409)))
        .willReturn(RESPONSE_ENTITY_OK);
  }

  private void whenEventIsSubmitted() {
    service.submit(stockEventDto);
  }

  private void thenPostRequestIsSent() {
    verifyRequest(1);
  }

  private void thenPostRequestIsSentTwice() {
    verifyRequest(2);
  }

  private void thenPostRequestIsSentThrice() {
    verifyRequest(3);
  }

  private void andUriIsCorrect() {
    assertEquals(RequestHelper.createUri(serviceUrl + "/api/stockEvents"), uriCaptor.getValue());
  }

  private void andEntityIsCorrect() {
    assertNotNull(entityCaptor.getValue().getBody());
    assertEquals(stockEventDto, entityCaptor.getValue().getBody());
    assertAuthHeader(entityCaptor.getValue());
  }

  private void assertAuthHeader(HttpEntity value) {
    List<String> authorization = value.getHeaders().get(HttpHeaders.AUTHORIZATION);

    assertThat(authorization, hasSize(1));
    assertThat(authorization, hasItem(TOKEN_HEADER));
  }

  private void verifyRequest(int times) {
    verify(restTemplate, times(times))
        .exchange(uriCaptor.capture(), eq(HttpMethod.POST), entityCaptor.capture(), eq(UUID.class));
  }

  @Override
  protected StockEventDto generateInstance() {
    return DtoGenerator.of(StockEventDto.class);
  }

  @Override
  protected BaseCommunicationService<StockEventDto> getService() {
    return new StockEventStockManagementService();
  }
}
