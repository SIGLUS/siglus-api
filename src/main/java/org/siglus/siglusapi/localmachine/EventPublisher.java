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
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.exception.NotFoundException;
import org.siglus.siglusapi.i18n.MessageKeys;
import org.siglus.siglusapi.localmachine.eventstore.EventStore;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventPublisher {
  public static final int PROTOCOL_VERSION = 1;
  private final EventStore eventStore;
  private final ApplicationEventPublisher applicationEventPublisher;
  private final SiglusAuthenticationHelper siglusAuthenticationHelper;
  private static final ThreadLocal<Boolean> isReplaying = ThreadLocal.withInitial(() -> Boolean.FALSE);

  public void emitGroupEvent(String groupId, UUID receiverId, Object payload) {
    Event.EventBuilder eventBuilder = baseEventBuilder(groupId, receiverId, payload);
    eventBuilder.groupSequenceNumber(eventStore.nextGroupSequenceNumber(groupId));
    Event event = eventBuilder.build();
    doEmit(event);
  }

  public void emitNonGroupEvent(Object payload) {
    // the only receiver is online web so don't need to set receiver id
    Event.EventBuilder eventBuilder = baseEventBuilder(null, null, payload);
    Event event = eventBuilder.build();
    doEmit(event);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void publishEvent(Event event) {
    if (event.isLocalReplayed()) {
      log.info("event {} is relayed already locally, skip", event.getId());
      return;
    }
    isReplaying.set(Boolean.TRUE);
    try {
      applicationEventPublisher.publishEvent(event.getPayload());
    } finally {
      isReplaying.remove();
    }
    event.setLocalReplayed(true);
    eventStore.confirmReplayed(event);
  }

  private void doEmit(Event event) {
    if (isReplaying.get()) {
      throw new IllegalStateException("emit event when replaying is not allowed");
    }
    eventStore.emit(event);
  }

  private Event.EventBuilder baseEventBuilder(String groupId, UUID receiverId, Object payload) {
    UserDto currentUser =
        Optional.ofNullable(siglusAuthenticationHelper.getCurrentUser())
            .orElseThrow(() -> new NotFoundException(MessageKeys.ERROR_USER_NOT_FOUND));
    return Event.builder()
        .id(UUID.randomUUID())
        .protocolVersion(PROTOCOL_VERSION)
        .occurredTime(ZonedDateTime.now())
        .senderId(currentUser.getHomeFacilityId())
        .receiverId(receiverId)
        .groupId(groupId)
        .payload(payload)
        .localReplayed(true); // marked as replayed at sender side
  }
}
