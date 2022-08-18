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

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import lombok.RequiredArgsConstructor;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.exception.NotFoundException;
import org.siglus.siglusapi.i18n.MessageKeys;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventPublisher {
  public static final int protocolVersion = 1;
  private final EventQueue eventQueue;
  private final ApplicationEventPublisher eventPublisher;
  private final SiglusAuthenticationHelper siglusAuthenticationHelper;
  private final Machine machine;

  public List<String> getKnownFacilityIds() {
    // FIXME: 2022/8/14 owner id + peering facility ids
    return null;
  }

  public void emitGroupEvent(String groupId, UUID receiverId, Object payload) {
    Event.EventBuilder eventBuilder = baseEventBuilder(groupId, receiverId, payload);
    eventBuilder.groupSequenceNumber(eventQueue.nextGroupSequenceNumber(groupId));
    eventQueue.put(eventBuilder.build());
  }

  private Event.EventBuilder baseEventBuilder(String groupId, UUID receiverId, Object payload) {
    UserDto currentUser =
        Optional.ofNullable(siglusAuthenticationHelper.getCurrentUser())
            .orElseThrow(() -> new NotFoundException(MessageKeys.ERROR_USER_NOT_FOUND));
    return Event.builder()
        .protocolVersion(protocolVersion)
        .timestamp(System.currentTimeMillis())
        .senderId(currentUser.getId())
        .receiverId(receiverId)
        .groupId(groupId)
        .groupSequenceNumber(0L)
        .payload(payload);
  }

  // publish event should be in an isolated transaction to make the replay process is transactional
  @Transactional(TxType.REQUIRES_NEW)
  public void publishEvent(Event event) {
    if ((machine.isOnlineWeb() && event.isOnlineWebReplayed())
        || (!machine.isOnlineWeb() && event.isReceiverReplayed())) {
      // TODO: 2022/8/18 check local facility id should be event receiver
      throw new IllegalStateException("Not allow to publish replayed event");
    }
    eventPublisher.publishEvent(event);
    if (machine.isOnlineWeb()) {
      event.setOnlineWebReplayed(true);
    } else {
      event.setReceiverReplayed(true);
    }
    eventQueue.save(event);
  }
}
