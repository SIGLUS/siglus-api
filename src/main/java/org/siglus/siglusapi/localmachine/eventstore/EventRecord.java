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

import java.util.UUID;
import java.util.function.Function;
import javax.persistence.Entity;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.siglus.common.domain.BaseEntity;
import org.siglus.siglusapi.localmachine.Event;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "events", schema = "localmachine")
@EqualsAndHashCode(callSuper = true)
public class EventRecord extends BaseEntity {
  private int protocolVersion;
  private Long localSequenceNumber;
  private long timestamp;
  private UUID senderId;
  private UUID receiverId;
  private String groupId;
  private long groupSequenceNumber;
  private byte[] payload;
  private boolean onlineWebConfirmed;
  private boolean receiverConfirmed;
  private String payloadClassName;

  public static EventRecord from(Event event, byte[] payload) {
    EventRecord eventRecord =
        EventRecord.builder()
            .protocolVersion(event.getProtocolVersion())
            .localSequenceNumber(null)
            .timestamp(event.getTimestamp())
            .senderId(event.getSenderId())
            .receiverId(event.getReceiverId())
            .groupId(event.getGroupId())
            .groupSequenceNumber(event.getGroupSequenceNumber())
            .payload(payload)
            .payloadClassName(event.getPayload().getClass().getName())
            .build();
    eventRecord.setId(event.getId());
    return eventRecord;
  }

  public Event toEvent(Function<byte[], Object> payloadMapper) {
    return Event.builder()
        .id(id)
        .protocolVersion(protocolVersion)
        .localSequenceNumber(localSequenceNumber)
        .timestamp(timestamp)
        .senderId(senderId)
        .receiverId(receiverId)
        .groupId(groupId)
        .groupSequenceNumber(groupSequenceNumber)
        .payload(payloadMapper.apply(payload))
        .onlineWebConfirmed(onlineWebConfirmed)
        .receiverConfirmed(receiverConfirmed)
        .build();
  }
}
