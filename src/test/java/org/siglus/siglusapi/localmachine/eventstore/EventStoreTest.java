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
import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Sets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.localmachine.Event;
import org.siglus.siglusapi.localmachine.MasterDataEvent;

@RunWith(MockitoJUnitRunner.class)
public class EventStoreTest {

  @InjectMocks
  private EventStore eventStore;
  @Mock
  private EventRecordRepository repository;
  @Mock
  private MasterDataEventRecordRepository masterDataEventRecordRepository;
  @Mock
  private MasterDataOffsetRepository masterDataOffsetRepository;
  @Mock
  private EventPayloadRepository eventPayloadRepository;
  @Mock
  private PayloadSerializer payloadSerializer;
  @Captor
  private ArgumentCaptor<MasterDataOffset> masterDataOffsetCaptor;

  private static final UUID facilityId = UUID.randomUUID();
  private static final UUID eventId1 = UUID.randomUUID();
  private static final UUID eventId2 = UUID.randomUUID();
  private static final UUID eventId3 = UUID.randomUUID();

  @Test
  public void shouldReturn0WhenGetNextGroupSeqGivenGroupNotExists() {
    given(repository.getNextGroupSequenceNumber(anyString())).willReturn(null);
    long nextGroupSeq = eventStore.nextGroupSequenceNumber("groupId");
    assertThat(nextGroupSeq).isZero();
  }

  @Test
  public void shouldSuccessWhenImportQuietlyEvent() {
    //when
    eventStore.importQuietly(buildMasterDataEvent());

    //then
    verify(payloadSerializer, times(1)).dump(any(Event.class));
    verify(repository, times(1)).importExternalEvent(any(EventRecord.class));
    verify(eventPayloadRepository, times(1)).save(any(EventPayload.class));
  }

  @Test
  public void shouldfilterExistsEventWhenImportEvents() {
    //given
    when(repository.filterExistsEventIds(Sets.newHashSet(eventId1, eventId2, eventId3)))
        .thenReturn(Sets.newHashSet(eventId1.toString(), eventId2.toString()));

    //when
    List<Event> excludeExistedEvents = eventStore.excludeExisted(buildImportEvents());

    //then
    assertEquals(1, excludeExistedEvents.size());
    assertEquals(eventId3, excludeExistedEvents.get(0).getId());
  }

  @Test
  public void shouldReturnMasterDataEventWhenLocalMachineGet() {
    // given
    when(masterDataEventRecordRepository.streamMasterDataEventRecordsByIdAfterOrderById(2L))
        .thenReturn(buildMasterDataEvents().stream());

    // when
    List<MasterDataEvent> masterDataEvents = eventStore.getMasterDataEvents(2L, facilityId);

    //then
    verify(masterDataOffsetRepository, times(1)).save(masterDataOffsetCaptor.capture());
    MasterDataOffset masterDataOffset = masterDataOffsetCaptor.getValue();
    assertEquals(5L, masterDataOffset.getRecordOffset());
    assertEquals(2, masterDataEvents.size());
  }

  @Test
  public void shouldReturnEmptyMasterDataWhenLocalMachineOffsetIsNull() {
    // when
    List<MasterDataEvent> masterDataEvents = eventStore.getMasterDataEvents(null, facilityId);

    //then
    assertEquals(Collections.emptyList(), masterDataEvents);
  }

  private List<MasterDataEventRecord> buildMasterDataEvents() {
    MasterDataEventRecord masterDataEventRecord1 = MasterDataEventRecord.builder()
        .id(3L)
        .facilityId(facilityId)
        .payload(new byte[1])
        .build();
    MasterDataEventRecord masterDataEventRecord2 = MasterDataEventRecord.builder()
        .id(4L)
        .snapshotVersion("masterdata_test.zip")
        .build();
    MasterDataEventRecord masterDataEventRecord3 = MasterDataEventRecord.builder()
        .id(5L)
        .payload(new byte[2])
        .build();
    return Arrays.asList(masterDataEventRecord1, masterDataEventRecord2, masterDataEventRecord3);
  }

  private List<Event> buildImportEvents() {
    Event event1 = Event.builder().id(eventId1).build();
    Event event2 = Event.builder().id(eventId2).build();
    Event event3 = Event.builder().id(eventId3).build();
    return Arrays.asList(event1, event2, event3);
  }

  private Event buildMasterDataEvent() {
    return Event.builder()
        .id(UUID.randomUUID())
        .senderId(UUID.randomUUID())
        .receiverId(facilityId)
        .localSequenceNumber(2)
        .build();
  }
}
