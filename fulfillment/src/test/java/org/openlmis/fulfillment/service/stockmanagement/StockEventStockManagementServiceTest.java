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

package org.openlmis.fulfillment.service.stockmanagement;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.openlmis.fulfillment.service.BaseCommunicationService;
import org.openlmis.fulfillment.service.BaseCommunicationServiceTest;
import org.openlmis.fulfillment.service.DataRetrievalException;
import org.openlmis.fulfillment.service.ExternalApiException;
import org.openlmis.fulfillment.web.stockmanagement.StockEventDto;
import org.openlmis.fulfillment.web.util.LocalizedMessageDto;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;

public class StockEventStockManagementServiceTest
    extends BaseCommunicationServiceTest<StockEventDto> {

  @Mock
  private ObjectMapper objectMapper;

  private StockEventStockManagementService service;

  @Before
  public void setUp() {
    service = (StockEventStockManagementService) prepareService();
    ReflectionTestUtils.setField(service, "objectMapper", objectMapper);
  }

  @Override
  protected BaseCommunicationService getService() {
    return new StockEventStockManagementService();
  }

  @Override
  protected StockEventDto generateInstance() {
    return new StockEventDto();
  }

  @Test
  public void shouldSubmitStockEvent() {
    ResponseEntity response = mock(ResponseEntity.class);
    when(restTemplate.exchange(any(URI.class), eq(HttpMethod.POST), any(HttpEntity.class),
        eq(UUID.class))).thenReturn(response);

    service.submit(new StockEventDto());

    verify(restTemplate).exchange(uriCaptor.capture(), eq(HttpMethod.POST),
        entityCaptor.capture(), eq(UUID.class));

    URI uri = uriCaptor.getValue();
    String url = service.getServiceUrl() + service.getUrl();

    assertThat(uri.toString(), is(equalTo(url)));
    assertThat(entityCaptor.getValue().getBody(), is(new StockEventDto()));
  }

  @Test(expected = ExternalApiException.class)
  public void shouldThrowExceptionOnBadRequest() throws IOException {
    when(objectMapper.readValue(anyString(), eq(LocalizedMessageDto.class)))
        .thenReturn(new LocalizedMessageDto());
    when(restTemplate.exchange(
        any(URI.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(UUID.class))
    ).thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

    service.submit(new StockEventDto());
  }

  @Test(expected = DataRetrievalException.class)
  public void shouldReturnDataRetrievalExceptionOnOtherErrorResponseCodes() throws IOException {
    when(restTemplate.exchange(
        any(URI.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(UUID.class))
    ).thenThrow(new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

    service.submit(new StockEventDto());
  }
}
