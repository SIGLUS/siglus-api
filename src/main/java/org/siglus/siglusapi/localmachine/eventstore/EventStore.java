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

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.siglus.siglusapi.localmachine.Ack;
import org.siglus.siglusapi.localmachine.Event;
import org.siglus.siglusapi.localmachine.MasterDataEvent;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventStore {

  private final EventRecordRepository repository;
  private final EventPayloadRepository eventPayloadRepository;
  private final MasterDataEventRecordRepository masterDataEventRecordRepository;
  private final MasterDataOffsetRepository masterDataOffsetRepository;
  private final PayloadSerializer payloadSerializer;
  private final AckRepository ackRepository;

  @SneakyThrows
  @Transactional
  public void emit(Event event) {
    EventRecord eventRecord = EventRecord.from(event, payloadSerializer.dump(event.getPayload()));
    log.info("insert event emitted event:{}", event.getId());
    repository.insertAndAllocateLocalSequenceNumber(eventRecord);
    eventPayloadRepository.save(new EventPayload(eventRecord.getId(), eventRecord.getPayload()));
  }

  @SneakyThrows
  @Transactional
  public void emit(MasterDataEvent masterDataEvent) {
    MasterDataEventRecord masterDataEventRecord = MasterDataEventRecord
        .from(masterDataEvent, payloadSerializer.dump(masterDataEvent.getPayload()));
    log.info("insert master data event emitted event:{}", masterDataEvent.getId());
    if (masterDataEvent.getFacilityId() == null) {
      masterDataEventRecordRepository.insertMasterDataEvents(masterDataEventRecord);
    } else {
      masterDataEventRecordRepository.insertMarkFacilityIdMasterDataEvents(masterDataEventRecord);
    }
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

  public List<Event> getEventsForExport(UUID facilityId) {
    List<UUID> exportEventIds = repository.findExportEventIds(facilityId).stream()
        .map(UUID::fromString)
        .collect(Collectors.toList());
    return repository.findAll(exportEventIds).stream()
        .map(it -> it.toEvent(payloadSerializer::load))
        .collect(Collectors.toList());
  }

  public List<MasterDataEvent> getMasterDataEvents(Long offsetId, UUID facilityId) {
    if (offsetId == null) {
      return Collections.emptyList();
    }
    List<MasterDataEvent> masterDataEvents = masterDataEventRecordRepository.findByIdAfterOrderById(offsetId).stream()
        .filter(it -> filterMasterDataEventRecord(it, facilityId))
        .map(it -> it.toMasterDataEvent(payloadSerializer::load))
        .collect(Collectors.toList());
    int size = masterDataEvents.size();
    if (size > 0) {
      MasterDataEvent masterDataEvent = masterDataEvents.get(size - 1);
      offsetId = masterDataEvent.getId();
      MasterDataOffset masterDataOffset = masterDataOffsetRepository.findByFacilityIdIs(facilityId);
      if (masterDataOffset == null) {
        masterDataOffset = MasterDataOffset.builder().id(UUID.randomUUID()).build();
      }
      masterDataOffset.setFacilityId(facilityId);
      masterDataOffset.setRecordOffset(offsetId);
      log.info("save facility increment master data offset: {}", masterDataOffset);
      masterDataOffsetRepository.save(masterDataOffset);
    }
    return masterDataEvents;
  }

  private boolean filterMasterDataEventRecord(MasterDataEventRecord eventRecord, UUID facilityId) {
    return (eventRecord.getFacilityId() == null && eventRecord.getSnapshotVersion() == null)
        || (eventRecord.getFacilityId() != null && eventRecord.getFacilityId().equals(facilityId));
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
    EventRecord eventRecord = EventRecord.from(event, payloadSerializer.dump(event.getPayload()));
    repository.importExternalEvent(eventRecord);
    eventPayloadRepository.save(new EventPayload(eventRecord.getId(), eventRecord.getPayload()));
    if (isMasterDataEvent(event)) {
      log.info("update master data record offset event:{}", event.getId());
      masterDataOffsetRepository.updateRecordOffsetByFacilityId(event.getLocalSequenceNumber(), event.getReceiverId());
    }
    emitAckForEvent(event);
  }

  @Transactional
  public void emitAckForEvent(Event event) {
    if (Objects.nonNull(event.getAck())) {
      emitAck(event.getAck());
    }
  }

  public List<Event> loadSortedGroupEvents(String groupId) {
    return repository.findEventRecordByGroupId(groupId).stream()
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
  public void markAsReceived(Set<UUID> eventIds) {
    if (CollectionUtils.isEmpty(eventIds)) {
      return;
    }
    repository.markAsReceived(eventIds);
  }

  @Transactional
  public void confirmAckShipped(Set<Ack> acks) {
    Set<UUID> eventIds = acks.stream().map(Ack::getEventId).collect(Collectors.toSet());
    if (CollectionUtils.isNotEmpty(eventIds)) {
      ackRepository.markShipped(eventIds);
    }
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

  public MasterDataOffset findMasterDataOffsetByFacilityId(UUID facilityId) {
    return masterDataOffsetRepository.findByFacilityIdIs(facilityId);
  }

  private boolean isMasterDataEvent(Event event) {
    //comment 背景，目前event分为三类:
    // 1，groupid不为空的，一般用于requisition order等业务；
    // 2，groupid为空，但是senderid = receiverid,一般用于movement业务；
    // 3，groupid为空，但是senderid ！= receiverid,senderid为onlineweb端的machineid,receiverid为localmachine的facilityid
    return event.getGroupId() == null && !event.getSenderId().equals(event.getReceiverId());
  }


  public List<Ack> getAcksForEventSender(UUID eventSender) {
    Example<AckRecord> example = Example.of(AckRecord.builder().sendTo(eventSender).shipped(Boolean.FALSE).build());
    return ackRepository.findAll(example).stream().map(AckRecord::toAck).collect(Collectors.toList());
  }

  public Set<Ack> getNotShippedAcks() {
    Example<AckRecord> example = Example.of(AckRecord.builder().shipped(Boolean.FALSE).build());
    return ackRepository.findAll(example).stream().map(AckRecord::toAck).collect(Collectors.toSet());
  }

  @Transactional
  public void confirmEventsByAcks(Set<Ack> acks) {
    Set<UUID> eventIds = acks.stream().map(Ack::getEventId).collect(Collectors.toSet());
    if (CollectionUtils.isNotEmpty(eventIds)) {
      this.markAsReceived(eventIds);
    }
  }

  @Transactional
  public void routeAcks(Set<Ack> acks) {
    confirmEventsByAcks(acks);
    saveAcks(acks);
  }

  private void saveAcks(Set<Ack> acks) {
    List<AckRecord> ackRecords = acks.stream().map(AckRecord::from).collect(Collectors.toList());
    ackRepository.save(ackRecords);
  }

  private void emitAck(Ack ack) {
    AckRecord ackRecord = AckRecord.from(ack);
    ackRepository.save(ackRecord);
  }
}
