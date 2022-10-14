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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.siglus.siglusapi.localmachine.agent.ErrorHandler;
import org.siglus.siglusapi.localmachine.agent.LocalSyncResultsService;
import org.siglus.siglusapi.localmachine.eventstore.AckRepository;
import org.siglus.siglusapi.localmachine.eventstore.EventPayloadRepository;
import org.siglus.siglusapi.localmachine.eventstore.EventRecord;
import org.siglus.siglusapi.localmachine.eventstore.EventRecordRepository;
import org.siglus.siglusapi.localmachine.eventstore.EventStore;
import org.siglus.siglusapi.localmachine.eventstore.PayloadSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest(
    classes = {
        EventPublisher.class,
        EventStore.class,
        PayloadSerializer.class,
    })
@RunWith(SpringRunner.class)
public class EventPublisherTest {

  @MockBean
  protected EventRecordRepository eventRecordRepository;
  @MockBean
  protected EventPayloadRepository eventPayloadRepository;
  @MockBean
  protected AckRepository ackRepository;
  @MockBean
  protected Machine machine;
  @MockBean
  protected ErrorHandler errorHandler;
  @MockBean
  protected LocalSyncResultsService localSyncResultsService;
  @Autowired
  private EventPublisher eventPublisher;

  @Test
  public void canEmitNonGroupEventSuccessfully() {
    // given
    TestEventPayload payload = TestEventPayload.builder().id("id").build();
    ArgumentCaptor<EventRecord> recordArgumentCaptor = ArgumentCaptor.forClass(EventRecord.class);
    UUID facilityId = getMockedFacility();
    // when
    eventPublisher.emitNonGroupEvent(payload);
    // then
    verify(eventRecordRepository)
        .insertAndAllocateLocalSequenceNumber(recordArgumentCaptor.capture());
    EventRecord eventRecord = recordArgumentCaptor.getValue();
    assertThat(eventRecord.getSenderId()).isEqualTo(facilityId);
    assertThat(eventRecord.getId()).isNotNull();
    assertThat(eventRecord.getGroupId()).isNull();
    assertThat(eventRecord.getReceiverId()).isEqualTo(facilityId);
  }

  @Test
  public void canEmitGroupEventSuccessfully() {
    // given
    TestEventPayload payload = TestEventPayload.builder().id("id").build();
    ArgumentCaptor<EventRecord> recordArgumentCaptor = ArgumentCaptor.forClass(EventRecord.class);
    String groupId = "groupId";
    UUID receiverId = UUID.randomUUID();
    UUID facilityId = getMockedFacility();
    // when
    eventPublisher.emitGroupEvent(groupId, receiverId, payload);
    // then
    verify(eventRecordRepository)
        .insertAndAllocateLocalSequenceNumber(recordArgumentCaptor.capture());
    EventRecord eventRecord = recordArgumentCaptor.getValue();
    assertThat(eventRecord.getSenderId()).isEqualTo(facilityId);
    assertThat(eventRecord.getId()).isNotNull();
    assertThat(eventRecord.getGroupId()).isEqualTo(groupId);
    assertThat(eventRecord.getReceiverId()).isEqualTo(receiverId);
  }

  private UUID getMockedFacility() {
    UUID facilityId = UUID.randomUUID();
    given(machine.getFacilityId()).willReturn(facilityId);
    return facilityId;
  }

  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  static class TestEventPayload {

    private String id;
  }
}
