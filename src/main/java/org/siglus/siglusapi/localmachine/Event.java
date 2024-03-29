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

import static org.siglus.siglusapi.constant.FieldConstants.UNDERSCORE;
import static org.siglus.siglusapi.dto.enums.EventCategoryEnum.MASTER_DATA;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.ZonedDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.siglus.common.util.Uuid5Generator;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Event {

  private UUID id;
  private int protocolVersion;
  private long localSequenceNumber;
  private ZonedDateTime occurredTime;
  private UUID senderId;
  private UUID receiverId;
  private String groupId;
  private UUID parentId;
  private Object payload;
  private boolean onlineWebSynced;
  private boolean receiverSynced;
  private boolean localReplayed;
  private ZonedDateTime syncedTime;
  private String category;

  @JsonIgnore
  private Ack ack;

  @JsonIgnore
  public void confirmedReceiverSynced() {
    this.setReceiverSynced(true);
    this.ack = new Ack(this.id, senderId);
  }

  public static Event from(MasterDataEvent masterDataEvent, UUID facilityId, Machine machine) {
    return Event.builder()
        .id(Uuid5Generator.fromUtf8(String.valueOf(masterDataEvent.getId())))
        .localSequenceNumber(masterDataEvent.getId())
        .occurredTime(masterDataEvent.getOccurredTime())
        .senderId(machine.getMachineId())
        .receiverId(facilityId)
        .payload(masterDataEvent.getPayload())
        .category(MASTER_DATA + UNDERSCORE + masterDataEvent.getTableFullName())
        .build();
  }
}
