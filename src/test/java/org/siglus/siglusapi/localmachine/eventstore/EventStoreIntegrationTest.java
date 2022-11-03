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
import static org.assertj.core.api.Assertions.fail;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.Before;
import org.junit.Test;
import org.siglus.siglusapi.localmachine.Event;
import org.siglus.siglusapi.localmachine.EventPayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class EventStoreIntegrationTest extends LocalMachineIntegrationTest {
  @Autowired private EventStore eventStore;
  @Autowired private EventRecordRepository eventRecordRepository;

  @Before
  public void setup() {
    eventRecordRepository.deleteAll();
  }

  @Test
  public void shouldThrowWhenImportThatAlreadyExists() {
    // given
    Event event = getEvent();
    eventStore.importQuietly(event);
    // when
    try {
      eventStore.importQuietly(event);
      fail("should not reach here");
    } catch (Exception e) {
      // then
      assertThat(e).isInstanceOf(DataIntegrityViolationException.class);
    }
  }

  @Test
  public void shouldReturnImportedEventWhenLoadGroupEvents() {
    // given
    Event event = getEvent();
    eventStore.importQuietly(event);
    // when
    Event got = eventStore.loadGroupEvents(event.getGroupId()).get(0);
    // then
    assertThat(got.getId()).isEqualTo(event.getId());
    assertThat(got.getPayload()).isEqualTo(event.getPayload());
    assertThat(got.getLocalSequenceNumber()).isEqualTo(event.getLocalSequenceNumber());
  }

  @Test
  public void shouldReturnEmittedEventWhenGetEventForWebGivenEventNotSyncedYet() {
    // given
    Event event = getEvent();
    eventStore.emit(event);
    // when
    Event eventForWeb = eventStore.getEventsForOnlineWeb().get(0);
    // then
    assertThat(eventForWeb.getId()).isEqualTo(event.getId());
    assertThat(eventForWeb.getPayload()).isEqualTo(event.getPayload());
  }

  private Event getEvent() {
    TestEventPayload eventPayload = new TestEventPayload(UUID.randomUUID(), "test event payload");
    String groupId = "group-id";
    int localSeq = 999;
    return Event.builder()
        .id(UUID.randomUUID())
        .senderId(UUID.randomUUID())
        .payload(eventPayload)
        .localSequenceNumber(localSeq)
        .groupId(groupId)
        .build();
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
