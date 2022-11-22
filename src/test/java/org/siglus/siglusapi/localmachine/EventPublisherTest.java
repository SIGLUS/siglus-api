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
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.siglus.siglusapi.dto.enums.EventCategoryEnum.REQUISITION_INTERNAL_APPROVED;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.assertj.core.util.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.siglus.siglusapi.localmachine.agent.ErrorHandler;
import org.siglus.siglusapi.localmachine.agent.SyncRecordService;
import org.siglus.siglusapi.localmachine.cdc.TableChangeEvent;
import org.siglus.siglusapi.localmachine.event.masterdata.MasterDataTableChangeEvent;
import org.siglus.siglusapi.localmachine.eventstore.AckRepository;
import org.siglus.siglusapi.localmachine.eventstore.EventPayloadRepository;
import org.siglus.siglusapi.localmachine.eventstore.EventRecord;
import org.siglus.siglusapi.localmachine.eventstore.EventRecordRepository;
import org.siglus.siglusapi.localmachine.eventstore.EventStore;
import org.siglus.siglusapi.localmachine.eventstore.MasterDataEventRecord;
import org.siglus.siglusapi.localmachine.eventstore.MasterDataEventRecordRepository;
import org.siglus.siglusapi.localmachine.eventstore.MasterDataOffsetRepository;
import org.siglus.siglusapi.localmachine.eventstore.PayloadSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

@SpringBootTest(
    classes = {
        EventPublisher.class,
        EventStore.class,
        PayloadSerializer.class,
    })
@RunWith(SpringRunner.class)
public class EventPublisherTest {

  private static final String GROUP_ID = "groupId";
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
  protected SyncRecordService syncRecordService;
  @Autowired
  protected EventStore eventStore;
  @MockBean
  protected MasterDataEventRecordRepository masterDataEventRecordRepository;
  @MockBean
  protected MasterDataOffsetRepository masterDataOffsetRepository;
  @MockBean
  protected ApplicationEventPublisher applicationEventPublisher;
  @Autowired
  private EventPublisher eventPublisher;
  private final UUID webFacilityId1 = UUID.randomUUID();

  @After
  public void clean() {
    EventPublisher.isReplaying.remove();
    ReflectionTestUtils.setField(eventPublisher, "eventStore", eventStore);
  }

  @Before
  public void setup() {
    given(eventRecordRepository.findLastEventIdGroupId(anyString()))
        .willReturn(Optional.of(UUID.randomUUID().toString()));
    given(machine.fetchSupportedFacilityIds()).willReturn(Collections.singleton(webFacilityId1.toString()));
    given(machine.isOnlineWeb()).willReturn(false);
  }

  @Test
  public void canEmitNonGroupEventSuccessfully() {
    // given
    TestEventPayload payload = TestEventPayload.builder().id("id").build();
    ArgumentCaptor<EventRecord> recordArgumentCaptor = ArgumentCaptor.forClass(EventRecord.class);
    UUID facilityId = getMockedFacility();
    // when
    eventPublisher.emitNonGroupEvent(payload, REQUISITION_INTERNAL_APPROVED);
    // then
    verify(eventRecordRepository)
        .insertAndAllocateLocalSequenceNumber(recordArgumentCaptor.capture());
    EventRecord eventRecord = recordArgumentCaptor.getValue();
    assertThat(eventRecord.getSenderId()).isEqualTo(facilityId);
    assertThat(eventRecord.getId()).isNotNull();
    assertThat(eventRecord.getGroupId()).isNull();
    assertThat(eventRecord.getReceiverId()).isEqualTo(facilityId);
    assertThat(eventRecord.getParentId()).isNull();
    assertThat(eventRecord.getSyncedTime()).isBeforeOrEqualTo(ZonedDateTime.now());
    assertThat(eventRecord.isLocalReplayed()).isTrue();
  }

  @Test
  public void eventParentIdShouldBeSetWhenEmitGivenOtherEventsInTheGroupExists() {
    // given
    UUID lastEventIdInTheGroup = UUID.randomUUID();
    given(eventRecordRepository.findLastEventIdGroupId(GROUP_ID))
        .willReturn(Optional.of(lastEventIdInTheGroup.toString()));
    TestEventPayload payload = TestEventPayload.builder().id("id").build();
    ArgumentCaptor<EventRecord> recordArgumentCaptor = ArgumentCaptor.forClass(EventRecord.class);
    UUID receiverId = UUID.randomUUID();
    // when
    eventPublisher.emitGroupEvent(GROUP_ID, receiverId, payload, REQUISITION_INTERNAL_APPROVED);
    // then
    verify(eventRecordRepository)
        .insertAndAllocateLocalSequenceNumber(recordArgumentCaptor.capture());
    EventRecord eventRecord = recordArgumentCaptor.getValue();
    assertThat(eventRecord.getParentId()).isEqualTo(lastEventIdInTheGroup);
  }

  @Test
  public void canEmitGroupEventSuccessfully() {
    // given
    TestEventPayload payload = TestEventPayload.builder().id("id").build();
    ArgumentCaptor<EventRecord> recordArgumentCaptor = ArgumentCaptor.forClass(EventRecord.class);
    String groupId = GROUP_ID;
    UUID receiverId = UUID.randomUUID();
    UUID facilityId = getMockedFacility();
    // when
    eventPublisher.emitGroupEvent(groupId, receiverId, payload, REQUISITION_INTERNAL_APPROVED);
    // then
    verify(eventRecordRepository)
        .insertAndAllocateLocalSequenceNumber(recordArgumentCaptor.capture());
    EventRecord eventRecord = recordArgumentCaptor.getValue();
    assertThat(eventRecord.getSenderId()).isEqualTo(facilityId);
    assertThat(eventRecord.getId()).isNotNull();
    assertThat(eventRecord.getGroupId()).isEqualTo(groupId);
    assertThat(eventRecord.getReceiverId()).isEqualTo(receiverId);
    assertThat(eventRecord.getSyncedTime()).isBeforeOrEqualTo(ZonedDateTime.now());
    assertThat(eventRecord.isLocalReplayed()).isTrue();
  }

  @Test(expected = IllegalStateException.class)
  public void shouldThrowWhenDoEmitGivenIsReplayingContext() {
    EventPublisher.isReplaying.set(Boolean.TRUE);
    eventPublisher.doEmit(Event.builder().build());
  }

  @Test
  public void shouldNotEmitEventGivenBothSenderAndReceiverIsUsingOnlineWeb() {
    // given
    EventStore eventStore = mockEventStore();
    given(machine.isOnlineWeb()).willReturn(Boolean.TRUE);
    // when
    eventPublisher.doEmit(Event.builder().senderId(UUID.randomUUID()).receiverId(webFacilityId1).build());
    // then
    verify(eventStore, times(0)).emit(any(Event.class));
  }

  @Test
  public void shouldEmitEventGivenIsOnLocalMachine() {
    // given
    EventStore eventStore = mockEventStore();
    given(machine.isOnlineWeb()).willReturn(false);
    Event event = Event.builder().senderId(UUID.randomUUID()).receiverId(webFacilityId1).build();
    // when
    eventPublisher.doEmit(event);
    // then
    verify(eventStore, times(1)).emit(event);
  }

  @Test
  public void shouldSetLocalReplayedAndSynceTimeWhenDoEmit() {
    // given
    mockEventStore();
    Event event = Event.builder().senderId(UUID.randomUUID()).receiverId(webFacilityId1).build();
    // when
    eventPublisher.doEmit(event);
    // then
    assertThat(event.getSyncedTime()).isBeforeOrEqualTo(ZonedDateTime.now());
    assertThat(event.isLocalReplayed()).isTrue();
  }

  @Test
  public void shouldSetOnlineWebSyncedWhenDoEmitWhenItIsOnWeb() {
    // given
    mockEventStore();
    Event event = Event.builder().senderId(UUID.randomUUID()).receiverId(UUID.randomUUID()).build();
    given(machine.isOnlineWeb()).willReturn(true);
    // when
    eventPublisher.doEmit(event);
    // then
    assertThat(event.isOnlineWebSynced()).isTrue();
  }

  @Test
  public void shouldNotSetOnlineWebSyncedWhenDoEmitWhenItIsNotOnWeb() {
    // given
    mockEventStore();
    Event event = Event.builder().senderId(UUID.randomUUID()).receiverId(UUID.randomUUID()).build();
    given(machine.isOnlineWeb()).willReturn(false);
    // when
    eventPublisher.doEmit(event);
    // then
    assertThat(event.isOnlineWebSynced()).isFalse();
  }

  private EventStore mockEventStore() {
    EventStore eventStore = mock(EventStore.class);
    ReflectionTestUtils.setField(eventPublisher, "eventStore", eventStore);
    return eventStore;
  }


  @Test
  public void canEmitMasterDataEventSuccessfully() {
    // given
    MasterDataTableChangeEvent event = MasterDataTableChangeEvent.builder()
        .tableChangeEvents(Lists.newArrayList(
            TableChangeEvent.builder()
                .schemaName("siglusintegration")
                .tableName("facility_extension")
                .build()))
        .build();
    ArgumentCaptor<MasterDataEventRecord> recordArgumentCaptor = ArgumentCaptor.forClass(MasterDataEventRecord.class);
    UUID facilityId = getMockedFacility();
    // when
    eventPublisher.emitMasterDataEvent(event, facilityId);
    // then
    verify(masterDataEventRecordRepository)
        .insertMarkFacilityIdMasterDataEvents(recordArgumentCaptor.capture());
    MasterDataEventRecord eventRecord = recordArgumentCaptor.getValue();
    assertThat(eventRecord.getFacilityId()).isEqualTo(facilityId);
    assertThat(eventRecord.getId()).isNotNull();
    assertThat(eventRecord.getSnapshotVersion()).isNull();
    assertThat(eventRecord.getPayload()).isNotNull();
    assertThat(eventRecord.getOccurredTime()).isNotNull();
  }

  @Test
  public void ignorePublishEventWhenLocalReplayedIsTrue() {
    // when
    eventPublisher.publishEvent(Event.builder().localReplayed(true).build());
    // then
    verify(applicationEventPublisher, times(0)).publishEvent(any());
  }

  @Test
  public void publishEventWhenLocalReplayedIsFalse() {
    // given
    TestEventPayload payload = new TestEventPayload();
    // when
    eventPublisher.publishEvent(Event.builder().payload(payload).localReplayed(false).build());
    // then
    verify(syncRecordService, times(1)).storeLastReplayRecord();
  }

  private UUID getMockedFacility() {
    UUID facilityId = UUID.randomUUID();
    given(machine.evalEventSenderId()).willReturn(facilityId);
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
