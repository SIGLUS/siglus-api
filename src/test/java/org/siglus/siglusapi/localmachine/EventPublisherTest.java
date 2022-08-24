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
import org.mockito.ArgumentCaptor;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.localmachine.eventstore.EventRecord;
import org.siglus.siglusapi.localmachine.eventstore.EventRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

public class EventPublisherTest extends LocalMachineIntegrationTest {
  @MockBean protected EventRecordRepository eventRecordRepository;
  @Autowired private EventPublisher eventPublisher;

  @Test
  public void canEmitNonGroupEventSuccessfully() {
    // given
    TestEventPayload payload = TestEventPayload.builder().id("id").build();
    ArgumentCaptor<EventRecord> recordArgumentCaptor = ArgumentCaptor.forClass(EventRecord.class);
    UserDto mockedCurrentUser = getMockedCurrentUser();
    // when
    eventPublisher.emitNonGroupEvent(payload);
    // then
    verify(eventRecordRepository)
        .insertAndAllocateLocalSequenceNumber(recordArgumentCaptor.capture());
    EventRecord eventRecord = recordArgumentCaptor.getValue();
    assertThat(eventRecord.getSenderId()).isEqualTo(mockedCurrentUser.getHomeFacilityId());
    assertThat(eventRecord.getId()).isNotNull();
    assertThat(eventRecord.getGroupId()).isNull();
    assertThat(eventRecord.getReceiverId()).isNull();
  }

  @Test
  public void canEmitGroupEventSuccessfully() {
    // given
    TestEventPayload payload = TestEventPayload.builder().id("id").build();
    ArgumentCaptor<EventRecord> recordArgumentCaptor = ArgumentCaptor.forClass(EventRecord.class);
    String groupId = "groupId";
    UUID receiverId = UUID.randomUUID();
    UserDto mockedCurrentUser = getMockedCurrentUser();
    // when
    eventPublisher.emitGroupEvent(groupId, receiverId, payload);
    // then
    verify(eventRecordRepository)
        .insertAndAllocateLocalSequenceNumber(recordArgumentCaptor.capture());
    EventRecord eventRecord = recordArgumentCaptor.getValue();
    assertThat(eventRecord.getSenderId()).isEqualTo(mockedCurrentUser.getHomeFacilityId());
    assertThat(eventRecord.getId()).isNotNull();
    assertThat(eventRecord.getGroupId()).isEqualTo(groupId);
    assertThat(eventRecord.getReceiverId()).isEqualTo(receiverId);
  }

  private UserDto getMockedCurrentUser() {
    UserDto mockedCurrentUser = new UserDto();
    mockedCurrentUser.setId(UUID.randomUUID());
    mockedCurrentUser.setHomeFacilityId(UUID.randomUUID());
    given(authenticationHelper.getCurrentUser()).willReturn(mockedCurrentUser);
    return mockedCurrentUser;
  }

  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  static class TestEventPayload {
    private String id;
  }
}
