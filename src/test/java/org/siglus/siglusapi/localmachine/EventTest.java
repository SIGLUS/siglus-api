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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.Test;

public class EventTest {
  @Test
  public void shouldNotSendAckGivenReceiverNotSynced() {
    // given
    UUID senderId = UUID.randomUUID();
    Event event =
        Event.builder()
            .senderId(senderId)
            .receiverId(UUID.randomUUID())
            .receiverSynced(false)
            .build();
    // then
    assertThat(event.shouldSendAck()).isFalse();
  }

  @Test
  public void shouldNotSendAckGivenNotPeeringEvent() {
    // given
    UUID senderId = UUID.randomUUID();
    Event event = Event.builder().senderId(senderId).receiverId(senderId).receiverSynced(true).build();
    // then
    assertThat(event.shouldSendAck()).isFalse();
  }

  @Test
  public void shouldSendAckGivenPeeringEventAndReceiverSynced() {
    // given
    Event event =
        Event.builder()
            .senderId(UUID.randomUUID())
            .receiverId(UUID.randomUUID())
            .receiverSynced(true)
            .build();
    // then
    assertThat(event.shouldSendAck()).isTrue();
  }
}
