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

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.ZonedDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
  private long groupSequenceNumber;
  private Object payload;
  private boolean onlineWebSynced;
  private boolean receiverSynced;
  private boolean localReplayed;

  @JsonIgnore
  private Ack ack;

  @JsonIgnore
  public void confirmedReceiverSynced() {
    this.setReceiverSynced(true);
    this.ack = new Ack(this.id, senderId);
  }
}
