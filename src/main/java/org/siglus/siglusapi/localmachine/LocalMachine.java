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
import lombok.RequiredArgsConstructor;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.exception.NotFoundException;
import org.siglus.siglusapi.i18n.MessageKeys;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LocalMachine {
  private static final int protocolVersion = 1;
  private final EventQueue eventQueue;
  private final ApplicationEventPublisher eventPublisher;
  private final SiglusAuthenticationHelper siglusAuthenticationHelper;

  public List<String> getKnownFacilityIds() {
    // FIXME: 2022/8/14 owner id + peering facility ids
    return null;
  }

  public void emitGroupEvent(String groupId, String receiverId, Object payload) {
    Event.EventBuilder eventBuilder = baseEventBuilder(groupId, receiverId, payload);
    eventBuilder.groupSequenceNumber(eventQueue.nextGroupSequenceNumber(groupId));
    eventQueue.put(eventBuilder.build());
  }

  private Event.EventBuilder baseEventBuilder(String peeringId, String receiverId, Object payload) {
    UserDto currentUser =
        Optional.ofNullable(siglusAuthenticationHelper.getCurrentUser())
            .orElseThrow(() -> new NotFoundException(MessageKeys.ERROR_USER_NOT_FOUND));
    return Event.builder()
        .protocolVersion(protocolVersion)
        .timestamp(System.currentTimeMillis())
        .senderId(currentUser.getId().toString())
        .receiverId(receiverId)
        .groupId(peeringId)
        .groupSequenceNumber(0L)
        .payload(payload);
  }

  public void publishEvent(Event event) {
    eventPublisher.publishEvent(event);
    // FIXME: 2022/8/14 update watermark of sender post payload published
  }
}
