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

import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.function.Function;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.SecondaryTable;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.siglus.siglusapi.localmachine.Event;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "events", schema = "localmachine")
@SecondaryTable(name = "event_payload", schema = "localmachine", pkJoinColumns = @PrimaryKeyJoinColumn(name =
    "eventid"))
public class EventRecord {
  @Id private UUID id;
  @Column(name = "protocolversion")
  private int protocolVersion;
  @Column(name = "localsequencenumber")
  private Long localSequenceNumber;
  @Column(name = "occurredtime")
  private ZonedDateTime occurredTime;
  @Column(name = "senderid")
  private UUID senderId;
  @Column(name = "receiverid")
  private UUID receiverId;
  @Column(name = "groupid")
  private String groupId;
  @Column(name = "groupsequencenumber")
  private long groupSequenceNumber;
  @Column(name = "payload", table = "event_payload")
  private byte[] payload;
  @Column(name = "archived")
  private boolean archived;
  @Column(name = "onlinewebsynced")
  private boolean onlineWebSynced;
  @Column(name = "receiversynced")
  private boolean receiverSynced;
  @Column(name = "localreplayed")
  private boolean localReplayed;
  @Column(name = "syncedtime")
  private ZonedDateTime syncedTime;

  public static EventRecord from(Event event, byte[] payload) {
    EventRecord eventRecord =
        EventRecord.builder()
            .id(event.getId())
            .protocolVersion(event.getProtocolVersion())
            .localSequenceNumber(event.getLocalSequenceNumber())
            .occurredTime(event.getOccurredTime())
            .senderId(event.getSenderId())
            .receiverId(event.getReceiverId())
            .groupId(event.getGroupId())
            .groupSequenceNumber(event.getGroupSequenceNumber())
            .payload(payload)
            .receiverSynced(event.isReceiverSynced())
            .onlineWebSynced(event.isOnlineWebSynced())
            .localReplayed(event.isLocalReplayed())
            .syncedTime(event.getSyncedTime())
            .build();
    eventRecord.setId(event.getId());
    return eventRecord;
  }

  public Event toEvent(Function<byte[], Object> payloadMapper) {
    return Event.builder()
        .id(id)
        .protocolVersion(protocolVersion)
        .localSequenceNumber(localSequenceNumber)
        .occurredTime(occurredTime)
        .senderId(senderId)
        .receiverId(receiverId)
        .groupId(groupId)
        .groupSequenceNumber(groupSequenceNumber)
        .payload(payloadMapper.apply(payload))
        .onlineWebSynced(onlineWebSynced)
        .receiverSynced(receiverSynced)
        .localReplayed(localReplayed)
        .syncedTime(syncedTime)
        .build();
  }
}
