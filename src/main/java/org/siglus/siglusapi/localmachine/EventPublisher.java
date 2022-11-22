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

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.siglus.siglusapi.localmachine.agent.ErrorHandler;
import org.siglus.siglusapi.localmachine.agent.SyncRecordService;
import org.siglus.siglusapi.localmachine.constant.ErrorType;
import org.siglus.siglusapi.localmachine.event.masterdata.MasterDataTableChangeEvent;
import org.siglus.siglusapi.localmachine.eventstore.EventStore;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventPublisher {

  public static final int PROTOCOL_VERSION = 1;
  public static final ThreadLocal<Boolean> isReplaying = ThreadLocal.withInitial(() -> Boolean.FALSE);
  private final EventStore eventStore;
  private final ApplicationEventPublisher applicationEventPublisher;
  private final Machine machine;
  private final ErrorHandler errorHandler;
  private final SyncRecordService syncRecordService;

  public void emitGroupEvent(String groupId, UUID receiverId, Object payload) {
    Optional<UUID> lastEventIdInGroup = eventStore.getLastEventIdInGroup(groupId);
    UUID parentId = lastEventIdInGroup.orElse(null);
    Event.EventBuilder eventBuilder = baseEventBuilder(groupId, parentId, receiverId, payload);
    Event event = eventBuilder.build();
    doEmit(event);
  }

  public void emitNonGroupEvent(Object payload) {
    Event.EventBuilder eventBuilder = baseEventBuilder(null, null, null, payload);
    Event event = eventBuilder.build();
    // the only receiver the local facility itself and online web
    event.setReceiverSynced(true);
    event.setReceiverId(event.getSenderId());
    doEmit(event);
  }

  public void emitMasterDataEvent(MasterDataTableChangeEvent payload, UUID facilityId) {
    if (CollectionUtils.isNotEmpty(payload.getTableChangeEvents())) {
      MasterDataEvent.MasterDataEventBuilder eventBuilder = baseMasterDataEventBuilder(payload, facilityId);
      MasterDataEvent masterDataEvent = eventBuilder.build();
      eventStore.emit(masterDataEvent);
    }
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void publishEvent(Event event) {
    if (event.isLocalReplayed()) {
      log.info("event {} is relayed already locally, skip", event.getId());
      return;
    }
    isReplaying.set(Boolean.TRUE);
    try {
      log.info("start publish event, event id = " + event.getId());
      applicationEventPublisher.publishEvent(event.getPayload());
    } catch (Exception e) {
      log.error("failed publish event, event id = {}, ex mag = {}", event.getId(), e.getMessage());
      errorHandler.storeErrorRecord(event.getId(), e, ErrorType.REPLAY);
      throw e;
    } finally {
      isReplaying.remove();
    }
    event.setLocalReplayed(true);
    eventStore.confirmReplayed(event);
    syncRecordService.storeLastReplayRecord();
  }

  void doEmit(Event event) {
    if (isReplaying.get()) {
      throw new IllegalStateException("emit event when replaying is not allowed");
    }
    // the event is created by me, so local replayed should be true, otherwise it will be replayed
    // by me causing error
    event.setLocalReplayed(true);
    event.setSyncedTime(ZonedDateTime.now());
    boolean isOnlineWeb = machine.isOnlineWeb();
    if (isOnlineWeb) {
      event.setOnlineWebSynced(true);
    }
    boolean receiverIsUsingOnlineWeb = machine.fetchSupportedFacilityIds().contains(event.getReceiverId().toString());
    if (isOnlineWeb && receiverIsUsingOnlineWeb) {
      // current machine is online web and receiver is also using online web, so don't need to emit event.
      return;
    }
    eventStore.emit(event);
  }

  private Event.EventBuilder baseEventBuilder(String groupId, UUID parentId, UUID receiverId, Object payload) {
    return Event.builder()
        .id(UUID.randomUUID())
        .protocolVersion(PROTOCOL_VERSION)
        .occurredTime(ZonedDateTime.now())
        .senderId(machine.evalEventSenderId())
        .receiverId(receiverId)
        .groupId(groupId)
        .parentId(parentId)
        .payload(payload)
        .localReplayed(true); // marked as replayed at sender side
  }

  private MasterDataEvent.MasterDataEventBuilder baseMasterDataEventBuilder(MasterDataTableChangeEvent payload,
      UUID facilityId) {
    return MasterDataEvent.builder()
        .payload(payload)
        .facilityId(facilityId)
        .tableFullName(payload.getTableChangeEvents().get(0).getTableFullName())
        .occurredTime(ZonedDateTime.now());
  }
}
