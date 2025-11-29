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

package org.siglus.siglusapi.web;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;


@RestController
@CrossOrigin  // Enable CORS for all routes
public class SiglusProxyController {

  private static final Logger logger = LoggerFactory.getLogger(SiglusProxyController.class);

  @PostMapping("/api/siglusapi/proxy")
  public ResponseEntity<?> proxy(@RequestBody Map<String, Object> reqBody) {
    logger.info("req.body: {}", reqBody);

    try {
      // Extract URL and method from the request payload.
      String url = (String) reqBody.get("url");
      String methodStr = (String) reqBody.getOrDefault("method", "GET");
      HttpMethod method = HttpMethod.valueOf(methodStr.toUpperCase());

      // Build headers if provided.
      HttpHeaders headers = new HttpHeaders();
      if (reqBody.containsKey("headers")) {
        Map<String, String> headerMap = (Map<String, String>) reqBody.get("headers");
        headerMap.forEach(headers::add);
      }

      // Extract body data if provided.
      Object data = reqBody.get("data");

      // Create an HttpEntity with the provided body and headers.
      HttpEntity<Object> entity = new HttpEntity<>(data, headers);

      // Use RestTemplate to send the HTTP request.
      RestTemplate restTemplate = new RestTemplate();
      ResponseEntity<String> response = restTemplate.exchange(url, method, entity, String.class);

      return ResponseEntity.ok(response.getBody());
    } catch (HttpClientErrorException ex) {
      // Forward the error status and body back to the client.
      return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
    } catch (Exception ex) {
      // Return a generic 500 error for other exceptions.
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
    }
  }
}