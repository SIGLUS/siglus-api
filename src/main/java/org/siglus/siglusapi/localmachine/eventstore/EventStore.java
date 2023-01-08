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
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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

  static final int MASTER_DATA_EVENT_BATCH_LIMIT = 1024;
  static final int PEERING_EVENT_BATCH_LIMIT = 24;
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

  public List<Event> getEventsForOnlineWeb() {
    return repository.findEventRecordByOnlineWebSyncedAndArchived(false, false).stream()
        .map(it -> it.toEvent(payloadSerializer::load))
        .collect(Collectors.toList());
  }

  public List<Event> getEventsForReceiver(UUID receiverId) {
    return repository.findEventsForReceiver(receiverId, PEERING_EVENT_BATCH_LIMIT).stream()
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

  @Transactional
  public List<MasterDataEvent> getMasterDataEvents(Long offsetId, UUID facilityId) {
    if (offsetId == null) {
      return Collections.emptyList();
    }
    updateMasterDataOffset(offsetId, facilityId);
    List<MasterDataEventRecord> bufferedMasterDataRecords = getMasterDataRecords(
        offsetId, facilityId, MASTER_DATA_EVENT_BATCH_LIMIT);
    return bufferedMasterDataRecords.stream()
        .map(it -> it.toMasterDataEvent(payloadSerializer::load))
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
    EventRecord eventRecord = EventRecord.from(event, payloadSerializer.dump(event.getPayload()));
    repository.importExternalEvent(eventRecord);
    emitAckForEvent(event);
  }

  @Transactional
  public void emitAckForEvent(Event event) {
    if (Objects.nonNull(event.getAck())) {
      emitAck(event.getAck());
    }
  }

  public List<Event> loadGroupEvents(String groupId) {
    return repository.findEventRecordByGroupId(groupId).stream()
        .map(it -> it.toEvent(payloadSerializer::load))
        .collect(Collectors.toList());
  }

  public List<Event> getNotReplayedLeafEventsInGroup(String groupId) {
    return repository.findNotReplayedLeafNodeIdsInGroup(groupId).stream()
        .map(it -> it.toEvent(payloadSerializer::load))
        .collect(Collectors.toList());
  }

  public Optional<Event> findEvent(UUID parentId) {
    return Optional.ofNullable(repository.findOne(parentId))
        .map(it -> it.toEvent(payloadSerializer::load));
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
    log.info("mark events events receiversynced:{}", eventIds);
    repository.markAsReceived(eventIds);
  }

  @Transactional
  public void confirmAckShipped(Set<Ack> acks) {
    Set<UUID> eventIds = acks.stream().map(Ack::getEventId).collect(Collectors.toSet());
    if (CollectionUtils.isNotEmpty(eventIds)) {
      log.info("mark ack shipped:{}", eventIds);
      ackRepository.markShipped(eventIds);
    }
  }

  @Transactional
  public List<Event> excludeExisted(List<Event> events) {
    if (events.isEmpty()) {
      return events;
    }
    Set<UUID> eventIds = events.stream().map(Event::getId).collect(Collectors.toSet());
    Set<UUID> existingIds =
        repository.filterExistsEventIds(eventIds).stream()
            .map(UUID::fromString)
            .collect(Collectors.toSet());
    List<Event> nonExistEvents = new LinkedList<>();
    for (Event evt : events) {
      if (existingIds.contains(evt.getId())) {
        emitAckForEvent(evt);
      } else {
        nonExistEvents.add(evt);
      }
    }
    return nonExistEvents;
  }

  public Stream<Event> streamNotReplayedEvents() {
    return repository
        .streamByLocalReplayedOrderBySyncedTime(Boolean.FALSE)
        .sequential()
        .map(it -> it.toEvent(payloadSerializer::load));
  }

  public MasterDataOffset findMasterDataOffsetByFacilityId(UUID facilityId) {
    return masterDataOffsetRepository.findByFacilityIdIs(facilityId);
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

  @Transactional
  public List<MasterDataEventRecord> getMasterDataRecords(Long offset, UUID facilityId, int limit) {
    LinkedList<MasterDataEventRecord> bufferedMasterDataRecords = new LinkedList<>();
    Spliterator<MasterDataEventRecord> spliterator =
        masterDataEventRecordRepository
            .streamMasterDataEventRecordsByIdAfterOrderById(offset)
            .sequential()
            .spliterator();
    boolean hasNext;
    do {
      hasNext =
          spliterator.tryAdvance(
              it -> {
                if (filterMasterDataEventRecord(it, facilityId)) {
                  bufferedMasterDataRecords.add(it);
                }
              });
    } while (hasNext && bufferedMasterDataRecords.size() < limit);
    return bufferedMasterDataRecords;
  }

  public long getCurrentMasterDataOffset() {
    return Optional.ofNullable(masterDataOffsetRepository.findLocalMasterDataOffset()).orElse(0L);
  }

  public void updateLocalMasterDataOffset(long newOffset) {
    masterDataOffsetRepository.updateLocalMasterDataOffset(newOffset);
  }

  public Optional<UUID> getLastEventIdInGroup(String groupId) {
    return repository.findLastEventIdGroupId(groupId).map(UUID::fromString);
  }

  boolean filterMasterDataEventRecord(MasterDataEventRecord eventRecord, UUID facilityId) {
    boolean isSharedIncrementalRecord =
        eventRecord.getFacilityId() == null && eventRecord.getSnapshotVersion() == null;
    boolean isFacilityOwnedRecord =
        eventRecord.getFacilityId() != null && eventRecord.getFacilityId().equals(facilityId);
    return isSharedIncrementalRecord || isFacilityOwnedRecord;
  }

  private void saveAcks(Set<Ack> acks) {
    List<AckRecord> ackRecords = acks.stream().map(AckRecord::from).collect(Collectors.toList());
    log.info("save acks:{}", ackRecords.stream().map(AckRecord::getEventId).collect(Collectors.toList()));
    ackRepository.save(ackRecords);
  }

  private void emitAck(Ack ack) {
    AckRecord ackRecord = AckRecord.from(ack);
    log.info("save ack for emit:{}", ackRecord.getEventId());
    ackRepository.save(ackRecord);
  }

  private void updateMasterDataOffset(Long offsetId, UUID facilityId) {
    MasterDataOffset masterDataOffset = masterDataOffsetRepository.findByFacilityIdIs(facilityId);
    if (masterDataOffset == null) {
      masterDataOffset = MasterDataOffset.builder().id(UUID.randomUUID()).build();
    }
    masterDataOffset.setFacilityId(facilityId);
    masterDataOffset.setRecordOffset(offsetId);
    log.info("save facility increment master data offset: {}", masterDataOffset);
    masterDataOffsetRepository.save(masterDataOffset);
  }
}
