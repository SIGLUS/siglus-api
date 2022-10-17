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

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isEmpty;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.siglus.siglusapi.localmachine.ShedLockFactory.AutoClosableLock;
import org.siglus.siglusapi.localmachine.eventstore.EventStore;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventReplayer {

  private static final String DEFAULT_REPLAY_LOCK = "lock.replay.default";
  private final EventPublisher eventPublisher;
  private final EventStore eventStore;
  private final ShedLockFactory lockFactory;

  public void replay(List<Event> events) {
    if (CollectionUtils.isEmpty(events)) {
      return;
    }
    // replay event one by one in order. if the event is a group event (e.g. event2 below), should
    // check dependency first
    // |-------|
    // |event 1|
    // |event 2|--->[group event M, group event M-1,..., group event 0] (dependent events are ready)
    // |event 3|
    // |event 4|--->[group event M, group event M-1] (dependent events are not ready)
    // |.......|
    // |event N|
    List<Event> defaultGroup = new LinkedList<>();
    List<Event> groupEvents = new LinkedList<>();
    events.forEach(
        it -> {
          if (Objects.isNull(it.getGroupId())) {
            defaultGroup.add(it);
          } else {
            groupEvents.add(it);
          }
        });
    this.playDefaultGroupEvents(defaultGroup);
    Map<String, List<Event>> eventGroups =
        groupEvents.stream().collect(groupingBy(Event::getGroupId, LinkedHashMap::new, toList()));
    eventGroups.forEach((groupId, value) -> this.playGroupEvents(groupId));
  }

  protected void playGroupEvents(String groupId) {
    try (AutoClosableLock lock = lockFactory.lock(groupId)) {
      if (!lock.isPresent()) {
        log.warn("fail to get lock, cancel this round of replay");
        return;
      }
      doPlayGroupEvents(groupId);
    }
  }

  protected void doPlayGroupEvents(String groupId) {
    List<Event> events = eventStore.loadSortedGroupEvents(groupId);
    for (int i = 0; i < events.size(); i++) {
      Event currentEvent = events.get(i);
      if (i != currentEvent.getGroupSequenceNumber()) {
        return;
      }
      eventPublisher.publishEvent(currentEvent);
    }
  }

  private void playDefaultGroupEvents(List<Event> events) {
    if (isEmpty(events)) {
      return;
    }
    final List<Event> eventsForReplaying = sortEventsByLocalSequence(events);
    try (AutoClosableLock lock = lockFactory.lock(DEFAULT_REPLAY_LOCK)) {
      lock.ifPresent(() -> eventsForReplaying.forEach(eventPublisher::publishEvent));
    }
  }

  private List<Event> sortEventsByLocalSequence(List<Event> events) {
    return events.stream()
        .sorted(Comparator.comparing(Event::getSyncedTime).thenComparingLong(Event::getLocalSequenceNumber))
        .collect(toList());
  }
}
