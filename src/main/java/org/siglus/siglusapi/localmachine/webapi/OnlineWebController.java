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

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.siglus.siglusapi.localmachine.Ack;
import org.siglus.siglusapi.localmachine.Event;
import org.siglus.siglusapi.localmachine.EventImporter;
import org.siglus.siglusapi.localmachine.ExternalEventDto;
import org.siglus.siglusapi.localmachine.ExternalEventDtoMapper;
import org.siglus.siglusapi.localmachine.auth.MachineToken;
import org.siglus.siglusapi.localmachine.eventstore.EventStore;
import org.siglus.siglusapi.localmachine.io.EventFileReader;
import org.siglus.siglusapi.localmachine.server.ActivationService;
import org.siglus.siglusapi.localmachine.server.OnlineWebService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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
  private final EventFileReader eventFileReader;

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

  @PostMapping("/eventFile")
  public void syncEventFile(@RequestParam("file") MultipartFile file) throws IOException {
    log.info("sync event file, size:{}", file.getSize());
    List<Event> events = eventFileReader.readAll(file);
    importer.importEvents(events);
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
  public AckExchange exchangeAcks(AckExchange request, MachineToken authentication) {
    eventStore.routeAcks(request.getAcks());
    List<Ack> acks = eventStore.getAcksForEventSender(authentication.getFacilityId());
    return new AckExchange(new HashSet<>(acks));
  }

  @PutMapping("/acks")
  public void confirmAcks(AckExchange request) {
    eventStore.confirmAckShipped(request.getAcks());
  }

  @GetMapping("/reSync")
  public void reSync(MachineToken machineToken, HttpServletResponse response) {
    onlineWebService.reSyncData(machineToken.getFacilityId(), response);
  }
}
