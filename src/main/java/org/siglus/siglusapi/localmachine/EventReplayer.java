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
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.siglus.siglusapi.exception.DeferredException;
import org.siglus.siglusapi.localmachine.ShedLockFactory.AutoClosableLock;
import org.siglus.siglusapi.localmachine.eventstore.EventStore;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventReplayer {

  private static final String DEFAULT_REPLAY_LOCK = "lock.replay.default";
  private static final String DEFAULT_REPLAY_GROUP_LOCK = "lock.replay.group.default.";
  private static final int TIMEOUT_MILLIS = 1000;
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
    playNonGroupEvents(nonGroupEvents);
    playGroups(groupEvents);
  }

  protected void playGroups(List<Event> groupEvents) {
    Map<String, List<Event>> eventGroups =
        groupEvents.stream().collect(groupingBy(Event::getGroupId, LinkedHashMap::new, toList()));
    DeferredException deferredExceptions = new DeferredException();
    for (String groupId : eventGroups.keySet()) {
      try {
        this.playGroup(groupId);
      } catch (Exception e) {
        log.error("fail to replay group:" + groupId, e);
        deferredExceptions.add(e);
      }
    }
    deferredExceptions.emit();
  }

  protected void playGroup(String groupId) throws InterruptedException {
    try (AutoClosableLock lock = lockFactory.waitLock(groupId, TIMEOUT_MILLIS)) {
      if (!lock.isPresent()) {
        log.warn("fail to get lock, cancel this round of replay, group:{}", groupId);
        return;
      }
      tryToPlayGroupEvents(groupId);
    }
  }

  protected void tryToPlayGroupEvents(String groupId) {
    List<Event> leafEvents = eventStore.getNotReplayedLeafEventsInGroup(groupId);
    List<UUID> leafEventIds = leafEvents.stream().map(Event::getId).collect(toList());
    if (CollectionUtils.isEmpty(leafEvents)) {
      log.info("no leaf events in group pending replay, group id:{}", groupId);
      return;
    }
    log.info("start to play group:{}, leaf event ids:{}", groupId, leafEventIds);
    for (Event event : leafEvents) {
      playGroupEvent(event);
    }
  }

  private void playGroupEvent(Event current) {
    if (Objects.isNull(current) || current.isLocalReplayed()) {
      return;
    }
    UUID parentId = current.getParentId();
    boolean canReplayCurrentEvent;
    if (Objects.nonNull(parentId)) {
      Optional<Event> parentEvent = eventStore.findEvent(parentId);
      playGroupEvent(parentEvent.orElse(null));
      canReplayCurrentEvent = parentEvent.map(Event::isLocalReplayed).orElse(false);
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
    try (AutoClosableLock waitLock = lockFactory.waitLock(lockId, TIMEOUT_MILLIS)) {
      if (!waitLock.isPresent()) {
        log.info("fail to get lock {} to replay event {}", lockId, current.getId());
      } else {
        eventPublisher.publishEvent(current);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  void playNonGroupEvents(List<Event> events) throws TimeoutException, InterruptedException {
    if (isEmpty(events)) {
      return;
    }
    final List<Event> eventsForReplaying = sortEventsByLocalSequence(events);
    try (AutoClosableLock lock = lockFactory.waitLock(DEFAULT_REPLAY_LOCK, TIMEOUT_MILLIS)) {
      lock.ifPresent(() -> eventsForReplaying.forEach(eventPublisher::publishEvent));
      if (!lock.isPresent()) {
        throw new TimeoutException();
      }
    }
  }

  private List<Event> sortEventsByLocalSequence(List<Event> events) {
    return events.stream()
        .sorted(
            Comparator.comparing(Event::getSyncedTime)
                .thenComparingLong(Event::getLocalSequenceNumber))
        .collect(toList());
  }
}
