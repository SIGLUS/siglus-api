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

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.siglus.siglusapi.localmachine.eventstore.EventRecord;
import org.siglus.siglusapi.localmachine.eventstore.EventRecordRepository;
import org.siglus.siglusapi.localmachine.eventstore.PayloadSerializer;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventQueue {
  private final EventRecordRepository repository;
  private final PayloadSerializer payloadSerializer;

  @SneakyThrows
  public void put(Event event) {
    EventRecord eventRecord = EventRecord.from(event, payloadSerializer.dump(event.getPayload()));
    repository.save(eventRecord);
  }

  public long nextGroupSequenceNumber(String peeringId) {
    return Optional.ofNullable(repository.getNextGroupSequenceNumber(peeringId)).orElse(0L);
  }

  public List<Event> getEventsForOnlineWeb() {
    return repository.findEventRecordByOnlineWebConfirmedFalse().stream()
        .map(it -> it.toEvent(payloadSerializer::load))
        .collect(Collectors.toList());
  }

  public void confirmEventsByWeb(List<Event> confirmedEvents) {
    repository.updateWebConfirmedToTrueByIds(
        confirmedEvents.stream().map(Event::getId).collect(Collectors.toList()));
  }

  @Transactional(TxType.REQUIRES_NEW)
  public List<Event> putAllGetNewAdded(List<Event> events) {
    List<Event> newAdded = new LinkedList<>();
    events.forEach(
        it -> {
          try {
            repository.saveAndFlush(EventRecord.from(it, payloadSerializer.dump(it.getPayload())));
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

  public void save(Event event) {
    EventRecord eventRecord = EventRecord.from(event, payloadSerializer.dump(event.getPayload()));
    repository.save(eventRecord);
  }
}
