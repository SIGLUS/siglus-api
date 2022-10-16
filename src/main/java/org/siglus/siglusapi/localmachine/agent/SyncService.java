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

import com.google.common.collect.Iterators;
import com.google.common.collect.UnmodifiableIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.siglus.siglusapi.localmachine.Ack;
import org.siglus.siglusapi.localmachine.Event;
import org.siglus.siglusapi.localmachine.EventImporter;
import org.siglus.siglusapi.localmachine.constant.ErrorType;
import org.siglus.siglusapi.localmachine.eventstore.EventStore;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile({"localmachine"})
public class SyncService {

  public static final int BATCH_PUSH_LIMIT = 5;
  private final EventStore localEventStore;
  private final OnlineWebClient webClient;
  private final EventImporter eventImporter;
  private final ErrorHandler errorHandler;
  private final SyncRecordService syncRecordService;

  @Transactional
  public void exchangeAcks() {
    Set<Ack> notShippedAcks = localEventStore.getNotShippedAcks();
    Set<Ack> downloadedAcks;
    try {
      downloadedAcks = webClient.exchangeAcks(notShippedAcks);
    } catch (Exception e) {
      List<UUID> eventIds = notShippedAcks.stream().map(Ack::getEventId).collect(Collectors.toList());
      errorHandler.storeErrorRecord(eventIds, e, ErrorType.EXCHANGE_DOWN);
      throw e;
    }
    localEventStore.confirmAckShipped(notShippedAcks);
    localEventStore.confirmEventsByAcks(downloadedAcks);
    try {
      webClient.confirmAcks(downloadedAcks);
    } catch (Exception e) {
      List<UUID> eventIds = downloadedAcks.stream().map(Ack::getEventId).collect(Collectors.toList());
      errorHandler.storeErrorRecord(eventIds, e, ErrorType.EXCHANGE_UP);
      throw e;
    }
  }

  @Transactional
  public void pull() {
    List<Event> events = null;
    try {
      events = webClient.exportPeeringEvents();
    } catch (Exception e) {
      errorHandler.storeErrorRecord(e, ErrorType.SYNC_DOWN);
    }
    if (CollectionUtils.isEmpty(events)) {
      log.info("pull events got empty");
      return;
    }
    events.forEach(it -> it.setOnlineWebSynced(true));
    eventImporter.importEvents(events);
    syncRecordService.storeLastSyncRecord();
  }

  @Transactional
  public void push() {
    List<Event> readyForHandleEvents = new ArrayList<>();
    try {
      List<Event> events = localEventStore.getEventsForOnlineWeb();
      if (isEmpty(events)) {
        return;
      }
      UnmodifiableIterator<List<Event>> partitionList = Iterators.partition(events.iterator(), BATCH_PUSH_LIMIT);
      while (partitionList.hasNext()) {
        readyForHandleEvents.addAll(partitionList.next());
        webClient.sync(readyForHandleEvents);
        localEventStore.confirmEventsByWeb(readyForHandleEvents);
        readyForHandleEvents.clear();
      }
    } catch (Throwable e) {
      log.error("push event failed", e);
      List<UUID> eventIds = readyForHandleEvents.stream().map(Event::getId).collect(Collectors.toList());
      errorHandler.storeErrorRecord(eventIds, e, ErrorType.SYNC_UP);
    }
  }
}
