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
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.siglus.siglusapi.localmachine.Event;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventStore {
  private final EventRecordRepository repository;
  private final PayloadSerializer payloadSerializer;

  @SneakyThrows
  public void emit(Event event) {
    EventRecord eventRecord = EventRecord.from(event, payloadSerializer.dump(event.getPayload()));
    log.info("insert event emitted event:{}", event.getId());
    repository.insertAndAllocateLocalSequenceNumber(eventRecord);
  }

  public long nextGroupSequenceNumber(String groupId) {
    return Optional.ofNullable(repository.getNextGroupSequenceNumber(groupId)).orElse(0L);
  }

  public List<Event> getEventsForOnlineWeb() {
    return repository.findEventRecordByOnlineWebSyncedIsFalse().stream()
        .map(it -> it.toEvent(payloadSerializer::load))
        .collect(Collectors.toList());
  }

  public List<Event> getEventsForReceiver(UUID receiverId) {
    return repository.findByReceiverId(receiverId).stream()
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
  public List<Event> importAllGetNewAdded(List<Event> events) {
    List<Event> newAdded = new LinkedList<>();
    events.forEach(
        it -> {
          try {
            log.info("insert event:{}", it.getId());
            repository.insert(EventRecord.from(it, payloadSerializer.dump(it.getPayload())));
            newAdded.add(it);
          } catch (DataIntegrityViolationException e) {
            log.info("event {} exists, skip", it.getId());
          }
        });
    return newAdded;
  }

  public List<Event> loadSortedGroupEvents(String groupId) {
    return repository.findEventRecordByGroupId(groupId).stream()
        .map(it -> it.toEvent(payloadSerializer::load))
        .sorted(Comparator.comparingLong(Event::getGroupSequenceNumber))
        .collect(Collectors.toList());
  }

  public void confirmReplayed(Event event) {
    log.info("mark event replayed:{}", event.getId());
    repository.markAsReplayed(event.getId());
  }

  @Transactional
  public void confirmReceived(UUID ackClaimerId, Set<UUID> eventIds) {
    repository.markAsReceived(ackClaimerId, eventIds);
  }
}
