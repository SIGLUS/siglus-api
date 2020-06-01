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

import static org.openlmis.notification.i18n.MessageKeys.ERROR_SEND_SMS_FAILURE;

import java.util.Collections;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.slf4j.profiler.Profiler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class SmsSender {

  private static final XLogger XLOGGER = XLoggerFactory.getXLogger(SmsSender.class);

  @Autowired
  RestTemplate restTemplate;

  @Value("${sms.send.api.url}")
  private String smsSendApiUrl;

  @Value("${sms.send.api.token}")
  private String smsSendApiToken;

  void sendMessage(String toPhoneNumber, String message) {
    XLOGGER.entry(toPhoneNumber, message);
    Profiler profiler = new Profiler("SEND_SMS_MESSAGE");
    profiler.setLogger(XLOGGER);

    profiler.start("PREPARE_HTTP_REQUEST");
    String toUrn = "tel:" + toPhoneNumber;
    SmsRequestDto payload = new SmsRequestDto(message, Collections.singletonList(toUrn));
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("Authorization", "Token " + smsSendApiToken);
    HttpEntity<SmsRequestDto> request = new HttpEntity<>(payload, headers);

    profiler.start("POST_TO_SMS_SEND_API");
    SmsRequestDto requestBody = request.getBody();
    XLOGGER.debug("request, url = {}, body = {}", smsSendApiUrl, requestBody.toString());

    int responseCode;
    String responseBody;
    try {
      ResponseEntity<String> response = restTemplate.postForEntity(
          smsSendApiUrl, request, String.class);

      responseCode = response.getStatusCodeValue();
      responseBody = response.getBody();
      XLOGGER.debug("Send successful, status code was {}, response = {}", responseCode,
          responseBody);
    } catch (RestClientException rce) {
      NotificationException exception = new ServerException(rce, ERROR_SEND_SMS_FAILURE);

      XLOGGER.throwing(exception);
      profiler.stop().log();

      throw exception;
    }

    profiler.stop().log();
    XLOGGER.exit();
  }
}
