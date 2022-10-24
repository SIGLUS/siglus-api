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
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.siglus.siglusapi.dto.Message;
import org.siglus.siglusapi.exception.UnableGetLockException;
import org.siglus.siglusapi.localmachine.Ack;
import org.siglus.siglusapi.localmachine.Event;
import org.siglus.siglusapi.localmachine.EventImporter;
import org.siglus.siglusapi.localmachine.ExternalEventDto;
import org.siglus.siglusapi.localmachine.ExternalEventDtoMapper;
import org.siglus.siglusapi.localmachine.Machine;
import org.siglus.siglusapi.localmachine.ShedLockFactory;
import org.siglus.siglusapi.localmachine.ShedLockFactory.AutoClosableLock;
import org.siglus.siglusapi.localmachine.auth.MachineToken;
import org.siglus.siglusapi.localmachine.eventstore.EventStore;
import org.siglus.siglusapi.localmachine.io.EventFileReader;
import org.siglus.siglusapi.localmachine.server.ActivationService;
import org.siglus.siglusapi.localmachine.server.OnlineWebService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
  private final Machine machine;
  private final EventFileReader eventFileReader;
  private final ShedLockFactory lockFactory;
  private static final String DEFAULT_RESYNC_LOCK = "lock.localmachine.resync.";

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

  @GetMapping("/getMasterDataEvents/{offsetId}")
  public EventsResponse exportMasterDataEvents(MachineToken machineToken, @PathVariable Long offsetId) {
    List<ExternalEventDto> eventForReceiver =
        eventStore.getMasterDataEvents(offsetId, machineToken.getFacilityId()).stream()
            .map(masterDataEvent ->
                externalEventDtoMapper.map(Event.from(masterDataEvent, machineToken.getFacilityId(), machine)))
            .collect(Collectors.toList());
    return EventsResponse.builder()
        .events(eventForReceiver)
        .build();
  }

  @GetMapping("/peeringEvents")
  public EventsResponse exportPeeringEvents(MachineToken machineToken) {
    List<ExternalEventDto> eventForReceiver =
        eventStore.getEventsForReceiver(machineToken.getFacilityId()).stream()
            .map(externalEventDtoMapper::map)
            .collect(Collectors.toList());
    return EventsResponse.builder()
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

  @GetMapping("/resync")
  public void resync(MachineToken machineToken, HttpServletResponse response) {
    try (AutoClosableLock waitLock = lockFactory.waitLock(DEFAULT_RESYNC_LOCK + getRuntimeMxBean(), 180000)) {
      if (!waitLock.isPresent()) {
        throw new UnableGetLockException(
            new Message("facility resync unable to get server lock," + machineToken.getFacilityId()));
      }
      waitLock.ifPresent(() -> onlineWebService.resyncData(machineToken.getFacilityId(), response));
    } catch (InterruptedException e) {
      log.error("facility: {} resync interrupt: {}", machineToken.getFacilityId(), e);
      Thread.currentThread().interrupt();
    }
  }

  @GetMapping("/resyncMasterData")
  public String resyncMasterData(MachineToken machineToken) {
    return onlineWebService.resyncMasterData(machineToken.getFacilityId());
  }

  static String getRuntimeMxBean() {
    RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
    String name = runtime.getName();
    return name.substring(0, name.indexOf('@'));
  }

}
