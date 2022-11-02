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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.SetUtils;
import org.siglus.siglusapi.localmachine.ShedLockFactory.AutoClosableLock;
import org.siglus.siglusapi.localmachine.eventstore.EventStore;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventReplayer {

  private static final String DEFAULT_REPLAY_LOCK = "lock.replay.default";
  private static final String DEFAULT_REPLAY_GROUP_LOCK = "lock.replay.group.default.";
  private final EventPublisher eventPublisher;
  private final EventStore eventStore;
  private final ShedLockFactory lockFactory;

  @SneakyThrows
  public void replay(List<Event> events) {
    if (CollectionUtils.isEmpty(events)) {
      return;
    }
    List<Event> nonGroupEvents = new LinkedList<>();
    List<Event> groupEvents = new LinkedList<>();
    events.forEach(
        it -> {
          if (Objects.isNull(it.getGroupId())) {
            nonGroupEvents.add(it);
          } else {
            groupEvents.add(it);
          }
        });
    this.playNonGroupEvents(nonGroupEvents);
    Map<String, List<Event>> eventGroups =
        groupEvents.stream().collect(groupingBy(Event::getGroupId, LinkedHashMap::new, toList()));
    eventGroups.forEach((groupId, value) -> this.playGroupEvents(groupId));
  }

  protected void playGroupEvents(String groupId) {
    try (AutoClosableLock lock = lockFactory.lock(groupId)) {
      if (!lock.isPresent()) {
        log.warn("fail to get lock, cancel this round of replay, group:{}", groupId);
        return;
      }
      tryToPlayGroupEvents(groupId);
    }
  }

  protected void tryToPlayGroupEvents(String groupId) {
    List<Event> events = eventStore.loadGroupEvents(groupId);
    Map<UUID, Event> idToEvent = new HashMap<>(events.size());
    Set<UUID> parentEventIds = new HashSet<>();
    for (Event it : events) {
      idToEvent.put(it.getId(), it);
      Optional.ofNullable(it.getParentId()).ifPresent(parentEventIds::add);
    }
    Set<UUID> leafEventIds = SetUtils.difference(idToEvent.keySet(), parentEventIds).toSet();
    log.info("start to play group:{}, leaf event ids:{}", groupId, leafEventIds);
    for (UUID leafId : leafEventIds) {
      Event leafEvent = idToEvent.get(leafId);
      playGroupEvent(leafEvent, idToEvent);
    }
  }

  private void playGroupEvent(Event current, Map<UUID, Event> idToEvent) {
    if (Objects.isNull(current) || current.isLocalReplayed()) {
      return;
    }
    UUID parentId = current.getParentId();
    boolean canReplayCurrentEvent = false;
    if (Objects.nonNull(parentId)) {
      Event parentEvent = idToEvent.getOrDefault(parentId, null);
      playGroupEvent(parentEvent, idToEvent);
      canReplayCurrentEvent = Optional.ofNullable(parentEvent).map(Event::isLocalReplayed).orElse(false);
    } else {
      // parent id is null, current event is root event
      canReplayCurrentEvent = true;
    }
    if (canReplayCurrentEvent) {
      playEventWithLock(current);
    }
  }

  private void playEventWithLock(Event current) {
    String lockId = DEFAULT_REPLAY_GROUP_LOCK + current.getReceiverId();
    try (AutoClosableLock waitLock = lockFactory
        .waitLock(lockId, 1000)) {
      if (!waitLock.isPresent()) {
        log.info("fail to get lock {} to replay event {}", lockId, current.getId());
        return;
      }
      waitLock.ifPresent(() -> eventPublisher.publishEvent(current));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  void playNonGroupEvents(List<Event> events) throws TimeoutException, InterruptedException {
    if (isEmpty(events)) {
      return;
    }
    final List<Event> eventsForReplaying = sortEventsByLocalSequence(events);
    try (AutoClosableLock lock = lockFactory.waitLock(DEFAULT_REPLAY_LOCK, 1000)) {
      lock.ifPresent(() -> eventsForReplaying.forEach(eventPublisher::publishEvent));
      if (!lock.isPresent()) {
        throw new TimeoutException();
      }
    }
  }

  private List<Event> sortEventsByLocalSequence(List<Event> events) {
    return events.stream()
        .sorted(Comparator.comparing(Event::getSyncedTime).thenComparingLong(Event::getLocalSequenceNumber))
        .collect(toList());
  }

}
