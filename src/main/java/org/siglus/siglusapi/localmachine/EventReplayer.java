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

import static ca.uhn.fhir.util.ElementUtil.isEmpty;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.stream.Collectors.toList;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventReplayer {
  private final EventPublisher eventPublisher;
  private final EventQueue eventQueue;
  private final LockProvider lockProvider;

  public void playGroupEvents(String groupId) {
    // TODO: 2022/8/18 implement heartbeat of lock
    Optional<SimpleLock> optionalLock =
        lockProvider.lock(new LockConfiguration(groupId, Instant.now().plus(6, MINUTES)));
    if (!optionalLock.isPresent()) {
      log.warn("fail to get lock");
      return;
    }
    SimpleLock lock = optionalLock.get();
    try {
      List<Event> events = eventQueue.loadSortedGroupEvents(groupId);
      for (int i = 0; i < events.size(); i++) {
        Event currentEvent = events.get(i);
        if (i != currentEvent.getGroupSequenceNumber()) {
          return;
        }
        if (currentEvent.isOnlineWebReplayed()) {
          continue;
        }
        eventPublisher.publishEvent(currentEvent);
      }
    } finally {
      lock.unlock();
    }
  }

  public void playDefaultGroupEvents(List<Event> events) {
    if (isEmpty(events)) {
      return;
    }
    events = sortEvents(events);
    events.forEach(eventPublisher::publishEvent);
  }

  private List<Event> sortEvents(List<Event> events) {
    return events.stream()
        .sorted(Comparator.comparingLong(Event::getGroupSequenceNumber))
        .collect(toList());
  }
}
