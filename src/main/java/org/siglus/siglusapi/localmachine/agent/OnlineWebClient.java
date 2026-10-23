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

package org.siglus.siglusapi.localmachine.agent;

import static java.util.Collections.singletonList;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.siglus.siglusapi.localmachine.Ack;
import org.siglus.siglusapi.localmachine.Event;
import org.siglus.siglusapi.localmachine.ExternalEventDtoMapper;
import org.siglus.siglusapi.localmachine.auth.LocalTokenInterceptor;
import org.siglus.siglusapi.localmachine.webapi.AckExchange;
import org.siglus.siglusapi.localmachine.webapi.ActivationResponse;
import org.siglus.siglusapi.localmachine.webapi.EventsResponse;
import org.siglus.siglusapi.localmachine.webapi.RemoteActivationRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class OnlineWebClient {

  private static final int ERROR_CONTEXT_CHARS = 500;
  private static final String PATH_ACTIVATE_AGENT = "/server/agents";
  private final RestTemplate restTemplate;
  private final ExternalEventDtoMapper externalEventDtoMapper;
  private final ObjectMapper objectMapper;

  @Value("${machine.web.url}")
  private String webBaseUrl;

  public OnlineWebClient(
      LocalTokenInterceptor localTokenInterceptor, ExternalEventDtoMapper externalEventDtoMapper) {
    this.externalEventDtoMapper = externalEventDtoMapper;
    this.restTemplate = new RestTemplateBuilder().setConnectTimeout(5 * 1000).build();
    configureLocalTokenInterceptor(localTokenInterceptor);
    restTemplate.setInterceptors(singletonList(localTokenInterceptor));
    this.objectMapper = restTemplate.getMessageConverters().stream()
        .filter(MappingJackson2HttpMessageConverter.class::isInstance)
        .map(c -> ((MappingJackson2HttpMessageConverter) c).getObjectMapper())
        .findFirst().orElseGet(ObjectMapper::new);
  }

  public void sync(ByteArrayResource resource) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("file", resource);
    HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
    URI url = URI.create(webBaseUrl + "/server/eventFile");
    restTemplate.postForEntity(url, request, Void.class);
  }

  public List<Event> exportPeeringEvents() {
    URI url = URI.create(webBaseUrl + "/server/peeringEvents");
    return getEvents(url);
  }

  public List<Event> exportMasterDataEvents(long offsetId) {
    URI url = URI.create(webBaseUrl + "/server/masterDataEvents/" + offsetId);
    return getEvents(url);
  }

  private List<Event> getEvents(URI url) {
    ResponseEntity<byte[]> response = restTemplate.getForEntity(url, byte[].class);
    byte[] body = response.getBody() == null ? new byte[0] : response.getBody();
    try {
      return objectMapper.readValue(body, EventsResponse.class).getEvents().stream()
          .map(externalEventDtoMapper::map)
          .collect(Collectors.toList());
    } catch (IOException e) {
      logBadResponse(url, response, body, e);
      throw new HttpMessageNotReadableException("fail to parse response of " + url, e);
    }
  }

  private void logBadResponse(URI url, ResponseEntity<byte[]> response, byte[] body, IOException e) {
    String text = new String(body, StandardCharsets.UTF_8);
    long offset = e instanceof JsonProcessingException && ((JsonProcessingException) e).getLocation() != null
        ? ((JsonProcessingException) e).getLocation().getCharOffset() : -1;
    int pos = (int) Math.max(0, Math.min(offset, text.length()));
    String around = text.substring(Math.max(0, pos - ERROR_CONTEXT_CHARS),
        Math.min(text.length(), pos + ERROR_CONTEXT_CHARS));
    HttpHeaders headers = response.getHeaders();
    log.error("fail to parse events response, url:{}, status:{}, contentType:{}, contentLength header:{}, "
            + "contentEncoding:{}, actual bytes:{}, chars:{}, error char offset:{}, endsWithBrace:{}, "
            + "headers:{}, error:{}, text around error:[{}]",
        url, response.getStatusCode(), headers.getContentType(), headers.getContentLength(),
        headers.getFirst(HttpHeaders.CONTENT_ENCODING), body.length, text.length(), offset,
        text.trim().endsWith("}"), headers, e.getMessage(), around);
    log.error("tail of events response:[{}]",
        text.substring(Math.max(0, text.length() - ERROR_CONTEXT_CHARS)));
  }

  public ActivationResponse activate(RemoteActivationRequest remoteActivationRequest) {
    URI url = URI.create(webBaseUrl + PATH_ACTIVATE_AGENT);
    return restTemplate.postForObject(url, remoteActivationRequest, ActivationResponse.class);
  }

  public Set<Ack> exchangeAcks(Set<Ack> notShippedAcks) {
    URI url = URI.create(webBaseUrl + "/server/acks");
    AckExchange request = new AckExchange(notShippedAcks);
    return restTemplate.postForEntity(url, request, AckExchange.class).getBody().getAcks();
  }

  public void confirmAcks(Set<Ack> acks) {
    URI url = URI.create(webBaseUrl + "/server/acks");
    AckExchange request = new AckExchange(acks);
    restTemplate.put(url, request);
  }

  private void configureLocalTokenInterceptor(LocalTokenInterceptor localTokenInterceptor) {
    localTokenInterceptor.setAcceptFunc(
        httpRequest -> !httpRequest.getURI().getRawPath().contains(PATH_ACTIVATE_AGENT));
  }
}
