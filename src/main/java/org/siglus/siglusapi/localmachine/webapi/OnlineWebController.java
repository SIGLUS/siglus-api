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

package org.siglus.siglusapi.localmachine.webapi;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.siglus.siglusapi.localmachine.EventImporter;
import org.siglus.siglusapi.localmachine.ExternalEventDto;
import org.siglus.siglusapi.localmachine.ExternalEventDtoMapper;
import org.siglus.siglusapi.localmachine.auth.MachineToken;
import org.siglus.siglusapi.localmachine.eventstore.AckRecord;
import org.siglus.siglusapi.localmachine.eventstore.EventStore;
import org.siglus.siglusapi.localmachine.server.ActivationService;
import org.siglus.siglusapi.localmachine.server.OnlineWebService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/siglusapi/localmachine/server")
public class OnlineWebController {

  private final EventImporter importer;
  private final EventStore eventStore;
  private final ActivationService activationService;
  private final ExternalEventDtoMapper externalEventDtoMapper;
  private final OnlineWebService onlineWebService;

  @PostMapping("/agents")
  public ActivationResponse activateAgent(@RequestBody @Validated RemoteActivationRequest request) {
    return activationService.activate(request);
  }

  @PostMapping("/events")
  public void syncEvents(@RequestBody @Validated SyncRequest request) {
    log.info("syncEvents, event size:{}", request.getEvents().size());
    importer.importEvents(request.getEvents().stream()
        .map(externalEventDtoMapper::map)
        .collect(Collectors.toList()));
  }

  @GetMapping("/peeringEvents")
  public PeeringEventsResponse exportPeeringEvents(MachineToken machineToken) {
    List<ExternalEventDto> eventForReceiver =
        eventStore.getEventsForReceiver(machineToken.getFacilityId()).stream()
            .map(externalEventDtoMapper::map)
            .collect(Collectors.toList());
    return PeeringEventsResponse.builder()
        .events(eventForReceiver)
        .build();
  }

  @PostMapping("/acks")
  public void confirmReceived(@RequestBody @Validated AckRequest request) {
    eventStore.confirmReceivedToOnlineWeb(request.getEventIds());
  }

  @PutMapping("/acks")
  public void confirmAcks(@RequestBody @Validated AckRequest request) {
    eventStore.confirmAckShipped(request.getEventIds());
  }

  @GetMapping("/acks")
  public AckResponse exportAcks(MachineToken authentication) {
    List<AckRecord> acks = eventStore.getAcksForEventSender(authentication.getFacilityId());
    Set<UUID> eventIds = acks.stream().map(AckRecord::getEventId).collect(Collectors.toSet());
    return new AckResponse(eventIds);
  }

  @GetMapping("/reSync")
  public void reSync(MachineToken machineToken, HttpServletResponse response) {
    onlineWebService.reSyncData(machineToken.getFacilityId(), response);
  }
}
