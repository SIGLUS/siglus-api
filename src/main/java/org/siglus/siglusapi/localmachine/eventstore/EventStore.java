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

package org.siglus.siglusapi.localmachine.eventstore;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.siglus.siglusapi.localmachine.Event;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventStore {
  private final EventRecordRepository repository;
  private final EventPayloadRepository eventPayloadRepository;
  private final PayloadSerializer payloadSerializer;

  @SneakyThrows
  @Transactional
  public void emit(Event event) {
    EventRecord eventRecord = EventRecord.from(event, payloadSerializer.dump(event.getPayload()));
    log.info("insert event emitted event:{}", event.getId());
    repository.insertAndAllocateLocalSequenceNumber(eventRecord);
    eventPayloadRepository.save(new EventPayload(eventRecord.getId(), eventRecord.getPayload()));
  }

  public long nextGroupSequenceNumber(String groupId) {
    return Optional.ofNullable(repository.getNextGroupSequenceNumber(groupId)).orElse(0L);
  }

  public List<Event> getEventsForOnlineWeb() {
    return repository.findEventRecordByOnlineWebSyncedAndArchived(false, false).stream()
        .map(it -> it.toEvent(payloadSerializer::load))
        .collect(Collectors.toList());
  }

  public List<Event> getEventsForReceiver(UUID receiverId) {
    return repository.findByReceiverIdAndReceiverSyncedAndArchived(
        receiverId, false, false).stream()
        .map(it -> it.toEvent(payloadSerializer::load))
        .collect(Collectors.toList());
  }

  @Transactional
  public void confirmEventsByWeb(List<Event> confirmedEvents) {
    confirmedEvents.forEach(it -> it.setOnlineWebSynced(true));
    List<UUID> eventIds = confirmedEvents.stream().map(Event::getId).collect(Collectors.toList());
    log.info("mark as online web confirmed, events:{}", eventIds);
    repository.updateOnlineWebSyncedToTrueByIds(eventIds);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void importQuietly(Event event) {
    log.info("insert event:{}", event.getId());
    // TODO: ??? ( 2022/9/30 by kourengang)
    repository.save(EventRecord.from(event, payloadSerializer.dump(event.getPayload())));
  }

  public List<Event> loadSortedGroupEvents(String groupId) {
    return repository.findEventRecordByGroupIdAndArchived(groupId, false).stream()
        .map(it -> it.toEvent(payloadSerializer::load))
        .sorted(Comparator.comparingLong(Event::getGroupSequenceNumber))
        .collect(Collectors.toList());
  }

  @Transactional
  public void confirmReplayed(Event event) {
    log.info("mark event replayed:{}", event.getId());
    repository.markAsReplayed(event.getId());
  }

  @Transactional
  public void confirmReceived(UUID ackClaimerId, Set<UUID> eventIds) {
    if (CollectionUtils.isEmpty(eventIds)) {
      return;
    }
    repository.markAsReceived(ackClaimerId, eventIds);
  }

  public List<Event> excludeExisted(List<Event> events) {
    if (events.isEmpty()) {
      return events;
    }
    Set<UUID> eventIds = events.stream().map(Event::getId).collect(Collectors.toSet());
    Set<UUID> existingIds =
        repository.filterExistsEventIds(eventIds).stream()
            .map(UUID::fromString)
            .collect(Collectors.toSet());
    return events.stream()
        .filter(it -> !existingIds.contains(it.getId()))
        .collect(Collectors.toList());
  }

  public List<Event> findNotReplayedEvents() {
    return repository.findEventRecordByLocalReplayed(false).stream()
        .map(it -> it.toEvent(payloadSerializer::load))
        .collect(Collectors.toList());
  }
}
