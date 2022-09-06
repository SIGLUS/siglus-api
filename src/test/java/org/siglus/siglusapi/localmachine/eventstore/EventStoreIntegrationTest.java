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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.Test;
import org.siglus.siglusapi.localmachine.Event;
import org.siglus.siglusapi.localmachine.EventPayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class EventStoreIntegrationTest extends LocalMachineIntegrationTest {
  @Autowired private EventStore eventStore;

  @Test
  public void shouldReturnEmittedEventWhenGetEventForWebGivenEventNotSyncedYet() {
    // given
    TestEventPayload eventPayload = new TestEventPayload(UUID.randomUUID(), "test event payload");
    Event event =
        Event.builder()
            .id(UUID.randomUUID())
            .senderId(UUID.randomUUID())
            .payload(eventPayload)
            .build();
    eventStore.emit(event);
    // when
    Event eventForWeb = eventStore.getEventsForOnlineWeb().get(0);
    // then
    assertThat(eventForWeb.getId()).isEqualTo(event.getId());
    assertThat(eventForWeb.getPayload()).isEqualTo(eventPayload);
  }

  @Data
  @EventPayload
  @AllArgsConstructor
  @NoArgsConstructor
  static class TestEventPayload {
    private UUID id;
    private String name;
  }
}
