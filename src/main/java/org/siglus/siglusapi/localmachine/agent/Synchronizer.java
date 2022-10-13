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
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.apache.commons.collections.CollectionUtils;
import org.siglus.siglusapi.localmachine.Event;
import org.siglus.siglusapi.localmachine.EventImporter;
import org.siglus.siglusapi.localmachine.Machine;
import org.siglus.siglusapi.localmachine.eventstore.EventStore;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile({"localmachine"})
public class Synchronizer {
  public static final int BATCH_PUSH_LIMIT = 5;
  private final EventStore localEventStore;
  private final OnlineWebClient webClient;
  private final EventImporter eventImporter;
  private final Machine machine;

  @Scheduled(fixedRate = 60 * 1000, initialDelay = 60 * 1000)
  @SchedulerLock(name = "localmachine_synchronizer", lockAtMostFor = "PT1M")
  @Transactional
  public void scheduledSync() {
    log.info("start scheduled synchronization with online web");
    if (machine.fetchSupportedFacilityIds().isEmpty()) {
      log.info("no need to sync");
      return;
    }
    this.sync();
  }

  @Transactional
  public void sync() {
    pull();
    push();
    pullAcks();
  }

  public void pullAcks() {
    Set<UUID> eventIds = webClient.exportAcks();
    localEventStore.markAsReceived(eventIds);
    webClient.confirmAcks(eventIds);
  }

  @Transactional
  public void pull() {
    List<Event> events = webClient.exportPeeringEvents();
    if (CollectionUtils.isEmpty(events)) {
      log.info("pull events got empty");
      return;
    }
    events.forEach(it -> it.setOnlineWebSynced(true));
    eventImporter.importEvents(events);
    webClient.confirmReceived(events);
  }

  public void push() {
    try {
      List<Event> events = localEventStore.getEventsForOnlineWeb();
      if (isEmpty(events)) {
        return;
      }
      UnmodifiableIterator<List<Event>> partitionList = Iterators.partition(events.iterator(), BATCH_PUSH_LIMIT);
      while (partitionList.hasNext()) {
        List<Event> partEvents = partitionList.next();
        webClient.sync(partEvents);
        localEventStore.confirmEventsByWeb(partEvents);
      }
    } catch (Throwable e) {
      log.error("push event failed", e);
    }
  }
}
