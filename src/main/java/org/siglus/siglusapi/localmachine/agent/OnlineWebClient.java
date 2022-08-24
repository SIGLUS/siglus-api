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
import static java.util.stream.Collectors.toSet;

import java.net.URI;
import java.util.List;
import org.siglus.siglusapi.localmachine.Event;
import org.siglus.siglusapi.localmachine.auth.LocalTokenInterceptor;
import org.siglus.siglusapi.localmachine.webapi.AckRequest;
import org.siglus.siglusapi.localmachine.webapi.PeeringEventsResponse;
import org.siglus.siglusapi.localmachine.webapi.SyncRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class OnlineWebClient {
  private final RestTemplate restTemplate;

  @Value("${machine.web.url}")
  private String webBaseUrl;

  public OnlineWebClient(LocalTokenInterceptor localTokenInterceptor) {
    this.restTemplate = new RestTemplate();
    restTemplate.setInterceptors(singletonList(localTokenInterceptor));
  }

  public void sync(List<Event> events) {
    URI url = URI.create(webBaseUrl + "/events");
    restTemplate.postForEntity(url, new SyncRequest(events), Void.class);
  }

  public List<Event> exportPeeringEvents() {
    URI url = URI.create(webBaseUrl + "/peeringEvents");
    return restTemplate.getForObject(url, PeeringEventsResponse.class).getEvents();
  }

  public void confirmReceived(List<Event> events) {
    URI url = URI.create(webBaseUrl + "/ack");
    AckRequest ackRequest = new AckRequest(events.stream().map(Event::getId).collect(toSet()));
    restTemplate.postForEntity(url, ackRequest, Void.class);
  }
}