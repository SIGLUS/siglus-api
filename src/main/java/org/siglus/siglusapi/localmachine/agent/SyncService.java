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

import static org.springframework.util.ObjectUtils.isEmpty;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.siglus.siglusapi.localmachine.Ack;
import org.siglus.siglusapi.localmachine.Event;
import org.siglus.siglusapi.localmachine.EventImporter;
import org.siglus.siglusapi.localmachine.ExternalEventDtoMapper;
import org.siglus.siglusapi.localmachine.Machine;
import org.siglus.siglusapi.localmachine.constant.ErrorType;
import org.siglus.siglusapi.localmachine.eventstore.EventStore;
import org.siglus.siglusapi.localmachine.eventstore.MasterDataOffset;
import org.siglus.siglusapi.localmachine.io.EventResourcePacker;
import org.siglus.siglusapi.service.SiglusCacheService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile({"localmachine"})
public class SyncService {

  static int PUSH_CAPACITY_BYTES_PER_REQUEST = 20 * 1024 * 1024;
  private final EventStore localEventStore;
  private final OnlineWebClient webClient;
  private final EventImporter eventImporter;
  private final ErrorHandler errorHandler;
  private final SyncRecordService syncRecordService;
  private final ExternalEventDtoMapper externalEventDtoMapper;
  private final Machine machine;
  private final SiglusCacheService siglusCacheService;

  @Transactional
  public void exchangeAcks() {
    Set<Ack> notShippedAcks = localEventStore.getNotShippedAcks();
    Set<Ack> downloadedAcks = new HashSet<>();
    try {
      downloadedAcks = webClient.exchangeAcks(notShippedAcks);
    } catch (Exception e) {
      List<UUID> eventIds = notShippedAcks.stream().map(Ack::getEventId).collect(Collectors.toList());
      errorHandler.storeErrorRecord(eventIds, e, ErrorType.EXCHANGE_DOWN);
    }
    localEventStore.confirmAckShipped(notShippedAcks);
    localEventStore.confirmEventsByAcks(downloadedAcks);
    try {
      webClient.confirmAcks(downloadedAcks);
    } catch (Exception e) {
      List<UUID> eventIds = downloadedAcks.stream().map(Ack::getEventId).collect(Collectors.toList());
      errorHandler.storeErrorRecord(eventIds, e, ErrorType.EXCHANGE_UP);
    }
  }

  @Transactional
  public void pull() {
    List<Event> events = null;
    try {
      events = Stream.of(getMasterDataEvents(), webClient.exportPeeringEvents())
          .flatMap(Collection::stream)
          .collect(Collectors.toList());
    } catch (Exception e) {
      errorHandler.storeErrorRecord(e, ErrorType.SYNC_DOWN);
    }
    if (CollectionUtils.isEmpty(events)) {
      log.info("pull events got empty");
      syncRecordService.storeLastSyncRecord();
      return;
    }
    events.forEach(it -> it.setOnlineWebSynced(true));
    eventImporter.importEvents(events);
    syncRecordService.storeLastSyncRecord();
  }

  private List<Event> getMasterDataEvents() {
    MasterDataOffset masterDataOffset = localEventStore.findMasterDataOffsetByFacilityId(machine.getFacilityId());
    if (masterDataOffset == null) {
      return Collections.emptyList();
    }
    List<Event> masterDataEvents = webClient.exportMasterDataEvents(masterDataOffset.getRecordOffset());
    if (!CollectionUtils.isEmpty(masterDataEvents)) {
      siglusCacheService.invalidateCache();
    }
    return masterDataEvents;
  }

  @Transactional
  public void push() {
    List<Event> workingQueue = new ArrayList<>();
    try {
      List<Event> events = localEventStore.getEventsForOnlineWeb();
      if (isEmpty(events)) {
        return;
      }
      pushAsEventResource(PUSH_CAPACITY_BYTES_PER_REQUEST, workingQueue, events);
    } catch (Throwable e) {
      log.error("push event failed", e);
      List<UUID> eventIds = workingQueue.stream().map(Event::getId).collect(Collectors.toList());
      errorHandler.storeErrorRecord(eventIds, e, ErrorType.SYNC_UP);
    }
  }

  private void pushAsEventResource(int sizePerRequest, List<Event> workingQueue, List<Event> events)
      throws IOException {
    EventResourcePacker eventResourcePacker =
        new EventResourcePacker(sizePerRequest, externalEventDtoMapper);
    for (Event evt : events) {
      int remainingCapacity = eventResourcePacker.writeEventAndGetRemainingCapacity(evt);
      workingQueue.add(evt);
      if (remainingCapacity > 0) {
        continue;
      }
      doPushAndReset(workingQueue, eventResourcePacker);
    }
    doPushAndReset(workingQueue, eventResourcePacker);
  }

  private void doPushAndReset(List<Event> currentEvents, EventResourcePacker eventResourcePacker) throws IOException {
    if (!currentEvents.isEmpty()) {
      webClient.sync(eventResourcePacker.toResource());
      localEventStore.confirmEventsByWeb(currentEvents);
      currentEvents.clear();
      eventResourcePacker.reset();
    }
  }
}
