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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.openlmis.notification.i18n.MessageKeys.ERROR_SEND_SMS_FAILURE;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@RunWith(MockitoJUnitRunner.class)
public class SmsSenderTest {

  private static final String SMS_SEND_API_URL = "http://localhost/api/send";
  private static final String SMS_SEND_API_TOKEN = "token";
  private static final String MESSAGE = "This is an SMS message";
  private static final String TO_PHONE_NUMBER = "12065551234";

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Mock
  private RestTemplate restTemplate;

  @InjectMocks
  private SmsSender sender;
  
  @Captor
  private ArgumentCaptor<String> urlCaptor;
  
  @Captor
  private ArgumentCaptor<HttpEntity> requestCaptor;

  @Before
  public void setUp() {
    ReflectionTestUtils.setField(sender, "smsSendApiUrl", SMS_SEND_API_URL);
    ReflectionTestUtils.setField(sender, "smsSendApiToken", SMS_SEND_API_TOKEN);

    given(restTemplate.postForEntity(
        any(String.class), any(HttpEntity.class), eq(String.class)))
        .willReturn(new ResponseEntity<>("Successful", HttpStatus.CREATED));
  }

  @Test
  public void sendMessageShouldSendMessageWithCorrectRequest() {
    // when
    sender.sendMessage(TO_PHONE_NUMBER, MESSAGE);

    // then
    verify(restTemplate).postForEntity(urlCaptor.capture(), requestCaptor.capture(),
        eq(String.class));

    String url = urlCaptor.getValue();
    assertThat(url).isEqualToIgnoringCase(SMS_SEND_API_URL);

    HttpEntity requestValue = requestCaptor.getValue();

    HttpHeaders requestHeaders = requestValue.getHeaders();
    assertThat(requestHeaders.getContentType()).isEqualByComparingTo(MediaType.APPLICATION_JSON);
    assertThat(requestHeaders.get("Authorization")).contains("Token " + SMS_SEND_API_TOKEN);

    SmsRequestDto requestBody = (SmsRequestDto) requestValue.getBody();
    assertThat(requestBody.getText()).isEqualToIgnoringCase(MESSAGE);
    assertThat(requestBody.getUrns()).contains("tel:" + TO_PHONE_NUMBER);
  }
  
  @Test
  public void sendMessageShouldThrowExceptionIfServiceReturnsAnErrorCode() {
    // given
    given(restTemplate.postForEntity(
        any(String.class), any(HttpEntity.class), eq(String.class)))
        .willThrow(new RestClientException("Bad request"));
    exception.expect(ServerException.class);
    exception.expectMessage(ERROR_SEND_SMS_FAILURE);

    // when
    sender.sendMessage(TO_PHONE_NUMBER, MESSAGE);
  }
}
