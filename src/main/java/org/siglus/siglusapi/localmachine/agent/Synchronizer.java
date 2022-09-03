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

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.SchedulerLock;
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
  private final EventStore localEventStore;
  private final OnlineWebClient webClient;
  private final EventImporter eventImporter;
  private final Machine machine;

  @Scheduled(fixedRate = 60 * 1000, initialDelay = 60 * 1000)
  @SchedulerLock(name = "localmachine_synchronizer")
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
    push();
    pull();
    // TODO: 2022/8/26 report local replay info to web for Ops
  }

  @Transactional
  public void pull() {
    List<Event> events = webClient.exportPeeringEvents();
    eventImporter.importEvents(events);
    webClient.confirmReceived(events);
  }

  @Transactional
  public void push() {
    List<Event> events = localEventStore.getEventsForOnlineWeb();
    if (isEmpty(events)) {
      return;
    }
    webClient.sync(events);
    localEventStore.confirmEventsByWeb(events);
  }
}
