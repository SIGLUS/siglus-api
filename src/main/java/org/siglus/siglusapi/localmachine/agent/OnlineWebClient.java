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

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.siglus.siglusapi.localmachine.Ack;
import org.siglus.siglusapi.localmachine.Event;
import org.siglus.siglusapi.localmachine.ExternalEventDto;
import org.siglus.siglusapi.localmachine.ExternalEventDtoMapper;
import org.siglus.siglusapi.localmachine.auth.LocalTokenInterceptor;
import org.siglus.siglusapi.localmachine.webapi.AckExchange;
import org.siglus.siglusapi.localmachine.webapi.ActivationResponse;
import org.siglus.siglusapi.localmachine.webapi.PeeringEventsResponse;
import org.siglus.siglusapi.localmachine.webapi.RemoteActivationRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Component
public class OnlineWebClient {

  private static final String PATH_ACTIVATE_AGENT = "/server/agents";
  private final RestTemplate restTemplate;
  private final ExternalEventDtoMapper externalEventDtoMapper;

  @Value("${machine.web.url}")
  private String webBaseUrl;

  public OnlineWebClient(
      LocalTokenInterceptor localTokenInterceptor, ExternalEventDtoMapper externalEventDtoMapper) {
    this.externalEventDtoMapper = externalEventDtoMapper;
    this.restTemplate = new RestTemplate();
    configureLocalTokenInterceptor(localTokenInterceptor);
    restTemplate.setInterceptors(singletonList(localTokenInterceptor));
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
    return restTemplate.getForObject(url, PeeringEventsResponse.class).getEvents().stream()
        .map(externalEventDtoMapper::map)
        .collect(Collectors.toList());
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

  private List<ExternalEventDto> dumpEventForSync(List<Event> events) {
    return events.stream().map(externalEventDtoMapper::map).collect(Collectors.toList());
  }
}
