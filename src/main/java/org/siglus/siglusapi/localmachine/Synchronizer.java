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

package org.siglus.siglusapi.localmachine;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.SchedulerLock;
import org.siglus.siglusapi.localmachine.webapi.SyncResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile({"localmachine"})
public class Synchronizer {
  private final EventQueue localEventQueue;
  private final EventPublisher eventPublisher;
  private final OnlineWebClient webClient;

  @Scheduled(fixedRate = 30 * 1000, initialDelay = 60 * 1000)
  @SchedulerLock(name = "localmachine_synchronizer")
  @Transactional
  public void scheduledSync() {
    // TODO: 2022/8/17 dynamic rate based on last sync status, also check if having new data to sync
    this.sync();
  }

  @Transactional
  public void sync() {
    push();
    pull();
  }

  @Transactional
  public void pull() {
    List<Event> events = webClient.fetchEvents(eventPublisher.getKnownFacilityIds());
    List<SyncResponse> acks = webClient.fetchAcks(eventPublisher.getKnownFacilityIds());
    // FIXME: 2022/8/14 relay events, send ack to web
  }

  @Transactional
  public void push() {
    List<Event> events = localEventQueue.getEventsForOnlineWeb();
    SyncResponse response = webClient.sync(events);
    List<Event> confirmedEvents =
        events.stream()
            .filter(it -> !response.getEventIdToError().containsKey(it.getId()))
            .peek(it -> it.setOnlineWebConfirmed(true))
            .collect(Collectors.toList());
    localEventQueue.confirmEventsByWeb(confirmedEvents);
    if (!response.getEventIdToError().isEmpty()) {
      this.handleError(response.getEventIdToError());
    }
  }

  private void handleError(Map<UUID, String> eventIdToError) {
    // TODO: 2022/8/17 handle error, maybe throw and show error messages when it's manual sync.
    // Anyway, should not
    //  cancel transaction.
  }
}
